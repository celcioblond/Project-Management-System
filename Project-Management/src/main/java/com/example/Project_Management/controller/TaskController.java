package com.example.Project_Management.controller;

import com.example.Project_Management.model.dto.TaskCreate;
import com.example.Project_Management.model.dto.TaskResponse;
import com.example.Project_Management.model.dto.TaskUpdate;
import com.example.Project_Management.service.JwtService;
import com.example.Project_Management.service.TaskService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/tasks")
    public ResponseEntity<List<TaskResponse>> getAllTasks(){
        List<TaskResponse> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/tasks/my-tasks")
    public ResponseEntity<List<TaskResponse>> getMyTasks(@RequestHeader("Authorization") String token){
        String  jwt = token.substring(7);
        String username = jwtService.extractUsername(jwt);
        List<TaskResponse> tasks = taskService.getTasksByUsername(username);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable int id){
        TaskResponse task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(@PathVariable long projectId){
        List<TaskResponse> tasks = taskService.getTasksByProjectById(projectId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/employees/{employeeId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksByEmployee(@PathVariable long employeeId){
        List<TaskResponse> tasks = taskService.getTasksByEmployeeId(employeeId);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskResponse> createTask(@RequestBody TaskCreate taskCreate, @RequestParam Long assignedByAdminId){
        TaskResponse taskResponse = taskService.createTask(taskCreate, assignedByAdminId);
        return new ResponseEntity<>(taskResponse, HttpStatus.CREATED);
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> udpateTask(@PathVariable Long id, @RequestBody TaskUpdate taskUpdate){
        TaskResponse taskResponse = taskService.updateTask(id, taskUpdate);
        return ResponseEntity.ok(taskResponse);
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id){
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }


}
