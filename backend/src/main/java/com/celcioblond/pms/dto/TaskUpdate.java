package com.celcioblond.pms.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record TaskUpdate(
        @Size(max = 255) String title,
        @Size(max = 255) String description,
        @Size(max = 50) String status,
        @Size(max = 50) String priority,
        LocalDateTime dueDate,
        List<Long> assignedEmployeeIds
) {
}
