package com.celcioblond.pms.controller;

import com.celcioblond.pms.dto.*;
import com.celcioblond.pms.security.JwtService;
import com.celcioblond.pms.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final JwtService jwtService;

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjectResponses());
    }

    @GetMapping("/projects/my-projects")
    public ResponseEntity<List<ProjectResponse>> getMyProjects(@RequestHeader("Authorization") String token) {
        String username = jwtService.extractUsername(token.substring(7));
        return ResponseEntity.ok(projectService.getProjectsByUsername(username));
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> addProject(@RequestBody ProjectCreate projectCreate) {
        return new ResponseEntity<>(projectService.addProject(projectCreate), HttpStatus.CREATED);
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long id, @RequestBody ProjectUpdate projectUpdate) {
        return ResponseEntity.ok(projectService.updateProject(id, projectUpdate));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/project/generate-project")
    public ResponseEntity<String> generateProject(@RequestBody GenerateProject request) {
        try {
            return ResponseEntity.ok(projectService.generateProject(request.name(), request.type()));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/project/generate-diagram")
    public ResponseEntity<byte[]> generateDiagram(@RequestBody GenerateDiagram request) {
        try {
            return ResponseEntity.ok(projectService.generateImage(request.title(), request.type(), request.description()));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
