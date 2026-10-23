package com.celcioblond.pms.service;

import com.celcioblond.pms.dto.TaskCommentCreate;
import com.celcioblond.pms.dto.TaskCommentResponse;
import com.celcioblond.pms.dto.TaskCommentUpdate;
import com.celcioblond.pms.exception.ResourceNotFoundException;
import com.celcioblond.pms.mapper.ResponseMapper;
import com.celcioblond.pms.model.Task;
import com.celcioblond.pms.model.TaskComment;
import com.celcioblond.pms.model.User;
import com.celcioblond.pms.repository.TaskCommentRepository;
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
public class TaskCommentService {

    private final TaskCommentRepository taskCommentRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final VectorStoreService vectorStoreService;
    private final AccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public List<TaskCommentResponse> getCommentsByTaskId(Long taskId, UserPrincipal currentUser) {
        accessPolicy.requireTaskAccess(findTask(taskId), currentUser);
        return toResponses(taskCommentRepository.findByTaskId(taskId));
    }

    @Transactional(readOnly = true)
    public List<TaskCommentResponse> getCommentsForUser(UserPrincipal currentUser) {
        return toResponses(taskCommentRepository.findByAuthorId(currentUser.getId()));
    }

    @Transactional
    public TaskCommentResponse createComment(TaskCommentCreate commentCreate, UserPrincipal currentUser) {
        Task task = findTask(commentCreate.taskId());
        accessPolicy.requireTaskAccess(task, currentUser);

        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        TaskComment comment = new TaskComment();
        comment.setContent(commentCreate.content());
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());

        TaskComment savedComment = taskCommentRepository.save(comment);
        task.getComments().add(savedComment);

        vectorStoreService.upsertTaskComment(savedComment);
        // Re-index the parent task so its comment list stays current
        vectorStoreService.upsertTask(task);

        return ResponseMapper.toTaskCommentResponse(savedComment);
    }

    @Transactional
    public TaskCommentResponse updateComment(Long id, TaskCommentUpdate taskCommentUpdate, UserPrincipal currentUser) {
        TaskComment comment = findComment(id);
        accessPolicy.requireAuthorOrAdmin(comment.getAuthor(), currentUser);

        comment.setContent(taskCommentUpdate.content());
        comment.setUpdatedAt(LocalDateTime.now());

        TaskComment updatedComment = taskCommentRepository.save(comment);
        vectorStoreService.upsertTaskComment(updatedComment);

        return ResponseMapper.toTaskCommentResponse(updatedComment);
    }

    @Transactional
    public void deleteComment(Long id, UserPrincipal currentUser) {
        TaskComment comment = findComment(id);
        accessPolicy.requireAuthorOrAdmin(comment.getAuthor(), currentUser);
        Task task = comment.getTask();

        vectorStoreService.deleteTaskComment(id);
        task.getComments().remove(comment);
        taskCommentRepository.delete(comment);

        // Re-index parent task so its comment list no longer includes the deleted one
        vectorStoreService.upsertTask(task);
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    private TaskComment findComment(Long id) {
        return taskCommentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment", id));
    }

    private static List<TaskCommentResponse> toResponses(List<TaskComment> comments) {
        return comments.stream().map(ResponseMapper::toTaskCommentResponse).toList();
    }
}
