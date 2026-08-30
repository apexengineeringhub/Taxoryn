package com.taxoryn.module.docrequest.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "document_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequestEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "request_number", nullable = false, length = 50, unique = true)
    private String requestNumber;

    @Column(name = "purpose", nullable = false)
    private String purpose;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private RequestStatus status = RequestStatus.SENT;

    @Column(name = "financial_year", length = 20)
    private String financialYear;

    @Column(name = "assessment_year", length = 20)
    private String assessmentYear;

    @Column(name = "requested_by_user_id")
    private UUID requestedByUserId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "compliance_id")
    private UUID complianceId;

    @Column(name = "gst_filing_id")
    private UUID gstFilingId;

    @Column(name = "itr_return_id")
    private UUID itrReturnId;

    @Column(name = "tds_return_id")
    private UUID tdsReturnId;

    @Column(name = "sent_at")
    @Builder.Default
    private Instant sentAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<DocumentRequestItemEntity> items = new ArrayList<>();

    public enum RequestStatus {
        DRAFT,
        SENT,
        PARTIALLY_COMPLETED,
        COMPLETED,
        CANCELLED,
        OVERDUE
    }
}