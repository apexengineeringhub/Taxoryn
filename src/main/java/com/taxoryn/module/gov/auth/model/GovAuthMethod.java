package com.taxoryn.module.gov.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Supported Government Authentication Methods")
public enum GovAuthMethod {
    OAUTH2,
    OTP,
    EVC,
    DSC
}
