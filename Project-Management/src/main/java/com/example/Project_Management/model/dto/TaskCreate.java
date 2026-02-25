package com.example.Project_Management.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TaskCreate(
        String title,
        String description,
        String status,
        String priority,
        LocalDateTime dueDate,
        Long projectId,
        List<Long> assignedEmployeeIds,
        Long assignedByAdminId
) {
}
