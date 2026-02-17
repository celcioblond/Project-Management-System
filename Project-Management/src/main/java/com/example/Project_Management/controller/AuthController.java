package com.example.Project_Management.controller;

import com.example.Project_Management.model.User;
import com.example.Project_Management.model.dto.Login;
import com.example.Project_Management.model.dto.LoginResponse;
import com.example.Project_Management.model.dto.UserRegister;
import com.example.Project_Management.model.dto.UserResponse;
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

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins="http://localhost:5173")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody UserRegister userRegister){
        UserResponse userResponse = userService.registerUser(userRegister);

        String token = jwtService.generateToken(userResponse.username());

        LoginResponse loginResponse = new LoginResponse(
                token,
                userResponse.username(),
                "EMPLOYEE"
        );

        return new ResponseEntity<>(loginResponse, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody Login credentials){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credentials.username(), credentials.password())
        );

        if(authentication.isAuthenticated()){
            String token = jwtService.generateToken(credentials.username());

            UserResponse userResponse = userService.getUserByUsername(credentials.username());

            LoginResponse loginResponse = new LoginResponse(
                    token,
                    userResponse.username(),
                    userResponse.role() != null ? userResponse.role() : "EMPLOYEE"
            );

            return ResponseEntity.ok(loginResponse);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
