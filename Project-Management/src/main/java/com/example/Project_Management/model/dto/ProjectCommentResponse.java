package com.example.Project_Management.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectCommentResponse(
        Long id,
        String content,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
