package com.example.Project_Management.model.dto;

public record UserResponse(
        Long id,
        String name,
        Integer age,
        String email,
        String username,
        String position,
        String department,
        String role
) {
}
