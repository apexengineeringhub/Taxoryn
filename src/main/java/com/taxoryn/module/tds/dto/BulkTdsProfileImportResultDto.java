package com.taxoryn.module.tds.dto;

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
@Schema(description = "Result of bulk importing TAN Deductor Profiles")
public class BulkTdsProfileImportResultDto {

    @Schema(description = "Total records in batch")
    private int totalProcessed;

    @Schema(description = "Successfully imported profiles")
    private int totalCreated;

    @Schema(description = "Skipped records (duplicates)")
    private int totalSkipped;

    @Schema(description = "Failed records due to validation")
    private int totalFailed;

    @Builder.Default
    @Schema(description = "List of imported profiles")
    private List<TdsProfileDto> importedProfiles = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Error messages for failed records")
    private List<String> errorMessages = new ArrayList<>();
}
