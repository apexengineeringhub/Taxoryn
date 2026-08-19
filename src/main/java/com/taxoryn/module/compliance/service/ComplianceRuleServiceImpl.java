package com.taxoryn.module.compliance.service;

import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.compliance.dto.ComplianceRuleDto;
import com.taxoryn.module.compliance.dto.CreateComplianceRuleRequest;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.mapper.ComplianceMapper;
import com.taxoryn.module.compliance.repository.ComplianceRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceRuleServiceImpl implements ComplianceRuleService {

    private final ComplianceRuleRepository complianceRuleRepository;
    private final ComplianceMapper complianceMapper;

    @Override
    @Transactional
    public ComplianceRuleDto createCustomRule(CreateComplianceRuleRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        String formattedCode = request.getRuleCode().toUpperCase().trim();
        if (complianceRuleRepository.existsByOrganizationIdAndRuleCode(organizationId, formattedCode)) {
            throw new DuplicateResourceException("Compliance Rule", "ruleCode", formattedCode);
        }

        ComplianceRuleEntity rule = ComplianceRuleEntity.builder()
                .ruleCode(formattedCode)
                .name(request.getName().trim())
                .complianceType(request.getComplianceType())
                .frequency(request.getFrequency())
                .dueDay(request.getDueDay())
                .dueMonthOffset(request.getDueMonthOffset())
                .fixedDueMonth(request.getFixedDueMonth())
                .descriptionTemplate(request.getDescriptionTemplate())
                .applicableClientTypes(request.getApplicableClientTypes())
                .active(true)
                .systemRule(false)
                .build();
        rule.setOrganizationId(organizationId);

        ComplianceRuleEntity saved = complianceRuleRepository.save(rule);
        log.info("Created custom compliance rule: id={}, code={} for tenant={}", saved.getId(), saved.getRuleCode(), organizationId);
        return complianceMapper.toRuleDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceRuleDto> getActiveRules() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<ComplianceRuleEntity> rules = complianceRuleRepository.findActiveRulesForOrganization(organizationId);
        return complianceMapper.toRuleDtoList(rules);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceRuleDto getRuleById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ComplianceRuleEntity rule = complianceRuleRepository.findByIdAndOrganizationIdOrSystem(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance Rule", "id", id));
        return complianceMapper.toRuleDto(rule);
    }

    @Override
    public LocalDate calculateDueDate(ComplianceRuleEntity rule, String period) {
        if (rule == null || !StringUtils.hasText(period)) {
            return LocalDate.now().plusDays(15);
        }

        try {
            switch (rule.getFrequency()) {
                case MONTHLY -> {
                    // Expecting period like "2026-08"
                    YearMonth ym = parseYearMonth(period);
                    YearMonth targetYm = ym.plusMonths(Math.max(1, rule.getDueMonthOffset()));
                    int day = Math.min(rule.getDueDay(), targetYm.lengthOfMonth());
                    return targetYm.atDay(day);
                }
                case QUARTERLY -> {
                    // Expecting period like "2026-Q1", "2026-Q2", "2026-Q3", "2026-Q4" or month "2026-06"
                    YearMonth quarterEndYm = parseQuarterEnd(period);
                    YearMonth targetYm = quarterEndYm.plusMonths(Math.max(1, rule.getDueMonthOffset()));
                    if (rule.getFixedDueMonth() != null && rule.getFixedDueMonth() > 0) {
                        int year = targetYm.getYear();
                        targetYm = YearMonth.of(year, rule.getFixedDueMonth());
                    }
                    int day = Math.min(rule.getDueDay(), targetYm.lengthOfMonth());
                    return targetYm.atDay(day);
                }
                case ANNUALLY -> {
                    // Expecting period like "2026-27" or "2026"
                    int startYear = parseStartYear(period);
                    int dueMonth = rule.getFixedDueMonth() != null && rule.getFixedDueMonth() > 0 ? rule.getFixedDueMonth() : 7;
                    YearMonth ym = YearMonth.of(startYear, dueMonth);
                    int day = Math.min(rule.getDueDay(), ym.lengthOfMonth());
                    return ym.atDay(day);
                }
                case ONE_TIME -> {
                    return LocalDate.now().plusDays(rule.getDueDay() > 0 ? rule.getDueDay() : 30);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to dynamically compute due date for rule {} and period {}: {}", rule.getRuleCode(), period, ex.getMessage());
        }

        return LocalDate.now().plusDays(15);
    }

    @Override
    public String formatTitle(ComplianceRuleEntity rule, String period) {
        if (rule == null) return "Compliance Obligation for " + period;
        return rule.getName() + " (" + period + ")";
    }

    @Override
    public String formatDescription(ComplianceRuleEntity rule, String period) {
        if (rule == null || !StringUtils.hasText(rule.getDescriptionTemplate())) {
            return "Statutory compliance filing obligation for period " + period;
        }
        return rule.getDescriptionTemplate().replace("{period}", period);
    }

    @Override
    public boolean isRuleApplicableToClient(ComplianceRuleEntity rule, ClientEntity client) {
        if (rule == null || client == null) return true;
        if (!StringUtils.hasText(rule.getApplicableClientTypes())) {
            return true;
        }

        String clientTypeName = client.getClientType() != null ? client.getClientType().name() : "";
        List<String> types = Arrays.stream(rule.getApplicableClientTypes().split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();

        return types.contains(clientTypeName);
    }

    private YearMonth parseYearMonth(String period) {
        if (period.contains("-") && period.length() >= 7) {
            String[] parts = period.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            return YearMonth.of(year, month);
        }
        return YearMonth.now();
    }

    private YearMonth parseQuarterEnd(String period) {
        int year = LocalDate.now().getYear();
        if (period.contains("-Q1") || period.toUpperCase().endsWith("Q1")) {
            int y = parseLeadingYear(period, year);
            return YearMonth.of(y, 6); // Q1 (Apr-Jun) ends in June
        } else if (period.contains("-Q2") || period.toUpperCase().endsWith("Q2")) {
            int y = parseLeadingYear(period, year);
            return YearMonth.of(y, 9); // Q2 (Jul-Sep) ends in September
        } else if (period.contains("-Q3") || period.toUpperCase().endsWith("Q3")) {
            int y = parseLeadingYear(period, year);
            return YearMonth.of(y, 12); // Q3 (Oct-Dec) ends in December
        } else if (period.contains("-Q4") || period.toUpperCase().endsWith("Q4")) {
            int y = parseLeadingYear(period, year);
            return YearMonth.of(y + 1, 3); // Q4 (Jan-Mar) ends in March of next calendar year
        }
        return parseYearMonth(period);
    }

    private int parseStartYear(String period) {
        if (period.contains("-")) {
            String leading = period.split("-")[0];
            return Integer.parseInt(leading);
        }
        if (period.length() >= 4) {
            return Integer.parseInt(period.substring(0, 4));
        }
        return LocalDate.now().getYear();
    }

    private int parseLeadingYear(String period, int defaultYear) {
        try {
            if (period.contains("-")) {
                return Integer.parseInt(period.split("-")[0]);
            }
            if (period.length() >= 4) {
                return Integer.parseInt(period.substring(0, 4));
            }
        } catch (Exception ignored) {}
        return defaultYear;
    }
}
