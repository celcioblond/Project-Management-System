package com.example.Project_Management.model.dto;

public record UserCreate(
        String name,
        String username,
        Integer age,
        String email,
        String password,
        String position,
        String department,
        String role
) {
}
