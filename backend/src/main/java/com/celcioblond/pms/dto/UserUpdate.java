package com.celcioblond.pms.dto;

import com.celcioblond.pms.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UserUpdate(
        @Size(max = 100) String name,
        @Min(0) @Max(120) Integer age,
        @Email String email,
        @Size(max = 100) String position,
        @Size(max = 100) String department,
        Role role
) {
}
