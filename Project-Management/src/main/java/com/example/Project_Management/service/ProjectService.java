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

    @Autowired
    private ProjectRepo projectRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private TaskRepo taskRepo;

    public List<ProjectResponse> getAllProjectResponses() {

        List<Project> projects = projectRepo.findAll();
        List<ProjectResponse> projectResponses = new ArrayList<>();

        for(Project project: projects) {

            List<TaskResponse> taskResponses = new ArrayList<>();

            for(Task task: project.getTasks()) {

                List<TaskCommentResponse> taskCommentResponses = new ArrayList<>();

                for(TaskComment comment: task.getComments()){
                    TaskCommentResponse commentResponse = new TaskCommentResponse(
                            comment.getId(),
                            comment.getContent(),
                            comment.getAuthor().getName(),
                            comment.getCreatedAt(),
                            comment.getUpdatedAt()

                    );
                    taskCommentResponses.add(commentResponse);
                }

                TaskResponse taskResponse = new TaskResponse(
                        task.getId(),
                        task.getTitle(),
                        task.getDescription(),
                        task.getPriority(),
                        task.getStatus(),
                        task.getDueDate(),
                        task.getProject() != null ? task.getProject().getName() : null,
                        task.getAssignedEmployee() != null ? task.getAssignedEmployee().getName() : null,
                        task.getAssignedByAdmin() != null ? task.getAssignedByAdmin().getName() : null,
                        taskCommentResponses,
                        task.getCreatedAt()
                );
                taskResponses.add(taskResponse);
            }

            List<ProjectCommentResponse> projCommentsResponses = new ArrayList<>();

            for(ProjectComment comment: project.getComments()){
                ProjectCommentResponse commentResponse = new ProjectCommentResponse(
                        comment.getId(),
                        comment.getContent(),
                        comment.getAuthor().getName(),
                        comment.getCreatedAt(),
                        comment.getUpdatedAt()
                );
                projCommentsResponses.add(commentResponse);
            }

            ProjectResponse projectResponse = new ProjectResponse(
                    project.getName(),
                    project.getDescription(),
                    project.getStatus(),
                    project.getStartDate(),
                    project.getEndDate(),
                    project.getAssignedEmployee().getName(),
                    project.getCreatedByAdmin().getName(),
                    taskResponses,
                    projCommentsResponses,
                    project.getCreatedAt()
            );
            projectResponses.add(projectResponse);
        }


        return projectResponses;

    }

    public ProjectResponse getProjectById(Long id){
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

        if(projectCreate.assignedEmployeeId() != null) {
            User assignedEmployee = userRepo.findById(projectCreate.assignedEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found with id: " + projectCreate.assignedEmployeeId()));
            project.setAssignedEmployee(assignedEmployee);
        }

        User createdByAdmin = userRepo.findById(projectCreate.createdByAdminId())
                .orElseThrow(() -> new RuntimeException("Admin not found with id: " + projectCreate.createdByAdminId()));
        project.setCreatedByAdmin(createdByAdmin);

        project.setCreatedAt(LocalDateTime.now());
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

        Project finalProject = projectRepo.save(savedProject);

        return convertToFullResponse(finalProject);
    }

    public ProjectResponse updateProject(Long id, ProjectUpdate projectUpdate) {
        // Find existing project
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        // Update simple fields (only if provided)
        if (projectUpdate.name() != null) {
            project.setName(projectUpdate.name());
        }
        if (projectUpdate.description() != null) {
            project.setDescription(projectUpdate.description());
        }
        if (projectUpdate.status() != null) {
            project.setStatus(projectUpdate.status());
        }
        if (projectUpdate.startDate() != null) {
            project.setStartDate(projectUpdate.startDate());
        }
        if (projectUpdate.endDate() != null) {
            project.setEndDate(projectUpdate.endDate());
        }

        // Update assigned employee
        if (projectUpdate.assignedEmployeeId() != null) {
            User assignedEmployee = userRepo.findById(projectUpdate.assignedEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found with id: " + projectUpdate.assignedEmployeeId()));
            project.setAssignedEmployee(assignedEmployee);
        }

        // Get admin who updated
        User updatedByAdmin = null;
        if (projectUpdate.updatedByAdminId() != null) {
            updatedByAdmin = userRepo.findById(projectUpdate.updatedByAdminId())
                    .orElseThrow(() -> new RuntimeException("Admin not found with id: " + projectUpdate.updatedByAdminId()));
        }

        // Add new tasks if provided
        if (projectUpdate.newTasks() != null && !projectUpdate.newTasks().isEmpty()) {
            for (TaskCreate taskCreate : projectUpdate.newTasks()) {
                createTaskForProject(project, taskCreate, updatedByAdmin);
            }
        }

        // Save updated project
        Project updatedProject = projectRepo.save(project);

        return convertToFullResponse(updatedProject);
    }

    public void deleteProject(Long id) {
        Project project = projectRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        projectRepo.delete(project);
        // Tasks and comments will be deleted automatically due to cascade
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

        // Set assigned employee if provided
        if (taskCreate.assignedEmployeeId() != null) {
            User taskEmployee = userRepo.findById(taskCreate.assignedEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Task employee not found"));
            task.setAssignedEmployee(taskEmployee);
        }

        // Set who assigned the task
        if (assignedByAdmin != null) {
            task.setAssignedByAdmin(assignedByAdmin);
        }

        taskRepo.save(task);
    }

    private ProjectResponse convertToFullResponse(Project project) {
        List<TaskResponse> taskResponses = new ArrayList<>();

        if (project.getTasks() != null) {
            for (Task task : project.getTasks()) {
                List<TaskCommentResponse> taskCommentResponses = new ArrayList<>();

                if (task.getComments() != null) {
                    for (TaskComment comment : task.getComments()) {
                        TaskCommentResponse commentResponse = new TaskCommentResponse(
                                comment.getId(),
                                comment.getContent(),
                                comment.getAuthor() != null ? comment.getAuthor().getName() : null,
                                comment.getCreatedAt(),
                                comment.getUpdatedAt()
                        );
                        taskCommentResponses.add(commentResponse);
                    }
                }

                TaskResponse taskResponse = new TaskResponse(
                        task.getId(),
                        task.getTitle(),
                        task.getDescription(),
                        task.getStatus(),
                        task.getPriority(),
                        task.getDueDate(),
                        task.getProject() != null ? task.getProject().getName() : null,
                        task.getAssignedEmployee() != null ? task.getAssignedEmployee().getName() : null,
                        task.getAssignedByAdmin() != null ? task.getAssignedByAdmin().getName() : null,
                        taskCommentResponses,
                        task.getCreatedAt()
                );
                taskResponses.add(taskResponse);
            }
        }

        List<ProjectCommentResponse> projectCommentResponses = new ArrayList<>();

        if (project.getComments() != null) {
            for (ProjectComment comment : project.getComments()) {
                ProjectCommentResponse commentResponse = new ProjectCommentResponse(
                        comment.getId(),
                        comment.getContent(),
                        comment.getAuthor() != null ? comment.getAuthor().getName() : null,
                        comment.getCreatedAt(),
                        comment.getUpdatedAt()
                );
                projectCommentResponses.add(commentResponse);
            }
        }

        return new ProjectResponse(
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                project.getAssignedEmployee() != null ? project.getAssignedEmployee().getName() : null,
                project.getCreatedByAdmin() != null ? project.getCreatedByAdmin().getName() : null,
                taskResponses,
                projectCommentResponses,
                project.getCreatedAt()
        );
    }

    public List<ProjectResponse> getProjectsByUsername(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Project> projects = projectRepo.findByAssignedEmployee(user);

        return projects.stream().map(this::convertToFullResponse).collect(Collectors.toList());
    }
}
