package com.celcioblond.pms.dto;

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
