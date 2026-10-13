package com.taxoryn.module.compliance.qc.service.impl;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowChecklistItemEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.qc.dto.AuthorizeFilingRequest;
import com.taxoryn.module.compliance.qc.dto.ComplianceQcCheckItemDto;
import com.taxoryn.module.compliance.qc.dto.ComplianceQcReviewCycleDto;
import com.taxoryn.module.compliance.qc.dto.ComplianceQcReviewFindingDto;
import com.taxoryn.module.compliance.qc.dto.ComplianceQcStatusDto;
import com.taxoryn.module.compliance.qc.dto.ComplianceQcValidationResultDto;
import com.taxoryn.module.compliance.qc.dto.CreateReviewFindingRequest;
import com.taxoryn.module.compliance.qc.dto.ResolveFindingRequest;
import com.taxoryn.module.compliance.qc.dto.RespondToFindingRequest;
import com.taxoryn.module.compliance.qc.dto.RevokeAuthorizationRequest;
import com.taxoryn.module.compliance.qc.entity.ComplianceQcReviewFindingEntity;
import com.taxoryn.module.compliance.qc.model.ComplianceQcCheckStatus;
import com.taxoryn.module.compliance.qc.model.ComplianceQcFindingStatus;
import com.taxoryn.module.compliance.qc.model.ComplianceQcStage;
import com.taxoryn.module.compliance.qc.repository.ComplianceQcReviewFindingRepository;
import com.taxoryn.module.compliance.qc.service.ComplianceQualityControlService;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowChecklistItemRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.compliance.service.ComplianceWorkflowService;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ComplianceQualityControlServiceImpl implements ComplianceQualityControlService {

    private static final String RULE_VERSION = "1.0";

    private final ComplianceWorkflowRepository workflowRepository;
    private final ComplianceWorkflowChecklistItemRepository checklistItemRepository;
    private final ComplianceObligationRepository obligationRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PracticeSecurityScopeEvaluator scopeEvaluator;
    private final AuditService auditService;
    private final ComplianceWorkflowService workflowService;
    private final com.taxoryn.module.compliance.qc.repository.ComplianceQcReviewFindingRepository findingRepository;

    public ComplianceQualityControlServiceImpl(
            ComplianceWorkflowRepository workflowRepository,
            ComplianceWorkflowChecklistItemRepository checklistItemRepository,
            ComplianceObligationRepository obligationRepository,
            ClientRepository clientRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            PracticeSecurityScopeEvaluator scopeEvaluator,
            AuditService auditService,
            @Lazy ComplianceWorkflowService workflowService,
            com.taxoryn.module.compliance.qc.repository.ComplianceQcReviewFindingRepository findingRepository
    ) {
        this.workflowRepository = workflowRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.obligationRepository = obligationRepository;
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.scopeEvaluator = scopeEvaluator;
        this.auditService = auditService;
        this.workflowService = workflowService;
        this.findingRepository = findingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceQcStatusDto getQcStatus(UUID workflowId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        ComplianceWorkflowEntity workflow = getWorkflowWithChecklistOrThrow(workflowId, orgId);
        validateClientAccess(scope, workflow.getClientId());

        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), orgId).orElse(null);
        ClientEntity client = clientRepository.findByIdAndOrganizationId(workflow.getClientId(), orgId).orElse(null);

        ComplianceQcValidationResultDto validationResult = evaluateValidation(workflow, obligation, client, orgId);

        ComplianceQcStage stage = determineQcStage(workflow, validationResult);
        String nextStep = determineNextActionableStep(stage, workflow, validationResult);
        List<String> allowedActions = determineAllowedActions(stage, workflow, scope);

        boolean prepComplete = isPreparationComplete(workflow);
        boolean underReview = workflow.getWorkflowStatus() == ComplianceWorkflowStatus.UNDER_REVIEW;
        boolean changesReq = workflow.getWorkflowStatus() == ComplianceWorkflowStatus.CHANGES_REQUIRED;
        boolean approved = workflow.getWorkflowStatus() == ComplianceWorkflowStatus.READY_FOR_FILING || workflow.getApprovedAt() != null;
        boolean authorized = workflow.isAuthorized();
        boolean submitted = workflow.getWorkflowStatus() == ComplianceWorkflowStatus.FILED
                || workflow.getWorkflowStatus() == ComplianceWorkflowStatus.ACKNOWLEDGEMENT_PENDING
                || workflow.getWorkflowStatus() == ComplianceWorkflowStatus.COMPLETED;

        return ComplianceQcStatusDto.builder()
                .workflowId(workflow.getId())
                .complianceObligationId(workflow.getComplianceObligationId())
                .clientId(workflow.getClientId())
                .clientName(client != null ? client.getDisplayName() : null)
                .stage(stage)
                .workflowStatus(workflow.getWorkflowStatus())
                .isPreparationComplete(prepComplete)
                .isValidationPassed(validationResult.isValid())
                .isUnderReview(underReview)
                .isChangesRequired(changesReq)
                .isApproved(approved)
                .isAuthorized(authorized)
                .isSubmitted(submitted)
                .approvedAt(workflow.getApprovedAt())
                .approvedBy(workflow.getApprovedBy())
                .reviewNotes(workflow.getReviewNotes())
                .authorizedAt(workflow.getAuthorizedAt())
                .authorizedBy(workflow.getAuthorizedBy())
                .authorizationMethod(workflow.getAuthorizationMethod())
                .authorizationReference(workflow.getAuthorizationReference())
                .authorizationNotes(workflow.getAuthorizationNotes())
                .filedDate(workflow.getFiledDate())
                .filedAt(workflow.getFiledAt())
                .filedBy(workflow.getFiledBy())
                .acknowledgementNumber(workflow.getAcknowledgementNumber())
                .validationSummary(validationResult)
                .reviewCycle(workflow.getReviewCycle())
                .unresolvedFindingsCount((int) findingRepository.countUnresolvedBlockingFindings(
                        orgId, workflow.getId(), List.of(ComplianceQcFindingStatus.OPEN, ComplianceQcFindingStatus.CORRECTED)))
                .findings(findingRepository.findByOrganizationIdAndWorkflowIdAndReviewCycleOrderByCreatedAtAsc(
                        orgId, workflow.getId(), workflow.getReviewCycle()).stream().map(this::mapFindingToDto).toList())
                .nextActionableStep(nextStep)
                .allowedActions(allowedActions)
                .build();
    }

    @Override
    @Transactional
    public ComplianceQcValidationResultDto validateForReview(UUID workflowId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        ComplianceWorkflowEntity workflow = getWorkflowWithChecklistOrThrow(workflowId, orgId);
        validateClientAccess(scope, workflow.getClientId());

        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), orgId).orElse(null);
        ClientEntity client = clientRepository.findByIdAndOrganizationId(workflow.getClientId(), orgId).orElse(null);

        ComplianceQcValidationResultDto result = evaluateValidation(workflow, obligation, client, orgId);

        // Persist validation freshness fingerprint and timestamp on workflow
        workflow.setLastValidationFingerprint(result.getPreparationFingerprint());
        workflow.setLastValidatedAt(result.getValidatedAt());
        workflowRepository.save(workflow);

        auditService.logEvent(
                "COMPLIANCE_PREPARATION_VALIDATED",
                "COMPLIANCE_WORKFLOW",
                workflow.getId().toString(),
                result.getPreparationFingerprint(),
                result.isValid() ? "VALIDATION_PASSED" : "VALIDATION_BLOCKED: " + result.getBlockers().size() + " blockers"
        );

        return result;
    }

    @Override
    @Transactional
    public ComplianceWorkflowDto authorizeFiling(UUID workflowId, AuthorizeFilingRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        ComplianceWorkflowEntity workflow = getWorkflowWithChecklistOrThrow(workflowId, orgId);
        validateClientAccess(scope, workflow.getClientId());

        requireAuthorizationPermission();

        if (workflow.getWorkflowStatus() != ComplianceWorkflowStatus.READY_FOR_FILING && workflow.getApprovedAt() == null) {
            throw new BusinessValidationException("Filing authorization can only be granted for approved workflows in READY_FOR_FILING status");
        }
        if (workflow.isAuthorized()) {
            throw new BusinessValidationException("Workflow is already authorized for filing");
        }
        if (request == null || request.getAuthorizationMethod() == null) {
            throw new BusinessValidationException("Filing authorization method is required");
        }

        String actorEmail = SecurityUtils.getCurrentUserEmail();
        String authorizedByName = StringUtils.hasText(request.getAuthorizedBy()) ? request.getAuthorizedBy() : actorEmail;

        workflow.setAuthorized(true);
        workflow.setAuthorizedAt(Instant.now());
        workflow.setAuthorizedBy(authorizedByName);
        workflow.setAuthorizationMethod(request.getAuthorizationMethod().name());
        workflow.setAuthorizationReference(request.getAuthorizationReference());
        workflow.setAuthorizationNotes(request.getAuthorizationNotes());

        // Auto-complete checklist item CLIENT_CONFIRMATION_RECEIVED if present
        if (workflow.getChecklistItems() != null) {
            workflow.getChecklistItems().stream()
                    .filter(i -> "CLIENT_CONFIRMATION_RECEIVED".equalsIgnoreCase(i.getItemKey()))
                    .findFirst()
                    .ifPresent(item -> {
                        item.setCompleted(true);
                        if (item.getCompletedAt() == null) item.setCompletedAt(Instant.now());
                        if (item.getCompletedByName() == null) item.setCompletedByName(actorEmail);
                        if (item.getCompletedByUserId() == null) item.setCompletedByUserId(SecurityUtils.getCurrentUserId());
                    });
        }

        ComplianceWorkflowEntity saved = workflowRepository.save(workflow);

        auditService.logEvent(
                "COMPLIANCE_FILING_AUTHORIZED",
                "COMPLIANCE_WORKFLOW",
                saved.getId().toString(),
                "UNAUTHORIZED",
                "AUTHORIZED (" + request.getAuthorizationMethod().name() + ")"
        );

        return workflowService.getWorkflowById(saved.getId()).getWorkflow();
    }

    @Override
    @Transactional
    public ComplianceWorkflowDto revokeFilingAuthorization(UUID workflowId, RevokeAuthorizationRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        ComplianceWorkflowEntity workflow = getWorkflowWithChecklistOrThrow(workflowId, orgId);
        validateClientAccess(scope, workflow.getClientId());

        requireAuthorizationPermission();

        if (workflow.getWorkflowStatus() == ComplianceWorkflowStatus.FILED
                || workflow.getWorkflowStatus() == ComplianceWorkflowStatus.ACKNOWLEDGEMENT_PENDING
                || workflow.getWorkflowStatus() == ComplianceWorkflowStatus.COMPLETED) {
            throw new BusinessValidationException("Cannot revoke filing authorization: return has already been filed or completed");
        }
        if (!workflow.isAuthorized()) {
            throw new BusinessValidationException("Workflow is not currently authorized for filing");
        }
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new BusinessValidationException("Revocation reason is required");
        }

        workflow.setAuthorized(false);
        workflow.setAuthorizedAt(null);
        workflow.setAuthorizedBy(null);
        workflow.setAuthorizationMethod(null);
        workflow.setAuthorizationReference(null);
        workflow.setAuthorizationNotes("Authorization revoked: " + request.getReason());

        ComplianceWorkflowEntity saved = workflowRepository.save(workflow);

        auditService.logEvent(
                "COMPLIANCE_AUTHORIZATION_REVOKED",
                "COMPLIANCE_WORKFLOW",
                saved.getId().toString(),
                "AUTHORIZED",
                "REVOKED: " + request.getReason()
        );

        return workflowService.getWorkflowById(saved.getId()).getWorkflow();
    }

    @Override
    @Transactional(readOnly = true)
    public void verifySubmissionPrerequisites(UUID workflowId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, orgId);

        if (workflow.getWorkflowStatus() != ComplianceWorkflowStatus.READY_FOR_FILING) {
            throw new BusinessValidationException("Workflow must be in READY_FOR_FILING status prior to submission");
        }
        if (workflow.getApprovedAt() == null) {
            throw new BusinessValidationException("Workflow has not received maker-checker review approval");
        }
        if (!workflow.isAuthorized()) {
            throw new BusinessValidationException("Cannot submit compliance filing: explicit filing authorization has not been granted. Authorization is required prior to statutory submission.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyValidationFreshness(UUID workflowId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        ComplianceWorkflowEntity workflow = getWorkflowWithChecklistOrThrow(workflowId, orgId);

        if (workflow.getLastValidationFingerprint() == null || workflow.getLastValidatedAt() == null) {
            throw new BusinessValidationException("Pre-review Quality Control validation has not been executed for this workflow");
        }

        String currentFingerprint = computePreparationFingerprint(workflow);
        if (!currentFingerprint.equals(workflow.getLastValidationFingerprint())) {
            throw new BusinessValidationException("Validation result is stale: preparation data or checkpoints have been modified since last validation. Re-run validation before submitting for review.");
        }

        // Check if any unresolved blocking findings exist from prior review cycles
        long unresolvedBlockers = findingRepository.countUnresolvedBlockingFindings(
                orgId, workflowId, List.of(ComplianceQcFindingStatus.OPEN));
        if (unresolvedBlockers > 0) {
            throw new BusinessValidationException("Workflow has " + unresolvedBlockers + " unresolved blocking review findings that require correction before resubmission");
        }

        // Re-evaluate validation rules to ensure valid=true
        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), orgId).orElse(null);
        ClientEntity client = clientRepository.findByIdAndOrganizationId(workflow.getClientId(), orgId).orElse(null);
        ComplianceQcValidationResultDto validationResult = evaluateValidation(workflow, obligation, client, orgId);
        if (!validationResult.isValid()) {
            throw new BusinessValidationException("Validation requirements not satisfied: " + String.join("; ", validationResult.getBlockers()));
        }
    }

    @Override
    @Transactional
    public ComplianceQcReviewFindingDto addReviewFinding(UUID workflowId, CreateReviewFindingRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, orgId);
        validateClientAccess(scope, workflow.getClientId());

        if (workflow.getWorkflowStatus() != ComplianceWorkflowStatus.UNDER_REVIEW) {
            throw new BusinessValidationException("Review findings can only be recorded when workflow is in UNDER_REVIEW status");
        }

        // Enforce Four-Eyes: only the assigned reviewer can record review findings
        requireReviewerAuthority(workflow, orgId);

        if (request == null || !StringUtils.hasText(request.getDescription())) {
            throw new BusinessValidationException("Finding description is required");
        }

        String severity = "ADVISORY".equalsIgnoreCase(request.getSeverity()) ? "ADVISORY" : "BLOCKING";

        ComplianceQcReviewFindingEntity finding = ComplianceQcReviewFindingEntity.builder()
                .workflow(workflow)
                .reviewCycle(workflow.getReviewCycle())
                .category(StringUtils.hasText(request.getCategory()) ? request.getCategory() : "GENERAL")
                .severity(severity)
                .description(request.getDescription())
                .correctiveGuidance(request.getCorrectiveGuidance())
                .sourceReference(request.getSourceReference())
                .status(ComplianceQcFindingStatus.OPEN)
                .build();
        finding.setOrganizationId(orgId);

        ComplianceQcReviewFindingEntity saved = findingRepository.save(finding);

        auditService.logEvent(
                "COMPLIANCE_REVIEW_FINDING_RECORDED",
                "COMPLIANCE_WORKFLOW",
                workflow.getId().toString(),
                "CYCLE_" + workflow.getReviewCycle(),
                severity + ": " + request.getDescription()
        );

        return mapFindingToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceQcReviewFindingDto> getReviewFindings(UUID workflowId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, orgId);
        validateClientAccess(scope, workflow.getClientId());

        return findingRepository.findByOrganizationIdAndWorkflowIdOrderByReviewCycleAscCreatedAtAsc(orgId, workflowId)
                .stream().map(this::mapFindingToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceQcReviewCycleDto> getReviewCycles(UUID workflowId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, orgId);
        validateClientAccess(scope, workflow.getClientId());

        List<ComplianceQcReviewFindingEntity> allFindings = findingRepository
                .findByOrganizationIdAndWorkflowIdOrderByReviewCycleAscCreatedAtAsc(orgId, workflowId);

        Map<Integer, List<ComplianceQcReviewFindingEntity>> grouped = allFindings.stream()
                .collect(Collectors.groupingBy(ComplianceQcReviewFindingEntity::getReviewCycle));

        int maxCycle = Math.max(workflow.getReviewCycle(), grouped.keySet().stream().max(Integer::compareTo).orElse(1));

        List<ComplianceQcReviewCycleDto> result = new ArrayList<>();
        for (int c = 1; c <= maxCycle; c++) {
            List<ComplianceQcReviewFindingEntity> cycleFindings = grouped.getOrDefault(c, Collections.emptyList());
            int unresolved = (int) cycleFindings.stream()
                    .filter(f -> f.getStatus() == ComplianceQcFindingStatus.OPEN || f.getStatus() == ComplianceQcFindingStatus.CORRECTED)
                    .count();
            result.add(ComplianceQcReviewCycleDto.builder()
                    .cycleNumber(c)
                    .totalFindings(cycleFindings.size())
                    .unresolvedFindings(unresolved)
                    .findings(cycleFindings.stream().map(this::mapFindingToDto).toList())
                    .build());
        }
        return result;
    }

    @Override
    @Transactional
    public ComplianceQcReviewFindingDto respondToFinding(UUID workflowId, UUID findingId, RespondToFindingRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, orgId);
        validateClientAccess(scope, workflow.getClientId());

        if (workflow.getWorkflowStatus() != ComplianceWorkflowStatus.CHANGES_REQUIRED
                && workflow.getWorkflowStatus() != ComplianceWorkflowStatus.IN_PROGRESS) {
            throw new BusinessValidationException("Responses can only be provided when workflow is in CHANGES_REQUIRED or IN_PROGRESS status");
        }

        ComplianceQcReviewFindingEntity finding = findingRepository.findByIdAndOrganizationId(findingId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Review finding not found: " + findingId));

        if (!finding.getWorkflow().getId().equals(workflowId)) {
            throw new BusinessValidationException("Finding does not belong to specified workflow");
        }

        if (request == null || !StringUtils.hasText(request.getResponse())) {
            throw new BusinessValidationException("Preparer response text is required");
        }

        finding.setPreparerResponse(request.getResponse());
        finding.setRespondedAt(Instant.now());
        finding.setRespondedByUserId(SecurityUtils.getCurrentUserId());
        finding.setStatus(ComplianceQcFindingStatus.CORRECTED);

        ComplianceQcReviewFindingEntity saved = findingRepository.save(finding);

        auditService.logEvent(
                "COMPLIANCE_REVIEW_FINDING_RESPONDED",
                "COMPLIANCE_QC_FINDING",
                saved.getId().toString(),
                "OPEN",
                "CORRECTED: " + request.getResponse()
        );

        return mapFindingToDto(saved);
    }

    @Override
    @Transactional
    public ComplianceQcReviewFindingDto resolveFinding(UUID workflowId, UUID findingId, ResolveFindingRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        ComplianceWorkflowEntity workflow = getWorkflowOrThrow(workflowId, orgId);
        validateClientAccess(scope, workflow.getClientId());

        // Enforce Four-Eyes: only the assigned reviewer can resolve review findings
        requireReviewerAuthority(workflow, orgId);

        ComplianceQcReviewFindingEntity finding = findingRepository.findByIdAndOrganizationId(findingId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Review finding not found: " + findingId));

        if (!finding.getWorkflow().getId().equals(workflowId)) {
            throw new BusinessValidationException("Finding does not belong to specified workflow");
        }

        boolean isDismiss = "DISMISSED".equalsIgnoreCase(request != null ? request.getDecision() : "");
        finding.setStatus(isDismiss ? ComplianceQcFindingStatus.DISMISSED : ComplianceQcFindingStatus.RESOLVED);
        finding.setResolutionDecision(isDismiss ? "DISMISSED" : "RESOLVED");
        finding.setResolvedAt(Instant.now());
        finding.setResolvedByUserId(SecurityUtils.getCurrentUserId());
        if (request != null && StringUtils.hasText(request.getReviewerNotes())) {
            finding.setReviewerNotes(request.getReviewerNotes());
        }

        ComplianceQcReviewFindingEntity saved = findingRepository.save(finding);

        auditService.logEvent(
                "COMPLIANCE_REVIEW_FINDING_RESOLVED",
                "COMPLIANCE_QC_FINDING",
                saved.getId().toString(),
                "CORRECTED",
                finding.getStatus().name()
        );

        return mapFindingToDto(saved);
    }

    private void requireReviewerAuthority(ComplianceWorkflowEntity workflow, UUID orgId) {
        if (!SecurityUtils.hasAuthority("REVIEW_APPROVE") && !SecurityUtils.hasAuthority("REVIEW_REJECT")) {
            throw new ForbiddenException("Missing review decision permissions (REVIEW_APPROVE / REVIEW_REJECT required)");
        }
        if (workflow.getReviewerEmployeeId() == null) {
            throw new BusinessValidationException("No reviewer assigned to this workflow");
        }
        EmployeeEntity reviewer = employeeRepository.findByIdAndOrganizationId(workflow.getReviewerEmployeeId(), orgId).orElse(null);
        if (reviewer == null || reviewer.getUserId() == null) {
            throw new BusinessValidationException("Assigned reviewer record is invalid");
        }
        if (reviewer.getUserId().equals(workflow.getAssignedUserId())) {
            throw new ForbiddenException("Four-Eyes violation: Preparer cannot act as reviewer");
        }
        if (!SecurityUtils.getCurrentUserId().equals(reviewer.getUserId()) && !SecurityUtils.hasRole("ORG_ADMIN")) {
            throw new ForbiddenException("Only the assigned reviewer can record or resolve review findings");
        }
    }

    private ComplianceQcReviewFindingDto mapFindingToDto(ComplianceQcReviewFindingEntity entity) {
        return ComplianceQcReviewFindingDto.builder()
                .id(entity.getId())
                .workflowId(entity.getWorkflow().getId())
                .reviewCycle(entity.getReviewCycle())
                .category(entity.getCategory())
                .severity(entity.getSeverity())
                .description(entity.getDescription())
                .correctiveGuidance(entity.getCorrectiveGuidance())
                .sourceReference(entity.getSourceReference())
                .status(entity.getStatus())
                .preparerResponse(entity.getPreparerResponse())
                .respondedAt(entity.getRespondedAt())
                .respondedByUserId(entity.getRespondedByUserId())
                .resolutionDecision(entity.getResolutionDecision())
                .resolvedAt(entity.getResolvedAt())
                .resolvedByUserId(entity.getResolvedByUserId())
                .reviewerNotes(entity.getReviewerNotes())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    // =========================================================================
    // Deterministic Validation & Stage Helpers
    // =========================================================================

    private ComplianceQcValidationResultDto evaluateValidation(
            ComplianceWorkflowEntity workflow,
            ComplianceObligationEntity obligation,
            ClientEntity client,
            UUID orgId
    ) {
        Instant now = Instant.now();
        List<ComplianceQcCheckItemDto> checks = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> correctiveActions = new ArrayList<>();

        // Check 1: Workflow Lifecycle State Eligibility
        boolean stateEligible = workflow.getWorkflowStatus() == ComplianceWorkflowStatus.IN_PROGRESS
                || workflow.getWorkflowStatus() == ComplianceWorkflowStatus.CHANGES_REQUIRED
                || workflow.getWorkflowStatus() == ComplianceWorkflowStatus.UNDER_REVIEW
                || workflow.getWorkflowStatus() == ComplianceWorkflowStatus.READY_FOR_FILING;
        String stateMsg = stateEligible
                ? "Workflow status " + workflow.getWorkflowStatus() + " is eligible for review"
                : "Workflow status is " + workflow.getWorkflowStatus() + ". Preparation must be started before review submission.";
        checks.add(ComplianceQcCheckItemDto.builder()
                .checkKey("STATE_ELIGIBILITY")
                .checkName("Workflow Execution State")
                .category("LIFECYCLE")
                .description("Verifies that the workflow is in an active preparation, review, or correction lifecycle state")
                .status(stateEligible ? ComplianceQcCheckStatus.PASS : ComplianceQcCheckStatus.FAIL)
                .passed(stateEligible)
                .required(true)
                .severity("BLOCKING")
                .message(stateMsg)
                .correctiveHint(stateEligible ? null : "Start workflow preparation before requesting review")
                .sourceReference(workflow.getId().toString())
                .evaluatedAt(now)
                .checkVersion(RULE_VERSION)
                .build());
        if (!stateEligible) {
            blockers.add(stateMsg);
            correctiveActions.add("Transition workflow to IN_PROGRESS to begin preparation");
        }

        // Check 2: Active Reviewer Assigned
        boolean reviewerAssigned = workflow.getReviewerEmployeeId() != null;
        EmployeeEntity reviewer = null;
        if (reviewerAssigned) {
            reviewer = employeeRepository.findByIdAndOrganizationId(workflow.getReviewerEmployeeId(), orgId)
                    .filter(e -> e.getStatus() == EmployeeEntity.EmployeeStatus.ACTIVE && e.getUserId() != null)
                    .orElse(null);
            reviewerAssigned = reviewer != null;
        }
        String reviewerMsg = reviewerAssigned
                ? "Active reviewer is assigned"
                : "A reviewer must be assigned to the compliance workflow before review";
        checks.add(ComplianceQcCheckItemDto.builder()
                .checkKey("REVIEWER_ASSIGNED")
                .checkName("Active Reviewer Assignment")
                .category("ASSIGNMENT")
                .description("Ensures an active practice employee with an associated user account is designated as the reviewer")
                .status(reviewerAssigned ? ComplianceQcCheckStatus.PASS : ComplianceQcCheckStatus.FAIL)
                .passed(reviewerAssigned)
                .required(true)
                .severity("BLOCKING")
                .message(reviewerMsg)
                .correctiveHint(reviewerAssigned ? null : "Assign an active employee with review privileges as the reviewer")
                .sourceReference(workflow.getReviewerEmployeeId() != null ? workflow.getReviewerEmployeeId().toString() : null)
                .evaluatedAt(now)
                .checkVersion(RULE_VERSION)
                .build());
        if (!reviewerAssigned) {
            blockers.add(reviewerMsg);
            correctiveActions.add("Assign an active manager or practitioner as checker");
        }

        // Check 3: Reviewer Entitlement (REVIEW_APPROVE)
        ComplianceQcCheckStatus authStatus;
        String authMsg;
        if (reviewer == null) {
            authStatus = ComplianceQcCheckStatus.NOT_EVALUATED;
            authMsg = "Reviewer authority cannot be evaluated because no active reviewer is assigned";
        } else {
            UserEntity user = userRepository.findByIdAndOrganizationId(reviewer.getUserId(), orgId)
                    .filter(u -> u.getStatus() == UserEntity.UserStatus.ACTIVE)
                    .orElse(null);
            if (user == null) {
                authStatus = ComplianceQcCheckStatus.FAIL;
                authMsg = "Assigned reviewer user account is missing or inactive";
            } else {
                boolean hasPerm = user.getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream())
                        .anyMatch(p -> "REVIEW_APPROVE".equals(p.getCode()));
                authStatus = hasPerm ? ComplianceQcCheckStatus.PASS : ComplianceQcCheckStatus.FAIL;
                authMsg = hasPerm
                        ? "Assigned reviewer has review approval authority"
                        : "Assigned reviewer must have REVIEW_APPROVE permissions";
            }
        }
        boolean authPassed = authStatus == ComplianceQcCheckStatus.PASS;
        checks.add(ComplianceQcCheckItemDto.builder()
                .checkKey("REVIEWER_AUTHORITY")
                .checkName("Reviewer Approval Authority")
                .category("RBAC")
                .description("Validates that the assigned reviewer possesses the REVIEW_APPROVE authority")
                .status(authStatus)
                .passed(authPassed)
                .required(true)
                .severity("BLOCKING")
                .message(authMsg)
                .correctiveHint(authPassed ? null : "Designate a reviewer who holds the REVIEW_APPROVE entitlement")
                .sourceReference(reviewer != null && reviewer.getUserId() != null ? reviewer.getUserId().toString() : null)
                .evaluatedAt(now)
                .checkVersion(RULE_VERSION)
                .build());
        if (!authPassed && authStatus != ComplianceQcCheckStatus.NOT_EVALUATED) {
            blockers.add(authMsg);
            correctiveActions.add("Reassign review to a qualified reviewer with REVIEW_APPROVE permissions");
        }

        // Check 4: Separation of Duties (Preparer != Reviewer)
        ComplianceQcCheckStatus sodStatus = ComplianceQcCheckStatus.PASS;
        String sodMsg = "Separation of duties satisfied";
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (reviewer == null) {
            sodStatus = ComplianceQcCheckStatus.NOT_EVALUATED;
            sodMsg = "Separation of duties cannot be evaluated because no active reviewer is assigned";
        } else if (reviewer.getUserId() != null) {
            UUID reviewerUserId = reviewer.getUserId();
            if (currentUserId != null && currentUserId.equals(reviewerUserId) && workflow.getWorkflowStatus() != ComplianceWorkflowStatus.UNDER_REVIEW) {
                sodStatus = ComplianceQcCheckStatus.FAIL;
                sodMsg = "Separation of duties: The preparer cannot approve their own review";
            }
            if (workflow.getAssignedEmployeeId() != null) {
                EmployeeEntity preparerEmployee = employeeRepository.findByIdAndOrganizationId(workflow.getAssignedEmployeeId(), orgId).orElse(null);
                if (preparerEmployee != null && reviewerUserId.equals(preparerEmployee.getUserId())) {
                    sodStatus = ComplianceQcCheckStatus.FAIL;
                    sodMsg = "Separation of duties violation: Assigned preparer and reviewer cannot be the same user";
                }
            }
            if (workflow.getAssignedUserId() != null && reviewerUserId.equals(workflow.getAssignedUserId())) {
                sodStatus = ComplianceQcCheckStatus.FAIL;
                sodMsg = "Separation of duties violation: Assigned preparer and reviewer cannot be the same user";
            }
        }
        boolean sodPassed = sodStatus == ComplianceQcCheckStatus.PASS;
        checks.add(ComplianceQcCheckItemDto.builder()
                .checkKey("SEPARATION_OF_DUTIES")
                .checkName("Maker-Checker Separation of Duties")
                .category("POLICY")
                .description("Enforces the Four-Eyes Principle: preparer and reviewer must be distinct users")
                .status(sodStatus)
                .passed(sodPassed)
                .required(true)
                .severity("BLOCKING")
                .message(sodMsg)
                .correctiveHint(sodPassed ? null : "Assign an independent colleague as checker to satisfy maker-checker policy")
                .sourceReference(reviewer != null ? reviewer.getId().toString() : null)
                .evaluatedAt(now)
                .checkVersion(RULE_VERSION)
                .build());
        if (!sodPassed && sodStatus != ComplianceQcCheckStatus.NOT_EVALUATED) {
            blockers.add(sodMsg);
            correctiveActions.add("Assign an independent reviewer distinct from the preparer");
        }

        // Check 5: Preparation Checkpoints Completeness
        List<ComplianceWorkflowChecklistItemEntity> items = workflow.getChecklistItems();
        if (items != null && !items.isEmpty()) {
            boolean docsReceived = items.stream().anyMatch(i -> "DOCUMENTS_RECEIVED".equalsIgnoreCase(i.getItemKey()) && i.isCompleted());
            boolean dataValidated = items.stream().anyMatch(i -> "DATA_VALIDATED".equalsIgnoreCase(i.getItemKey()) && i.isCompleted());
            boolean compPrepared = items.stream().anyMatch(i -> "COMPUTATION_PREPARED".equalsIgnoreCase(i.getItemKey()) && i.isCompleted());

            checks.add(ComplianceQcCheckItemDto.builder()
                    .checkKey("CHECKLIST_DOCUMENTS_RECEIVED")
                    .checkName("Supporting Documents Received")
                    .category("CHECKLIST")
                    .description("Confirms that client documents, books, and statements have been received")
                    .status(docsReceived ? ComplianceQcCheckStatus.PASS : ComplianceQcCheckStatus.FAIL)
                    .passed(docsReceived)
                    .required(false)
                    .severity("ADVISORY")
                    .message(docsReceived ? "Client accounting documents confirmed" : "Supporting documents not marked received")
                    .correctiveHint(docsReceived ? null : "Mark the Receive Client Documents checklist checkpoint once records are obtained")
                    .evaluatedAt(now)
                    .checkVersion(RULE_VERSION)
                    .build());
            if (!docsReceived) warnings.add("Client supporting documents not marked received");

            checks.add(ComplianceQcCheckItemDto.builder()
                    .checkKey("CHECKLIST_DATA_VALIDATED")
                    .checkName("Data Reconciled & Validated")
                    .category("CHECKLIST")
                    .description("Verifies that data reconciliation (e.g. 2B vs purchase register) has been performed")
                    .status(dataValidated ? ComplianceQcCheckStatus.PASS : ComplianceQcCheckStatus.FAIL)
                    .passed(dataValidated)
                    .required(false)
                    .severity("ADVISORY")
                    .message(dataValidated ? "2B / books reconciliation completed" : "Data validation & reconciliation checkpoint incomplete")
                    .correctiveHint(dataValidated ? null : "Complete data reconciliation checkpoint before submitting for review")
                    .evaluatedAt(now)
                    .checkVersion(RULE_VERSION)
                    .build());
            if (!dataValidated) warnings.add("Data reconciliation checkpoint is incomplete");

            checks.add(ComplianceQcCheckItemDto.builder()
                    .checkKey("CHECKLIST_COMPUTATION_PREPARED")
                    .checkName("Tax Computation Drafted")
                    .category("CHECKLIST")
                    .description("Ensures that the draft statutory return computation sheet is prepared")
                    .status(compPrepared ? ComplianceQcCheckStatus.PASS : ComplianceQcCheckStatus.FAIL)
                    .passed(compPrepared)
                    .required(false)
                    .severity("ADVISORY")
                    .message(compPrepared ? "Draft computation sheet prepared" : "Draft tax computation not marked prepared")
                    .correctiveHint(compPrepared ? null : "Prepare computation sheet and mark the tax computation checkpoint")
                    .evaluatedAt(now)
                    .checkVersion(RULE_VERSION)
                    .build());
            if (!compPrepared) warnings.add("Draft tax computation sheet is not marked prepared");
        } else {
            // Checklist items missing or not seeded
            checks.add(ComplianceQcCheckItemDto.builder()
                    .checkKey("CHECKLIST_PREPARATION")
                    .checkName("Preparation Checkpoints")
                    .category("CHECKLIST")
                    .description("Evaluates operational preparation checklist items")
                    .status(ComplianceQcCheckStatus.NOT_EVALUATED)
                    .passed(false)
                    .required(false)
                    .severity("ADVISORY")
                    .message("No checklist checkpoints seeded for this compliance workflow")
                    .correctiveHint("Initialize standard operational checklist items")
                    .evaluatedAt(now)
                    .checkVersion(RULE_VERSION)
                    .build());
            warnings.add("Checklist items are not initialized on this workflow");
        }

        // Check 6: Statutory Identifiers
        if (obligation != null && obligation.getObligationType() != null && client != null) {
            String obType = obligation.getObligationType().name();
            boolean isGst = obType.contains("GST");
            boolean isItr = obType.contains("ITR");
            boolean isTds = obType.contains("TDS");

            if (isGst) {
                boolean gstPresent = StringUtils.hasText(client.getGstin());
                checks.add(ComplianceQcCheckItemDto.builder()
                        .checkKey("STATUTORY_IDENTIFIER")
                        .checkName("Statutory Registration Identifier (GSTIN)")
                        .category("PROFILE")
                        .description("Validates that the client has a registered GSTIN for GST filings")
                        .status(gstPresent ? ComplianceQcCheckStatus.PASS : ComplianceQcCheckStatus.FAIL)
                        .passed(gstPresent)
                        .required(false)
                        .severity("ADVISORY")
                        .message(gstPresent ? "Client GSTIN is registered: " + maskIdentifier(client.getGstin()) : "Client has no registered GSTIN for GST obligation")
                        .correctiveHint(gstPresent ? null : "Update client profile with a valid statutory GSTIN")
                        .sourceReference(client.getId().toString())
                        .evaluatedAt(now)
                        .checkVersion(RULE_VERSION)
                        .build());
                if (!gstPresent) warnings.add("Client has no registered GSTIN for GST obligation");
            } else if (isItr) {
                boolean panPresent = StringUtils.hasText(client.getPan());
                checks.add(ComplianceQcCheckItemDto.builder()
                        .checkKey("STATUTORY_IDENTIFIER")
                        .checkName("Statutory Registration Identifier (PAN)")
                        .category("PROFILE")
                        .description("Validates that the client has a registered PAN for Income Tax filings")
                        .status(panPresent ? ComplianceQcCheckStatus.PASS : ComplianceQcCheckStatus.FAIL)
                        .passed(panPresent)
                        .required(false)
                        .severity("ADVISORY")
                        .message(panPresent ? "Client PAN is registered: " + maskIdentifier(client.getPan()) : "Client has no registered PAN for Income Tax obligation")
                        .correctiveHint(panPresent ? null : "Update client profile with a valid PAN")
                        .sourceReference(client.getId().toString())
                        .evaluatedAt(now)
                        .checkVersion(RULE_VERSION)
                        .build());
                if (!panPresent) warnings.add("Client has no registered PAN for Income Tax obligation");
            } else if (isTds) {
                boolean panOrTan = StringUtils.hasText(client.getPan());
                checks.add(ComplianceQcCheckItemDto.builder()
                        .checkKey("STATUTORY_IDENTIFIER")
                        .checkName("Statutory Registration Identifier (TDS/TAN)")
                        .category("PROFILE")
                        .description("Validates client identification for TDS/TCS obligations")
                        .status(panOrTan ? ComplianceQcCheckStatus.PASS : ComplianceQcCheckStatus.FAIL)
                        .passed(panOrTan)
                        .required(false)
                        .severity("ADVISORY")
                        .message(panOrTan ? "Client identifier present" : "Client has no registered PAN/TAN for TDS obligation")
                        .correctiveHint(panOrTan ? null : "Update client profile with tax identification")
                        .sourceReference(client.getId().toString())
                        .evaluatedAt(now)
                        .checkVersion(RULE_VERSION)
                        .build());
                if (!panOrTan) warnings.add("Client has no registered PAN/TAN for TDS obligation");
            } else {
                checks.add(ComplianceQcCheckItemDto.builder()
                        .checkKey("STATUTORY_IDENTIFIER")
                        .checkName("Statutory Registration Identifier")
                        .category("PROFILE")
                        .description("Validates statutory registration requirements")
                        .status(ComplianceQcCheckStatus.NOT_APPLICABLE)
                        .passed(true)
                        .required(false)
                        .severity("ADVISORY")
                        .message("No statutory registration requirement defined for obligation type: " + obType)
                        .evaluatedAt(now)
                        .checkVersion(RULE_VERSION)
                        .build());
            }
        } else {
            checks.add(ComplianceQcCheckItemDto.builder()
                    .checkKey("STATUTORY_IDENTIFIER")
                    .checkName("Statutory Registration Identifier")
                    .category("PROFILE")
                    .description("Validates statutory registration requirements")
                    .status(ComplianceQcCheckStatus.NOT_EVALUATED)
                    .passed(false)
                    .required(false)
                    .severity("ADVISORY")
                    .message("Client or obligation record is not linked to workflow")
                    .correctiveHint("Link an obligation and client to the compliance workflow")
                    .evaluatedAt(now)
                    .checkVersion(RULE_VERSION)
                    .build());
            warnings.add("Client or obligation record is missing for statutory identifier validation");
        }

        // Check 7: Review Findings Resolution (Enforce no open blocking findings)
        long openBlockers = findingRepository.countUnresolvedBlockingFindings(
                orgId, workflow.getId(), List.of(ComplianceQcFindingStatus.OPEN));
        boolean findingsPassed = openBlockers == 0;
        checks.add(ComplianceQcCheckItemDto.builder()
                .checkKey("REVIEW_FINDINGS_RESOLVED")
                .checkName("Review Findings Resolution")
                .category("REVIEW")
                .description("Ensures all blocking review findings recorded by reviewers are addressed")
                .status(findingsPassed ? ComplianceQcCheckStatus.PASS : ComplianceQcCheckStatus.FAIL)
                .passed(findingsPassed)
                .required(true)
                .severity("BLOCKING")
                .message(findingsPassed ? "Zero open blocking review findings" : openBlockers + " open blocking review finding(s) require correction")
                .correctiveHint(findingsPassed ? null : "Review the recorded findings list, provide correction responses, and address findings before resubmission")
                .sourceReference("CYCLE_" + workflow.getReviewCycle())
                .evaluatedAt(now)
                .checkVersion(RULE_VERSION)
                .build());
        if (!findingsPassed) {
            blockers.add(openBlockers + " blocking review finding(s) remain open and require correction");
            correctiveActions.add("Address and resolve open review findings from cycle " + workflow.getReviewCycle());
        }

        int total = checks.size();
        int passed = (int) checks.stream().filter(c -> c.getStatus() == ComplianceQcCheckStatus.PASS).count();
        int failed = (int) checks.stream().filter(c -> c.getStatus() == ComplianceQcCheckStatus.FAIL).count();
        int notEvaluated = (int) checks.stream().filter(c -> c.getStatus() == ComplianceQcCheckStatus.NOT_EVALUATED).count();
        int notApplicable = (int) checks.stream().filter(c -> c.getStatus() == ComplianceQcCheckStatus.NOT_APPLICABLE).count();

        // Blocking: Any check with required=true that has status FAIL or NOT_EVALUATED
        boolean valid = blockers.isEmpty();

        String fingerprint = computePreparationFingerprint(workflow);

        return ComplianceQcValidationResultDto.builder()
                .valid(valid)
                .totalChecks(total)
                .passedChecks(passed)
                .failedChecks(failed)
                .notEvaluatedChecks(notEvaluated)
                .notApplicableChecks(notApplicable)
                .validatedAt(now)
                .preparationFingerprint(fingerprint)
                .checks(checks)
                .blockers(blockers)
                .warnings(warnings)
                .correctiveActions(correctiveActions)
                .build();
    }

    private String computePreparationFingerprint(ComplianceWorkflowEntity workflow) {
        int completedCount = 0;
        int totalItems = 0;
        if (workflow.getChecklistItems() != null) {
            totalItems = workflow.getChecklistItems().size();
            completedCount = (int) workflow.getChecklistItems().stream()
                    .filter(ComplianceWorkflowChecklistItemEntity::isCompleted)
                    .count();
        }
        return String.format("chk:%d/%d:st:%s",
                completedCount,
                totalItems,
                workflow.getWorkflowStatus().name());
    }

    private String maskIdentifier(String id) {
        if (!StringUtils.hasText(id) || id.length() <= 4) return "****";
        return id.substring(0, 2) + "******" + id.substring(id.length() - 2);
    }

    private ComplianceQcStage determineQcStage(ComplianceWorkflowEntity workflow, ComplianceQcValidationResultDto validationResult) {
        if (workflow.getWorkflowStatus() == ComplianceWorkflowStatus.FILED
                || workflow.getWorkflowStatus() == ComplianceWorkflowStatus.ACKNOWLEDGEMENT_PENDING
                || workflow.getWorkflowStatus() == ComplianceWorkflowStatus.COMPLETED) {
            return ComplianceQcStage.SUBMISSION;
        }
        if (workflow.isAuthorized()) {
            return ComplianceQcStage.AUTHORIZATION;
        }
        if (workflow.getWorkflowStatus() == ComplianceWorkflowStatus.READY_FOR_FILING || workflow.getApprovedAt() != null) {
            return ComplianceQcStage.APPROVAL;
        }
        if (workflow.getWorkflowStatus() == ComplianceWorkflowStatus.CHANGES_REQUIRED) {
            return ComplianceQcStage.CORRECTION;
        }
        if (workflow.getWorkflowStatus() == ComplianceWorkflowStatus.UNDER_REVIEW) {
            return ComplianceQcStage.REVIEW;
        }
        if (validationResult.isValid() && isPreparationComplete(workflow)) {
            return ComplianceQcStage.VALIDATION;
        }
        return ComplianceQcStage.PREPARATION;
    }

    private String determineNextActionableStep(ComplianceQcStage stage, ComplianceWorkflowEntity workflow, ComplianceQcValidationResultDto val) {
        return switch (stage) {
            case PREPARATION -> {
                if (!val.getBlockers().isEmpty()) {
                    yield "Resolve preparation requirements: " + val.getBlockers().get(0);
                }
                yield "Gather client records and complete tax computation draft";
            }
            case VALIDATION -> "Pre-review validation checks passed; submit for maker-checker review";
            case REVIEW -> "Awaiting assigned reviewer examination and approval decision";
            case CORRECTION -> "Reviewer requested changes: " + (workflow.getChangesRequestedReason() != null ? workflow.getChangesRequestedReason() : "Address findings and resubmit for review");
            case APPROVAL -> "Computation approved; obtain client / signatory filing authorization";
            case AUTHORIZATION -> "Filing authorization verified; ready to submit return to statutory portal";
            case SUBMISSION -> "Filing submitted to statutory portal; archive acknowledgement receipt";
        };
    }

    private List<String> determineAllowedActions(ComplianceQcStage stage, ComplianceWorkflowEntity workflow, PracticeSecurityScope scope) {
        List<String> actions = new ArrayList<>();
        actions.add("VIEW_STATUS");

        if (stage == ComplianceQcStage.PREPARATION || stage == ComplianceQcStage.CORRECTION || stage == ComplianceQcStage.VALIDATION) {
            actions.add("VALIDATE");
            if (SecurityUtils.hasAuthority("REVIEW_SUBMIT")) {
                actions.add("SUBMIT_REVIEW");
            }
            if (stage == ComplianceQcStage.CORRECTION) {
                actions.add("RESPOND_FINDING");
            }
        }
        if (stage == ComplianceQcStage.REVIEW) {
            if (SecurityUtils.hasAuthority("REVIEW_APPROVE")) {
                actions.add("APPROVE");
            }
            if (SecurityUtils.hasAuthority("REVIEW_REJECT")) {
                actions.add("REQUEST_CHANGES");
            }
            actions.add("RECORD_FINDING");
            actions.add("RESOLVE_FINDING");
        }
        if (stage == ComplianceQcStage.APPROVAL) {
            actions.add("AUTHORIZE_FILING");
        }
        if (stage == ComplianceQcStage.AUTHORIZATION) {
            actions.add("RECORD_FILING");
            actions.add("REVOKE_AUTHORIZATION");
        }
        if (stage == ComplianceQcStage.SUBMISSION) {
            actions.add("COMPLETE_WORKFLOW");
        }
        return actions;
    }

    private boolean isPreparationComplete(ComplianceWorkflowEntity workflow) {
        if (workflow.getChecklistItems() == null || workflow.getChecklistItems().isEmpty()) {
            return workflow.getWorkflowStatus() != ComplianceWorkflowStatus.CREATED
                    && workflow.getWorkflowStatus() != ComplianceWorkflowStatus.NOT_STARTED;
        }
        // Preparation complete if first 4 operational checkpoints are completed
        return workflow.getChecklistItems().stream()
                .filter(i -> i.getSequenceOrder() <= 4 && i.isRequired())
                .allMatch(ComplianceWorkflowChecklistItemEntity::isCompleted);
    }

    private ComplianceWorkflowEntity getWorkflowOrThrow(UUID workflowId, UUID orgId) {
        return workflowRepository.findByIdAndOrganizationId(workflowId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance workflow not found: " + workflowId));
    }

    private ComplianceWorkflowEntity getWorkflowWithChecklistOrThrow(UUID workflowId, UUID orgId) {
        return workflowRepository.findByIdWithChecklist(workflowId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance workflow not found: " + workflowId));
    }

    private void validateClientAccess(PracticeSecurityScope scope, UUID clientId) {
        if (scope == null || scope.isFirmAdmin()) {
            return;
        }
        Set<UUID> accessibleClients = scopeEvaluator.getAccessibleClientIds(scope);
        if (accessibleClients != null && !accessibleClients.contains(clientId)) {
            throw new ForbiddenException("Access denied: You do not have permission to access client portfolio record: " + clientId);
        }
    }

    private void requireAuthorizationPermission() {
        boolean authorized = SecurityUtils.hasAuthority("REVIEW_APPROVE")
                || SecurityUtils.hasAuthority("CLIENT_EDIT")
                || SecurityUtils.hasAuthority("COMPLIANCE_EDIT")
                || SecurityUtils.hasRole("ORG_ADMIN")
                || SecurityUtils.hasRole("ADMIN")
                || SecurityUtils.hasRole("PRACTITIONER")
                || SecurityUtils.hasRole("MANAGER");
        if (!authorized) {
            throw new ForbiddenException("Missing required permission to authorize compliance filing");
        }
    }
}
