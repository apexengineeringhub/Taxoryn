package com.taxoryn.module.reminder.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.reminder.dto.AutomationRuleDto;
import com.taxoryn.module.reminder.dto.SaveAutomationRuleRequest;
import com.taxoryn.module.reminder.entity.AutomationActionType;
import com.taxoryn.module.reminder.entity.AutomationEventType;
import com.taxoryn.module.reminder.entity.AutomationRuleEntity;
import com.taxoryn.module.reminder.repository.AutomationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AutomationService}.
 *
 * <p>Key design decisions:
 * <ol>
 *   <li>System-default rules (organizationId = null) are <em>read-only</em> via this service.
 *       Only super-admin seeding (migration / CLI) creates system defaults.</li>
 *   <li>{@link #findEnabledRulesForEvent} returns org-specific rules first, then system defaults.
 *       Callers (e.g. the event listener) can decide whether org-specific rules <em>replace</em>
 *       or <em>augment</em> system defaults; for P0.5, both fire.</li>
 *   <li>No TenantAuditableEntity for AutomationRuleEntity because system defaults have null org.
 *       Org-scoping is enforced manually here.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationServiceImpl implements AutomationService {

    private final AutomationRuleRepository ruleRepository;

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<AutomationRuleDto> listRules() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        List<AutomationRuleEntity> orgRules = ruleRepository.findAllByOrganizationIdOrderByEventTypeAscCreatedAtAsc(orgId);
        List<AutomationRuleEntity> systemRules = ruleRepository.findAllByOrganizationIdIsNullOrderByEventTypeAsc();

        List<AutomationRuleDto> combined = new ArrayList<>(orgRules.size() + systemRules.size());
        orgRules.forEach(r -> combined.add(AutomationRuleDto.from(r)));
        systemRules.forEach(r -> combined.add(AutomationRuleDto.from(r)));
        return combined;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationRuleDto> listOrgRules() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        return ruleRepository.findAllByOrganizationIdOrderByEventTypeAscCreatedAtAsc(orgId)
                .stream()
                .map(AutomationRuleDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationRuleDto> listSystemDefaultRules() {
        return ruleRepository.findAllByOrganizationIdIsNullOrderByEventTypeAsc()
                .stream()
                .map(AutomationRuleDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationRuleDto getRule(UUID ruleId) {
        AutomationRuleEntity rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationRule", "id", ruleId));
        validateOrgAccess(rule);
        return AutomationRuleDto.from(rule);
    }

    // -------------------------------------------------------------------------
    // Write operations (org-owned rules only)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AutomationRuleDto createRule(SaveAutomationRuleRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();

        AutomationRuleEntity rule = AutomationRuleEntity.builder()
                .organizationId(orgId)
                .name(request.getName().trim())
                .description(request.getDescription())
                .eventType(request.getEventType())
                .daysOffset(request.getDaysOffset())
                .actionType(AutomationActionType.CREATE_REMINDER)
                .targetType(request.getTargetType())
                .enabled(request.getEnabled())
                .build();

        AutomationRuleEntity saved = ruleRepository.save(rule);
        log.info("Created automation rule {} '{}' for org {}", saved.getId(), saved.getName(), orgId);
        return AutomationRuleDto.from(saved);
    }

    @Override
    @Transactional
    public AutomationRuleDto updateRule(UUID ruleId, SaveAutomationRuleRequest request) {
        AutomationRuleEntity rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationRule", "id", ruleId));
        requireOrgOwned(rule);
        validateOrgAccess(rule);

        rule.setName(request.getName().trim());
        rule.setDescription(request.getDescription());
        rule.setEventType(request.getEventType());
        rule.setDaysOffset(request.getDaysOffset());
        rule.setTargetType(request.getTargetType());
        rule.setEnabled(request.getEnabled());

        AutomationRuleEntity saved = ruleRepository.save(rule);
        log.info("Updated automation rule {} for org {}", saved.getId(), saved.getOrganizationId());
        return AutomationRuleDto.from(saved);
    }

    @Override
    @Transactional
    public AutomationRuleDto enableRule(UUID ruleId) {
        AutomationRuleEntity rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationRule", "id", ruleId));
        requireOrgOwned(rule);
        validateOrgAccess(rule);

        rule.setEnabled(true);
        return AutomationRuleDto.from(ruleRepository.save(rule));
    }

    @Override
    @Transactional
    public AutomationRuleDto disableRule(UUID ruleId) {
        AutomationRuleEntity rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationRule", "id", ruleId));
        requireOrgOwned(rule);
        validateOrgAccess(rule);

        rule.setEnabled(false);
        return AutomationRuleDto.from(ruleRepository.save(rule));
    }

    @Override
    @Transactional
    public void deleteRule(UUID ruleId) {
        AutomationRuleEntity rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationRule", "id", ruleId));
        requireOrgOwned(rule);
        validateOrgAccess(rule);

        ruleRepository.delete(rule);
        log.info("Deleted automation rule {} for org {}", ruleId, rule.getOrganizationId());
    }

    // -------------------------------------------------------------------------
    // Internal: event processing query
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<AutomationRuleDto> findEnabledRulesForEvent(AutomationEventType eventType, UUID organizationId) {
        return ruleRepository.findEnabledByEventTypeForOrganization(eventType, organizationId)
                .stream()
                .map(AutomationRuleDto::from)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Validates caller's org matches the rule's org (or rule is a system default). */
    private void validateOrgAccess(AutomationRuleEntity rule) {
        if (rule.getOrganizationId() == null) {
            // System default — visible to everyone (read-only)
            return;
        }
        UUID callerOrg = SecurityUtils.getCurrentOrganizationId();
        if (!rule.getOrganizationId().equals(callerOrg)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: automation rule belongs to a different organization.");
        }
    }

    /** Ensures the rule is org-owned (not a system default). Write ops on system defaults are blocked. */
    private void requireOrgOwned(AutomationRuleEntity rule) {
        if (rule.getOrganizationId() == null) {
            throw new IllegalStateException("System-default automation rules cannot be modified via this API.");
        }
    }
}
