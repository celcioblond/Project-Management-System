package com.celcioblond.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCommentUpdate(
        @NotBlank @Size(max = 2000) String content
) {
}
