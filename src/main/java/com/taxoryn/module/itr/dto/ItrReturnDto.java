package com.taxoryn.module.itr.dto;

import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
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
@Schema(description = "ITR Return Filing Record Details")
public class ItrReturnDto {

    @Schema(description = "Return Record ID")
    private UUID id;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Client display name", example = "Anand Ramesh Joshi")
    private String clientName;

    @Schema(description = "Client PAN", example = "ABCPJ9876M")
    private String pan;

    @Schema(description = "ITR Profile ID")
    private UUID itrProfileId;

    @Schema(description = "Assessment Year (AY)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Financial Year (FY)", example = "2025-26")
    private String financialYear;

    @Schema(description = "ITR Form Type (ITR_1 to ITR_7)", example = "ITR_1")
    private ItrType itrType;

    @Schema(description = "Taxpayer Category / Legal Constitution", example = "INDIVIDUAL")
    private TaxpayerType taxpayerType;

    @Schema(description = "Statutory due date for filing", example = "2026-07-31")
    private LocalDate dueDate;

    @Schema(description = "Actual date of filing", example = "2026-07-28")
    private LocalDate filingDate;

    @Schema(description = "Income Tax e-Filing Acknowledgement Number / ITR-V Ack", example = "123456789012345")
    private String acknowledgementNumber;

    @Schema(description = "Date of e-Verification / Aadhaar OTP verification", example = "2026-07-28")
    private LocalDate verificationDate;

    @Schema(description = "Current return workflow lifecycle status", example = "DOCUMENTS_PENDING")
    private ItrStatus status;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned practitioner full name", example = "Vikram Sharma")
    private String assignedEmployeeName;

    @Schema(description = "Practitioner internal notes & remarks")
    private String notes;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Updated timestamp")
    private Instant updatedAt;
}
