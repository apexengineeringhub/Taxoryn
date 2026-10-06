package com.taxoryn.module.gst.dto;

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
@Schema(description = "Normalized taxpayer registration profile returned from Government GST portal verification")
public class GstTaxpayerProfileDto {

    @Schema(description = "15-character statutory GSTIN", example = "27AAAPL1234C1ZV")
    private String gstin;

    @Schema(description = "Legal registered name of business entity", example = "Apex Enterprises Private Limited")
    private String legalName;

    @Schema(description = "Trade or brand name", example = "Apex Solutions")
    private String tradeName;

    @Schema(description = "Registration status on GST portal (ACTIVE, CANCELLED, SUSPENDED, INACTIVE)", example = "ACTIVE")
    private String status;

    @Schema(description = "Date of initial GST registration", example = "2017-07-01")
    private String registrationDate;

    @Schema(description = "Taxpayer registration category (REGULAR, COMPOSITION)", example = "REGULAR")
    private String registrationType;

    @Schema(description = "2-digit state code prefix", example = "27")
    private String stateCode;

    @Schema(description = "Central tax administrative jurisdiction", example = "COMMISSIONERATE MUMBAI WEST, DIVISION IV, RANGE II")
    private String centerJurisdiction;

    @Schema(description = "State tax administrative jurisdiction", example = "MAHARASHTRA, WARD 101")
    private String stateJurisdiction;

    @Schema(description = "Constitution of business entity (e.g. Private Limited Company, Proprietorship)", example = "Private Limited Company")
    private String constitutionOfBusiness;

    @Schema(description = "Taxpayer classification type", example = "Taxpayer")
    private String taxpayerType;

    @Schema(description = "Principal place of business address", example = "Plot No 42, Bandra Kurla Complex, Mumbai, Maharashtra, 400051")
    private String address;

    @Schema(description = "Last update timestamp from GST portal", example = "2026-01-15")
    private String lastUpdatedDate;

    @Schema(description = "True if GSTIN was successfully verified and profile retrieved", example = "true")
    private boolean valid;

    @Schema(description = "Provider transaction or acknowledgement reference", example = "ARN-GST-8F32BA")
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
