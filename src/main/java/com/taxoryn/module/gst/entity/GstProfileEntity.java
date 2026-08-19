package com.taxoryn.module.gst.entity;

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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "gst_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GstProfileEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "gstin", nullable = false, length = 15)
    private String gstin;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gst_type", nullable = false, length = 50)
    @Builder.Default
    private GstType gstType = GstType.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "filing_frequency", nullable = false, length = 50)
    @Builder.Default
    private FilingFrequency filingFrequency = FilingFrequency.MONTHLY;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "state_code", length = 10)
    private String stateCode;

    @Column(name = "principal_place_of_business", columnDefinition = "TEXT")
    private String principalPlaceOfBusiness;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private GstProfileStatus status = GstProfileStatus.ACTIVE;

    public enum GstType {
        REGULAR,
        COMPOSITION,
        QRMP,
        CASUAL,
        NON_RESIDENT,
        ISD,
        TDS_DEDUCTOR,
        TCS_COLLECTOR,
        OVERSEAS_OIDAR
    }

    public enum FilingFrequency {
        MONTHLY,
        QUARTERLY,
        ANNUALLY
    }

    public enum GstProfileStatus {
        ACTIVE,
        SUSPENDED,
        CANCELLED,
        SURRENDERED
    }
}
