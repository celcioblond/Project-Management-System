package com.example.Project_Management.model.dto;

public record TaskCommentCreate(
        String content,
        Long taskId,
        Long authorId
) {
}
