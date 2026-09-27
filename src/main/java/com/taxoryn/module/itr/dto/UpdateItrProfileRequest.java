package com.taxoryn.module.itr.dto;

import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrProfileStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ResidentialStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
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
@Schema(description = "Update ITR Profile Request Payload")
public class UpdateItrProfileRequest {

    @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format (expected e.g. ABCDE1234F)")
    @Schema(description = "Permanent Account Number (PAN)", example = "ABCPJ9876M")
    private String pan;

    @Schema(description = "Location ID")
    private UUID locationId;

    @Schema(description = "Taxpayer entity type", example = "INDIVIDUAL")
    private TaxpayerType taxpayerType;

    @Schema(description = "Default ITR form", example = "ITR_1")
    private ItrType defaultItrType;

    @Schema(description = "Applicable return form", example = "ITR_1")
    private ItrType applicableReturnType;

    @Schema(description = "Default Assessment Year", example = "2026-27")
    private String defaultAssessmentYear;

    @Schema(description = "Assessment category/type", example = "REGULAR")
    private String assessmentCategory;

    @Schema(description = "Residential status", example = "RESIDENT")
    private ResidentialStatus residentialStatus;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Active flag")
    private Boolean active;

    @Schema(description = "Profile active status")
    private ItrProfileStatus status;
}
