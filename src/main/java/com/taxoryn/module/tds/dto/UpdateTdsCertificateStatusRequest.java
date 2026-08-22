package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsCertificateEntity.DispatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update certificate dispatch status")
public class UpdateTdsCertificateStatusRequest {

    @NotNull(message = "Dispatch status is required")
    @Schema(description = "Dispatch status", example = "SENT_TO_CLIENT", requiredMode = Schema.RequiredMode.REQUIRED)
    private DispatchStatus dispatchStatus;

    @Schema(description = "Certificate Serial Number")
    private String certificateNumber;

    @Schema(description = "Notes")
    private String notes;
}
