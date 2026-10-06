package com.taxoryn.module.compliance.applicability.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Immutable snapshot of resolved client facts used by the Applicability Engine.
 * Supports tri-state logic (true / false / null for unknown).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceFactContext {

    private UUID clientId;
    private UUID organizationId;
    private String clientType; // e.g. COMPANY, INDIVIDUAL, LLP, PARTNERSHIP, etc.
    private boolean profilePresent;

    // GST Facts
    private Boolean gstApplicable;
    private String gstRegistrationType;
    private String gstFilingFrequency;
    private Boolean gstCompositionScheme;
    private Boolean gstEinvoiceApplicable;
    private Boolean gstEwaybillApplicable;

    // TDS / TCS Facts
    private Boolean tdsApplicable;
    private String tdsFilingFrequency;
    private String tdsDeductorCategory;
    private Boolean tdsLowerDeductionCertificate;

    // ITR Facts
    private Boolean itrApplicable;
    private String itrCategory;
    private Boolean itrTaxAuditApplicable;
    private Boolean itrTransferPricingApplicable;
    private Boolean advanceTaxApplicable;

    // Corporate / MCA Facts
    private Boolean mcaFilingApplicable;

    // Payroll & Labour Facts
    private Boolean pfEsiApplicable;
    private Boolean professionalTaxApplicable;

    /**
     * Resolves the value of a specific fact attribute.
     *
     * @param attribute the attribute to look up
     * @return the resolved value, or null if unknown / not configured
     */
    public Object getFactValue(ComplianceFactAttribute attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case CLIENT_TYPE -> clientType;
            case GST_APPLICABLE -> gstApplicable;
            case GST_REGISTRATION_TYPE -> gstRegistrationType;
            case GST_FILING_FREQUENCY -> gstFilingFrequency;
            case GST_COMPOSITION_SCHEME -> gstCompositionScheme;
            case GST_EINVOICE_APPLICABLE -> gstEinvoiceApplicable;
            case GST_EWAYBILL_APPLICABLE -> gstEwaybillApplicable;
            case TDS_APPLICABLE -> tdsApplicable;
            case TDS_FILING_FREQUENCY -> tdsFilingFrequency;
            case TDS_DEDUCTOR_CATEGORY -> tdsDeductorCategory;
            case TDS_LOWER_DEDUCTION_CERTIFICATE -> tdsLowerDeductionCertificate;
            case ITR_APPLICABLE -> itrApplicable;
            case ITR_CATEGORY -> itrCategory;
            case ITR_TAX_AUDIT_APPLICABLE -> itrTaxAuditApplicable;
            case ITR_TRANSFER_PRICING_APPLICABLE -> itrTransferPricingApplicable;
            case ADVANCE_TAX_APPLICABLE -> advanceTaxApplicable;
            case MCA_FILING_APPLICABLE -> mcaFilingApplicable;
            case PF_ESI_APPLICABLE -> pfEsiApplicable;
            case PROFESSIONAL_TAX_APPLICABLE -> professionalTaxApplicable;
        };
    }
}
