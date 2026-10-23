package com.celcioblond.pms.controller;

import com.celcioblond.pms.dto.*;
import com.celcioblond.pms.security.UserPrincipal;
import com.celcioblond.pms.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjectResponses());
    }

    @GetMapping("/projects/my-projects")
    public ResponseEntity<List<ProjectResponse>> getMyProjects(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(projectService.getProjectsForUser(currentUser));
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(projectService.getProjectById(id, currentUser));
    }

    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> addProject(@Valid @RequestBody ProjectCreate projectCreate,
                                                      @AuthenticationPrincipal UserPrincipal currentUser) {
        return new ResponseEntity<>(projectService.addProject(projectCreate, currentUser), HttpStatus.CREATED);
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long id,
                                                         @Valid @RequestBody ProjectUpdate projectUpdate,
                                                         @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(projectService.updateProject(id, projectUpdate, currentUser));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/project/generate-project")
    public ResponseEntity<String> generateProject(@Valid @RequestBody GenerateProject request) {
        return ResponseEntity.ok(projectService.generateProject(request.name(), request.type()));
    }

    @PostMapping(value = "/project/generate-diagram", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateDiagram(@Valid @RequestBody GenerateDiagram request) {
        return ResponseEntity.ok(projectService.generateImage(request.title(), request.type(), request.description()));
    }
}
