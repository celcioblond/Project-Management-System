package com.celcioblond.pms.controller;

import com.celcioblond.pms.dto.ProjectCommentCreate;
import com.celcioblond.pms.dto.ProjectCommentResponse;
import com.celcioblond.pms.dto.ProjectCommentUpdate;
import com.celcioblond.pms.security.UserPrincipal;
import com.celcioblond.pms.service.ProjectCommentService;
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
public class ProjectCommentController {

    private final ProjectCommentService projectCommentService;

    @GetMapping("/projects/{projectId}/comments")
    public ResponseEntity<List<ProjectCommentResponse>> getAllProjectComments(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(projectCommentService.getAllProjectComments(projectId, currentUser));
    }

    @GetMapping("/project-comments/my-comments")
    public ResponseEntity<List<ProjectCommentResponse>> getMyProjectComments(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(projectCommentService.getCommentsForUser(currentUser));
    }

    @PostMapping("/project-comments")
    public ResponseEntity<ProjectCommentResponse> createComment(
            @Valid @RequestBody ProjectCommentCreate projectCommentCreate,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return new ResponseEntity<>(projectCommentService.createComment(projectCommentCreate, currentUser),
                HttpStatus.CREATED);
    }

    @PutMapping("/project-comments/{id}")
    public ResponseEntity<ProjectCommentResponse> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody ProjectCommentUpdate projectCommentUpdate,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(projectCommentService.updateComment(id, projectCommentUpdate, currentUser));
    }

    @DeleteMapping("/project-comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id,
                                              @AuthenticationPrincipal UserPrincipal currentUser) {
        projectCommentService.deleteComment(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
