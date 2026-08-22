package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsProfileEntity.DeductorType;
import com.taxoryn.module.tds.entity.TdsProfileEntity.TdsProfileStatus;
import com.taxoryn.module.tds.entity.TdsProfileEntity.TracesStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "TDS Deductor Profile Details")
public class TdsProfileDto {

    @Schema(description = "Profile ID")
    private UUID id;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Client Display Name")
    private String clientName;

    @Schema(description = "Tax Deduction and Collection Account Number (TAN)", example = "BLRP12345A")
    private String tan;

    @Schema(description = "Constitution of Deductor", example = "COMPANY")
    private DeductorType deductorType;

    @Schema(description = "Branch or Division Name")
    private String branchDivisionName;

    @Schema(description = "PAO Code (for Government Deductors)")
    private String paCode;

    @Schema(description = "DDO Code (for Government Deductors)")
    private String ddoCode;

    @Schema(description = "Ministry Name (for Government Deductors)")
    private String ministryName;

    @Schema(description = "Principal Officer / Responsible Person Name", example = "Rajesh Sharma")
    private String responsiblePersonName;

    @Schema(description = "Responsible Person PAN", example = "ABCPS1234F")
    private String responsiblePersonPan;

    @Schema(description = "Responsible Person Designation", example = "Director - Finance")
    private String responsiblePersonDesignation;

    @Schema(description = "Responsible Person Father's Name")
    private String responsiblePersonFatherName;

    @Schema(description = "Responsible Person Email")
    private String responsiblePersonEmail;

    @Schema(description = "Responsible Person Mobile")
    private String responsiblePersonMobile;

    @Schema(description = "Responsible Person Address")
    private String responsiblePersonAddress;

    @Schema(description = "Assigned Employee / Tax Professional ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned Employee Full Name")
    private String assignedEmployeeName;

    @Schema(description = "TDS Profile Status", example = "ACTIVE")
    private TdsProfileStatus status;

    @Schema(description = "TRACES Portal Username")
    private String tracesUsername;

    @Schema(description = "TRACES Registration Status", example = "REGISTERED_ACTIVE")
    private TracesStatus tracesStatus;

    @Schema(description = "Created Timestamp")
    private Instant createdAt;

    @Schema(description = "Updated Timestamp")
    private Instant updatedAt;
}
