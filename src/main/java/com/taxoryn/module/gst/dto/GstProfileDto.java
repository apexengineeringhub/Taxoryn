package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.entity.GstProfileEntity.FilingFrequency;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstProfileStatus;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GST Profile & Registration Details")
public class GstProfileDto {

    @Schema(description = "GST Profile ID")
    private UUID id;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Client display name", example = "ABC Traders")
    private String clientName;

    @Schema(description = "GSTIN", example = "27AAACZ1234D1Z8")
    private String gstin;

    @Schema(description = "Legal registered business name", example = "ABC Traders Private Limited")
    private String legalName;

    @Schema(description = "Trade name / Brand name", example = "ABC Traders")
    private String tradeName;

    @Schema(description = "GST registration type", example = "REGULAR")
    private GstType gstType;

    @Schema(description = "Filing frequency", example = "MONTHLY")
    private FilingFrequency filingFrequency;

    @Schema(description = "GST registration effective date", example = "2020-07-01")
    private LocalDate registrationDate;

    @Schema(description = "State code (first 2 digits of GSTIN)", example = "27")
    private String stateCode;

    @Schema(description = "Principal place of business address")
    private String principalPlaceOfBusiness;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned practitioner full name", example = "Rahul Sharma")
    private String assignedEmployeeName;

    @Schema(description = "Profile active status", example = "ACTIVE")
    private GstProfileStatus status;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
