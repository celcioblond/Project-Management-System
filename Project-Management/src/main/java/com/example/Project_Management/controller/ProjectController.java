package com.example.Project_Management.controller;

import com.example.Project_Management.model.Project;
import com.example.Project_Management.model.dto.ProjectCreate;
import com.example.Project_Management.model.dto.ProjectResponse;
import com.example.Project_Management.model.dto.ProjectUpdate;
import com.example.Project_Management.service.JwtService;
import com.example.Project_Management.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponse>> getAllProjectResponses(){
        List<ProjectResponse> projects = projectService.getAllProjectResponses();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/projects/my-projects")
    public ResponseEntity<List<ProjectResponse>> getMyProjects(@RequestHeader("Authorization") String token){

        String jwt = token.substring(7);
        String username = jwtService.extractUsername(jwt);

        List<ProjectResponse> projects = projectService.getProjectsByUsername(username);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        ProjectResponse project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> addProject(@RequestBody ProjectCreate projectCreate){
        ProjectResponse projectResponse = projectService.addProject(projectCreate);
        return new ResponseEntity<>(projectResponse, HttpStatus.CREATED);
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @RequestBody ProjectUpdate projectUpdate
    ) {
        ProjectResponse updatedProject = projectService.updateProject(id, projectUpdate);
        return ResponseEntity.ok(updatedProject);
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
