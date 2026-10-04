package com.taxoryn.module.consent.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of representative delegation")
public enum DelegationType {

    @Schema(description = "Assigned Tax Practitioner")
    PRACTITIONER,

    @Schema(description = "Chartered Accountant / Certified Auditor")
    CA,

    @Schema(description = "Authorized Legal Representative / Power of Attorney")
    AUTHORIZED_REPRESENTATIVE,

    @Schema(description = "Authorized Client Staff / Employee")
    EMPLOYEE,

    @Schema(description = "Client Portal User / Direct Account Holder")
    CLIENT_USER
}
