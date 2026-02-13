package com.example.Project_Management.controller;


import com.example.Project_Management.model.dto.ProjectCommentCreate;
import com.example.Project_Management.model.dto.ProjectCommentResponse;
import com.example.Project_Management.service.ProjectCommentService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProjectCommentController {

    @Autowired
    ProjectCommentService projectCommentService;

    //Get all comments
    @GetMapping("/projects/{authorId}/comments")
    public ResponseEntity<List<ProjectCommentResponse>> getAllProjectComments(@PathVariable long authorId) {
        ;List<ProjectCommentResponse> comments = projectCommentService.getAllProjectComments(authorId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/project-comments/{id}")
    public ResponseEntity<ProjectCommentResponse> getCommentById(@PathVariable long id) {
        ProjectCommentResponse comment = projectCommentService.getCommentById(id);
        return ResponseEntity.ok(comment);
    }

    @PutMapping("/project-comments")
    public ResponseEntity<ProjectCommentResponse> createComment(@RequestBody ProjectCommentCreate projectCommentCreate){
        ProjectCommentResponse comment = projectCommentService.createComment(projectCommentCreate);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/project-comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable long id) {
        projectCommentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }

}
