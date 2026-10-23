package com.celcioblond.pms.controller;

import com.celcioblond.pms.dto.TaskCommentCreate;
import com.celcioblond.pms.dto.TaskCommentResponse;
import com.celcioblond.pms.dto.TaskCommentUpdate;
import com.celcioblond.pms.security.UserPrincipal;
import com.celcioblond.pms.service.TaskCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService taskCommentService;

    @GetMapping("/tasks/{taskId}/comments")
    public ResponseEntity<List<TaskCommentResponse>> getAllTaskComments(
            @PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(taskCommentService.getCommentsByTaskId(taskId, currentUser));
    }

    @GetMapping("/tasks-comments/my-comments")
    public ResponseEntity<List<TaskCommentResponse>> getMyTaskComments(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(taskCommentService.getCommentsForUser(currentUser));
    }

    @PostMapping("/task-comments")
    public ResponseEntity<TaskCommentResponse> createComment(
            @Valid @RequestBody TaskCommentCreate commentCreate,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return new ResponseEntity<>(taskCommentService.createComment(commentCreate, currentUser), HttpStatus.CREATED);
    }

    @PutMapping("/task-comments/{id}")
    public ResponseEntity<TaskCommentResponse> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody TaskCommentUpdate commentUpdate,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(taskCommentService.updateComment(id, commentUpdate, currentUser));
    }

    @DeleteMapping("/task-comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id,
                                              @AuthenticationPrincipal UserPrincipal currentUser) {
        taskCommentService.deleteComment(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
