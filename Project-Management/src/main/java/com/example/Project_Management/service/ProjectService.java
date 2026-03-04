package com.example.Project_Management.service;


import com.example.Project_Management.model.*;
import com.example.Project_Management.model.dto.*;
import com.example.Project_Management.repo.ProjectRepo;
import com.example.Project_Management.repo.TaskRepo;
import com.example.Project_Management.repo.UserRepo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired private ProjectRepo projectRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private TaskRepo taskRepo;
    @Autowired private ChatClient chatClient;
    @Autowired private AiImageGeneratorService aiImageGeneratorService;
    @Autowired private VectorStore pgVectorStore;

    // ─── Vector Store helpers ────────────────────────────────────────────────

    /**
     * Builds a rich plain-text document from a saved Project so the RAG
     * chatbot can answer questions about projects, tasks, deadlines, and team.
     */
    private Document buildProjectDocument(Project project) {

        String assignedEmployees = project.getAssignedEmployees() != null
                ? project.getAssignedEmployees().stream()
                .map(User::getName)
                .collect(Collectors.joining(", "))
                : "None";

        String taskSummary = project.getTasks() != null && !project.getTasks().isEmpty()
                ? project.getTasks().stream()
                .map(t -> String.format("  - %s (Status: %s, Priority: %s, Due: %s, Assigned to: %s)",
                        t.getTitle(),
                        t.getStatus(),
                        t.getPriority(),
                        t.getDueDate() != null ? t.getDueDate().toLocalDate() : "No due date",
                        t.getAssignedEmployees() != null
                                ? t.getAssignedEmployees().stream().map(User::getName).collect(Collectors.joining(", "))
                                : "Unassigned"))
                .collect(Collectors.joining("\n"))
                : "  No tasks assigned yet.";

        String content = String.format("""
                Project Name: %s
                Description: %s
                Type: %s
                Status: %s
                Start Date: %s
                End Date: %s
                Created At: %s
                Created By Admin: %s
                Assigned Employees: %s
                Tasks:
                %s
                """,
                project.getName(),
                project.getDescription(),
                project.getType(),
                project.getStatus(),
                project.getStartDate() != null ? project.getStartDate().toLocalDate() : "N/A",
                project.getEndDate() != null ? project.getEndDate().toLocalDate() : "N/A",
                project.getCreatedAt() != null ? project.getCreatedAt().toLocalDate() : "N/A",
                project.getCreatedByAdmin() != null ? project.getCreatedByAdmin().getName() : "Unknown",
                assignedEmployees,
                taskSummary
        );

        return new Document(
                UUID.randomUUID().toString(),
                content,
                Map.of("projectId", String.valueOf(project.getId()))
        );
    }

    /**
     * Deletes all vector store documents that belong to a given projectId,
     * then inserts a fresh document. Used on update so data stays current.
     */
    private void upsertProjectDocument(Project project) {
        // Delete the old document(s) for this project from the vector store
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        pgVectorStore.delete(b.eq("projectId", String.valueOf(project.getId())).build());

        // Insert the fresh document
        pgVectorStore.add(List.of(buildProjectDocument(project)));
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    public List<ProjectResponse> getAllProjectResponses() {
        return projectRepo.findAll().stream()
                .map(this::convertToFullResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        return convertToFullResponse(project);
    }

    public ProjectResponse addProject(ProjectCreate projectCreate) {
        Project project = new Project();
        project.setName(projectCreate.name());
        project.setType(projectCreate.type());
        project.setDescription(projectCreate.description());
        project.setStatus("Priority");
        project.setStartDate(projectCreate.startDate());
        project.setEndDate(projectCreate.endDate());
        project.setCreatedAt(LocalDateTime.now());
        project.setProjectDiagram(projectCreate.projectDiagram());

        if (projectCreate.assignedEmployeeIds() != null && !projectCreate.assignedEmployeeIds().isEmpty()) {
            List<User> employees = userRepo.findAllById(projectCreate.assignedEmployeeIds());
            project.setAssignedEmployees(employees);
        }

        User createdByAdmin = userRepo.findById(projectCreate.createdByAdminId())
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        project.setCreatedByAdmin(createdByAdmin);

        Project savedProject = projectRepo.save(project);

        if (projectCreate.tasks() != null && !projectCreate.tasks().isEmpty()) {
            for (TaskCreate taskCreate : projectCreate.tasks()) {
                createTaskForProject(savedProject, taskCreate, createdByAdmin);
            }
        }

        if (projectCreate.comments() != null && !projectCreate.comments().isEmpty()) {
            for (String commentContent : projectCreate.comments()) {
                ProjectComment comment = new ProjectComment();
                comment.setContent(commentContent);
                comment.setProject(savedProject);
                comment.setAuthor(createdByAdmin);
                comment.setCreatedAt(LocalDateTime.now());
                savedProject.getComments().add(comment);
            }
        }

        // Save again to persist tasks/comments, then reload so relationships are hydrated
        Project finalProject = projectRepo.save(savedProject);

        // ── Embed in vector store so the chatbot can retrieve this project ──
        pgVectorStore.add(List.of(buildProjectDocument(finalProject)));

        return convertToFullResponse(finalProject);
    }

    public ProjectResponse updateProject(Long id, ProjectUpdate projectUpdate) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        if (projectUpdate.name() != null) project.setName(projectUpdate.name());
        if (projectUpdate.description() != null) project.setDescription(projectUpdate.description());
        if (projectUpdate.type() != null) project.setType(projectUpdate.type());
        if (projectUpdate.status() != null) project.setStatus(projectUpdate.status());
        if (projectUpdate.startDate() != null) project.setStartDate(projectUpdate.startDate());
        if (projectUpdate.endDate() != null) project.setEndDate(projectUpdate.endDate());

        if (projectUpdate.assignedEmployeeIds() != null && !projectUpdate.assignedEmployeeIds().isEmpty()) {
            List<User> employees = userRepo.findAllById(projectUpdate.assignedEmployeeIds());
            project.setAssignedEmployees(employees);
        }

        User updatedByAdmin = null;
        if (projectUpdate.updatedByAdminId() != null) {
            updatedByAdmin = userRepo.findById(projectUpdate.updatedByAdminId())
                    .orElseThrow(() -> new RuntimeException("Admin not found"));
        }

        if (projectUpdate.newTasks() != null && !projectUpdate.newTasks().isEmpty()) {
            for (TaskCreate taskCreate : projectUpdate.newTasks()) {
                createTaskForProject(project, taskCreate, updatedByAdmin);
            }
        }

        if (projectUpdate.projectDiagram() != null) {
            project.setProjectDiagram(projectUpdate.projectDiagram());
        }

        Project updatedProject = projectRepo.save(project);

        // ── Replace the old vector store document with fresh data ──
        upsertProjectDocument(updatedProject);

        return convertToFullResponse(updatedProject);
    }

    public void deleteProject(Long id) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        // ── Remove from vector store when project is deleted ──
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        pgVectorStore.delete(b.eq("projectId", String.valueOf(id)).build());

        projectRepo.delete(project);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void createTaskForProject(Project project, TaskCreate taskCreate, User assignedByAdmin) {
        Task task = new Task();
        task.setTitle(taskCreate.title());
        task.setDescription(taskCreate.description());
        task.setStatus(taskCreate.status());
        task.setPriority(taskCreate.priority());
        task.setDueDate(taskCreate.dueDate());
        task.setProject(project);
        task.setCreatedAt(LocalDateTime.now());

        if (taskCreate.assignedEmployeeIds() != null && !taskCreate.assignedEmployeeIds().isEmpty()) {
            List<User> employees = userRepo.findAllById(taskCreate.assignedEmployeeIds());
            task.setAssignedEmployees(employees);
        }

        if (assignedByAdmin != null) task.setAssignedByAdmin(assignedByAdmin);

        taskRepo.save(task);
    }

    private ProjectResponse convertToFullResponse(Project project) {
        List<TaskResponse> taskResponses = new ArrayList<>();

        if (project.getTasks() != null) {
            for (Task task : project.getTasks()) {
                List<TaskCommentResponse> taskCommentResponses = new ArrayList<>();
                if (task.getComments() != null) {
                    for (TaskComment comment : task.getComments()) {
                        taskCommentResponses.add(new TaskCommentResponse(
                                comment.getId(),
                                comment.getContent(),
                                comment.getAuthor() != null ? comment.getAuthor().getName() : null,
                                comment.getCreatedAt(),
                                comment.getUpdatedAt()
                        ));
                    }
                }

                List<String> employeeNames = task.getAssignedEmployees() != null
                        ? task.getAssignedEmployees().stream().map(User::getName).collect(Collectors.toList())
                        : new ArrayList<>();

                taskResponses.add(new TaskResponse(
                        task.getId(),
                        task.getTitle(),
                        task.getDescription(),
                        task.getStatus(),
                        task.getPriority(),
                        task.getDueDate(),
                        task.getProject() != null ? task.getProject().getName() : null,
                        employeeNames,
                        task.getAssignedByAdmin() != null ? task.getAssignedByAdmin().getName() : null,
                        taskCommentResponses,
                        task.getCreatedAt()
                ));
            }
        }

        List<ProjectCommentResponse> projectCommentResponses = new ArrayList<>();
        if (project.getComments() != null) {
            for (ProjectComment comment : project.getComments()) {
                projectCommentResponses.add(new ProjectCommentResponse(
                        comment.getId(),
                        comment.getContent(),
                        comment.getAuthor() != null ? comment.getAuthor().getName() : null,
                        comment.getCreatedAt(),
                        comment.getUpdatedAt()
                ));
            }
        }

        List<String> employeeNames = project.getAssignedEmployees() != null
                ? project.getAssignedEmployees().stream().map(User::getName).collect(Collectors.toList())
                : new ArrayList<>();

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getType(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                employeeNames,
                project.getCreatedByAdmin() != null ? project.getCreatedByAdmin().getName() : null,
                taskResponses,
                projectCommentResponses,
                project.getCreatedAt(),
                project.getProjectDiagram()
        );
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByUsername(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return projectRepo.findByAssignedEmployee(user).stream()
                .map(this::convertToFullResponse)
                .collect(Collectors.toList());
    }

    // ─── AI generation ───────────────────────────────────────────────────────

    public String generateProject(String name, String type) {
        String descPrompt = String.format("""
            You are generating a project description for an internal engineering project management platform.

            Project Title: %s
            Category: %s

            Instructions:
            - Write in a precise, professional engineering tone.
            - Clearly define the primary objective and core deliverables.
            - Mention key technical components, systems, or engineering considerations relevant to the category.
            - Focus on WHAT will be built or executed, not general methodology.
            - Avoid vague phrases like "ensure high quality" or "use best practices".
            - Do NOT include bullet points, headings, or quotation marks.
            - Avoid starting with "Develop a system" or "Design a system" unless necessary.
            - Maximum 220 characters.
            - Output only the description text.

            """, name, type);

        return chatClient.prompt(descPrompt)
                .call().chatResponse().getResult().getOutput().getText();
    }

    public byte[] generateImage(String title, String type, String description) {
        String imagePrompt = String.format("""
            Create a professional technical diagram for this engineering project.
            Use the correct diagram style for the engineering field detected from the project type.

            Project: %s
            Type: %s
            Description: %s

            Diagram style by field:
            - Software/IT: system architecture or UML activity diagram (components, APIs, databases, data flow)
            - Civil/Structural: CPM network or WBS diagram (project phases, dependencies, milestones)
            - Construction: workflow diagram with swim lanes (stakeholders, approvals, site activities)
            - Mechanical: PFD or system schematic (components, force/fluid flow, control loops)
            - Electrical: single-line or block diagram (power sources, loads, IEC/ANSI symbols)
            - Electronics/Embedded: block diagram (MCU, sensors, actuators, communication buses)
            - Chemical: P&ID or PFD (reactors, valves, instrumentation, material streams)
            - Industrial/Manufacturing: VSM or process flow (workstations, material flow, cycle times)
            - Environmental: process flow (treatment stages, inputs, outputs, monitoring points)
            - Aerospace: subsystem hierarchy (propulsion, avionics, thermal, power interfaces)
            - Biomedical: block diagram (sensors, signal processing, feedback loops, device interfaces)
            - Petroleum/Oil & Gas: production flow (wellbore, separators, pipelines, processing)
            - Geotechnical: investigation flowchart (soil layers, testing stages, design outputs)
            - Nuclear: reactor system diagram (cooling circuits, containment, safety systems)
            - Materials: processing flow (raw input, treatment stages, quality checkpoints)
            - Telecommunications: network topology (base stations, routers, signal paths)
            - Naval/Marine: systems diagram (propulsion, electrical, ballast, navigation)
            - Mining: operation flow (extraction, crushing, separation, waste management)
            - Agricultural/Food: process flow (inputs, processing units, QC, distribution)
            - Robotics/Automation: control block diagram (PLC, sensors, actuators, feedback)

                Requirements: white background, black lines only, no color fills or dark themes,
                clean vector-like engineering drawing style similar to AutoCAD or Visio output,
                high contrast, readable labels, professional technical blueprint appearance,
                labeled nodes and arrows, decision diamonds where needed, directional flow,
                reflect actual project scope. No decorative art or photos. Diagram only.
            """, title, type, description);

        return aiImageGeneratorService.generateImage(imagePrompt);
    }
}