package com.taxoryn.module.itr.dto;

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
@Schema(description = "Bulk ITR Import & Migration Result Summary")
public class BulkItrImportResultDto {

    @Schema(description = "Total records processed in batch", example = "10")
    private int totalProcessed;

    @Schema(description = "Total new records successfully imported", example = "8")
    private int totalCreated;

    @Schema(description = "Total duplicate records skipped", example = "2")
    private int totalSkipped;

    @Schema(description = "Total records failed validation", example = "0")
    private int totalFailed;

    @Builder.Default
    @Schema(description = "List of successfully imported item descriptions")
    private List<String> importedItems = new ArrayList<>();

    @Builder.Default
    @Schema(description = "List of warning or error messages")
    private List<String> errors = new ArrayList<>();
}
