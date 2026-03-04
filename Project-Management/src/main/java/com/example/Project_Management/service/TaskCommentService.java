package com.example.Project_Management.service;

import com.example.Project_Management.model.Task;
import com.example.Project_Management.model.TaskComment;
import com.example.Project_Management.model.User;
import com.example.Project_Management.model.dto.TaskCommentCreate;
import com.example.Project_Management.model.dto.TaskCommentResponse;
import com.example.Project_Management.model.dto.TaskCommentUpdate;
import com.example.Project_Management.repo.TaskCommentRepo;
import com.example.Project_Management.repo.TaskRepo;
import com.example.Project_Management.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskCommentService {

    @Autowired private TaskCommentRepo taskCommentRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private TaskRepo taskRepo;
    @Autowired private VectorStoreService vectorStoreService;

    public List<TaskCommentResponse> getCommentsByTaskId(Long taskId) {
        return taskCommentRepo.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TaskCommentResponse getCommentById(Long id) {
        TaskComment comment = taskCommentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
        return convertToResponse(comment);
    }

    @Transactional
    public TaskCommentResponse createComment(TaskCommentCreate commentCreate) {
        Task task = taskRepo.findById(commentCreate.taskId())
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + commentCreate.taskId()));

        User author = userRepo.findById(commentCreate.authorId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + commentCreate.authorId()));

        TaskComment comment = new TaskComment();
        comment.setContent(commentCreate.content());
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());

        TaskComment savedComment = taskCommentRepo.save(comment);

        // Index the new comment
        vectorStoreService.upsertTaskComment(savedComment);

        // Re-index the parent task so its comment list stays current
        vectorStoreService.upsertTask(taskRepo.findById(task.getId()).orElse(task));

        return convertToResponse(savedComment);
    }

    @Transactional
    public TaskCommentResponse updateComment(Long id, TaskCommentUpdate taskCommentUpdate) {
        TaskComment comment = taskCommentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));

        comment.setContent(taskCommentUpdate.content());
        comment.setUpdatedAt(LocalDateTime.now());

        TaskComment updatedComment = taskCommentRepo.save(comment);

        // Re-index the updated comment
        vectorStoreService.upsertTaskComment(updatedComment);

        return convertToResponse(updatedComment);
    }

    @Transactional
    public void deleteComment(Long id) {
        TaskComment comment = taskCommentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));

        Long taskId = comment.getTask() != null ? comment.getTask().getId() : null;

        // Remove from vector store before deleting from DB
        vectorStoreService.deleteTaskComment(id);

        taskCommentRepo.delete(comment);

        // Re-index parent task so its comment list no longer includes the deleted one
        if (taskId != null) {
            taskRepo.findById(taskId).ifPresent(vectorStoreService::upsertTask);
        }
    }

    public List<TaskCommentResponse> getCommentsByUsername(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return taskCommentRepo.findByAuthorId(user.getId()).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private TaskCommentResponse convertToResponse(TaskComment comment) {
        return new TaskCommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getAuthor() != null ? comment.getAuthor().getName() : null,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}