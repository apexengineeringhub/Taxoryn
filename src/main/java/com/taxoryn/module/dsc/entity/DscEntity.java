package com.taxoryn.module.dsc.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.dsc.model.DscCertificateType;
import com.taxoryn.module.dsc.model.DscStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Entity representing a Digital Signature Certificate (DSC) entry in the practice register.
 * Stores strictly public metadata; never stores private keys, PINs, or credentials.
 */
@Entity
@Table(name = "dsc_register")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DscEntity extends TenantAuditableEntity {

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "holder_name", nullable = false)
    private String holderName;

    @Column(name = "certificate_identifier")
    private String certificateIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", nullable = false, length = 50)
    @Builder.Default
    private DscCertificateType certificateType = DscCertificateType.CLASS_3;

    @Column(name = "issuer")
    private String issuer;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private DscStatus status = DscStatus.ACTIVE;

    @Column(name = "applicable_services", length = 500)
    private String applicableServices;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Resolves the effective status considering the expiry date and threshold.
     * If explicitly REVOKED or INACTIVE, that status is preserved.
     * Otherwise, derived based on today's date:
     * - expiryDate < today => EXPIRED
     * - expiryDate <= today + 30 days => EXPIRING
     * - else => ACTIVE
     */
    public DscStatus getEffectiveStatus(LocalDate today) {
        if (this.status == DscStatus.REVOKED || this.status == DscStatus.INACTIVE) {
            return this.status;
        }
        if (this.expiryDate == null) {
            return this.status;
        }
        if (this.expiryDate.isBefore(today)) {
            return DscStatus.EXPIRED;
        }
        if (!this.expiryDate.isAfter(today.plusDays(30))) {
            return DscStatus.EXPIRING;
        }
        return DscStatus.ACTIVE;
    }

    /**
     * Calculates days until expiry relative to given date.
     */
    public long getDaysUntilExpiry(LocalDate today) {
        if (this.expiryDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(today, this.expiryDate);
    }
}
