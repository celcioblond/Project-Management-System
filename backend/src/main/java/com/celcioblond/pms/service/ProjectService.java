package com.celcioblond.pms.service;

import com.celcioblond.pms.dto.ProjectCreate;
import com.celcioblond.pms.dto.ProjectResponse;
import com.celcioblond.pms.dto.ProjectUpdate;
import com.celcioblond.pms.dto.TaskCreate;
import com.celcioblond.pms.exception.ResourceNotFoundException;
import com.celcioblond.pms.mapper.ResponseMapper;
import com.celcioblond.pms.model.Project;
import com.celcioblond.pms.model.ProjectComment;
import com.celcioblond.pms.model.Task;
import com.celcioblond.pms.model.User;
import com.celcioblond.pms.repository.ProjectRepository;
import com.celcioblond.pms.repository.TaskRepository;
import com.celcioblond.pms.repository.UserRepository;
import com.celcioblond.pms.security.AccessPolicy;
import com.celcioblond.pms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    /** Used when a project is created without a status; matches the values offered by the SPA. */
    static final String DEFAULT_STATUS = "NOT_STARTED";

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ChatClient chatClient;
    private final AiImageGeneratorService aiImageGeneratorService;
    private final VectorStoreService vectorStoreService;
    private final AccessPolicy accessPolicy;

    // ─── CRUD ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjectResponses() {
        return toResponses(projectRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id, UserPrincipal currentUser) {
        accessPolicy.requireProjectAccess(id, currentUser);
        return ResponseMapper.toProjectResponse(findProject(id));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsForUser(UserPrincipal currentUser) {
        User user = findUser(currentUser.getId());
        return toResponses(projectRepository.findByAssignedEmployee(user));
    }

    @Transactional
    public ProjectResponse addProject(ProjectCreate projectCreate, UserPrincipal currentUser) {
        User admin = findUser(currentUser.getId());

        Project project = new Project();
        project.setName(projectCreate.name());
        project.setType(projectCreate.type());
        project.setDescription(projectCreate.description());
        project.setStatus(projectCreate.status() != null && !projectCreate.status().isBlank()
                ? projectCreate.status() : DEFAULT_STATUS);
        project.setStartDate(projectCreate.startDate());
        project.setEndDate(projectCreate.endDate());
        project.setCreatedAt(LocalDateTime.now());
        project.setProjectDiagram(projectCreate.projectDiagram());
        project.setCreatedByAdmin(admin);

        if (projectCreate.assignedEmployeeIds() != null && !projectCreate.assignedEmployeeIds().isEmpty()) {
            project.setAssignedEmployees(userRepository.findAllById(projectCreate.assignedEmployeeIds()));
        }

        Project savedProject = projectRepository.save(project);

        if (projectCreate.tasks() != null) {
            for (TaskCreate taskCreate : projectCreate.tasks()) {
                createTaskForProject(savedProject, taskCreate, admin);
            }
        }

        if (projectCreate.comments() != null) {
            for (String commentContent : projectCreate.comments()) {
                ProjectComment comment = new ProjectComment();
                comment.setContent(commentContent);
                comment.setProject(savedProject);
                comment.setAuthor(admin);
                comment.setCreatedAt(LocalDateTime.now());
                savedProject.getComments().add(comment);
            }
        }

        // Save again to persist tasks/comments so relationships are hydrated
        Project finalProject = projectRepository.save(savedProject);
        vectorStoreService.upsertProject(finalProject);

        return ResponseMapper.toProjectResponse(finalProject);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectUpdate projectUpdate, UserPrincipal currentUser) {
        Project project = findProject(id);

        if (projectUpdate.name() != null) project.setName(projectUpdate.name());
        if (projectUpdate.description() != null) project.setDescription(projectUpdate.description());
        if (projectUpdate.type() != null) project.setType(projectUpdate.type());
        if (projectUpdate.status() != null) project.setStatus(projectUpdate.status());
        if (projectUpdate.startDate() != null) project.setStartDate(projectUpdate.startDate());
        if (projectUpdate.endDate() != null) project.setEndDate(projectUpdate.endDate());

        if (projectUpdate.assignedEmployeeIds() != null && !projectUpdate.assignedEmployeeIds().isEmpty()) {
            project.setAssignedEmployees(userRepository.findAllById(projectUpdate.assignedEmployeeIds()));
        }

        if (projectUpdate.newTasks() != null && !projectUpdate.newTasks().isEmpty()) {
            User admin = findUser(currentUser.getId());
            for (TaskCreate taskCreate : projectUpdate.newTasks()) {
                createTaskForProject(project, taskCreate, admin);
            }
        }

        if (projectUpdate.projectDiagram() != null) {
            project.setProjectDiagram(projectUpdate.projectDiagram());
        }

        Project updatedProject = projectRepository.save(project);
        vectorStoreService.upsertProject(updatedProject);

        return ResponseMapper.toProjectResponse(updatedProject);
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = findProject(id);
        vectorStoreService.deleteProject(id);
        projectRepository.delete(project);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private Project findProject(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void createTaskForProject(Project project, TaskCreate taskCreate, User assignedByAdmin) {
        Task task = new Task();
        task.setTitle(taskCreate.title());
        task.setDescription(taskCreate.description());
        task.setStatus(taskCreate.status());
        task.setPriority(taskCreate.priority());
        task.setDueDate(taskCreate.dueDate());
        task.setProject(project);
        task.setCreatedAt(LocalDateTime.now());
        task.setAssignedByAdmin(assignedByAdmin);

        if (taskCreate.assignedEmployeeIds() != null && !taskCreate.assignedEmployeeIds().isEmpty()) {
            task.setAssignedEmployees(userRepository.findAllById(taskCreate.assignedEmployeeIds()));
        }

        project.getTasks().add(taskRepository.save(task));
    }

    private static List<ProjectResponse> toResponses(List<Project> projects) {
        return projects.stream().map(ResponseMapper::toProjectResponse).toList();
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