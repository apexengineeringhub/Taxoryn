package com.taxoryn.module.udin.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.udin.model.UdinDocumentType;
import com.taxoryn.module.udin.model.UdinStatus;
import com.taxoryn.module.udin.model.UdinVerificationStatus;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity representing a Unique Document Identification Number (UDIN) record in the practice register.
 * Used for maintaining, tracking, and verifying UDINs associated with clients and compliance documents.
 */
@Entity
@Table(name = "udin_register")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UdinEntity extends TenantAuditableEntity {

    @Column(name = "udin", nullable = false, length = 18)
    private String udin;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "engagement_id")
    private UUID engagementId;

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 100)
    private UdinDocumentType documentType;

    @Column(name = "document_title", nullable = false)
    private String documentTitle;

    @Column(name = "document_description", columnDefinition = "TEXT")
    private String documentDescription;

    @Column(name = "signatory_name", nullable = false)
    private String signatoryName;

    @Column(name = "signatory_membership_no", length = 50)
    private String signatoryMembershipNo;

    @Column(name = "generation_date", nullable = false)
    private LocalDate generationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private UdinStatus status = UdinStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    @Builder.Default
    private UdinVerificationStatus verificationStatus = UdinVerificationStatus.NOT_VERIFIED;

    @Column(name = "verification_source", length = 100)
    @Builder.Default
    private String verificationSource = "MANUAL";

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verification_remarks", columnDefinition = "TEXT")
    private String verificationRemarks;

    @Column(name = "financial_figures_json", columnDefinition = "JSONB")
    private String financialFiguresJson;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
