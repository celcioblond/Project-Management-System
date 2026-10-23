package com.celcioblond.pms.controller;

import com.celcioblond.pms.dto.Login;
import com.celcioblond.pms.dto.LoginResponse;
import com.celcioblond.pms.dto.UserRegister;
import com.celcioblond.pms.dto.UserResponse;
import com.celcioblond.pms.security.JwtService;
import com.celcioblond.pms.security.UserPrincipal;
import com.celcioblond.pms.service.UserService;
import jakarta.validation.Valid;
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
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody UserRegister userRegister) {
        UserResponse user = userService.registerUser(userRegister);
        String token = jwtService.generateToken(user.username());
        return new ResponseEntity<>(new LoginResponse(token, user.username(), user.role()), HttpStatus.CREATED);
    }

    /** Bad credentials raise an AuthenticationException, answered with 401 by the exception handler. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody Login credentials) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credentials.username(), credentials.password())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal.getUsername());

        return ResponseEntity.ok(new LoginResponse(token, principal.getUsername(), principal.getRole().name()));
    }
}
