package com.taxoryn.module.tds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Normalized Deductor Profile DTO retrieved from Government TRACES / TDS gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Normalized TDS Deductor Profile")
public class TdsDeductorProfileDto {

    @Schema(description = "10-character Tax Deduction and Collection Account Number (TAN)", example = "MUMB12345A")
    private String tan;

    @Schema(description = "Official legal name of deductor registered on TRACES", example = "ACME ENTERPRISES PRIVATE LIMITED")
    private String deductorName;

    @Schema(description = "Deductor category / constitution", example = "COMPANY")
    private String category;

    @Schema(description = "Operational status of the TAN profile", example = "ACTIVE")
    private String status;

    @Schema(description = "TAN validity status", example = "VALID")
    private String tanStatus;

    @Schema(description = "TRACES registration status", example = "REGISTERED_ACTIVE")
    private String tracesStatus;

    @Schema(description = "Entity PAN associated with the TAN", example = "AAACA1234C")
    private String pan;

    @Schema(description = "Registered state of the deductor", example = "MAHARASHTRA")
    private String state;

    @Schema(description = "Registered PIN code", example = "400001")
    private String pinCode;

    @Schema(description = "Registered office address")
    private String address;

    @Schema(description = "Whether the TAN is statutory valid")
    private boolean valid;

    @Schema(description = "Whether the deductor profile is officially verified against TRACES portal")
    private boolean verified;

    @Schema(description = "Provider transaction / verification reference", example = "TRACES-VERIFY_TAN-ACK-9F8A2B")
    private String providerReferenceId;

    @Schema(description = "Audit ID of the underlying government operation")
    private UUID operationId;

    @Schema(description = "Timestamp when verification was performed")
    private Instant verifiedAt;

    @Schema(description = "Error code if verification failed")
    private String errorCode;

    @Schema(description = "Error message if verification failed")
    private String errorMessage;

    @Schema(description = "Raw normalized response data from provider")
    @Builder.Default
    private Map<String, Object> rawData = new HashMap<>();
}
