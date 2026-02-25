package com.example.Project_Management.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TaskUpdate(
        String title,
        String description,
        String status,
        String priority,
        LocalDateTime dueDate,
        Long updatedByAdminId,
        List<Long> assignedEmployeeIds
) {
}
