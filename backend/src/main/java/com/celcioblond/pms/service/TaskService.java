package com.celcioblond.pms.service;

import com.celcioblond.pms.dto.TaskCreate;
import com.celcioblond.pms.dto.TaskResponse;
import com.celcioblond.pms.dto.TaskUpdate;
import com.celcioblond.pms.exception.BadRequestException;
import com.celcioblond.pms.exception.ResourceNotFoundException;
import com.celcioblond.pms.mapper.ResponseMapper;
import com.celcioblond.pms.model.Project;
import com.celcioblond.pms.model.Task;
import com.celcioblond.pms.model.User;
import com.celcioblond.pms.repository.ProjectRepository;
import com.celcioblond.pms.repository.TaskRepository;
import com.celcioblond.pms.repository.UserRepository;
import com.celcioblond.pms.security.AccessPolicy;
import com.celcioblond.pms.security.UserPrincipal;
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
    private final AccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        return toResponses(taskRepository.findAll());
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id, UserPrincipal currentUser) {
        Task task = findTask(id);
        accessPolicy.requireTaskAccess(task, currentUser);
        return ResponseMapper.toTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProjectId(Long projectId, UserPrincipal currentUser) {
        accessPolicy.requireProjectAccess(projectId, currentUser);
        return toResponses(taskRepository.findByProjectId(projectId));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByEmployeeId(Long employeeId) {
        return toResponses(taskRepository.findByAssignedEmployeeId(employeeId));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksForUser(UserPrincipal currentUser) {
        return toResponses(taskRepository.findByAssignedEmployeeId(currentUser.getId()));
    }

    @Transactional
    public TaskResponse createTask(TaskCreate taskCreate, UserPrincipal currentUser) {
        if (taskCreate.projectId() == null) {
            throw new BadRequestException("projectId is required");
        }
        Project project = projectRepository.findById(taskCreate.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", taskCreate.projectId()));

        Task task = new Task();
        task.setTitle(taskCreate.title());
        task.setDescription(taskCreate.description());
        task.setStatus(taskCreate.status());
        task.setPriority(taskCreate.priority());
        task.setDueDate(taskCreate.dueDate());
        task.setCreatedAt(LocalDateTime.now());
        task.setProject(project);
        task.setAssignedByAdmin(findUser(currentUser.getId()));

        if (taskCreate.assignedEmployeeIds() != null && !taskCreate.assignedEmployeeIds().isEmpty()) {
            task.setAssignedEmployees(userRepository.findAllById(taskCreate.assignedEmployeeIds()));
        }

        Task savedTask = taskRepository.save(task);
        project.getTasks().add(savedTask);

        vectorStoreService.upsertTask(savedTask);
        // Re-index the parent project so its embedded task list stays current
        vectorStoreService.upsertProject(project);

        return ResponseMapper.toTaskResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskUpdate taskUpdate, UserPrincipal currentUser) {
        Task task = findTask(id);

        if (taskUpdate.title() != null) task.setTitle(taskUpdate.title());
        if (taskUpdate.description() != null) task.setDescription(taskUpdate.description());
        if (taskUpdate.status() != null) task.setStatus(taskUpdate.status());
        if (taskUpdate.priority() != null) task.setPriority(taskUpdate.priority());
        if (taskUpdate.dueDate() != null) task.setDueDate(taskUpdate.dueDate());

        if (taskUpdate.assignedEmployeeIds() != null && !taskUpdate.assignedEmployeeIds().isEmpty()) {
            task.setAssignedEmployees(userRepository.findAllById(taskUpdate.assignedEmployeeIds()));
        }

        task.setAssignedByAdmin(findUser(currentUser.getId()));

        Task updatedTask = taskRepository.save(task);

        vectorStoreService.upsertTask(updatedTask);
        // Re-index parent project so its task summary reflects changes
        vectorStoreService.upsertProject(updatedTask.getProject());

        return ResponseMapper.toTaskResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = findTask(id);
        Project project = task.getProject();

        vectorStoreService.deleteTask(id);
        project.getTasks().remove(task);
        taskRepository.delete(task);

        // Re-index parent project so the deleted task is removed from its summary
        vectorStoreService.upsertProject(project);
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private static List<TaskResponse> toResponses(List<Task> tasks) {
        return tasks.stream().map(ResponseMapper::toTaskResponse).toList();
    }
}
