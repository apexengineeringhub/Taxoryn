package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.CustomerTaxpayerType;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.LeadStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.Urgency;
import com.taxoryn.module.marketplace.entity.PrivacyDataLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Level 2: Early Enquiry / Inquiry View DTO.
 * Enforces Minimum Necessary Disclosure for early marketplace discovery and inquiry triage.
 * Sensitive Level 3 (PAN, Aadhaar, detailed income/salary, bank accounts) and Level 4 (tax documents)
 * are strictly omitted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Level 2 Minimum Disclosure Early Enquiry View DTO for Practices")
public class EarlyEnquiryViewDto {

    @Schema(description = "Enquiry Reference ID")
    private UUID id;

    @Schema(description = "Practice Organization ID")
    private UUID organizationId;

    @Schema(description = "Target Marketplace Profile ID")
    private UUID marketplaceProfileId;

    @Schema(description = "Requested Tax Service (Controlled Master)")
    private PublicTaxServiceDto service;

    @Schema(description = "Service Category", example = "Direct Tax")
    private String serviceCategory;

    @Schema(description = "Financial Year (Canonical)", example = "2025-26")
    private String financialYear;

    @Schema(description = "Financial Year Display Label", example = "FY 2025-26")
    private String financialYearDisplay;

    @Schema(description = "Broad Taxpayer Classification", example = "SALARIED")
    private CustomerTaxpayerType customerType;

    @Schema(description = "Taxpayer Classification Label", example = "Salaried Individual")
    private String customerTypeDisplayName;

    @Schema(description = "Sanitized Early Enquiry Summary (Level 2 Disclosure)")
    private String requirementSummary;

    @Schema(description = "Client Display / Reference Name", example = "Rahul S.")
    private String clientName;

    @Schema(description = "Privacy Masked Contact Email", example = "r***a@gmail.com")
    private String maskedEmail;

    @Schema(description = "Privacy Masked Contact Phone", example = "+91******3210")
    private String maskedPhone;

    @Schema(description = "Approximate City", example = "Bengaluru")
    private String city;

    @Schema(description = "Approximate State", example = "Karnataka")
    private String state;

    @Schema(description = "Budget Range", example = "₹2,000 - ₹5,000")
    private String budgetRange;

    @Schema(description = "Inquiry Urgency")
    private Urgency urgency;

    @Schema(description = "Inquiry / Lead Pipeline Status")
    private LeadStatus leadStatus;

    @Schema(description = "Assigned Employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned Employee Name")
    private String assignedEmployeeName;

    @Schema(description = "Practice Internal Notes")
    private String practitionerNotes;

    @Schema(description = "Data Classification Level")
    @Builder.Default
    private PrivacyDataLevel privacyLevel = PrivacyDataLevel.LEVEL_2_EARLY_ENQUIRY;

    @Schema(description = "Inquiry Creation Timestamp")
    private Instant createdAt;
}
