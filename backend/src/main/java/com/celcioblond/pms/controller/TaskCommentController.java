package com.celcioblond.pms.controller;

import com.celcioblond.pms.dto.TaskCommentCreate;
import com.celcioblond.pms.dto.TaskCommentResponse;
import com.celcioblond.pms.dto.TaskCommentUpdate;
import com.celcioblond.pms.security.JwtService;
import com.celcioblond.pms.service.TaskCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService taskCommentService;
    private final JwtService jwtService;

    @GetMapping("/tasks/{taskId}/comments")
    public ResponseEntity<List<TaskCommentResponse>> getAllTaskComments(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskCommentService.getCommentsByTaskId(taskId));
    }

    @GetMapping("/tasks-comments/my-comments")
    public ResponseEntity<List<TaskCommentResponse>> getMyTaskComments(@RequestHeader("Authorization") String token) {
        String username = jwtService.extractUsername(token.substring(7));
        return ResponseEntity.ok(taskCommentService.getCommentsByUsername(username));
    }

    @PostMapping("/task-comments")
    public ResponseEntity<TaskCommentResponse> createComment(@RequestBody TaskCommentCreate commentCreate) {
        return new ResponseEntity<>(taskCommentService.createComment(commentCreate), HttpStatus.CREATED);
    }

    @PutMapping("/task-comments/{id}")
    public ResponseEntity<TaskCommentResponse> updateComment(@PathVariable Long id,
                                                             @RequestBody TaskCommentUpdate commentUpdate) {
        return ResponseEntity.ok(taskCommentService.updateComment(id, commentUpdate));
    }

    @DeleteMapping("/task-comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        taskCommentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
