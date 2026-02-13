package com.example.Project_Management.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectCreate(
        String name,
        String description,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long assignedEmployeeId,
        Long createdByAdminId,
        List<TaskCreate> tasks,
        List<String> comments

) {
}
