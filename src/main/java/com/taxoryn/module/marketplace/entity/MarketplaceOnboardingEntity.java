package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.AuditableEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_onboardings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceOnboardingEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "marketplace_profile_id", nullable = false)
    private UUID marketplaceProfileId;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "proposal_id")
    private UUID proposalId;

    @Column(name = "access_token", nullable = false, unique = true, length = 100)
    private String accessToken;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "client_email", nullable = false)
    private String clientEmail;

    @Column(name = "client_phone", nullable = false, length = 20)
    private String clientPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    @Builder.Default
    private ClientType entityType = ClientType.INDIVIDUAL;

    @Column(name = "pan", length = 10)
    private String pan;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "tan", length = 10)
    private String tan;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 50)
    @Builder.Default
    private OnboardingStatus onboardingStatus = OnboardingStatus.INITIATED;

    @Column(name = "engagement_letter_signed", nullable = false)
    @Builder.Default
    private Boolean engagementLetterSigned = false;

    @Column(name = "engagement_signed_at")
    private Instant engagementSignedAt;

    @Column(name = "engagement_letter_url", columnDefinition = "TEXT")
    private String engagementLetterUrl;

    @Column(name = "fee_agreement_agreed", nullable = false)
    @Builder.Default
    private Boolean feeAgreementAgreed = false;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "promoted_client_id")
    private UUID promotedClientId; // NULL until final approval promotes to Client Master!

    @Column(name = "portal_user_id")
    private UUID portalUserId; // NULL until Client Portal is provisioned!

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "completed_at")
    private Instant completedAt;

    public enum OnboardingStatus {
        INITIATED,
        DOCUMENTS_PENDING,
        UNDER_REVIEW,
        APPROVED,
        REJECTED
    }
}
