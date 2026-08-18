package com.taxoryn.module.organization.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationEntity extends AuditableEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    @Builder.Default
    private String country = "India";

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Column(name = "pan", length = 10)
    private String pan;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "tax_registration_number", length = 50)
    private String taxRegistrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 50)
    @Builder.Default
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.STARTER;

    @OneToOne(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private OrganizationSettingsEntity settings;

    public enum OrganizationStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        DEACTIVATED
    }

    public enum SubscriptionPlan {
        STARTER,
        PROFESSIONAL,
        ENTERPRISE
    }
}
