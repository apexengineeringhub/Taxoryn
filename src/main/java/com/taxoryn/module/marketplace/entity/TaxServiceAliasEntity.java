package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "marketplace_tax_service_aliases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxServiceAliasEntity extends BaseEntity {

    @Column(name = "tax_service_id", nullable = false)
    private UUID taxServiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_service_id", insertable = false, updatable = false)
    private TaxServiceEntity taxService;

    @Column(name = "alias", nullable = false)
    private String alias;

    @Column(name = "normalized_alias", nullable = false)
    private String normalizedAlias;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
