package com.celcioblond.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectCommentCreate(
        @NotBlank @Size(max = 2000) String content,
        @NotNull Long projectId
) {
}
