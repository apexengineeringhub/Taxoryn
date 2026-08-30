package com.taxoryn.module.itr.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
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
@Table(name = "itr_returns")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItrReturnEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "itr_profile_id")
    private UUID itrProfileId;

    @Column(name = "assessment_year", nullable = false, length = 20)
    private String assessmentYear;

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "itr_type", nullable = false, length = 50)
    private ItrType itrType;

    @Enumerated(EnumType.STRING)
    @Column(name = "taxpayer_type", nullable = false, length = 50)
    private TaxpayerType taxpayerType;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "acknowledgement_number", length = 100)
    private String acknowledgementNumber;

    @Column(name = "verification_date")
    private LocalDate verificationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ItrStatus status = ItrStatus.DOCUMENTS_PENDING;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "compliance_id")
    private UUID complianceId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "document_request_id")
    private UUID documentRequestId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum ItrStatus {
        DOCUMENTS_PENDING,
        DATA_ENTRY,
        UNDER_REVIEW,
        READY_TO_FILE,
        FILED,
        VERIFICATION_PENDING,
        COMPLETED,
        CANCELLED
    }
}
