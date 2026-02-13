package com.example.Project_Management.model.dto;

import java.time.LocalDateTime;

public record TaskCreate(
        String title,
        String description,
        String status,
        String priority,
        LocalDateTime dueDate,
        Long projectId,
        Long assignedEmployeeId,
        Long assignedByAdminId
) {
}
