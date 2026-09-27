package com.taxoryn.module.itr.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
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

import java.util.UUID;

@Entity
@Table(name = "itr_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItrProfileEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "pan", nullable = false, length = 10)
    private String pan;

    @Enumerated(EnumType.STRING)
    @Column(name = "taxpayer_type", nullable = false, length = 50)
    @Builder.Default
    private TaxpayerType taxpayerType = TaxpayerType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_itr_type", nullable = false, length = 50)
    @Builder.Default
    private ItrType defaultItrType = ItrType.ITR_1;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_return_type", length = 50)
    private ItrType applicableReturnType;

    @Column(name = "default_assessment_year", length = 20)
    private String defaultAssessmentYear;

    @Column(name = "assessment_category", length = 50)
    private String assessmentCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "residential_status", nullable = false, length = 50)
    @Builder.Default
    private ResidentialStatus residentialStatus = ResidentialStatus.RESIDENT;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ItrProfileStatus status = ItrProfileStatus.ACTIVE;

    public ItrType getEffectiveReturnType() {
        if (applicableReturnType != null) {
            return applicableReturnType;
        }
        return defaultItrType != null ? defaultItrType : ItrType.ITR_1;
    }

    public enum TaxpayerType {
        INDIVIDUAL,
        HUF,
        FIRM,
        LLP,
        COMPANY,
        TRUST,
        AOP,
        BOI,
        AOP_BOI,
        OTHER
    }

    public enum ItrType {
        ITR_1,
        ITR_2,
        ITR_3,
        ITR_4,
        ITR_5,
        ITR_6,
        ITR_7,
        OTHER
    }

    public enum ResidentialStatus {
        RESIDENT,
        NON_RESIDENT,
        RNOR,
        RESIDENT_NOT_ORDINARILY_RESIDENT
    }

    public enum ItrProfileStatus {
        ACTIVE,
        INACTIVE
    }
}
