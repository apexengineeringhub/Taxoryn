package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "marketplace_proposals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceProposalEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "marketplace_profile_id", nullable = false)
    private UUID marketplaceProfileId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "proposal_title", nullable = false)
    private String proposalTitle;

    @Column(name = "scope_of_work", nullable = false, columnDefinition = "TEXT")
    private String scopeOfWork;

    @Column(name = "deliverables", columnDefinition = "TEXT")
    private String deliverables;

    @Column(name = "fee_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 50)
    @Builder.Default
    private MarketplaceServiceEntity.PricingType pricingType = MarketplaceServiceEntity.PricingType.FIXED;

    @Column(name = "estimated_timeline_days")
    @Builder.Default
    private Integer estimatedTimelineDays = 7;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposal_status", nullable = false, length = 50)
    @Builder.Default
    private ProposalStatus proposalStatus = ProposalStatus.SENT;

    @Column(name = "access_token", nullable = false, unique = true, length = 100)
    private String accessToken;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    public enum ProposalStatus {
        DRAFT,
        SENT,
        ACCEPTED,
        REJECTED,
        EXPIRED
    }
}
