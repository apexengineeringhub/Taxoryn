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

    @Column(name = "notice_id")
    private UUID noticeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_type", nullable = false, length = 50)
    @Builder.Default
    private ExchangeType exchangeType = ExchangeType.DOCUMENT_REQUEST;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 50)
    @Builder.Default
    private RequestDirection direction = RequestDirection.PRACTITIONER_TO_CLIENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private DocumentCategory category;

    @Column(name = "tax_period", length = 50)
    private String taxPeriod;

    @Column(name = "delivered_document_id")
    private UUID deliveredDocumentId;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "declined_at")
    private Instant declinedAt;

    @Column(name = "decline_reason", columnDefinition = "TEXT")
    private String declineReason;

    @Column(name = "sent_at")
    @Builder.Default
    private Instant sentAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<DocumentRequestItemEntity> items = new ArrayList<>();

    public enum ExchangeType {
        DOCUMENT_REQUEST,
        ACKNOWLEDGEMENT_REQUEST,
        DOCUMENT_DELIVERY
    }

    public enum RequestDirection {
        PRACTITIONER_TO_CLIENT,
        CLIENT_TO_PRACTITIONER
    }

    public enum DocumentCategory {
        ACKNOWLEDGEMENT,
        RETURN_COPY,
        TAX_DOCUMENT,
        CERTIFICATE,
        OTHER
    }

    public enum RequestStatus {
        DRAFT,
        REQUESTED,
        IN_REVIEW,
        SENT,
        PARTIALLY_COMPLETED,
        COMPLETED,
        VIEWED,
        DOWNLOADED,
        CANCELLED,
        DECLINED,
        OVERDUE
    }
}