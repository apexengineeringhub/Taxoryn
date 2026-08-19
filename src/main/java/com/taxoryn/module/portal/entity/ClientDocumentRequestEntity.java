package com.taxoryn.module.portal.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
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
@Table(name = "client_document_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDocumentRequestEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "financial_year", length = 20)
    private String financialYear;

    @Column(name = "assessment_year", length = 20)
    private String assessmentYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "uploaded_document_id")
    private UUID uploadedDocumentId;

    public enum RequestStatus {
        PENDING,
        SUBMITTED,
        VERIFIED,
        REJECTED,
        CANCELLED
    }
}
