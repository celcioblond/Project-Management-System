package com.celcioblond.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskCommentUpdate(
        @NotBlank @Size(max = 2000) String content
) {
}
