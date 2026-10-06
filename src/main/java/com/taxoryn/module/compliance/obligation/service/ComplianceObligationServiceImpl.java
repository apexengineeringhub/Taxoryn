package com.taxoryn.module.compliance.obligation.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.applicability.dto.ClientComplianceApplicabilityDto;
import com.taxoryn.module.compliance.applicability.dto.EvaluatedRuleApplicabilityDto;
import com.taxoryn.module.compliance.applicability.model.ApplicabilityResultState;
import com.taxoryn.module.compliance.applicability.service.ComplianceApplicabilityService;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.UpdateObligationStatusRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.obligation.dto.CancelObligationRequest;
import com.taxoryn.module.compliance.obligation.dto.ComplianceObligationFilterParams;
import com.taxoryn.module.compliance.obligation.dto.ComplianceObligationSummaryDto;
import com.taxoryn.module.compliance.obligation.dto.GenerateObligationsRequest;
import com.taxoryn.module.compliance.obligation.dto.GeneratedObligationsResponseDto;
import com.taxoryn.module.compliance.obligation.util.PeriodValidator;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceObligationServiceImpl implements ComplianceObligationService {

    private final ComplianceObligationRepository obligationRepository;
    private final ClientRepository clientRepository;
    private final ComplianceApplicabilityService applicabilityService;
    private final AuditService auditService;

    @Override
    @Transactional
    public GeneratedObligationsResponseDto generateObligations(UUID clientId, GenerateObligationsRequest request) {
        UUID orgId = TenantContext.getTenantId();
        ClientEntity client = getClientOrThrow(clientId, orgId);

        PeriodValidator.validatePeriodKey(request.getPeriodType(), request.getPeriodKey());
        String periodKey = request.getPeriodKey().trim();
        CompliancePeriodType periodType = request.getPeriodType();

        // 1. Evaluate applicability for this client
        ClientComplianceApplicabilityDto applicability = applicabilityService.evaluateClientApplicability(
                clientId, java.time.LocalDate.now(), request.getDomain(), null);

        List<EvaluatedRuleApplicabilityDto> evaluatedRules = applicability.getRules();

        // If specific rule was requested, check explicit evaluation status
        if (request.getRuleCode() != null && !request.getRuleCode().trim().isEmpty()) {
            String targetCode = request.getRuleCode().trim();
            Optional<EvaluatedRuleApplicabilityDto> targetEval = evaluatedRules.stream()
                    .filter(r -> r.getRuleCode().equalsIgnoreCase(targetCode))
                    .findFirst();

            if (targetEval.isPresent()) {
                EvaluatedRuleApplicabilityDto ruleEval = targetEval.get();
                if (ruleEval.getResult() == ApplicabilityResultState.NOT_APPLICABLE) {
                    throw new BadRequestException("Cannot create obligation: Rule '" + targetCode + "' is NOT_APPLICABLE for client. Reason: " + ruleEval.getReason());
                }
                if (ruleEval.getResult() == ApplicabilityResultState.INSUFFICIENT_DATA) {
                    throw new BadRequestException("Cannot create obligation: Insufficient data to evaluate rule '" + targetCode + "'. Reason: " + ruleEval.getReason());
                }
            } else {
                throw new ResourceNotFoundException("Rule not found in catalog or domain: '" + targetCode + "'");
            }
        }

        // 2. Filter strictly APPLICABLE and frequency-compatible rules
        List<EvaluatedRuleApplicabilityDto> applicableRules = evaluatedRules.stream()
                .filter(r -> r.getResult() == ApplicabilityResultState.APPLICABLE)
                .filter(r -> request.getRuleCode() == null || request.getRuleCode().trim().isEmpty() || r.getRuleCode().equalsIgnoreCase(request.getRuleCode().trim()))
                .filter(r -> PeriodValidator.isCompatible(r.getFrequency(), periodType))
                .toList();

        List<ComplianceObligationDto> resultDtos = new ArrayList<>();
        int createdCount = 0;
        int existingCount = 0;

        for (EvaluatedRuleApplicabilityDto ruleEval : applicableRules) {
            Integer version = 1;
            
            // 3. Idempotency Check: query existing obligation
            Optional<ComplianceObligationEntity> existingOpt = obligationRepository
                    .findByOrganizationIdAndClientIdAndRuleCodeAndRuleVersionAndPeriodTypeAndPeriodKey(
                            orgId, clientId, ruleEval.getRuleCode(), version, periodType, periodKey);

            if (existingOpt.isPresent()) {
                existingCount++;
                resultDtos.add(mapToDto(existingOpt.get(), client));
            } else {
                // 4. Create new obligation snapshot
                ComplianceObligationEntity newObligation = ComplianceObligationEntity.builder()
                        .clientId(clientId)
                        .ruleId(ruleEval.getRuleId())
                        .ruleCode(ruleEval.getRuleCode())
                        .ruleVersion(version)
                        .ruleNameSnapshot(ruleEval.getRuleName())
                        .domain(ruleEval.getDomain() != null ? ruleEval.getDomain() : ComplianceRuleDomain.OTHER)
                        .periodType(periodType)
                        .periodKey(periodKey)
                        .periodLabel(periodKey)
                        .title(ruleEval.getRuleName() + " - " + periodKey)
                        .obligationType(mapDomainToObligationType(ruleEval.getDomain()))
                        .status(ComplianceObligationStatus.UPCOMING)
                        .priority(TaskPriority.MEDIUM)
                        .applicabilityState(ApplicabilityResultState.APPLICABLE)
                        .applicabilityReason(ruleEval.getReason())
                        .generatedAt(Instant.now())
                        .build();
                newObligation.setOrganizationId(orgId);

                try {
                    ComplianceObligationEntity saved = obligationRepository.save(newObligation);
                    createdCount++;

                    // Emit Audit Event only on creation
                    auditService.logEvent(
                            "COMPLIANCE_OBLIGATION_CREATED",
                            "COMPLIANCE_OBLIGATION",
                            saved.getId().toString(),
                            null,
                            "Generated obligation for rule " + ruleEval.getRuleCode() + " (" + ruleEval.getRuleName() + ") period " + periodKey
                    );

                    log.info("Generated compliance obligation id={} for client={}, rule={}, period={}",
                            saved.getId(), clientId, ruleEval.getRuleCode(), periodKey);

                    resultDtos.add(mapToDto(saved, client));
                } catch (DataIntegrityViolationException ex) {
                    // Handled concurrency collision: reload existing record
                    log.warn("Concurrent obligation generation conflict caught for client={}, rule={}, period={}",
                            clientId, ruleEval.getRuleCode(), periodKey);
                    ComplianceObligationEntity existing = obligationRepository
                            .findByOrganizationIdAndClientIdAndRuleCodeAndRuleVersionAndPeriodTypeAndPeriodKey(
                                    orgId, clientId, ruleEval.getRuleCode(), version, periodType, periodKey)
                            .orElseThrow(() -> ex);
                    existingCount++;
                    resultDtos.add(mapToDto(existing, client));
                }
            }
        }

        return GeneratedObligationsResponseDto.builder()
                .clientId(clientId)
                .periodType(periodType)
                .periodKey(periodKey)
                .totalCount(resultDtos.size())
                .createdCount(createdCount)
                .existingCount(existingCount)
                .obligations(resultDtos)
                .build();
    }

    @Override
    @Transactional
    public ComplianceObligationDto createOrGetSingleObligation(UUID clientId, String ruleCode, CompliancePeriodType periodType, String periodKey) {
        if (ruleCode == null || ruleCode.trim().isEmpty()) {
            throw new BadRequestException("Rule code is required");
        }

        GenerateObligationsRequest req = GenerateObligationsRequest.builder()
                .periodType(periodType)
                .periodKey(periodKey)
                .ruleCode(ruleCode.trim())
                .build();

        GeneratedObligationsResponseDto resp = generateObligations(clientId, req);
        if (resp.getObligations().isEmpty()) {
            throw new BadRequestException("No obligation generated for rule '" + ruleCode + "' and period '" + periodKey + "' (check applicability and period compatibility).");
        }
        return resp.getObligations().get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceObligationDto getObligationById(UUID clientId, UUID obligationId) {
        UUID orgId = TenantContext.getTenantId();
        ClientEntity client = getClientOrThrow(clientId, orgId);

        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(obligationId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance obligation not found with id: '" + obligationId + "'"));

        if (!obligation.getClientId().equals(clientId)) {
            throw new ResourceNotFoundException("Compliance obligation not found for client id: '" + clientId + "'");
        }

        return mapToDto(obligation, client);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceObligationDto> listClientObligations(UUID clientId, ComplianceObligationFilterParams params) {
        UUID orgId = TenantContext.getTenantId();
        ClientEntity client = getClientOrThrow(clientId, orgId);

        List<ComplianceObligationEntity> entities = obligationRepository.findAllByOrganizationIdAndClientId(
                orgId, clientId, Sort.by(Sort.Direction.DESC, "createdAt"));

        return entities.stream()
                .filter(e -> params == null || params.getStatus() == null || e.getStatus() == params.getStatus())
                .filter(e -> params == null || params.getDomain() == null || e.getDomain() == params.getDomain())
                .filter(e -> params == null || params.getPeriodType() == null || e.getPeriodType() == params.getPeriodType())
                .filter(e -> params == null || params.getPeriodKey() == null || params.getPeriodKey().trim().isEmpty() || (e.getPeriodKey() != null && e.getPeriodKey().equalsIgnoreCase(params.getPeriodKey().trim())))
                .filter(e -> params == null || params.getRuleCode() == null || params.getRuleCode().trim().isEmpty() || (e.getRuleCode() != null && e.getRuleCode().equalsIgnoreCase(params.getRuleCode().trim())))
                .map(e -> mapToDto(e, client))
                .toList();
    }

    @Override
    @Transactional
    public ComplianceObligationDto updateObligationStatus(UUID clientId, UUID obligationId, UpdateObligationStatusRequest request) {
        UUID orgId = TenantContext.getTenantId();
        ClientEntity client = getClientOrThrow(clientId, orgId);

        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(obligationId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance obligation not found with id: '" + obligationId + "'"));

        if (!obligation.getClientId().equals(clientId)) {
            throw new ResourceNotFoundException("Compliance obligation not found for client id: '" + clientId + "'");
        }

        ComplianceObligationStatus targetStatus = request.getStatus();
        if (targetStatus == null) {
            throw new BadRequestException("Target obligation status is required");
        }

        if (!obligation.getStatus().canTransitionTo(targetStatus)) {
            throw new BadRequestException("Invalid obligation status transition from " + obligation.getStatus() + " to " + targetStatus);
        }

        ComplianceObligationStatus previousStatus = obligation.getStatus();
        obligation.setStatus(targetStatus);

        if (request.getNotes() != null) {
            obligation.setNotes(request.getNotes().trim());
        }

        if (targetStatus == ComplianceObligationStatus.COMPLETED) {
            obligation.setCompletedAt(Instant.now());
        }

        ComplianceObligationEntity saved = obligationRepository.save(obligation);

        auditService.logEvent(
                targetStatus == ComplianceObligationStatus.COMPLETED ? "COMPLIANCE_OBLIGATION_COMPLETED" : "COMPLIANCE_OBLIGATION_STATUS_CHANGED",
                "COMPLIANCE_OBLIGATION",
                saved.getId().toString(),
                null,
                "Status changed from " + previousStatus + " to " + targetStatus + " for obligation " + saved.getTitle()
        );

        log.info("Updated status of obligation id={} for client={} to {}", obligationId, clientId, targetStatus);
        return mapToDto(saved, client);
    }

    @Override
    @Transactional
    public ComplianceObligationDto cancelObligation(UUID clientId, UUID obligationId, CancelObligationRequest request) {
        UUID orgId = TenantContext.getTenantId();
        ClientEntity client = getClientOrThrow(clientId, orgId);

        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(obligationId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance obligation not found with id: '" + obligationId + "'"));

        if (!obligation.getClientId().equals(clientId)) {
            throw new ResourceNotFoundException("Compliance obligation not found for client id: '" + clientId + "'");
        }

        if (request == null || request.getCancellationReason() == null || request.getCancellationReason().trim().isEmpty()) {
            throw new BadRequestException("Cancellation reason is required to cancel a compliance obligation");
        }

        if (obligation.getStatus() == ComplianceObligationStatus.CANCELLED) {
            return mapToDto(obligation, client);
        }

        if (!obligation.getStatus().canTransitionTo(ComplianceObligationStatus.CANCELLED)) {
            throw new BadRequestException("Cannot cancel obligation in terminal state: " + obligation.getStatus());
        }

        obligation.setStatus(ComplianceObligationStatus.CANCELLED);
        obligation.setCancelledAt(Instant.now());
        obligation.setCancellationReason(request.getCancellationReason().trim());

        ComplianceObligationEntity saved = obligationRepository.save(obligation);

        auditService.logEvent(
                "COMPLIANCE_OBLIGATION_CANCELLED",
                "COMPLIANCE_OBLIGATION",
                saved.getId().toString(),
                null,
                "Cancelled obligation: " + saved.getTitle() + ". Reason: " + saved.getCancellationReason()
        );

        log.info("Cancelled obligation id={} for client={}. Reason: {}", obligationId, clientId, saved.getCancellationReason());
        return mapToDto(saved, client);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceObligationSummaryDto getClientObligationSummary(UUID clientId) {
        UUID orgId = TenantContext.getTenantId();
        getClientOrThrow(clientId, orgId);

        List<ComplianceObligationEntity> obligations = obligationRepository.findAllByOrganizationIdAndClientId(orgId, clientId);

        long openCount = 0;
        long inProgressCount = 0;
        long completedCount = 0;
        long cancelledCount = 0;
        Map<String, Long> byDomain = new HashMap<>();
        Map<String, Long> byStatus = new HashMap<>();

        for (ComplianceObligationEntity ob : obligations) {
            ComplianceObligationStatus status = ob.getStatus();
            if (status != null) {
                byStatus.merge(status.name(), 1L, Long::sum);
                switch (status) {
                    case UPCOMING, READY -> openCount++;
                    case IN_PROGRESS, WAITING_FOR_CLIENT, READY_FOR_FILING, FILED, OVERDUE -> inProgressCount++;
                    case COMPLETED -> completedCount++;
                    case CANCELLED -> cancelledCount++;
                }
            }

            ComplianceRuleDomain domain = ob.getDomain();
            String domainKey = domain != null ? domain.name() : (ob.getComplianceType() != null ? ob.getComplianceType().name() : "OTHER");
            byDomain.merge(domainKey, 1L, Long::sum);
        }

        return ComplianceObligationSummaryDto.builder()
                .totalCount(obligations.size())
                .openCount(openCount)
                .inProgressCount(inProgressCount)
                .completedCount(completedCount)
                .cancelledCount(cancelledCount)
                .byDomain(byDomain)
                .byStatus(byStatus)
                .build();
    }

    private ClientEntity getClientOrThrow(UUID clientId, UUID orgId) {
        return clientRepository.findByIdAndOrganizationId(clientId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: '" + clientId + "'"));
    }

    private ComplianceObligationType mapDomainToObligationType(ComplianceRuleDomain domain) {
        if (domain == null) return ComplianceObligationType.OTHER;
        return switch (domain) {
            case GST -> ComplianceObligationType.GST_RETURN;
            case TDS -> ComplianceObligationType.TDS_RETURN;
            case INCOME_TAX -> ComplianceObligationType.ITR_FILING;
            case MCA_ROC -> ComplianceObligationType.ROC_COMPLIANCE;
            case STATUTORY_AUDIT, PAYROLL_LABOUR -> ComplianceObligationType.ADVANCE_TAX;
            default -> ComplianceObligationType.OTHER;
        };
    }

    private ComplianceObligationDto mapToDto(ComplianceObligationEntity entity, ClientEntity client) {
        return ComplianceObligationDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .clientName(client != null ? client.getDisplayName() : null)
                .clientDisplayName(client != null ? client.getDisplayName() : null)
                .pan(client != null ? client.getPan() : null)
                .ruleId(entity.getRuleId())
                .ruleCode(entity.getRuleCode())
                .ruleVersion(entity.getRuleVersion())
                .ruleNameSnapshot(entity.getRuleNameSnapshot())
                .domain(entity.getDomain())
                .periodType(entity.getPeriodType())
                .periodKey(entity.getPeriodKey())
                .applicabilityState(entity.getApplicabilityState())
                .applicabilityReason(entity.getApplicabilityReason())
                .generatedAt(entity.getGeneratedAt())
                .cancelledAt(entity.getCancelledAt())
                .cancelledBy(entity.getCancelledBy())
                .cancellationReason(entity.getCancellationReason())
                .title(entity.getTitle() != null ? entity.getTitle() : (entity.getRuleNameSnapshot() != null ? entity.getRuleNameSnapshot() : "Compliance Obligation"))
                .description(entity.getDescription())
                .obligationType(entity.getObligationType())
                .periodLabel(entity.getPeriodLabel() != null ? entity.getPeriodLabel() : entity.getPeriodKey())
                .financialYear(entity.getFinancialYear())
                .assessmentYear(entity.getAssessmentYear())
                .statutoryDueDate(entity.getStatutoryDueDate())
                .internalTargetDate(entity.getInternalTargetDate())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .assignedUserId(entity.getAssignedUserId())
                .assignedEmployeeId(entity.getAssignedEmployeeId())
                .completedAt(entity.getCompletedAt())
                .completedBy(entity.getCompletedBy())
                .filedDate(entity.getFiledDate())
                .filedAt(entity.getFiledAt())
                .filedBy(entity.getFiledBy())
                .notes(entity.getNotes())
                .dueDate(entity.getDueDate() != null ? entity.getDueDate() : entity.getStatutoryDueDate())
                .period(entity.getPeriod() != null ? entity.getPeriod() : entity.getPeriodKey())
                .complianceType(entity.getComplianceType() != null ? entity.getComplianceType().name() : (entity.getDomain() != null ? entity.getDomain().name() : null))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
