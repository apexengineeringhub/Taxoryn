package com.taxoryn.module.compliance.applicability.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.applicability.dto.ClientComplianceApplicabilityDto;
import com.taxoryn.module.compliance.applicability.dto.ComplianceApplicabilitySummaryDto;
import com.taxoryn.module.compliance.applicability.dto.EvaluatedRuleApplicabilityDto;
import com.taxoryn.module.compliance.applicability.evaluator.RuleApplicabilityEvaluator;
import com.taxoryn.module.compliance.applicability.model.ApplicabilityResultState;
import com.taxoryn.module.compliance.applicability.model.ComplianceFactContext;
import com.taxoryn.module.compliance.applicability.provider.ComplianceFactProvider;
import com.taxoryn.module.compliance.rule.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleStatus;
import com.taxoryn.module.compliance.rule.repository.ComplianceRuleCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceApplicabilityServiceImpl implements ComplianceApplicabilityService {

    private final ComplianceFactProvider factProvider;
    private final RuleApplicabilityEvaluator ruleEvaluator;
    private final ComplianceRuleCatalogRepository ruleRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional(readOnly = true)
    public ClientComplianceApplicabilityDto evaluateClientApplicability(
            UUID clientId,
            LocalDate evaluationDate,
            ComplianceRuleDomain domainFilter,
            ApplicabilityResultState statusFilter) {

        UUID orgId = TenantContext.getTenantId();
        if (orgId == null) {
            throw new BadRequestException("Organization tenant context is required.");
        }

        LocalDate evalDate = evaluationDate != null ? evaluationDate : LocalDate.now();

        // 1. Resolve Client & Facts in a single pass
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: '" + clientId + "'"));

        ComplianceFactContext facts = factProvider.resolveFacts(orgId, clientId);

        // 2. Load all active catalog rules accessible to tenant (System rules + Tenant custom rules)
        List<ComplianceRuleEntity> rules = loadEffectiveRules(orgId);

        // 3. Evaluate rules in memory
        List<EvaluatedRuleApplicabilityDto> allEvaluated = new ArrayList<>();
        Map<String, Integer> applicableByDomain = new HashMap<>();
        int applicableCount = 0;
        int notApplicableCount = 0;
        int insufficientDataCount = 0;

        for (ComplianceRuleEntity rule : rules) {
            EvaluatedRuleApplicabilityDto evaluated = ruleEvaluator.evaluate(rule, facts, evalDate);
            allEvaluated.add(evaluated);

            if (evaluated.getResult() == ApplicabilityResultState.APPLICABLE) {
                applicableCount++;
                String domainName = evaluated.getDomain() != null ? evaluated.getDomain().name() : "OTHER";
                applicableByDomain.put(domainName, applicableByDomain.getOrDefault(domainName, 0) + 1);
            } else if (evaluated.getResult() == ApplicabilityResultState.NOT_APPLICABLE) {
                notApplicableCount++;
            } else if (evaluated.getResult() == ApplicabilityResultState.INSUFFICIENT_DATA) {
                insufficientDataCount++;
            }
        }

        ComplianceApplicabilitySummaryDto summary = ComplianceApplicabilitySummaryDto.builder()
                .clientId(clientId)
                .evaluationDate(evalDate)
                .totalEvaluatedRules(allEvaluated.size())
                .applicableCount(applicableCount)
                .notApplicableCount(notApplicableCount)
                .insufficientDataCount(insufficientDataCount)
                .applicableByDomain(applicableByDomain)
                .profileConfigured(facts.isProfilePresent())
                .evaluatedAt(Instant.now())
                .build();

        // 4. Apply optional filters
        List<EvaluatedRuleApplicabilityDto> filteredRules = allEvaluated.stream()
                .filter(r -> domainFilter == null || r.getDomain() == domainFilter)
                .filter(r -> statusFilter == null || r.getResult() == statusFilter)
                .collect(Collectors.toList());

        return ClientComplianceApplicabilityDto.builder()
                .clientId(clientId)
                .clientDisplayName(client.getDisplayName())
                .clientType(client.getClientType() != null ? client.getClientType().name() : null)
                .evaluationDate(evalDate)
                .summary(summary)
                .rules(filteredRules)
                .evaluatedAt(Instant.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluatedRuleApplicabilityDto evaluateClientRule(
            UUID clientId,
            String ruleCode,
            LocalDate evaluationDate) {

        UUID orgId = TenantContext.getTenantId();
        if (orgId == null) {
            throw new BadRequestException("Organization tenant context is required.");
        }
        if (ruleCode == null || ruleCode.isBlank()) {
            throw new BadRequestException("Rule code must not be null or blank.");
        }

        LocalDate evalDate = evaluationDate != null ? evaluationDate : LocalDate.now();

        // 1. Resolve facts
        ComplianceFactContext facts = factProvider.resolveFacts(orgId, clientId);

        // 2. Resolve rule (tenant custom takes precedence over system rule)
        String normalizedCode = ruleCode.trim().toUpperCase();
        ComplianceRuleEntity rule = ruleRepository.findByRuleCodeAndOrganizationId(normalizedCode, orgId)
                .or(() -> ruleRepository.findByRuleCodeAndOrganizationIdIsNull(normalizedCode))
                .orElseThrow(() -> new ResourceNotFoundException("Compliance rule not found with code: '" + normalizedCode + "'"));

        // 3. Evaluate rule
        return ruleEvaluator.evaluate(rule, facts, evalDate);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceApplicabilitySummaryDto getApplicabilitySummary(UUID clientId, LocalDate evaluationDate) {
        UUID orgId = TenantContext.getTenantId();
        if (orgId == null) {
            throw new BadRequestException("Organization tenant context is required.");
        }
        return getApplicabilitySummary(orgId, clientId, evaluationDate)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: '" + clientId + "'"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComplianceApplicabilitySummaryDto> getApplicabilitySummary(UUID organizationId, UUID clientId, LocalDate evaluationDate) {
        if (organizationId == null || clientId == null) {
            return Optional.empty();
        }

        Optional<ClientEntity> clientOpt = clientRepository.findByIdAndOrganizationId(clientId, organizationId);
        if (clientOpt.isEmpty()) {
            return Optional.empty();
        }

        LocalDate evalDate = evaluationDate != null ? evaluationDate : LocalDate.now();
        ComplianceFactContext facts = factProvider.resolveFacts(organizationId, clientId);
        List<ComplianceRuleEntity> rules = loadEffectiveRules(organizationId);

        Map<String, Integer> applicableByDomain = new HashMap<>();
        int applicableCount = 0;
        int notApplicableCount = 0;
        int insufficientDataCount = 0;

        for (ComplianceRuleEntity rule : rules) {
            EvaluatedRuleApplicabilityDto evaluated = ruleEvaluator.evaluate(rule, facts, evalDate);
            if (evaluated.getResult() == ApplicabilityResultState.APPLICABLE) {
                applicableCount++;
                String domainName = evaluated.getDomain() != null ? evaluated.getDomain().name() : "OTHER";
                applicableByDomain.put(domainName, applicableByDomain.getOrDefault(domainName, 0) + 1);
            } else if (evaluated.getResult() == ApplicabilityResultState.NOT_APPLICABLE) {
                notApplicableCount++;
            } else if (evaluated.getResult() == ApplicabilityResultState.INSUFFICIENT_DATA) {
                insufficientDataCount++;
            }
        }

        return Optional.of(ComplianceApplicabilitySummaryDto.builder()
                .clientId(clientId)
                .evaluationDate(evalDate)
                .totalEvaluatedRules(rules.size())
                .applicableCount(applicableCount)
                .notApplicableCount(notApplicableCount)
                .insufficientDataCount(insufficientDataCount)
                .applicableByDomain(applicableByDomain)
                .profileConfigured(facts.isProfilePresent())
                .evaluatedAt(Instant.now())
                .build());
    }

    /**
     * Loads active catalog rules with tenant override deduplication.
     * If tenant defines custom rule with same code as system rule, custom rule takes precedence.
     */
    private List<ComplianceRuleEntity> loadEffectiveRules(UUID organizationId) {
        List<ComplianceRuleEntity> allRules = ruleRepository.findRulesForTenant(
                organizationId,
                true,
                null,
                null,
                ComplianceRuleStatus.ACTIVE,
                null
        );

        // Deduplicate by ruleCode: custom rule overrides system rule
        Map<String, ComplianceRuleEntity> effectiveMap = new LinkedHashMap<>();
        for (ComplianceRuleEntity rule : allRules) {
            String code = rule.getRuleCode().toUpperCase();
            if (!effectiveMap.containsKey(code) || !rule.isSystemRule()) {
                effectiveMap.put(code, rule);
            }
        }

        return new ArrayList<>(effectiveMap.values());
    }
}
