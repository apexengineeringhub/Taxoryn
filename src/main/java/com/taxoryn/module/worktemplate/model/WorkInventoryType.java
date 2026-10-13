package com.taxoryn.module.worktemplate.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Categorization of work inventory items across Taxoryn domains")
public enum WorkInventoryType {
    @Schema(description = "Concrete generated practice work occurrence from a recurring or service template")
    WORK_INSTANCE,

    @Schema(description = "Statutory compliance obligation (e.g. GST, TDS, ITR return)")
    COMPLIANCE_OBLIGATION,

    @Schema(description = "Actionable standalone task")
    TASK
}
