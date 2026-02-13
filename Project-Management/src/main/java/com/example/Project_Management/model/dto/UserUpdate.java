package com.example.Project_Management.model.dto;

public record UserUpdate(
        String name,
        Integer age,
        String email,
        String position,
        String department,
        String role
) {
}
