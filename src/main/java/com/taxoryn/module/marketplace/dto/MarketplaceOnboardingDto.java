package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.marketplace.entity.MarketplaceOnboardingEntity.OnboardingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer Onboarding & KYC Pipeline Record DTO")
public class MarketplaceOnboardingDto {

    private UUID id;
    private UUID organizationId;
    private String practiceDisplayName;
    private UUID marketplaceProfileId;
    private UUID leadId;
    private UUID proposalId;
    private String proposalTitle;
    private String accessToken;
    private String clientName;
    private String legalName;
    private String clientEmail;
    private String clientPhone;
    private ClientType entityType;
    private String pan;
    private String gstin;
    private String tan;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private OnboardingStatus onboardingStatus;
    private Boolean engagementLetterSigned;
    private Instant engagementSignedAt;
    private String engagementLetterUrl;
    private Boolean feeAgreementAgreed;
    private UUID assignedEmployeeId;
    private String assignedEmployeeName;
    private UUID promotedClientId;
    private UUID portalUserId;
    private String reviewerNotes;
    private String rejectionReason;
    private Instant completedAt;
    private Instant createdAt;
    private List<OnboardingDocumentDto> documents;
}
