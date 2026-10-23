package com.celcioblond.pms.dto;

public record ProjectCommentCreate(
        String content,
        Long projectId,
        Long authorId
) {
}
