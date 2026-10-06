package com.taxoryn.module.gov.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Provider-neutral purpose scope for Government Authentication, EVC Verification, and DSC Signing.
 * Enforces strict operational boundaries (ITR vs TDS, EVC vs DSC, GST vs returns).
 */
@Schema(description = "Supported Government Authentication & Signing Purposes")
public enum GovAuthPurpose {

    @Schema(description = "GST portal authentication and general return filing")
    GST,

    @Schema(description = "E-Way Bill generation and tracking")
    EWAY_BILL,

    @Schema(description = "E-Invoice and IRN operations")
    E_INVOICE,

    @Schema(description = "Income Tax Return Electronic Verification Code (EVC)")
    ITR_EVC,

    @Schema(description = "Income Tax Return Digital Signature Certificate (DSC) Signing")
    ITR_DSC,

    @Schema(description = "TDS / TRACES Electronic Verification Code (EVC)")
    TDS_EVC,

    @Schema(description = "TDS / TRACES Digital Signature Certificate (DSC) Signing")
    TDS_DSC;

    public boolean isEvc() {
        return this == ITR_EVC || this == TDS_EVC;
    }

    public boolean isDsc() {
        return this == ITR_DSC || this == TDS_DSC;
    }

    public boolean isItr() {
        return this == ITR_EVC || this == ITR_DSC;
    }

    public boolean isTds() {
        return this == TDS_EVC || this == TDS_DSC;
    }

    public boolean isGst() {
        return this == GST || this == EWAY_BILL || this == E_INVOICE;
    }
}
