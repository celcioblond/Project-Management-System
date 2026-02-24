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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {
    @Autowired
    private TaskRepo taskRepo;

    @Autowired
    private ProjectRepo projectRepo;

    @Autowired
    private UserRepo userRepo;

    public List<TaskResponse> getAllTasks(){
        return taskRepo.findAll().stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(long id){
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found id: " + id));
        return convertToTaskResponse(task);
    }

    public List<TaskResponse> getTasksByProjectById(Long projectId){
        return taskRepo.findByProjectId(projectId).stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getTasksByEmployeeId(Long emloyeeId){
        return taskRepo.findByAssignedEmployeeId(emloyeeId).stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse createTask(TaskCreate taskCreate, Long assignedByAdminId){
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

        User assignedEmployee = userRepo.findById(taskCreate.assignedEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + taskCreate.assignedEmployeeId()));
        task.setAssignedEmployee(assignedEmployee);

        User assignedAdmin = userRepo.findById(taskCreate.assignedByAdminId())
                .orElseThrow(() -> new RuntimeException("Admin not found with id: " + taskCreate.assignedByAdminId()));
        task.setAssignedByAdmin(assignedAdmin);

        Task savedTask = taskRepo.save(task);

        return convertToTaskResponse(savedTask);
    }

    public TaskResponse updateTask(Long id, TaskUpdate taskUpdate){
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id " + id));

        if (taskUpdate.title() != null) {
            task.setTitle(taskUpdate.title());
        }
        if (taskUpdate.description() != null) {
            task.setDescription(taskUpdate.description());
        }
        if (taskUpdate.status() != null) {
            task.setStatus(taskUpdate.status());
        }
        if (taskUpdate.priority() != null) {
            task.setPriority(taskUpdate.priority());
        }
        if (taskUpdate.dueDate() != null) {
            task.setDueDate(taskUpdate.dueDate());
        }

        // Update assigned employee if provided
        if (taskUpdate.assignedEmployeeId() != null) {
            User assignedEmployee = userRepo.findById(taskUpdate.assignedEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found with id: " + taskUpdate.assignedEmployeeId()));
            task.setAssignedEmployee(assignedEmployee);
        }

        // Update who modified it
        if (taskUpdate.updatedByAdminId() != null) {
            User updatedByAdmin = userRepo.findById(taskUpdate.updatedByAdminId())
                    .orElseThrow(() -> new RuntimeException("Admin not found with id: " + taskUpdate.updatedByAdminId()));
            task.setAssignedByAdmin(updatedByAdmin);
        }

        // Save updated task
        Task updatedTask = taskRepo.save(task);

        return convertToTaskResponse(updatedTask);
    }

    public void deleteTask(Long id){
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        taskRepo.delete(task);
    }

    private TaskResponse convertToTaskResponse(Task task) {
        List<TaskCommentResponse> commentResponses = new ArrayList<>();

        if(task.getComments()!= null){
            for(TaskComment comment: task.getComments()){
                TaskCommentResponse commentResponse = new TaskCommentResponse(
                        comment.getId(),
                        comment.getContent(),
                        comment.getAuthor()!=null ? comment.getAuthor().getName() : null,
                        comment.getCreatedAt(),
                        comment.getUpdatedAt()
                );
                commentResponses.add(commentResponse);
            }
        }

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getProject() != null ? task.getProject().getName() : null,
                task.getAssignedEmployee() != null ? task.getAssignedEmployee().getName() : null,
                task.getAssignedByAdmin() != null ? task.getAssignedByAdmin().getName() : null,
                commentResponses,
                task.getCreatedAt()
        );
    }

    public List<TaskResponse> getTasksByUsername(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return taskRepo.findByAssignedEmployeeId(user.getId()).stream().map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }
}
