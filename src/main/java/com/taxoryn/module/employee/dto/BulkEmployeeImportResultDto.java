package com.taxoryn.module.employee.dto;

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
@Schema(description = "Bulk Employee / Practitioner Onboarding Result")
public class BulkEmployeeImportResultDto {

    @Schema(description = "Total records attempted", example = "10")
    private int totalProcessed;

    @Schema(description = "Successfully onboarded employees", example = "10")
    private int totalCreated;

    @Schema(description = "Failed records", example = "0")
    private int totalFailed;

    @Schema(description = "Skipped duplicate records", example = "0")
    private int totalSkipped;

    @Schema(description = "List of created employee DTOs")
    @Builder.Default
    private List<EmployeeDto> createdEmployees = new ArrayList<>();

    @Schema(description = "Error messages if any")
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
