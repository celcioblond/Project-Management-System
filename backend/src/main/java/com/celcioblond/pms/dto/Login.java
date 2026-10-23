package com.celcioblond.pms.dto;

import jakarta.validation.constraints.NotBlank;

public record Login(
        @NotBlank String username,
        @NotBlank String password
) {
}
