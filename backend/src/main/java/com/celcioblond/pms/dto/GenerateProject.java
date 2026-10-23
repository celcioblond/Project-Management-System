package com.celcioblond.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateProject(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) String type
) {
}
