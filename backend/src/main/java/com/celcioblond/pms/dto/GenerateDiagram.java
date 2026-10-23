package com.celcioblond.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateDiagram(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 100) String type,
        @Size(max = 1000) String description
) {
}
