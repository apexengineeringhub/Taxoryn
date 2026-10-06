package com.taxoryn.module.itr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Normalized taxpayer profile returned from Government Income Tax portal PAN verification")
public class ItrTaxpayerProfileDto {

    @Schema(description = "10-character statutory Permanent Account Number", example = "ABCDE1234F")
    private String pan;

    @Schema(description = "Legal registered name of taxpayer or business entity", example = "Apex Enterprise Solutions")
    private String taxpayerName;

    @Schema(description = "PAN operational status (e.g. ACTIVE, DEACTIVATED, INOPERATIVE)", example = "ACTIVE")
    private String status;

    @Schema(description = "Detailed PAN status (e.g. OPERATIVE, INOPERATIVE)", example = "OPERATIVE")
    private String panStatus;

    @Schema(description = "Taxpayer entity category (e.g. INDIVIDUAL, COMPANY, FIRM, HUF, TRUST)", example = "COMPANY")
    private String category;

    @Schema(description = "Status of Aadhaar-PAN linkage (e.g. LINKED, NOT_LINKED, NOT_APPLICABLE)", example = "LINKED")
    private String aadhaarSeedingStatus;

    @Schema(description = "Jurisdiction Assessing Officer charge details", example = "WARD 12(1), MUMBAI")
    private String jurisdictionAssessingOfficer;

    @Schema(description = "True if PAN was successfully verified and profile retrieved", example = "true")
    private boolean valid;

    @Schema(description = "True if PAN verification succeeded against provider", example = "true")
    private boolean verified;

    @Schema(description = "Provider transaction or acknowledgement reference", example = "ITD-PAN-ACK-8F32BA")
    private String providerReferenceId;

    @Schema(description = "Unique audit ID of the integration operation")
    private UUID operationId;

    @Schema(description = "Timestamp when verification was performed")
    private Instant verifiedAt;

    @Schema(description = "Normalized error code if verification failed", example = "NOT_FOUND")
    private String errorCode;

    @Schema(description = "Detailed error message if verification failed")
    private String errorMessage;

    @Schema(description = "Additional normalized provider response attributes")
    private Map<String, Object> rawData;
}
