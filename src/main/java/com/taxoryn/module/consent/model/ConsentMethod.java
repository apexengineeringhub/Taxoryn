package com.taxoryn.module.consent.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Method through which taxpayer consent was granted")
public enum ConsentMethod {

    @Schema(description = "Direct authorization within Taxoryn application")
    IN_APP,

    @Schema(description = "Taxpayer authorization via Client Portal")
    PORTAL,

    @Schema(description = "Electronic Signature / Signed Mandate")
    E_SIGN,

    @Schema(description = "Electronic Verification Code (EVC) Consent")
    EVC,

    @Schema(description = "Digital Signature Certificate (DSC) Consent")
    DSC,

    @Schema(description = "Offline / Physical Power of Attorney / Manual Letter")
    MANUAL
}
