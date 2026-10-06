package com.taxoryn.module.compliance.rule.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.compliance.rule.dto.ComplianceRuleCatalogSummaryDto;
import com.taxoryn.module.compliance.rule.dto.ComplianceRuleDto;
import com.taxoryn.module.compliance.rule.dto.CreateComplianceRuleRequest;
import com.taxoryn.module.compliance.rule.dto.UpdateComplianceRuleRequest;
import com.taxoryn.module.compliance.rule.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleStatus;
import com.taxoryn.module.compliance.rule.model.DueDateRuleType;
import com.taxoryn.module.compliance.rule.repository.ComplianceRuleCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service("complianceRuleCatalogService")
@RequiredArgsConstructor
@Slf4j
public class ComplianceRuleCatalogServiceImpl implements ComplianceRuleCatalogService {

    private final ComplianceRuleCatalogRepository complianceRuleRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceRuleDto> getRules(
            ComplianceRuleDomain domain,
            ComplianceRuleFrequency frequency,
            ComplianceRuleStatus status,
            String search,
            Boolean includeSystem
    ) {
        UUID orgId = TenantContext.getTenantId();
        boolean incSystem = includeSystem == null || includeSystem;

        List<ComplianceRuleEntity> entities = complianceRuleRepository.findRulesForTenant(
                orgId,
                incSystem,
                domain,
                frequency,
                status,
                search != null && !search.isBlank() ? search.trim() : null
        );

        return entities.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceRuleDto getRuleById(UUID ruleId) {
        UUID orgId = TenantContext.getTenantId();
        ComplianceRuleEntity entity = complianceRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance rule not found with id: '" + ruleId + "'"));

        // Validate access: must be system rule (orgId IS NULL) OR belong to current tenant
        if (entity.getOrganizationId() != null && (orgId == null || !entity.getOrganizationId().equals(orgId))) {
            throw new ResourceNotFoundException("Compliance rule not found with id: '" + ruleId + "'");
        }

        return mapToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceRuleDto getRuleByCode(String ruleCode) {
        UUID orgId = TenantContext.getTenantId();

        // 1. First check tenant-custom rule if in tenant context
        if (orgId != null) {
            var customRuleOpt = complianceRuleRepository.findByRuleCodeAndOrganizationId(ruleCode, orgId);
            if (customRuleOpt.isPresent()) {
                return mapToDto(customRuleOpt.get());
            }
        }

        // 2. Fall back to system rule
        var systemRuleOpt = complianceRuleRepository.findByRuleCodeAndOrganizationIdIsNull(ruleCode);
        if (systemRuleOpt.isPresent()) {
            return mapToDto(systemRuleOpt.get());
        }

        throw new ResourceNotFoundException("Compliance rule not found with code: '" + ruleCode + "'");
    }

    @Override
    @Transactional
    public ComplianceRuleDto createCustomRule(CreateComplianceRuleRequest request) {
        UUID orgId = TenantContext.getTenantId();
        if (orgId == null) {
            throw new BadRequestException("Practice tenant organization context required to create a custom compliance rule.");
        }

        String normalizedCode = request.getRuleCode().trim().toUpperCase();

        // Check duplicate code within tenant
        if (complianceRuleRepository.existsByRuleCodeAndOrganizationId(normalizedCode, orgId)) {
            throw new DuplicateResourceException("Compliance rule with code '" + normalizedCode + "' already exists for this organization.");
        }

        // Check if identical code exists in system rules
        if (complianceRuleRepository.existsByRuleCodeAndOrganizationIdIsNull(normalizedCode)) {
            throw new DuplicateResourceException("Compliance rule with code '" + normalizedCode + "' already exists as a standard system rule. Use a custom rule code prefix.");
        }

        ComplianceRuleEntity entity = ComplianceRuleEntity.builder()
                .organizationId(orgId)
                .ruleCode(normalizedCode)
                .ruleName(request.getRuleName().trim())
                .domain(request.getDomain())
                .frequency(request.getFrequency())
                .periodType(request.getPeriodType())
                .status(ComplianceRuleStatus.ACTIVE)
                .isSystemRule(false)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .statutoryAct(request.getStatutoryAct() != null ? request.getStatutoryAct().trim() : null)
                .statutorySection(request.getStatutorySection() != null ? request.getStatutorySection().trim() : null)
                .statutoryFormCode(request.getStatutoryFormCode() != null ? request.getStatutoryFormCode().trim() : null)
                .penaltyDetails(request.getPenaltyDetails() != null ? request.getPenaltyDetails().trim() : null)
                .dueDateRuleType(request.getDueDateRuleType() != null ? request.getDueDateRuleType() : DueDateRuleType.DAY_OF_FOLLOWING_MONTH)
                .dueDayOffset(request.getDueDayOffset())
                .dueMonthOffset(request.getDueMonthOffset())
                .fixedMonth(request.getFixedMonth())
                .fixedDay(request.getFixedDay())
                .statutoryGraceDays(request.getStatutoryGraceDays())
                .dueDateDescription(request.getDueDateDescription() != null ? request.getDueDateDescription().trim() : null)
                .requiredModule(request.getRequiredModule() != null ? request.getRequiredModule().trim() : null)
                .applicableEntityTypes(request.getApplicableEntityTypes() != null ? request.getApplicableEntityTypes().trim() : null)
                .applicableGstRegistrationTypes(request.getApplicableGstRegistrationTypes() != null ? request.getApplicableGstRegistrationTypes().trim() : null)
                .applicableFilingFrequencies(request.getApplicableFilingFrequencies() != null ? request.getApplicableFilingFrequencies().trim() : null)
                .requiresTaxAudit(request.getRequiresTaxAudit())
                .requiresTransferPricing(request.getRequiresTransferPricing())
                .requiresTdsDeductor(request.getRequiresTdsDeductor())
                .requiresMcaFiling(request.getRequiresMcaFiling())
                .defaultWorkTemplateCode(request.getDefaultWorkTemplateCode() != null ? request.getDefaultWorkTemplateCode().trim() : null)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .build();

        ComplianceRuleEntity saved = complianceRuleRepository.save(entity);

        auditService.logEvent(
                "COMPLIANCE_RULE_CREATED",
                "COMPLIANCE_RULE",
                saved.getId().toString(),
                null,
                "Created rule: " + saved.getRuleCode() + " (" + saved.getRuleName() + ")"
        );

        log.info("Created custom compliance rule id={}, code='{}' for organization={}", saved.getId(), saved.getRuleCode(), orgId);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ComplianceRuleDto updateRule(UUID ruleId, UpdateComplianceRuleRequest request) {
        UUID orgId = TenantContext.getTenantId();
        ComplianceRuleEntity entity = complianceRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance rule not found with id: '" + ruleId + "'"));

        if (entity.isSystemRule() || entity.getOrganizationId() == null) {
            throw new BadRequestException("Standard system compliance rules are immutable. Create a custom rule in your practice catalog.");
        }

        if (orgId != null && !orgId.equals(entity.getOrganizationId())) {
            throw new ResourceNotFoundException("Compliance rule not found with id: '" + ruleId + "'");
        }

        String oldSummary = entity.getRuleName() + " (" + entity.getStatus() + ")";

        if (request.getRuleName() != null && !request.getRuleName().isBlank()) {
            entity.setRuleName(request.getRuleName().trim());
        }
        if (request.getDomain() != null) {
            entity.setDomain(request.getDomain());
        }
        if (request.getFrequency() != null) {
            entity.setFrequency(request.getFrequency());
        }
        if (request.getPeriodType() != null) {
            entity.setPeriodType(request.getPeriodType());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription().trim());
        }
        if (request.getStatutoryAct() != null) {
            entity.setStatutoryAct(request.getStatutoryAct().trim());
        }
        if (request.getStatutorySection() != null) {
            entity.setStatutorySection(request.getStatutorySection().trim());
        }
        if (request.getStatutoryFormCode() != null) {
            entity.setStatutoryFormCode(request.getStatutoryFormCode().trim());
        }
        if (request.getPenaltyDetails() != null) {
            entity.setPenaltyDetails(request.getPenaltyDetails().trim());
        }
        if (request.getDueDateRuleType() != null) {
            entity.setDueDateRuleType(request.getDueDateRuleType());
        }
        if (request.getDueDayOffset() != null) {
            entity.setDueDayOffset(request.getDueDayOffset());
        }
        if (request.getDueMonthOffset() != null) {
            entity.setDueMonthOffset(request.getDueMonthOffset());
        }
        if (request.getFixedMonth() != null) {
            entity.setFixedMonth(request.getFixedMonth());
        }
        if (request.getFixedDay() != null) {
            entity.setFixedDay(request.getFixedDay());
        }
        if (request.getStatutoryGraceDays() != null) {
            entity.setStatutoryGraceDays(request.getStatutoryGraceDays());
        }
        if (request.getDueDateDescription() != null) {
            entity.setDueDateDescription(request.getDueDateDescription().trim());
        }
        if (request.getRequiredModule() != null) {
            entity.setRequiredModule(request.getRequiredModule().trim());
        }
        if (request.getApplicableEntityTypes() != null) {
            entity.setApplicableEntityTypes(request.getApplicableEntityTypes().trim());
        }
        if (request.getApplicableGstRegistrationTypes() != null) {
            entity.setApplicableGstRegistrationTypes(request.getApplicableGstRegistrationTypes().trim());
        }
        if (request.getApplicableFilingFrequencies() != null) {
            entity.setApplicableFilingFrequencies(request.getApplicableFilingFrequencies().trim());
        }
        if (request.getRequiresTaxAudit() != null) {
            entity.setRequiresTaxAudit(request.getRequiresTaxAudit());
        }
        if (request.getRequiresTransferPricing() != null) {
            entity.setRequiresTransferPricing(request.getRequiresTransferPricing());
        }
        if (request.getRequiresTdsDeductor() != null) {
            entity.setRequiresTdsDeductor(request.getRequiresTdsDeductor());
        }
        if (request.getRequiresMcaFiling() != null) {
            entity.setRequiresMcaFiling(request.getRequiresMcaFiling());
        }
        if (request.getDefaultWorkTemplateCode() != null) {
            entity.setDefaultWorkTemplateCode(request.getDefaultWorkTemplateCode().trim());
        }
        if (request.getEffectiveFrom() != null) {
            entity.setEffectiveFrom(request.getEffectiveFrom());
        }
        if (request.getEffectiveTo() != null) {
            entity.setEffectiveTo(request.getEffectiveTo());
        }

        ComplianceRuleEntity saved = complianceRuleRepository.save(entity);
        String newSummary = saved.getRuleName() + " (" + saved.getStatus() + ")";

        auditService.logEvent(
                "COMPLIANCE_RULE_UPDATED",
                "COMPLIANCE_RULE",
                saved.getId().toString(),
                oldSummary,
                newSummary
        );

        log.info("Updated custom compliance rule id={}, code='{}'", saved.getId(), saved.getRuleCode());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteRule(UUID ruleId) {
        UUID orgId = TenantContext.getTenantId();
        ComplianceRuleEntity entity = complianceRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance rule not found with id: '" + ruleId + "'"));

        if (entity.isSystemRule() || entity.getOrganizationId() == null) {
            throw new BadRequestException("Standard system compliance rules cannot be deleted.");
        }

        if (orgId != null && !orgId.equals(entity.getOrganizationId())) {
            throw new ResourceNotFoundException("Compliance rule not found with id: '" + ruleId + "'");
        }

        entity.setStatus(ComplianceRuleStatus.INACTIVE);
        complianceRuleRepository.save(entity);

        auditService.logEvent(
                "COMPLIANCE_RULE_DEACTIVATED",
                "COMPLIANCE_RULE",
                entity.getId().toString(),
                "Status: ACTIVE",
                "Status: INACTIVE"
        );

        log.info("Deactivated compliance rule id={}, code='{}'", entity.getId(), entity.getRuleCode());
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceRuleCatalogSummaryDto getCatalogSummary() {
        UUID orgId = TenantContext.getTenantId();
        List<ComplianceRuleEntity> rules = complianceRuleRepository.findRulesForTenant(
                orgId, true, null, null, null, null
        );

        long total = rules.size();
        long active = rules.stream().filter(r -> r.getStatus() == ComplianceRuleStatus.ACTIVE).count();
        long system = rules.stream().filter(ComplianceRuleEntity::isSystemRule).count();
        long custom = rules.stream().filter(r -> !r.isSystemRule()).count();

        Map<ComplianceRuleDomain, Long> byDomain = new EnumMap<>(ComplianceRuleDomain.class);
        for (ComplianceRuleDomain d : ComplianceRuleDomain.values()) {
            byDomain.put(d, 0L);
        }
        for (ComplianceRuleEntity r : rules) {
            if (r.getDomain() != null) {
                byDomain.put(r.getDomain(), byDomain.getOrDefault(r.getDomain(), 0L) + 1L);
            }
        }

        return ComplianceRuleCatalogSummaryDto.builder()
                .totalRules(total)
                .activeRules(active)
                .systemRules(system)
                .customRules(custom)
                .rulesByDomain(byDomain)
                .build();
    }

    private ComplianceRuleDto mapToDto(ComplianceRuleEntity entity) {
        return ComplianceRuleDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .ruleCode(entity.getRuleCode())
                .ruleName(entity.getRuleName())
                .domain(entity.getDomain())
                .domainDisplayName(entity.getDomain() != null ? entity.getDomain().getDisplayName() : null)
                .frequency(entity.getFrequency())
                .periodType(entity.getPeriodType())
                .status(entity.getStatus())
                .systemRule(entity.isSystemRule())
                .description(entity.getDescription())
                .statutoryAct(entity.getStatutoryAct())
                .statutorySection(entity.getStatutorySection())
                .statutoryFormCode(entity.getStatutoryFormCode())
                .penaltyDetails(entity.getPenaltyDetails())
                .dueDateRuleType(entity.getDueDateRuleType())
                .dueDayOffset(entity.getDueDayOffset())
                .dueMonthOffset(entity.getDueMonthOffset())
                .fixedMonth(entity.getFixedMonth())
                .fixedDay(entity.getFixedDay())
                .statutoryGraceDays(entity.getStatutoryGraceDays())
                .dueDateDescription(entity.getDueDateDescription())
                .requiredModule(entity.getRequiredModule())
                .applicableEntityTypes(entity.getApplicableEntityTypes())
                .applicableGstRegistrationTypes(entity.getApplicableGstRegistrationTypes())
                .applicableFilingFrequencies(entity.getApplicableFilingFrequencies())
                .requiresTaxAudit(entity.getRequiresTaxAudit())
                .requiresTransferPricing(entity.getRequiresTransferPricing())
                .requiresTdsDeductor(entity.getRequiresTdsDeductor())
                .requiresMcaFiling(entity.getRequiresMcaFiling())
                .defaultWorkTemplateCode(entity.getDefaultWorkTemplateCode())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .version(entity.getVersion())
                .build();
    }
}
