package com.example.Project_Management.controller;


import com.example.Project_Management.model.dto.TaskCommentCreate;
import com.example.Project_Management.model.dto.TaskCommentResponse;
import com.example.Project_Management.model.dto.TaskCommentUpdate;
import com.example.Project_Management.repo.TaskCommentRepo;
import com.example.Project_Management.service.JwtService;
import com.example.Project_Management.service.TaskCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskCommentController {

    @Autowired
    private TaskCommentService taskCommentService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/tasks/{taskId}/comments")
    public ResponseEntity<List<TaskCommentResponse>> getAllTaskComments(@PathVariable Long taskId) {
        List<TaskCommentResponse> comments = taskCommentService.getCommentsByTaskId(taskId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/tasks-comments/my-comments")
    public ResponseEntity<List<TaskCommentResponse>> getMyTaskComments(@RequestHeader ("Authorization") String token) {
        String jwt = token.substring(7);
        String username = jwtService.extractUsername(jwt);

        List<TaskCommentResponse> comments = taskCommentService.getCommentsByUsername(username);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/task-comments/{id}")
    public ResponseEntity<TaskCommentResponse> getCommentById(@PathVariable Long id) {
        TaskCommentResponse comment = taskCommentService.getCommentById(id);
        return ResponseEntity.ok(comment);
    }

    @PostMapping("/task-comments")
    public ResponseEntity<TaskCommentResponse> createComment(
            @RequestBody TaskCommentCreate commentCreate
    ) {
        TaskCommentResponse comment = taskCommentService.createComment(commentCreate);
        return new ResponseEntity<>(comment, HttpStatus.CREATED);
    }

    @PutMapping("/task-comments/{id}")
    public ResponseEntity<TaskCommentResponse> updateComment(
            @PathVariable Long id,
            @RequestBody TaskCommentUpdate commentUpdate
    ) {
        TaskCommentResponse updatedComment = taskCommentService.updateComment(id, commentUpdate);
        return ResponseEntity.ok(updatedComment);
    }

    @DeleteMapping("/task-comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        taskCommentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
