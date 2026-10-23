package com.celcioblond.pms.dto;

public record LoginResponse(
        String token,
        String username,
        String role
) {
}
