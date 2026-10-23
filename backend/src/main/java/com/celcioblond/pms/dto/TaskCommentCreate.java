package com.celcioblond.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskCommentCreate(
        @NotBlank @Size(max = 2000) String content,
        @NotNull Long taskId
) {
}
