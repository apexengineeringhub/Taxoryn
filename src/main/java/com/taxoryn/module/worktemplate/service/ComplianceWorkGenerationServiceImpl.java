package com.taxoryn.module.worktemplate.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.compliance.work.dto.ComplianceWorkGenerationRequest;
import com.taxoryn.module.compliance.work.dto.ComplianceWorkGenerationResultDto;
import com.taxoryn.module.compliance.work.dto.ComplianceWorkGenerationResultDto.GenerationStatus;
import com.taxoryn.module.compliance.work.port.ComplianceWorkGenerationPort;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.worktemplate.entity.WorkInstanceEntity;
import com.taxoryn.module.worktemplate.entity.WorkTemplateEntity;
import com.taxoryn.module.worktemplate.entity.WorkTemplateTaskEntity;
import com.taxoryn.module.worktemplate.model.WorkInstanceStatus;
import com.taxoryn.module.worktemplate.model.WorkTemplateStatus;
import com.taxoryn.module.worktemplate.repository.WorkInstanceRepository;
import com.taxoryn.module.worktemplate.repository.WorkTemplateRepository;
import com.taxoryn.module.worktemplate.repository.WorkTemplateTaskRepository;
import com.taxoryn.module.service.model.ServiceCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Work-module implementation of {@link ComplianceWorkGenerationPort}.
 *
 * <p>This service generates (or idempotently retrieves) a {@link WorkInstanceEntity}
 * from a compliance obligation request. It lives in the work module and owns all
 * access to {@link WorkInstanceRepository} and {@link TaskRepository}.
 *
 * <p>Architectural invariants:
 * <ul>
 *   <li>Compliance module calls ONLY via {@link ComplianceWorkGenerationPort}.</li>
 *   <li>This class NEVER imports compliance-domain repositories.</li>
 *   <li>Idempotency is guaranteed via the DB unique constraint on
 *       {@code (organization_id, compliance_obligation_id)} in work_instances.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceWorkGenerationServiceImpl implements ComplianceWorkGenerationPort {

    private final WorkTemplateRepository workTemplateRepository;
    private final WorkTemplateTaskRepository workTemplateTaskRepository;
    private final WorkInstanceRepository workInstanceRepository;
    private final TaskRepository taskRepository;
    private final EngagementRepository engagementRepository;
    private final AuditService auditService;

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    @Override
    @Transactional
    public ComplianceWorkGenerationResultDto generateWork(ComplianceWorkGenerationRequest request) {
        UUID orgId = request.getOrganizationId();
        UUID obligationId = request.getObligationId();

        log.info("ComplianceWorkGeneration: obligationId={}, ruleCode={}, templateCode={}, org={}",
                obligationId, request.getRuleCode(), request.getWorkTemplateCode(), orgId);

        // ---------------------------------------------------------------
        // 1. Idempotency guard — check if work already exists
        // ---------------------------------------------------------------
        Optional<WorkInstanceEntity> existing = workInstanceRepository
                .findByOrganizationIdAndComplianceObligationId(orgId, obligationId);
        if (existing.isPresent()) {
            log.info("ComplianceWorkGeneration: ALREADY_EXISTS workInstanceId={} for obligationId={}",
                    existing.get().getId(), obligationId);
            return ComplianceWorkGenerationResultDto.builder()
                    .status(GenerationStatus.ALREADY_EXISTS)
                    .workInstanceId(existing.get().getId())
                    .obligationId(obligationId)
                    .message("Work instance already exists for this obligation.")
                    .build();
        }

        // ---------------------------------------------------------------
        // 2. Resolve work template by code
        // ---------------------------------------------------------------
        if (!StringUtils.hasText(request.getWorkTemplateCode())) {
            log.warn("ComplianceWorkGeneration: TEMPLATE_NOT_CONFIGURED for obligationId={}", obligationId);
            return ComplianceWorkGenerationResultDto.builder()
                    .status(GenerationStatus.TEMPLATE_NOT_CONFIGURED)
                    .obligationId(obligationId)
                    .message("No work template code is configured on the compliance rule. Set defaultWorkTemplateCode.")
                    .build();
        }

        List<WorkTemplateEntity> templates = workTemplateRepository
                .findAccessibleByTemplateCode(request.getWorkTemplateCode(), orgId);
        if (templates.isEmpty()) {
            log.warn("ComplianceWorkGeneration: TEMPLATE_NOT_FOUND code={} for obligationId={}",
                    request.getWorkTemplateCode(), obligationId);
            return ComplianceWorkGenerationResultDto.builder()
                    .status(GenerationStatus.TEMPLATE_NOT_FOUND)
                    .obligationId(obligationId)
                    .message("Work template '" + request.getWorkTemplateCode() + "' not found or is not active.")
                    .build();
        }
        // Prefer org-specific (non-null orgId) over system default
        WorkTemplateEntity template = templates.stream()
                .filter(t -> t.getOrganizationId() != null)
                .findFirst()
                .orElse(templates.get(0));

        // ---------------------------------------------------------------
        // 3. Resolve active engagement for client + service
        // ---------------------------------------------------------------
        EngagementEntity engagement = resolveEngagement(request, template, orgId);
        if (engagement == null) {
            log.warn("ComplianceWorkGeneration: ENGAGEMENT_NOT_CONFIGURED clientId={} for obligationId={}",
                    request.getClientId(), obligationId);
            return ComplianceWorkGenerationResultDto.builder()
                    .status(GenerationStatus.ENGAGEMENT_NOT_CONFIGURED)
                    .obligationId(obligationId)
                    .message("No active engagement found for client matching this compliance service.")
                    .build();
        }

        // ---------------------------------------------------------------
        // 4. Build work instance
        // ---------------------------------------------------------------
        LocalDate periodStart = request.getPeriodStart();
        LocalDate periodEnd = request.getPeriodEnd();
        LocalDate dueDate = request.getDueDate();

        // Fallback: if period dates are null, use current month boundaries
        if (periodStart == null) {
            periodStart = LocalDate.now().withDayOfMonth(1);
        }
        if (periodEnd == null) {
            periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        }

        String title = buildTitle(template, request, periodStart);

        WorkInstanceEntity instance = WorkInstanceEntity.builder()
                .engagementId(engagement.getId())
                .templateId(template.getId())
                .title(title)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .dueDate(dueDate)
                .status(WorkInstanceStatus.NOT_STARTED)
                .assignedUserId(engagement.getAssignedUserId())
                .reviewerUserId(engagement.getReviewerUserId())
                .generatedAt(Instant.now())
                .notes("Generated by Compliance Engine — Rule: " + request.getRuleCode()
                        + " | Period: " + request.getPeriodLabel()
                        + " | ObligationId: " + obligationId)
                .complianceObligationId(obligationId)
                .build();
        instance.setOrganizationId(orgId);

        // ---------------------------------------------------------------
        // 5. Persist with idempotency protection via DB unique constraint
        // ---------------------------------------------------------------
        WorkInstanceEntity saved;
        try {
            saved = workInstanceRepository.save(instance);
        } catch (DataIntegrityViolationException ex) {
            // Race condition — concurrent request already inserted; return ALREADY_EXISTS
            log.warn("ComplianceWorkGeneration: concurrent insert detected for obligationId={}, returning ALREADY_EXISTS", obligationId);
            Optional<WorkInstanceEntity> raceExisting = workInstanceRepository
                    .findByOrganizationIdAndComplianceObligationId(orgId, obligationId);
            return raceExisting.map(w -> ComplianceWorkGenerationResultDto.builder()
                    .status(GenerationStatus.ALREADY_EXISTS)
                    .workInstanceId(w.getId())
                    .obligationId(obligationId)
                    .message("Work instance already exists (concurrent creation detected).")
                    .build()
            ).orElseGet(() -> ComplianceWorkGenerationResultDto.builder()
                    .status(GenerationStatus.FAILED)
                    .obligationId(obligationId)
                    .message("Concurrent insert conflict could not be resolved: " + ex.getMessage())
                    .build());
        }

        log.info("ComplianceWorkGeneration: WorkInstance CREATED id={} for obligationId={}", saved.getId(), obligationId);

        // ---------------------------------------------------------------
        // 6. Create tasks from template
        // ---------------------------------------------------------------
        createTasksFromTemplate(template, saved, engagement, periodStart, orgId);

        // ---------------------------------------------------------------
        // 7. Audit log
        // ---------------------------------------------------------------
        auditService.logEvent("COMPLIANCE_WORK_GENERATED", "WORK_INSTANCE", saved.getId().toString(),
                null, saved);

        return ComplianceWorkGenerationResultDto.builder()
                .status(GenerationStatus.CREATED)
                .workInstanceId(saved.getId())
                .obligationId(obligationId)
                .message("Work instance created successfully.")
                .build();
    }

    // -----------------------------------------------------------------------
    // Engagement Resolution Strategy
    // Priority 1: clientServiceId match (obligation.clientServiceId == engagement.clientServiceId)
    // Priority 2: serviceId match (template.serviceId == engagement.serviceId), most recent ACTIVE
    // -----------------------------------------------------------------------
    private EngagementEntity resolveEngagement(ComplianceWorkGenerationRequest request,
                                               WorkTemplateEntity template,
                                               UUID orgId) {
        UUID clientId = request.getClientId();

        // Priority 1: Match by clientServiceId
        if (request.getClientServiceId() != null) {
            List<EngagementEntity> byClientService = engagementRepository
                    .findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(orgId, clientId);
            Optional<EngagementEntity> matched = byClientService.stream()
                    .filter(e -> e.getStatus() == EngagementStatus.ACTIVE
                            && request.getClientServiceId().equals(e.getClientServiceId()))
                    .findFirst();
            if (matched.isPresent()) {
                return matched.get();
            }
        }

        // Priority 2: Match by serviceId from template
        if (template.getServiceId() != null) {
            List<EngagementEntity> allEngagements = engagementRepository
                    .findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(orgId, clientId);
            return allEngagements.stream()
                    .filter(e -> e.getStatus() == EngagementStatus.ACTIVE
                            && template.getServiceId().equals(e.getServiceId()))
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    // -----------------------------------------------------------------------
    // Task creation from template tasks
    // -----------------------------------------------------------------------
    private void createTasksFromTemplate(WorkTemplateEntity template,
                                         WorkInstanceEntity instance,
                                         EngagementEntity engagement,
                                         LocalDate periodStart,
                                         UUID orgId) {
        List<WorkTemplateTaskEntity> templateTasks = workTemplateTaskRepository
                .findAllByTemplateIdOrderBySequenceOrderAsc(template.getId());

        TaskCategory taskCategory = mapServiceCategoryToTaskCategory(template.getCategory());
        String periodFormatted = periodStart.format(PERIOD_FORMATTER);

        for (WorkTemplateTaskEntity tplTask : templateTasks) {
            if (!tplTask.isActive()) {
                continue;
            }

            LocalDate taskDueDate = periodStart.plusDays(tplTask.getRelativeDueDays());

            TaskEntity task = TaskEntity.builder()
                    .clientId(engagement.getClientId())
                    .engagementId(engagement.getId())
                    .assignedTo(engagement.getAssignedUserId())
                    .workInstanceId(instance.getId())
                    .workTemplateTaskId(tplTask.getId())
                    .locationId(engagement.getLocationId())
                    .title(tplTask.getName() + " — " + periodFormatted)
                    .description(tplTask.getDescription())
                    .taskCategory(taskCategory)
                    .status(TaskStatus.TODO)
                    .priority(tplTask.getDefaultPriority())
                    .startDate(periodStart)
                    .dueDate(taskDueDate)
                    .notes("Generated from compliance obligation via template: " + template.getName())
                    .build();
            task.setOrganizationId(orgId);
            taskRepository.save(task);
        }

        log.info("ComplianceWorkGeneration: created {} tasks for workInstanceId={}", templateTasks.size(), instance.getId());
    }

    // -----------------------------------------------------------------------
    // Title generation
    // -----------------------------------------------------------------------
    private String buildTitle(WorkTemplateEntity template,
                              ComplianceWorkGenerationRequest request,
                              LocalDate periodStart) {
        String label = StringUtils.hasText(request.getPeriodLabel())
                ? request.getPeriodLabel()
                : periodStart.format(PERIOD_FORMATTER);
        return template.getName() + " — " + label;
    }

    // -----------------------------------------------------------------------
    // Map service category to task category
    // -----------------------------------------------------------------------
    private TaskCategory mapServiceCategoryToTaskCategory(ServiceCategory category) {
        if (category == null) return TaskCategory.COMPLIANCE;
        return switch (category) {
            case GST -> TaskCategory.GST;
            case TDS -> TaskCategory.TDS;
            case ITR -> TaskCategory.ITR;
            case AUDIT -> TaskCategory.AUDIT;
            default -> TaskCategory.COMPLIANCE;
        };
    }
}

