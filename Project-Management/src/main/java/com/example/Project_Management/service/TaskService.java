package com.example.Project_Management.service;

import com.example.Project_Management.model.Project;
import com.example.Project_Management.model.Task;
import com.example.Project_Management.model.TaskComment;
import com.example.Project_Management.model.User;
import com.example.Project_Management.model.dto.TaskCommentResponse;
import com.example.Project_Management.model.dto.TaskCreate;
import com.example.Project_Management.model.dto.TaskResponse;
import com.example.Project_Management.model.dto.TaskUpdate;
import com.example.Project_Management.repo.ProjectRepo;
import com.example.Project_Management.repo.TaskRepo;
import com.example.Project_Management.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired private TaskRepo taskRepo;
    @Autowired private ProjectRepo projectRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private VectorStoreService vectorStoreService;

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        return taskRepo.findAll().stream().map(this::convertToTaskResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(long id) {
        return convertToTaskResponse(taskRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found id: " + id)));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProjectById(Long projectId) {
        return taskRepo.findByProjectId(projectId).stream().map(this::convertToTaskResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByEmployeeId(Long employeeId) {
        return taskRepo.findByAssignedEmployeeId(employeeId).stream().map(this::convertToTaskResponse).collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse createTask(TaskCreate taskCreate, Long assignedByAdminId) {
        Task task = new Task();
        task.setTitle(taskCreate.title());
        task.setDescription(taskCreate.description());
        task.setStatus(taskCreate.status());
        task.setPriority(taskCreate.priority());
        task.setDueDate(taskCreate.dueDate());
        task.setCreatedAt(LocalDateTime.now());

        Project project = projectRepo.findById(taskCreate.projectId())
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + taskCreate.projectId()));
        task.setProject(project);

        if (taskCreate.assignedEmployeeIds() != null && !taskCreate.assignedEmployeeIds().isEmpty()) {
            List<User> employees = userRepo.findAllById(taskCreate.assignedEmployeeIds());
            task.setAssignedEmployees(employees);
        }

        User assignedAdmin = userRepo.findById(taskCreate.assignedByAdminId())
                .orElseThrow(() -> new RuntimeException("Admin not found with id: " + taskCreate.assignedByAdminId()));
        task.setAssignedByAdmin(assignedAdmin);

        Task savedTask = taskRepo.save(task);

        // Index the new task in the vector store
        vectorStoreService.upsertTask(savedTask);

        // Re-index the parent project so its embedded task list stays current
        vectorStoreService.upsertProject(projectRepo.findById(project.getId())
                .orElse(project));

        return convertToTaskResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskUpdate taskUpdate) {
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id " + id));

        if (taskUpdate.title() != null) task.setTitle(taskUpdate.title());
        if (taskUpdate.description() != null) task.setDescription(taskUpdate.description());
        if (taskUpdate.status() != null) task.setStatus(taskUpdate.status());
        if (taskUpdate.priority() != null) task.setPriority(taskUpdate.priority());
        if (taskUpdate.dueDate() != null) task.setDueDate(taskUpdate.dueDate());

        if (taskUpdate.assignedEmployeeIds() != null && !taskUpdate.assignedEmployeeIds().isEmpty()) {
            List<User> employees = userRepo.findAllById(taskUpdate.assignedEmployeeIds());
            task.setAssignedEmployees(employees);
        }

        if (taskUpdate.updatedByAdminId() != null) {
            User updatedByAdmin = userRepo.findById(taskUpdate.updatedByAdminId())
                    .orElseThrow(() -> new RuntimeException("Admin not found with id: " + taskUpdate.updatedByAdminId()));
            task.setAssignedByAdmin(updatedByAdmin);
        }

        Task updatedTask = taskRepo.save(task);

        // Re-index the updated task
        vectorStoreService.upsertTask(updatedTask);

        // Re-index parent project so its task summary reflects changes
        if (updatedTask.getProject() != null) {
            vectorStoreService.upsertProject(projectRepo.findById(updatedTask.getProject().getId())
                    .orElse(updatedTask.getProject()));
        }

        return convertToTaskResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        // Remove from vector store before deleting from DB
        vectorStoreService.deleteTask(id);

        // Re-index parent project so the deleted task is removed from its summary
        if (task.getProject() != null) {
            Long projectId = task.getProject().getId();
            taskRepo.delete(task);
            projectRepo.findById(projectId).ifPresent(vectorStoreService::upsertProject);
        } else {
            taskRepo.delete(task);
        }
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByUsername(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return taskRepo.findByAssignedEmployeeId(user.getId()).stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    private TaskResponse convertToTaskResponse(Task task) {
        List<TaskCommentResponse> commentResponses = new ArrayList<>();
        if (task.getComments() != null) {
            for (TaskComment comment : task.getComments()) {
                commentResponses.add(new TaskCommentResponse(
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

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getProject() != null ? task.getProject().getName() : null,
                employeeNames,
                task.getAssignedByAdmin() != null ? task.getAssignedByAdmin().getName() : null,
                commentResponses,
                task.getCreatedAt()
        );
    }
}