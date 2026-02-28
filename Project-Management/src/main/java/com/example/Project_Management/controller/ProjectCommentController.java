package com.example.Project_Management.controller;


import com.example.Project_Management.model.ProjectComment;
import com.example.Project_Management.model.dto.ProjectCommentCreate;
import com.example.Project_Management.model.dto.ProjectCommentResponse;
import com.example.Project_Management.model.dto.ProjectCommentUpdate;
import com.example.Project_Management.service.JwtService;
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
    private ProjectCommentService projectCommentService;

    @Autowired
    private JwtService jwtService;

    //Get all comments
    @GetMapping("/projects/{projectId}/comments")
    public ResponseEntity<List<ProjectCommentResponse>> getAllProjectComments(@PathVariable long projectId) {
        List<ProjectCommentResponse> comments = projectCommentService.getAllProjectComments(projectId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("project-comments/my-comments")
    public ResponseEntity<List<ProjectCommentResponse>> getMyProjectComments(@RequestHeader ("Authorization") String token) {
        String jwt = token.substring(7);
        String username = jwtService.extractUsername(jwt);

        List<ProjectCommentResponse> comments = projectCommentService.getCommentsByUsername(username);
        return ResponseEntity.ok(comments);
    }


    @PostMapping ("/project-comments")
    public ResponseEntity<ProjectCommentResponse> createComment(@RequestBody ProjectCommentCreate projectCommentCreate){
        ProjectCommentResponse comment = projectCommentService.createComment(projectCommentCreate);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/project-comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable long id) {
        projectCommentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/project-comments/{id}")
    public ResponseEntity<ProjectCommentResponse> updateComment(@PathVariable long id, @RequestBody ProjectCommentUpdate projectCommentUpdate){
        ProjectCommentResponse comment = projectCommentService.updateComment(id, projectCommentUpdate);
        return ResponseEntity.ok(comment);
    }

}
