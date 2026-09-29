package com.taxoryn.module.worktemplate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateWorkInstanceRequest {

    @NotNull(message = "Template ID is required")
    private UUID templateId;

    private String title;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private LocalDate dueDate;

    private UUID assignedUserId;

    private UUID reviewerUserId;

    private String notes;
}
