package com.example.Project_Management.model.dto;

public record UserCreate(
        String name,
        Integer age,
        String email,
        String password,
        String position,
        String department,
        String role
) {
}
