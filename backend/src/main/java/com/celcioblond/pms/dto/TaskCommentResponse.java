package com.celcioblond.pms.dto;

import java.time.LocalDateTime;

public record TaskCommentResponse(
        Long id,
        String content,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
