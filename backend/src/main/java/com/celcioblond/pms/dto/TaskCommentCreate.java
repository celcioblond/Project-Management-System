package com.celcioblond.pms.dto;

public record TaskCommentCreate(
        String content,
        Long taskId,
        Long authorId
) {
}
