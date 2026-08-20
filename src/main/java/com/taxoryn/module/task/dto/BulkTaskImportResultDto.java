package com.taxoryn.module.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk Task Creation Result")
public class BulkTaskImportResultDto {

    @Schema(description = "Total tasks attempted", example = "25")
    private int totalProcessed;

    @Schema(description = "Successfully created tasks", example = "25")
    private int totalCreated;

    @Schema(description = "Failed tasks", example = "0")
    private int totalFailed;

    @Schema(description = "List of created Task DTOs")
    @Builder.Default
    private List<TaskDto> createdTasks = new ArrayList<>();

    @Schema(description = "Error messages if any")
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
