package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "marketplace_practice_services", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mp_prac_profile_svc", columnNames = {"marketplace_profile_id", "tax_service_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeServiceEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "marketplace_profile_id", nullable = false)
    private UUID marketplaceProfileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marketplace_profile_id", insertable = false, updatable = false)
    private MarketplaceProfileEntity marketplaceProfile;

    @Column(name = "tax_service_id", nullable = false)
    private UUID taxServiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_service_id", insertable = false, updatable = false)
    private TaxServiceEntity taxService;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
