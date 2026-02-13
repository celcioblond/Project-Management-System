package com.example.Project_Management.model.dto;

import com.example.Project_Management.model.TaskComment;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.List;

public record TaskResponse(
        Long id,
        String title,
        String description,
        String priority,
        String status,
        LocalDateTime dueDate,
        String projectName,
        String assignedEmployeeName,
        String assignedByAdminName,
        List<TaskCommentResponse> comments,
        LocalDateTime createdAt
) {
}
