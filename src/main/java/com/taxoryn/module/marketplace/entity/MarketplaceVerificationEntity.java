package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_verifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceVerificationEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "marketplace_profile_id", nullable = false)
    private UUID marketplaceProfileId;

    @Column(name = "professional_body", nullable = false, length = 100)
    private String professionalBody;

    @Column(name = "membership_number", nullable = false, length = 100)
    private String membershipNumber;

    @Column(name = "cop_number", length = 100)
    private String copNumber;

    @Column(name = "firm_registration_number", length = 100)
    private String firmRegistrationNumber;

    @Column(name = "document_url", columnDefinition = "TEXT")
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    @Builder.Default
    private MarketplaceProfileEntity.VerificationStatus verificationStatus = MarketplaceProfileEntity.VerificationStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by")
    private String verifiedBy;
}
