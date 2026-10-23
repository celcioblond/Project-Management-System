package com.celcioblond.pms.dto;

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
