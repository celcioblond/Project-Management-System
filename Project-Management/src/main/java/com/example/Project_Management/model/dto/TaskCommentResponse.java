package com.example.Project_Management.model.dto;

import java.time.LocalDateTime;

public record TaskCommentResponse(
        Long id,
        String content,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
