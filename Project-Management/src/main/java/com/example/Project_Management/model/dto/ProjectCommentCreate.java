package com.example.Project_Management.model.dto;

public record ProjectCommentCreate(
        String content,
        Long projectId,
        Long authorId
) {
}
