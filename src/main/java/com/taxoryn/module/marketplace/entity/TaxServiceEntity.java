package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "marketplace_tax_services")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxServiceEntity extends AuditableEntity {

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private TaxServiceCategoryEntity category;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "taxService", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TaxServiceAliasEntity> aliases = new ArrayList<>();
}
