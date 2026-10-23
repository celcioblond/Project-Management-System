package com.celcioblond.pms.dto;

import java.time.LocalDateTime;

public record ProjectCommentResponse(
        Long id,
        String content,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
