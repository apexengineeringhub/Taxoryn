package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.Urgency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer inquiry submission payload from public marketplace")
public class CreateMarketplaceLeadRequest {

    @NotNull(message = "Marketplace profile ID is required")
    @Schema(description = "Target Firm / Practitioner Marketplace Profile ID")
    private UUID marketplaceProfileId;

    @Schema(description = "Selected service package ID (if inquiring from a package)")
    private UUID serviceId;

    @NotBlank(message = "Your name is required")
    @Schema(description = "Client full name", example = "Suresh Raina")
    private String clientName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Schema(description = "Client email address", example = "suresh@raina.com")
    private String clientEmail;

    @NotBlank(message = "Phone number is required")
    @Schema(description = "Contact phone number", example = "+919876543210")
    private String clientPhone;

    @Schema(description = "City", example = "Bengaluru")
    private String city;

    @Schema(description = "Client PAN (optional)")
    private String pan;

    @Schema(description = "Client GSTIN (optional)")
    private String gstin;

    @Schema(description = "Service category", example = "ITR")
    private String serviceCategory;

    @NotBlank(message = "Requirement description is required")
    @Schema(description = "Tax or compliance requirement description", example = "Need consultation on capital gains from real estate sale and foreign equity stock vesting.")
    private String requirementDescription;

    @Schema(description = "Expected budget range", example = "₹2,000 - ₹5,000")
    private String budgetRange;

    @Builder.Default
    @Schema(description = "Urgency level", example = "STANDARD")
    private Urgency urgency = Urgency.STANDARD;
}
