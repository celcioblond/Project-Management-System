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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskCommentService {

    @Autowired
    private TaskCommentRepo taskCommentRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private TaskRepo taskRepo;

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

    public TaskCommentResponse createComment(TaskCommentCreate commentCreate) {
        // Find task
        Task task = taskRepo.findById(commentCreate.taskId())
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + commentCreate.taskId()));

        // Find author
        User author = userRepo.findById(commentCreate.authorId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + commentCreate.authorId()));

        // Create comment
        TaskComment comment = new TaskComment();
        comment.setContent(commentCreate.content());
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());

        // Save comment
        TaskComment savedComment = taskCommentRepo.save(comment);

        return convertToResponse(savedComment);
    }

    public TaskCommentResponse updateComment(Long id, TaskCommentUpdate taskCommentUpdate){
        TaskComment comment = taskCommentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));

        // Update content
        comment.setContent(taskCommentUpdate.content());
        comment.setUpdatedAt(LocalDateTime.now());

        // Save updated comment
        TaskComment updatedComment = taskCommentRepo.save(comment);

        return convertToResponse(updatedComment);
    }

    public void deleteComment(Long id) {
        TaskComment comment = taskCommentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
        taskCommentRepo.delete(comment);
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

    public List<TaskCommentResponse> getCommentsByUsername(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return taskCommentRepo.findByAuthorId(user.getId()).stream().map(this::convertToResponse)
                .collect(Collectors.toList());
    }
}
