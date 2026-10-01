package com.taxoryn.module.reminder.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.reminder.dto.AutomationRuleDto;
import com.taxoryn.module.reminder.dto.SaveAutomationRuleRequest;
import com.taxoryn.module.reminder.entity.AutomationEventType;

import java.util.List;
import java.util.UUID;

/**
 * Manages AutomationRule CRUD and retrieval for the Settings → Automations page.
 *
 * <p>System-default rules (organizationId = null) are visible to all orgs but
 * can only be modified by super-admins. Org-specific rules can override defaults.
 */
public interface AutomationService {

    /** List all rules visible to the current organization (own + system defaults). */
    List<AutomationRuleDto> listRules();

    /** List only this organization's own rules (excludes system defaults). */
    List<AutomationRuleDto> listOrgRules();

    /** List system-default rules (organizationId = null). Visible to all orgs. */
    List<AutomationRuleDto> listSystemDefaultRules();

    /** Get a single rule by ID. Validates org access. */
    AutomationRuleDto getRule(UUID ruleId);

    /**
     * Create an org-specific automation rule.
     * Only admins may create rules for their own organization.
     */
    AutomationRuleDto createRule(SaveAutomationRuleRequest request);

    /**
     * Update an existing org-owned rule.
     * System-default rules (null org) cannot be updated via this method.
     */
    AutomationRuleDto updateRule(UUID ruleId, SaveAutomationRuleRequest request);

    /** Enable a rule (org-owned only). */
    AutomationRuleDto enableRule(UUID ruleId);

    /** Disable a rule (org-owned only). */
    AutomationRuleDto disableRule(UUID ruleId);

    /**
     * Delete an org-owned rule.
     * System-default rules cannot be deleted via the public API.
     */
    void deleteRule(UUID ruleId);

    /**
     * Finds all enabled rules applicable to the given event and organization.
     * Returns org-specific overrides first, then system defaults.
     * Used internally by {@code ReminderEventListener}.
     */
    List<AutomationRuleDto> findEnabledRulesForEvent(AutomationEventType eventType, UUID organizationId);
}
