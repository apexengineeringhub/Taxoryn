package com.taxoryn.module.billing.dto;

import com.taxoryn.module.billing.entity.TdsCreditEntity.TdsCreditSourceType;
import com.taxoryn.module.billing.entity.TdsCreditEntity.TdsCreditStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Inward TDS Withholding Credit Record")
public class TdsCreditDto {

    private UUID id;
    private UUID organizationId;

    @NotNull(message = "Client ID is required")
    private UUID clientId;
    private String clientName;

    @NotBlank(message = "Deductor PAN is required")
    private String deductorPan;
    private String deductorTan;
    private String deductorName;

    @Builder.Default
    private String sectionCode = "194J";

    @NotBlank(message = "Financial Year is required")
    private String financialYear;

    private String assessmentYear;
    private String quarter;

    @NotNull(message = "Gross amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Gross amount must be positive")
    private BigDecimal grossAmount;

    @NotNull(message = "TDS deducted is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "TDS deducted must be positive")
    private BigDecimal tdsDeducted;

    @Builder.Default
    private TdsCreditSourceType sourceType = TdsCreditSourceType.MANUAL_ENTRY;
    private String sourceDocumentRef;
    private String certificateNumber;
    private LocalDate certificateDate;

    @Builder.Default
    private TdsCreditStatus creditStatus = TdsCreditStatus.UNRECONCILED;
    private String notes;
}
