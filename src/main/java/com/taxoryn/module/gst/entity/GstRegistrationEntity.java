package com.taxoryn.module.gst.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.gst.model.GstFilingFrequency;
import com.taxoryn.module.gst.model.GstRegistrationStatus;
import com.taxoryn.module.gst.model.GstRegistrationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Concrete domain model for a Client GSTIN Registration within a tenant.
 */
@Entity
@Table(
        name = "gst_registrations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_gst_registrations_org_gstin", columnNames = {"organization_id", "gstin"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GstRegistrationEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "gstin", nullable = false, length = 15)
    private String gstin;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", nullable = false, length = 50)
    @Builder.Default
    private GstRegistrationType registrationType = GstRegistrationType.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 50)
    @Builder.Default
    private GstRegistrationStatus registrationStatus = GstRegistrationStatus.ACTIVE;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "state_code", length = 10)
    private String stateCode;

    @Column(name = "jurisdiction")
    private String jurisdiction;

    @Enumerated(EnumType.STRING)
    @Column(name = "filing_frequency", nullable = false, length = 50)
    @Builder.Default
    private GstFilingFrequency filingFrequency = GstFilingFrequency.MONTHLY;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
