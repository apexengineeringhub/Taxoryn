package com.taxoryn.module.client.dto;

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
@Schema(description = "Bulk Client Migration Import Result")
public class BulkImportResultDto {

    @Schema(description = "Total records in batch", example = "50")
    private int totalProcessed;

    @Schema(description = "Successfully imported clients", example = "48")
    private int totalSuccess;

    @Schema(description = "Failed client records", example = "2")
    private int totalFailed;

    @Schema(description = "Skipped duplicate clients", example = "0")
    private int totalSkipped;

    @Schema(description = "List of successfully imported client DTOs")
    @Builder.Default
    private List<ClientDto> importedClients = new ArrayList<>();

    @Schema(description = "Granular row-level import failure logs")
    @Builder.Default
    private List<BulkImportError> errors = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkImportError {
        private int rowNumber;
        private String clientName;
        private String pan;
        private String reason;
    }
}
