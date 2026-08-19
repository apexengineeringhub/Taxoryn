package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.entity.GstProfileEntity.FilingFrequency;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstProfileStatus;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Register / Create GST Profile Request Payload")
public class CreateGstProfileRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "Client ID to associate GST profile with", example = "d1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID clientId;

    @NotBlank(message = "GSTIN is required")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format (expected 15-character valid GSTIN)")
    @Schema(description = "15-digit GSTIN", example = "27AAACZ1234D1Z8")
    private String gstin;

    @Size(max = 255, message = "Legal name cannot exceed 255 characters")
    @Schema(description = "Legal registered name", example = "ABC Traders Private Limited")
    private String legalName;

    @Size(max = 255, message = "Trade name cannot exceed 255 characters")
    @Schema(description = "Trade name / Brand name", example = "ABC Traders")
    private String tradeName;

    @NotNull(message = "GST type is required")
    @Schema(description = "GST scheme / type", example = "REGULAR")
    private GstType gstType;

    @NotNull(message = "Filing frequency is required")
    @Schema(description = "Filing frequency", example = "MONTHLY")
    private FilingFrequency filingFrequency;

    @Schema(description = "Registration effective date", example = "2020-07-01")
    private LocalDate registrationDate;

    @Schema(description = "State code", example = "27")
    private String stateCode;

    @Schema(description = "Principal place of business address")
    private String principalPlaceOfBusiness;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Profile active status", defaultValue = "ACTIVE")
    private GstProfileStatus status;
}
