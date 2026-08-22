package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsProfileEntity.DeductorType;
import com.taxoryn.module.tds.entity.TdsProfileEntity.TdsProfileStatus;
import com.taxoryn.module.tds.entity.TdsProfileEntity.TracesStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to register a TAN Deductor Profile")
public class CreateTdsProfileRequest {

    @Schema(description = "Client ID (optional, auto-resolved if not provided)")
    private UUID clientId;

    @Schema(description = "Client Display Name / Legal Name for Auto-Onboarding", example = "Acme Corporation Pvt Ltd")
    private String displayName;

    @Schema(description = "Client Legal Name", example = "Acme Corporation Private Limited")
    private String legalName;

    @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Client PAN must be a valid 10-character PAN")
    @Schema(description = "Client PAN", example = "AABCA1234K")
    private String pan;

    @NotBlank(message = "TAN is required")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{5}[A-Z]{1}$", message = "TAN must be a valid 10-character alphanumeric code (e.g., BLRP12345A)")
    @Schema(description = "Tax Deduction and Collection Account Number", example = "BLRP12345A", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tan;

    @Builder.Default
    @Schema(description = "Constitution of Deductor", example = "COMPANY")
    private DeductorType deductorType = DeductorType.COMPANY;

    @Schema(description = "Branch or Division Name")
    private String branchDivisionName;

    @Schema(description = "PAO Code (Government Deductors)")
    private String paCode;

    @Schema(description = "DDO Code (Government Deductors)")
    private String ddoCode;

    @Schema(description = "Ministry Name (Government Deductors)")
    private String ministryName;

    @Schema(description = "Responsible Person Name", example = "Rajesh Sharma")
    private String responsiblePersonName;

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$|^$", message = "Responsible Person PAN must be a valid 10-character PAN")
    @Schema(description = "Responsible Person PAN", example = "ABCPS1234F")
    private String responsiblePersonPan;

    @Schema(description = "Responsible Person Designation", example = "Director")
    private String responsiblePersonDesignation;

    @Schema(description = "Responsible Person Father's Name")
    private String responsiblePersonFatherName;

    @Schema(description = "Responsible Person Email")
    private String responsiblePersonEmail;

    @Schema(description = "Responsible Person Mobile")
    private String responsiblePersonMobile;

    @Schema(description = "Responsible Person Address")
    private String responsiblePersonAddress;

    @Schema(description = "Assigned Employee ID")
    private UUID assignedEmployeeId;

    @Builder.Default
    @Schema(description = "Status", example = "ACTIVE")
    private TdsProfileStatus status = TdsProfileStatus.ACTIVE;

    @Schema(description = "TRACES Portal Username")
    private String tracesUsername;

    @Builder.Default
    @Schema(description = "TRACES Status", example = "NOT_REGISTERED")
    private TracesStatus tracesStatus = TracesStatus.NOT_REGISTERED;
}
