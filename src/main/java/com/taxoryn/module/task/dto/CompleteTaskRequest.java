package com.taxoryn.module.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Complete Task Request")
public class CompleteTaskRequest {

    @Min(value = 0, message = "Actual minutes must be non-negative")
    private Integer actualMinutes;

    private String notes;
}
