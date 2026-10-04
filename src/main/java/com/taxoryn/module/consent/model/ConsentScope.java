package com.taxoryn.module.consent.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Scoped operational permissions granted by taxpayer consent")
public enum ConsentScope {

    @Schema(description = "All government and tax compliance operations")
    ALL,

    // GST Scopes
    @Schema(description = "General GST operations (includes returns, status, eway bill, einvoice)")
    GST,

    @Schema(description = "GST return preparation and filing")
    GST_RETURN,

    @Schema(description = "GST status inquiry and ledger viewing")
    GST_STATUS,

    @Schema(description = "E-Way Bill generation and management")
    EWAY_BILL,

    @Schema(description = "E-Invoice and IRN management")
    E_INVOICE,

    // ITR Scopes
    @Schema(description = "General Income Tax Return operations (includes preparation, filing, verification, DSC)")
    ITR,

    @Schema(description = "ITR return preparation and viewing")
    ITR_RETURN,

    @Schema(description = "ITR verification and status checks")
    ITR_VERIFICATION,

    @Schema(description = "ITR Electronic Verification Code (EVC) signing")
    ITR_EVC,

    @Schema(description = "ITR Digital Signature Certificate (DSC) signing")
    ITR_DSC,

    // TDS Scopes
    @Schema(description = "General TDS / TRACES operations (includes return preparation, filing, challan, DSC)")
    TDS,

    @Schema(description = "TDS return preparation and viewing")
    TDS_RETURN,

    @Schema(description = "TDS return verification and challan reconciliation")
    TDS_VERIFICATION,

    @Schema(description = "TDS Electronic Verification Code (EVC) signing")
    TDS_EVC,

    @Schema(description = "TDS Digital Signature Certificate (DSC) signing")
    TDS_DSC;

    /**
     * Determines whether this granted scope satisfies the required operational scope.
     * Follows least-privilege principles and domain boundaries.
     */
    public boolean satisfies(ConsentScope requiredScope) {
        if (requiredScope == null) {
            return false;
        }
        if (this == ALL) {
            return true;
        }
        if (this == requiredScope) {
            return true;
        }

        // GST umbrella hierarchy
        if (this == GST) {
            return requiredScope == GST_RETURN
                    || requiredScope == GST_STATUS
                    || requiredScope == EWAY_BILL
                    || requiredScope == E_INVOICE;
        }

        // ITR umbrella hierarchy
        if (this == ITR) {
            return requiredScope == ITR_RETURN
                    || requiredScope == ITR_VERIFICATION
                    || requiredScope == ITR_EVC
                    || requiredScope == ITR_DSC;
        }

        // TDS umbrella hierarchy
        if (this == TDS) {
            return requiredScope == TDS_RETURN
                    || requiredScope == TDS_VERIFICATION
                    || requiredScope == TDS_EVC
                    || requiredScope == TDS_DSC;
        }

        // Narrower scopes (e.g. ITR_RETURN) do not imply broader or sensitive scopes (e.g. ITR_DSC)
        return false;
    }

    /**
     * Helper to verify if any granted scope in a set satisfies the required scope.
     */
    public static boolean satisfiesAny(Set<ConsentScope> grantedScopes, ConsentScope requiredScope) {
        if (grantedScopes == null || grantedScopes.isEmpty() || requiredScope == null) {
            return false;
        }
        return grantedScopes.stream().anyMatch(scope -> scope.satisfies(requiredScope));
    }
}
