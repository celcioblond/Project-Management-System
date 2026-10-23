package com.celcioblond.pms.controller;

import com.celcioblond.pms.dto.PasswordUpdate;
import com.celcioblond.pms.dto.UserCreate;
import com.celcioblond.pms.dto.UserResponse;
import com.celcioblond.pms.dto.UserUpdate;
import com.celcioblond.pms.security.UserPrincipal;
import com.celcioblond.pms.service.UserService;
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
public class UserController {

    private final UserService userService;

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
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreate userCreate) {
        return new ResponseEntity<>(userService.createUser(userCreate), HttpStatus.CREATED);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UserUpdate userUpdate,
                                                   @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(userService.updateUser(id, userUpdate, currentUser));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser) {
        userService.deleteUser(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable Long id,
                                               @Valid @RequestBody PasswordUpdate passwordUpdate,
                                               @AuthenticationPrincipal UserPrincipal currentUser) {
        userService.updatePassword(id, passwordUpdate, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects/{projectId}/colleagues")
    public ResponseEntity<List<UserResponse>> getColleaguesByProject(@PathVariable Long projectId,
                                                                     @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(userService.getColleaguesByProjectId(projectId, currentUser));
    }
}
