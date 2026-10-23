package com.celcioblond.pms.controller;

import com.celcioblond.pms.dto.Login;
import com.celcioblond.pms.dto.LoginResponse;
import com.celcioblond.pms.dto.UserRegister;
import com.celcioblond.pms.dto.UserResponse;
import com.celcioblond.pms.security.JwtService;
import com.celcioblond.pms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody UserRegister userRegister) {
        UserResponse userResponse = userService.registerUser(userRegister);
        String token = jwtService.generateToken(userResponse.username());

        return new ResponseEntity<>(new LoginResponse(token, userResponse.username(), "EMPLOYEE"), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody Login credentials) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credentials.username(), credentials.password())
        );

        if (!authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtService.generateToken(credentials.username());
        UserResponse userResponse = userService.getUserByUsername(credentials.username());
        String role = userResponse.role() != null ? userResponse.role() : "EMPLOYEE";

        return ResponseEntity.ok(new LoginResponse(token, userResponse.username(), role));
    }
}
