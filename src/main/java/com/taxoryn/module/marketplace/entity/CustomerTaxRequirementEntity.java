package com.taxoryn.module.marketplace.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "customer_tax_requirements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTaxRequirementEntity extends AuditableEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private MarketplaceCustomerProfileEntity customerProfile;

    @Column(name = "tax_service_id", nullable = false)
    private UUID taxServiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_service_id", insertable = false, updatable = false)
    private TaxServiceEntity taxService;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private TaxRequirementStatus status = TaxRequirementStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", length = 50)
    private CustomerTaxpayerType customerType;

    @Column(name = "financial_year", length = 20)
    private String financialYear;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Column(name = "search_radius_km")
    private Integer searchRadiusKm;
}
