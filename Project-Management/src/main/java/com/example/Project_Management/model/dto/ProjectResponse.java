package com.example.Project_Management.model.dto;

import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectResponse(
        Long id,
        String title,
        String description,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        List<String> assignedEmployeeNames,
        String createdByAdminName,
        List<TaskResponse> tasks,
        List<ProjectCommentResponse> comments,
        LocalDateTime createdAt
) {
}
