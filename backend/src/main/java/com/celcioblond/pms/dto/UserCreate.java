package com.celcioblond.pms.dto;

import com.celcioblond.pms.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreate(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(min = 3, max = 50) String username,
        @Min(0) @Max(120) Integer age,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 100) String position,
        @Size(max = 100) String department,
        @NotNull Role role
) {
}
