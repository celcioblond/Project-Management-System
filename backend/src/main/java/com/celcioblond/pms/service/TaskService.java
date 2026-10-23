package com.celcioblond.pms.service;

import com.celcioblond.pms.dto.TaskCreate;
import com.celcioblond.pms.dto.TaskResponse;
import com.celcioblond.pms.dto.TaskUpdate;
import com.celcioblond.pms.mapper.ResponseMapper;
import com.celcioblond.pms.model.Project;
import com.celcioblond.pms.model.Task;
import com.celcioblond.pms.model.User;
import com.celcioblond.pms.repository.ProjectRepository;
import com.celcioblond.pms.repository.TaskRepository;
import com.celcioblond.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final VectorStoreService vectorStoreService;

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        return toResponses(taskRepository.findAll());
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        return ResponseMapper.toTaskResponse(findTask(id));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProjectId(Long projectId) {
        return toResponses(taskRepository.findByProjectId(projectId));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByEmployeeId(Long employeeId) {
        return toResponses(taskRepository.findByAssignedEmployeeId(employeeId));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toResponses(taskRepository.findByAssignedEmployeeId(user.getId()));
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

        Project project = projectRepository.findById(taskCreate.projectId())
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + taskCreate.projectId()));
        task.setProject(project);

        if (taskCreate.assignedEmployeeIds() != null && !taskCreate.assignedEmployeeIds().isEmpty()) {
            task.setAssignedEmployees(userRepository.findAllById(taskCreate.assignedEmployeeIds()));
        }

        User assignedAdmin = userRepository.findById(taskCreate.assignedByAdminId())
                .orElseThrow(() -> new RuntimeException("Admin not found with id: " + taskCreate.assignedByAdminId()));
        task.setAssignedByAdmin(assignedAdmin);

        Task savedTask = taskRepository.save(task);

        vectorStoreService.upsertTask(savedTask);
        // Re-index the parent project so its embedded task list stays current
        vectorStoreService.upsertProject(projectRepository.findById(project.getId()).orElse(project));

        return ResponseMapper.toTaskResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskUpdate taskUpdate) {
        Task task = findTask(id);

        if (taskUpdate.title() != null) task.setTitle(taskUpdate.title());
        if (taskUpdate.description() != null) task.setDescription(taskUpdate.description());
        if (taskUpdate.status() != null) task.setStatus(taskUpdate.status());
        if (taskUpdate.priority() != null) task.setPriority(taskUpdate.priority());
        if (taskUpdate.dueDate() != null) task.setDueDate(taskUpdate.dueDate());

        if (taskUpdate.assignedEmployeeIds() != null && !taskUpdate.assignedEmployeeIds().isEmpty()) {
            task.setAssignedEmployees(userRepository.findAllById(taskUpdate.assignedEmployeeIds()));
        }

        if (taskUpdate.updatedByAdminId() != null) {
            User updatedByAdmin = userRepository.findById(taskUpdate.updatedByAdminId())
                    .orElseThrow(() -> new RuntimeException("Admin not found with id: " + taskUpdate.updatedByAdminId()));
            task.setAssignedByAdmin(updatedByAdmin);
        }

        Task updatedTask = taskRepository.save(task);

        vectorStoreService.upsertTask(updatedTask);
        // Re-index parent project so its task summary reflects changes
        if (updatedTask.getProject() != null) {
            vectorStoreService.upsertProject(projectRepository.findById(updatedTask.getProject().getId())
                    .orElse(updatedTask.getProject()));
        }

        return ResponseMapper.toTaskResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = findTask(id);
        Long projectId = task.getProject() != null ? task.getProject().getId() : null;

        vectorStoreService.deleteTask(id);
        taskRepository.delete(task);

        // Re-index parent project so the deleted task is removed from its summary
        if (projectId != null) {
            projectRepository.findById(projectId).ifPresent(vectorStoreService::upsertProject);
        }
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    private static List<TaskResponse> toResponses(List<Task> tasks) {
        return tasks.stream().map(ResponseMapper::toTaskResponse).toList();
    }
}
