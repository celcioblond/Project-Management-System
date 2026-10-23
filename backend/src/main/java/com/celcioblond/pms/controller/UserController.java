package com.celcioblond.pms.controller;

import com.celcioblond.pms.dto.PasswordUpdate;
import com.celcioblond.pms.dto.UserCreate;
import com.celcioblond.pms.dto.UserResponse;
import com.celcioblond.pms.dto.UserUpdate;
import com.celcioblond.pms.security.JwtService;
import com.celcioblond.pms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/users/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreate userCreate) {
        return new ResponseEntity<>(userService.createUser(userCreate), HttpStatus.CREATED);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserUpdate userUpdate) {
        return ResponseEntity.ok(userService.updateUser(id, userUpdate));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable Long id, @RequestBody PasswordUpdate passwordUpdate) {
        userService.updatePassword(id, passwordUpdate.newPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/projects/{projectId}/colleagues")
    public ResponseEntity<List<UserResponse>> getColleaguesByProject(@PathVariable Long projectId,
                                                                     @RequestHeader("Authorization") String token) {
        String username = jwtService.extractUsername(token.substring(7));
        return ResponseEntity.ok(userService.getColleaguesByProjectId(projectId, username));
    }
}
