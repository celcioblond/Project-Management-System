package com.example.Project_Management.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectUpdate(
        String name,
        String description,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long assignedEmployeeId,
        Long updatedByAdminId,
        List<TaskCreate> newTasks
) {
}
