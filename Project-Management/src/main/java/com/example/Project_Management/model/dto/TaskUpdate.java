package com.example.Project_Management.model.dto;

import java.time.LocalDateTime;

public record TaskUpdate(
        String title,
        String description,
        String status,
        String priority,
        LocalDateTime dueDate,
        Long updatedByAdminId,
        Long assignedEmployeeId
) {
}
