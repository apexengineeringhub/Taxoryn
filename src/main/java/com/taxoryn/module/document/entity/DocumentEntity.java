package com.taxoryn.module.document.entity;

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
@Table(name = "documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity extends TenantAuditableEntity {

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "gst_filing_id")
    private UUID gstFilingId;

    @Column(name = "itr_return_id")
    private UUID itrReturnId;

    @Column(name = "tds_return_id")
    private UUID tdsReturnId;

    @Column(name = "task_id")
    private UUID taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    @Builder.Default
    private DocumentType documentType = DocumentType.OTHER;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 50)
    @Builder.Default
    private StorageProvider storageProvider = StorageProvider.LOCAL;

    @Column(name = "financial_year", length = 20)
    private String financialYear;

    @Column(name = "assessment_year", length = 20)
    private String assessmentYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.ACTIVE;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum DocumentType {
        PAN_CARD,
        AADHAAR_CARD,
        FORM_16,
        FORM_16A,
        FORM_27D,
        FORM_26AS,
        AIS_TIS,
        BANK_STATEMENT,
        GST_INVOICE_PURCHASE,
        GST_INVOICE_SALE,
        GST_REGISTRATION_CERTIFICATE,
        ITR_ACKNOWLEDGEMENT,
        ITR_COMPUTATION_SHEET,
        TDS_RETURN_ACKNOWLEDGEMENT,
        CHALLAN_RECEIPT,
        TAX_AUDIT_REPORT,
        FINANCIAL_STATEMENTS,
        BOARD_RESOLUTION,
        MOA_AOA,
        PARTNERSHIP_DEED,
        OTHER
    }

    public enum DocumentStatus {
        ACTIVE,
        ARCHIVED,
        DELETED
    }

    public enum StorageProvider {
        LOCAL,
        S3
    }
}
