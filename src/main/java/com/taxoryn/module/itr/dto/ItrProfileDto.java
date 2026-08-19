package com.taxoryn.module.itr.dto;

import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrProfileStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ResidentialStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
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
@Schema(description = "ITR Client Profile Details")
public class ItrProfileDto {

    @Schema(description = "ITR Profile ID")
    private UUID id;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Client display name", example = "Anand Ramesh Joshi")
    private String clientName;

    @Schema(description = "Permanent Account Number (PAN)", example = "ABCPJ9876M")
    private String pan;

    @Schema(description = "Taxpayer entity type", example = "INDIVIDUAL")
    private TaxpayerType taxpayerType;

    @Schema(description = "Default ITR form type", example = "ITR_1")
    private ItrType defaultItrType;

    @Schema(description = "Residential status under Income Tax Act", example = "RESIDENT")
    private ResidentialStatus residentialStatus;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned practitioner name", example = "Vikram Sharma")
    private String assignedEmployeeName;

    @Schema(description = "Profile active status", example = "ACTIVE")
    private ItrProfileStatus status;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Updated timestamp")
    private Instant updatedAt;
}
