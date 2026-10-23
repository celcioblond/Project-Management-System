package com.celcioblond.pms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectUpdate(
        @Size(max = 255) String name,
        @Size(max = 255) String description,
        @Size(max = 100) String type,
        @Size(max = 50) String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        List<Long> assignedEmployeeIds,
        List<@Valid TaskCreate> newTasks,
        byte[] projectDiagram
) {
}
