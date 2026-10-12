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
import com.taxoryn.module.compliance.qc.dto.ComplianceQcStatusDto;
import com.taxoryn.module.compliance.qc.dto.ComplianceQcValidationResultDto;
import com.taxoryn.module.compliance.qc.dto.RevokeAuthorizationRequest;
import com.taxoryn.module.compliance.qc.model.ComplianceQcStage;
import com.taxoryn.module.compliance.qc.service.ComplianceQualityControlService;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowChecklistItemRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.compliance.service.ComplianceWorkflowService;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class ComplianceQualityControlServiceImpl implements ComplianceQualityControlService {

    private final ComplianceWorkflowRepository workflowRepository;
    private final ComplianceWorkflowChecklistItemRepository checklistItemRepository;
    private final ComplianceObligationRepository obligationRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PracticeSecurityScopeEvaluator scopeEvaluator;
    private final AuditService auditService;
    private final ComplianceWorkflowService workflowService;

    public ComplianceQualityControlServiceImpl(
            ComplianceWorkflowRepository workflowRepository,
            ComplianceWorkflowChecklistItemRepository checklistItemRepository,
            ComplianceObligationRepository obligationRepository,
            ClientRepository clientRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            PracticeSecurityScopeEvaluator scopeEvaluator,
            AuditService auditService,
            @Lazy ComplianceWorkflowService workflowService
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
                .nextActionableStep(nextStep)
                .allowedActions(allowedActions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceQcValidationResultDto validateForReview(UUID workflowId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        ComplianceWorkflowEntity workflow = getWorkflowWithChecklistOrThrow(workflowId, orgId);
        validateClientAccess(scope, workflow.getClientId());

        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(workflow.getComplianceObligationId(), orgId).orElse(null);
        ClientEntity client = clientRepository.findByIdAndOrganizationId(workflow.getClientId(), orgId).orElse(null);

        ComplianceQcValidationResultDto result = evaluateValidation(workflow, obligation, client, orgId);

        auditService.logEvent(
                "COMPLIANCE_PREPARATION_VALIDATED",
                "COMPLIANCE_WORKFLOW",
                workflow.getId().toString(),
                null,
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

    // =========================================================================
    // Deterministic Validation & Stage Helpers
    // =========================================================================

    private ComplianceQcValidationResultDto evaluateValidation(
            ComplianceWorkflowEntity workflow,
            ComplianceObligationEntity obligation,
            ClientEntity client,
            UUID orgId
    ) {
        List<ComplianceQcCheckItemDto> checks = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check 1: Workflow Lifecycle State Eligibility
        boolean stateEligible = workflow.getWorkflowStatus() == ComplianceWorkflowStatus.IN_PROGRESS
                || workflow.getWorkflowStatus() == ComplianceWorkflowStatus.CHANGES_REQUIRED
                || workflow.getWorkflowStatus() == ComplianceWorkflowStatus.UNDER_REVIEW
                || workflow.getWorkflowStatus() == ComplianceWorkflowStatus.READY_FOR_FILING;
        String stateMsg = stateEligible ? "Workflow status " + workflow.getWorkflowStatus() + " is eligible for review"
                : "Workflow status is " + workflow.getWorkflowStatus() + ". Preparation must be started before review submission.";
        checks.add(ComplianceQcCheckItemDto.builder()
                .checkKey("STATE_ELIGIBILITY")
                .checkName("Workflow Execution State")
                .category("LIFECYCLE")
                .passed(stateEligible)
                .required(true)
                .message(stateMsg)
                .build());
        if (!stateEligible) blockers.add(stateMsg);

        // Check 2: Active Reviewer Assigned
        boolean reviewerAssigned = workflow.getReviewerEmployeeId() != null;
        EmployeeEntity reviewer = null;
        if (reviewerAssigned) {
            reviewer = employeeRepository.findByIdAndOrganizationId(workflow.getReviewerEmployeeId(), orgId)
                    .filter(e -> e.getStatus() == EmployeeEntity.EmployeeStatus.ACTIVE && e.getUserId() != null)
                    .orElse(null);
            reviewerAssigned = reviewer != null;
        }
        String reviewerMsg = reviewerAssigned ? "Active reviewer is assigned" : "A reviewer must be assigned to the compliance workflow before review";
        checks.add(ComplianceQcCheckItemDto.builder()
                .checkKey("REVIEWER_ASSIGNED")
                .checkName("Active Reviewer Assignment")
                .category("ASSIGNMENT")
                .passed(reviewerAssigned)
                .required(true)
                .message(reviewerMsg)
                .build());
        if (!reviewerAssigned) blockers.add(reviewerMsg);

        // Check 3: Reviewer Entitlement (REVIEW_APPROVE)
        boolean reviewerAuthorized = false;
        if (reviewer != null && reviewer.getUserId() != null) {
            UserEntity user = userRepository.findByIdAndOrganizationId(reviewer.getUserId(), orgId)
                    .filter(u -> u.getStatus() == UserEntity.UserStatus.ACTIVE)
                    .orElse(null);
            if (user != null) {
                reviewerAuthorized = user.getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream())
                        .anyMatch(p -> "REVIEW_APPROVE".equals(p.getCode()));
            }
        }
        String authMsg = reviewerAuthorized ? "Assigned reviewer has review approval authority" : "Assigned reviewer must have REVIEW_APPROVE permissions";
        checks.add(ComplianceQcCheckItemDto.builder()
                .checkKey("REVIEWER_AUTHORITY")
                .checkName("Reviewer Approval Authority")
                .category("RBAC")
                .passed(reviewerAuthorized)
                .required(true)
                .message(authMsg)
                .build());
        if (reviewer != null && !reviewerAuthorized) blockers.add(authMsg);

        // Check 4: Separation of Duties (Preparer != Reviewer)
        boolean separationPass = true;
        String sodMsg = "Separation of duties satisfied";
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (reviewer != null && reviewer.getUserId() != null) {
            UUID reviewerUserId = reviewer.getUserId();
            // Check against current actor
            if (currentUserId != null && currentUserId.equals(reviewerUserId) && workflow.getWorkflowStatus() != ComplianceWorkflowStatus.UNDER_REVIEW) {
                // If caller is submitting review, preparer cannot be the assigned reviewer
                separationPass = false;
                sodMsg = "Separation of duties: The preparer cannot approve their own review";
            }
            // Check against assigned preparer employee
            if (workflow.getAssignedEmployeeId() != null) {
                EmployeeEntity preparerEmployee = employeeRepository.findByIdAndOrganizationId(workflow.getAssignedEmployeeId(), orgId).orElse(null);
                if (preparerEmployee != null && reviewerUserId.equals(preparerEmployee.getUserId())) {
                    separationPass = false;
                    sodMsg = "Separation of duties violation: Assigned preparer and reviewer cannot be the same user";
                }
            }
            if (workflow.getAssignedUserId() != null && reviewerUserId.equals(workflow.getAssignedUserId())) {
                separationPass = false;
                sodMsg = "Separation of duties violation: Assigned preparer and reviewer cannot be the same user";
            }
        }
        checks.add(ComplianceQcCheckItemDto.builder()
                .checkKey("SEPARATION_OF_DUTIES")
                .checkName("Maker-Checker Separation of Duties")
                .category("POLICY")
                .passed(separationPass)
                .required(true)
                .message(sodMsg)
                .build());
        if (!separationPass) blockers.add(sodMsg);

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
                    .passed(docsReceived)
                    .required(false)
                    .message(docsReceived ? "Client accounting documents confirmed" : "Supporting documents not marked received")
                    .build());
            if (!docsReceived) warnings.add("Client supporting documents not marked received");

            checks.add(ComplianceQcCheckItemDto.builder()
                    .checkKey("CHECKLIST_DATA_VALIDATED")
                    .checkName("Data Reconciled & Validated")
                    .category("CHECKLIST")
                    .passed(dataValidated)
                    .required(false)
                    .message(dataValidated ? "2B / books reconciliation completed" : "Data validation & reconciliation checkpoint incomplete")
                    .build());
            if (!dataValidated) warnings.add("Data reconciliation checkpoint is incomplete");

            checks.add(ComplianceQcCheckItemDto.builder()
                    .checkKey("CHECKLIST_COMPUTATION_PREPARED")
                    .checkName("Tax Computation Drafted")
                    .category("CHECKLIST")
                    .passed(compPrepared)
                    .required(false)
                    .message(compPrepared ? "Draft computation sheet prepared" : "Draft tax computation not marked prepared")
                    .build());
            if (!compPrepared) warnings.add("Draft tax computation sheet is not marked prepared");
        }

        // Check 6: Statutory Identifiers
        if (obligation != null && obligation.getObligationType() != null && client != null) {
            String obType = obligation.getObligationType().name();
            boolean idPresent = true;
            String idMsg = "Statutory registration present";
            if (obType.contains("GST") && !StringUtils.hasText(client.getGstin())) {
                idPresent = false;
                idMsg = "Client has no registered GSTIN for GST obligation";
            } else if (obType.contains("ITR") && !StringUtils.hasText(client.getPan())) {
                idPresent = false;
                idMsg = "Client has no registered PAN for Income Tax obligation";
            }
            checks.add(ComplianceQcCheckItemDto.builder()
                    .checkKey("STATUTORY_IDENTIFIER")
                    .checkName("Statutory Registration Identifier")
                    .category("PROFILE")
                    .passed(idPresent)
                    .required(false)
                    .message(idMsg)
                    .build());
            if (!idPresent) warnings.add(idMsg);
        }

        int total = checks.size();
        int passed = (int) checks.stream().filter(ComplianceQcCheckItemDto::isPassed).count();
        boolean valid = blockers.isEmpty();

        return ComplianceQcValidationResultDto.builder()
                .valid(valid)
                .totalChecks(total)
                .passedChecks(passed)
                .checks(checks)
                .blockers(blockers)
                .warnings(warnings)
                .build();
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
        }
        if (stage == ComplianceQcStage.REVIEW) {
            if (SecurityUtils.hasAuthority("REVIEW_APPROVE")) {
                actions.add("APPROVE");
            }
            if (SecurityUtils.hasAuthority("REVIEW_REJECT")) {
                actions.add("REQUEST_CHANGES");
            }
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
