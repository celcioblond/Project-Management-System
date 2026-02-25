package com.example.Project_Management.service;


import com.example.Project_Management.model.*;
import com.example.Project_Management.model.dto.*;
import com.example.Project_Management.repo.ProjectRepo;
import com.example.Project_Management.repo.TaskRepo;
import com.example.Project_Management.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired private ProjectRepo projectRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private TaskRepo taskRepo;

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
        project.setDescription(projectCreate.description());
        project.setStatus("Priority");
        project.setStartDate(projectCreate.startDate());
        project.setEndDate(projectCreate.endDate());
        project.setCreatedAt(LocalDateTime.now());

        // Set multiple assigned employees
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

        return convertToFullResponse(projectRepo.save(savedProject));
    }

    public ProjectResponse updateProject(Long id, ProjectUpdate projectUpdate) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        if (projectUpdate.name() != null) project.setName(projectUpdate.name());
        if (projectUpdate.description() != null) project.setDescription(projectUpdate.description());
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

        return convertToFullResponse(projectRepo.save(project));
    }

    public void deleteProject(Long id) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        projectRepo.delete(project);
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

                // collect assigned employee names
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
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                employeeNames,
                project.getCreatedByAdmin() != null ? project.getCreatedByAdmin().getName() : null,
                taskResponses,
                projectCommentResponses,
                project.getCreatedAt()
        );
    }

    public List<ProjectResponse> getProjectsByUsername(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return projectRepo.findByAssignedEmployee(user).stream()
                .map(this::convertToFullResponse)
                .collect(Collectors.toList());
    }
}
