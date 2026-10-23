package com.celcioblond.pms.controller;

import com.celcioblond.pms.dto.ProjectCommentCreate;
import com.celcioblond.pms.dto.ProjectCommentResponse;
import com.celcioblond.pms.dto.ProjectCommentUpdate;
import com.celcioblond.pms.security.JwtService;
import com.celcioblond.pms.service.ProjectCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectCommentController {

    private final ProjectCommentService projectCommentService;
    private final JwtService jwtService;

    @GetMapping("/projects/{projectId}/comments")
    public ResponseEntity<List<ProjectCommentResponse>> getAllProjectComments(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectCommentService.getAllProjectComments(projectId));
    }

    @GetMapping("/project-comments/my-comments")
    public ResponseEntity<List<ProjectCommentResponse>> getMyProjectComments(@RequestHeader("Authorization") String token) {
        String username = jwtService.extractUsername(token.substring(7));
        return ResponseEntity.ok(projectCommentService.getCommentsByUsername(username));
    }

    @PostMapping("/project-comments")
    public ResponseEntity<ProjectCommentResponse> createComment(@RequestBody ProjectCommentCreate projectCommentCreate) {
        return ResponseEntity.ok(projectCommentService.createComment(projectCommentCreate));
    }

    @PutMapping("/project-comments/{id}")
    public ResponseEntity<ProjectCommentResponse> updateComment(@PathVariable Long id,
                                                                @RequestBody ProjectCommentUpdate projectCommentUpdate) {
        return ResponseEntity.ok(projectCommentService.updateComment(id, projectCommentUpdate));
    }

    @DeleteMapping("/project-comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        projectCommentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
