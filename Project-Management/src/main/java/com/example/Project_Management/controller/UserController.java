package com.example.Project_Management.controller;

import com.example.Project_Management.model.User;
import com.example.Project_Management.model.dto.PasswordUpdate;
import com.example.Project_Management.model.dto.UserCreate;
import com.example.Project_Management.model.dto.UserResponse;
import com.example.Project_Management.model.dto.UserUpdate;
import com.example.Project_Management.service.JwtService;
import com.example.Project_Management.service.UserService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers(){
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id){
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/users/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email){
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<List<UserResponse>> getUserByRole(@PathVariable String role) {
        List<UserResponse> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreate userCreate){
        UserResponse userResponse = userService.createUser(userCreate);
        return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserUpdate userUpdate){
        UserResponse updatedUser = userService.updateUser(id, userUpdate);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id){
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable Long id, @RequestBody PasswordUpdate passwordUpdate) {
        userService.updatePassword(id, passwordUpdate.newPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/projects/{projectId}/colleagues")
    public ResponseEntity<List<UserResponse>> getColleaguesByProject(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {

        String jwt = token.substring(7);
        String username = jwtService.extractUsername(jwt);

        List<UserResponse> colleagues = userService.getColleaguesByProjectId(projectId, username);
        return ResponseEntity.ok(colleagues);
    }



}
