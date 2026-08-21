package com.taxoryn.module.gst.dto;

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
@Schema(description = "Bulk GST Data Migration Result")
public class BulkGstImportResultDto {

    @Schema(description = "Total records attempted", example = "10")
    private int totalProcessed;

    @Schema(description = "Successfully imported records", example = "10")
    private int totalCreated;

    @Schema(description = "Skipped duplicate records", example = "0")
    private int totalSkipped;

    @Schema(description = "Failed records", example = "0")
    private int totalFailed;

    @Schema(description = "Imported item descriptions / IDs")
    @Builder.Default
    private List<String> importedItems = new ArrayList<>();

    @Schema(description = "Error messages if any")
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
