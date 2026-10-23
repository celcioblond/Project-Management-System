package com.celcioblond.pms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectCreate(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String description,
        @Size(max = 100) String type,
        @Size(max = 50) String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        List<Long> assignedEmployeeIds,
        List<@Valid TaskCreate> tasks,
        List<@NotBlank @Size(max = 2000) String> comments,
        byte[] projectDiagram
) {
}
