package com.taxoryn.module.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Client Actionable Task Summary")
public class ClientTaskDto {

    private UUID id;
    private String title;
    private String description;
    private String taskCategory;
    private String status;
    private String priority;
    private LocalDate dueDate;
}
