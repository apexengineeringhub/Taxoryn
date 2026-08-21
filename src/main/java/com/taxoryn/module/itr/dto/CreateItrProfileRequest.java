package com.taxoryn.module.itr.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ResidentialStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Create / Register ITR Profile Request Payload")
public class CreateItrProfileRequest {

    @Schema(description = "Client ID to associate ITR profile with (optional if PAN is provided)")
    private UUID clientId;

    @NotBlank(message = "PAN is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format (expected e.g. ABCDE1234F)")
    @JsonAlias({"clientPan", "panNumber"})
    @Schema(description = "Permanent Account Number (PAN)", example = "ABCPJ9876M")
    private String pan;

    @Schema(description = "Client or Taxpayer Display Name", example = "Zenith Infotech Pvt Ltd")
    private String displayName;

    @Schema(description = "Legal registered name")
    private String legalName;

    @JsonAlias({"category", "constitution", "entityType"})
    @Schema(description = "Taxpayer entity type", example = "INDIVIDUAL")
    @Builder.Default
    private TaxpayerType taxpayerType = TaxpayerType.INDIVIDUAL;

    @JsonAlias({"formType", "itrType", "itrForm", "defaultForm"})
    @Schema(description = "Default ITR form", example = "ITR_1")
    @Builder.Default
    private ItrType defaultItrType = ItrType.ITR_1;

    @JsonAlias({"residence", "residenceStatus"})
    @Schema(description = "Residential status", defaultValue = "RESIDENT")
    @Builder.Default
    private ResidentialStatus residentialStatus = ResidentialStatus.RESIDENT;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;
}
