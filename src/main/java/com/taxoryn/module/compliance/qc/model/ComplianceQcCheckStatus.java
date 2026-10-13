package com.taxoryn.module.compliance.qc.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Quality Control Check Evaluation Status")
public enum ComplianceQcCheckStatus {
    @Schema(description = "The check was evaluated and passed successfully")
    PASS,

    @Schema(description = "The check was evaluated and did not pass")
    FAIL,

    @Schema(description = "The system could not evaluate the check reliably or safely")
    NOT_EVALUATED,

    @Schema(description = "The check does not apply under established domain rules")
    NOT_APPLICABLE
}
