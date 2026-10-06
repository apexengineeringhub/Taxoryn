package com.taxoryn.module.compliance.work.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Request DTO crossing the compliance → work generation boundary.
 * Passed from ComplianceWorkOrchestrationService to ComplianceWorkGenerationPort.
 */
@Getter
@Builder
public class ComplianceWorkGenerationRequest {

    /** ID of the compliance obligation to generate work for. */
    private final UUID obligationId;

    /** Client UUID for engagement resolution. */
    private final UUID clientId;

    /** Organization (tenant) UUID. */
    private final UUID organizationId;

    /**
     * Stable work template code from ComplianceRuleEntity.defaultWorkTemplateCode.
     * Must match a WorkTemplateEntity.templateCode.
     */
    private final String workTemplateCode;

    /** Period start date derived from the obligation's period. */
    private final java.time.LocalDate periodStart;

    /** Period end date derived from the obligation's period. */
    private final java.time.LocalDate periodEnd;

    /** Due date from the obligation (statutory or internal), used as work instance due date. */
    private final java.time.LocalDate dueDate;

    /** UUID of the client service — used for engagement resolution. */
    private final UUID clientServiceId;

    /** Human-readable label for the work instance title. */
    private final String periodLabel;

    /** Rule code snapshot for audit. */
    private final String ruleCode;
}
