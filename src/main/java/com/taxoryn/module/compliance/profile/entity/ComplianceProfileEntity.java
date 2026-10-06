package com.taxoryn.module.compliance.profile.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.compliance.profile.model.ComplianceFilingFrequency;
import com.taxoryn.module.compliance.profile.model.ComplianceGstRegistrationType;
import com.taxoryn.module.compliance.profile.model.ComplianceItrCategory;
import com.taxoryn.module.compliance.profile.model.ComplianceProfileStatus;
import com.taxoryn.module.compliance.profile.model.ComplianceTdsDeductorCategory;
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

/**
 * Compliance Profile Foundation Model (Phase 29.1).
 * <p>
 * Encapsulates client-specific statutory and compliance facts used by future applicability
 * and obligation engines without duplicating master client identity data (such as PAN/GSTIN/TAN).
 */
@Entity
@Table(name = "compliance_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceProfileEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ComplianceProfileStatus status = ComplianceProfileStatus.ACTIVE;

    // --- GST Configuration Facts ---
    @Column(name = "gst_applicable", nullable = false)
    @Builder.Default
    private boolean gstApplicable = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "gst_registration_type", length = 50)
    private ComplianceGstRegistrationType gstRegistrationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "gst_filing_frequency", length = 50)
    private ComplianceFilingFrequency gstFilingFrequency;

    @Column(name = "gst_composition_scheme", nullable = false)
    @Builder.Default
    private boolean gstCompositionScheme = false;

    @Column(name = "gst_einvoice_applicable", nullable = false)
    @Builder.Default
    private boolean gstEinvoiceApplicable = false;

    @Column(name = "gst_ewaybill_applicable", nullable = false)
    @Builder.Default
    private boolean gstEwaybillApplicable = false;

    // --- TDS Configuration Facts ---
    @Column(name = "tds_applicable", nullable = false)
    @Builder.Default
    private boolean tdsApplicable = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tds_filing_frequency", length = 50)
    private ComplianceFilingFrequency tdsFilingFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "tds_deductor_category", length = 50)
    private ComplianceTdsDeductorCategory tdsDeductorCategory;

    @Column(name = "tds_lower_deduction_certificate", nullable = false)
    @Builder.Default
    private boolean tdsLowerDeductionCertificate = false;

    // --- ITR Configuration Facts ---
    @Column(name = "itr_applicable", nullable = false)
    @Builder.Default
    private boolean itrApplicable = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "itr_category", length = 50)
    private ComplianceItrCategory itrCategory;

    @Column(name = "itr_tax_audit_applicable", nullable = false)
    @Builder.Default
    private boolean itrTaxAuditApplicable = false;

    @Column(name = "itr_transfer_pricing_applicable", nullable = false)
    @Builder.Default
    private boolean itrTransferPricingApplicable = false;

    // --- Other Statutory Compliance Flags ---
    @Column(name = "advance_tax_applicable", nullable = false)
    @Builder.Default
    private boolean advanceTaxApplicable = false;

    @Column(name = "mca_filing_applicable", nullable = false)
    @Builder.Default
    private boolean mcaFilingApplicable = false;

    @Column(name = "professional_tax_applicable", nullable = false)
    @Builder.Default
    private boolean professionalTaxApplicable = false;

    @Column(name = "pf_esi_applicable", nullable = false)
    @Builder.Default
    private boolean pfEsiApplicable = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
