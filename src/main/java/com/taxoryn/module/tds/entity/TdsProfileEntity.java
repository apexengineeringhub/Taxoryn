package com.taxoryn.module.tds.entity;

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
@Table(name = "tds_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TdsProfileEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "tan", nullable = false, length = 10)
    private String tan;

    @Enumerated(EnumType.STRING)
    @Column(name = "deductor_type", nullable = false, length = 50)
    @Builder.Default
    private DeductorType deductorType = DeductorType.COMPANY;

    @Column(name = "branch_division_name")
    private String branchDivisionName;

    @Column(name = "pa_code", length = 50)
    private String paCode;

    @Column(name = "ddo_code", length = 50)
    private String ddoCode;

    @Column(name = "ministry_name")
    private String ministryName;

    @Column(name = "responsible_person_name")
    private String responsiblePersonName;

    @Column(name = "responsible_person_pan", length = 10)
    private String responsiblePersonPan;

    @Column(name = "responsible_person_designation", length = 100)
    private String responsiblePersonDesignation;

    @Column(name = "responsible_person_father_name")
    private String responsiblePersonFatherName;

    @Column(name = "responsible_person_email")
    private String responsiblePersonEmail;

    @Column(name = "responsible_person_mobile", length = 20)
    private String responsiblePersonMobile;

    @Column(name = "responsible_person_address", columnDefinition = "TEXT")
    private String responsiblePersonAddress;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private TdsProfileStatus status = TdsProfileStatus.ACTIVE;

    @Column(name = "traces_username", length = 100)
    private String tracesUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "traces_status", nullable = false, length = 50)
    @Builder.Default
    private TracesStatus tracesStatus = TracesStatus.NOT_REGISTERED;

    public enum DeductorType {
        COMPANY,
        INDIVIDUAL_HUF,
        FIRM,
        LLP,
        BRANCH_DIVISION,
        GOVERNMENT_CENTRAL,
        GOVERNMENT_STATE,
        STATUTORY_BODY,
        AUTONOMOUS_BODY,
        OTHER
    }

    public enum TdsProfileStatus {
        ACTIVE,
        INACTIVE,
        SURRENDERED
    }

    public enum TracesStatus {
        NOT_REGISTERED,
        REGISTERED_ACTIVE,
        PASSWORD_EXPIRED,
        SUSPENDED
    }
}
