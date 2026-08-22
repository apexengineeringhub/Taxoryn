package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsProfileEntity.DeductorType;
import com.taxoryn.module.tds.entity.TdsProfileEntity.TdsProfileStatus;
import com.taxoryn.module.tds.entity.TdsProfileEntity.TracesStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to update an existing TAN Deductor Profile")
public class UpdateTdsProfileRequest {

    @Schema(description = "Constitution of Deductor", example = "COMPANY")
    private DeductorType deductorType;

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

    @Schema(description = "Profile Status", example = "ACTIVE")
    private TdsProfileStatus status;

    @Schema(description = "TRACES Portal Username")
    private String tracesUsername;

    @Schema(description = "TRACES Status", example = "REGISTERED_ACTIVE")
    private TracesStatus tracesStatus;
}
