package com.celcioblond.pms.dto;

public record UserUpdate(
        String name,
        Integer age,
        String email,
        String position,
        String department,
        String role
) {
}
