package com.celcioblond.pms.service;

import com.celcioblond.pms.dto.TaskCommentCreate;
import com.celcioblond.pms.dto.TaskCommentResponse;
import com.celcioblond.pms.dto.TaskCommentUpdate;
import com.celcioblond.pms.mapper.ResponseMapper;
import com.celcioblond.pms.model.Task;
import com.celcioblond.pms.model.TaskComment;
import com.celcioblond.pms.model.User;
import com.celcioblond.pms.repository.TaskCommentRepository;
import com.celcioblond.pms.repository.TaskRepository;
import com.celcioblond.pms.repository.UserRepository;
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

    @Transactional(readOnly = true)
    public List<TaskCommentResponse> getCommentsByTaskId(Long taskId) {
        return toResponses(taskCommentRepository.findByTaskId(taskId));
    }

    @Transactional(readOnly = true)
    public List<TaskCommentResponse> getCommentsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return toResponses(taskCommentRepository.findByAuthorId(user.getId()));
    }

    @Transactional
    public TaskCommentResponse createComment(TaskCommentCreate commentCreate) {
        Task task = taskRepository.findById(commentCreate.taskId())
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + commentCreate.taskId()));

        User author = userRepository.findById(commentCreate.authorId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + commentCreate.authorId()));

        TaskComment comment = new TaskComment();
        comment.setContent(commentCreate.content());
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());

        TaskComment savedComment = taskCommentRepository.save(comment);

        vectorStoreService.upsertTaskComment(savedComment);
        // Re-index the parent task so its comment list stays current
        vectorStoreService.upsertTask(taskRepository.findById(task.getId()).orElse(task));

        return ResponseMapper.toTaskCommentResponse(savedComment);
    }

    @Transactional
    public TaskCommentResponse updateComment(Long id, TaskCommentUpdate taskCommentUpdate) {
        TaskComment comment = findComment(id);
        comment.setContent(taskCommentUpdate.content());
        comment.setUpdatedAt(LocalDateTime.now());

        TaskComment updatedComment = taskCommentRepository.save(comment);
        vectorStoreService.upsertTaskComment(updatedComment);

        return ResponseMapper.toTaskCommentResponse(updatedComment);
    }

    @Transactional
    public void deleteComment(Long id) {
        TaskComment comment = findComment(id);
        Long taskId = comment.getTask() != null ? comment.getTask().getId() : null;

        vectorStoreService.deleteTaskComment(id);
        taskCommentRepository.delete(comment);

        // Re-index parent task so its comment list no longer includes the deleted one
        if (taskId != null) {
            taskRepository.findById(taskId).ifPresent(vectorStoreService::upsertTask);
        }
    }

    private TaskComment findComment(Long id) {
        return taskCommentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
    }

    private static List<TaskCommentResponse> toResponses(List<TaskComment> comments) {
        return comments.stream().map(ResponseMapper::toTaskCommentResponse).toList();
    }
}
