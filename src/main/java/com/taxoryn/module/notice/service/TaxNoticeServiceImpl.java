package com.taxoryn.module.notice.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.notice.dto.AdjournHearingRequest;
import com.taxoryn.module.notice.dto.ClientNoticeDto;
import com.taxoryn.module.notice.dto.CloseNoticeRequest;
import com.taxoryn.module.notice.dto.CreateNoticeResponseRequest;
import com.taxoryn.module.notice.dto.CreateTaxNoticeRequest;
import com.taxoryn.module.notice.dto.NoticeActivityDto;
import com.taxoryn.module.notice.dto.NoticeDashboardStatsDto;
import com.taxoryn.module.notice.dto.NoticeHearingDto;
import com.taxoryn.module.notice.dto.NoticeResponseDto;
import com.taxoryn.module.notice.dto.RecordHearingOutcomeRequest;
import com.taxoryn.module.notice.dto.ReviewNoticeResponseRequest;
import com.taxoryn.module.notice.dto.ScheduleHearingRequest;
import com.taxoryn.module.notice.dto.SetFollowUpRequest;
import com.taxoryn.module.notice.dto.SetWaitingForClientRequest;
import com.taxoryn.module.notice.dto.SubmitNoticeRequest;
import com.taxoryn.module.notice.dto.TaxNoticeDto;
import com.taxoryn.module.notice.dto.TaxNoticeFilterRequest;
import com.taxoryn.module.notice.dto.UpdateNoticeHearingRequest;
import com.taxoryn.module.notice.dto.UpdateNoticeResponseRequest;
import com.taxoryn.module.notice.dto.UpdateTaxNoticeRequest;
import com.taxoryn.module.notice.enums.NoticeRisk;
import com.taxoryn.module.notice.entity.NoticeActivityEntity;
import com.taxoryn.module.notice.entity.NoticeHearingEntity;
import com.taxoryn.module.notice.entity.NoticeResponseEntity;
import com.taxoryn.module.notice.entity.TaxNoticeEntity;
import com.taxoryn.module.notice.enums.HearingMode;
import com.taxoryn.module.notice.enums.HearingStatus;
import com.taxoryn.module.notice.enums.NoticeActivityType;
import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeResponseStatus;
import com.taxoryn.module.notice.enums.NoticeStatus;
import com.taxoryn.module.notice.enums.ReviewStatus;
import com.taxoryn.module.notice.mapper.TaxNoticeMapper;
import com.taxoryn.module.notice.repository.NoticeActivityRepository;
import com.taxoryn.module.notice.repository.NoticeHearingRepository;
import com.taxoryn.module.notice.repository.NoticeResponseRepository;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.notification.entity.NotificationEntity.Category;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.entity.NotificationEntity.Severity;
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxNoticeServiceImpl implements TaxNoticeService {

    private final TaxNoticeRepository noticeRepository;
    private final NoticeResponseRepository responseRepository;
    private final NoticeHearingRepository hearingRepository;
    private final NoticeActivityRepository activityRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final DocumentRepository documentRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final TaxNoticeMapper noticeMapper;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final TaxNoticeConfigurationService configurationService;

    private static final Set<NoticeStatus> CLOSED_STATUSES = Set.of(
            NoticeStatus.RESOLVED,
            NoticeStatus.DEMAND_DROPPED,
            NoticeStatus.APPEAL_FILED,
            NoticeStatus.CLOSED
    );

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TaxNoticeDto> getNotices(TaxNoticeFilterRequest filterRequest, PageRequestDto pageRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        Pageable pageable = pageRequest != null ? pageRequest.toPageable() : PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "responseDueDate"));
        Specification<TaxNoticeEntity> spec = buildSpecification(organizationId, filterRequest, accessibleClientIds);
        Page<TaxNoticeEntity> page = noticeRepository.findAll(spec, pageable);

        List<TaxNoticeDto> dtos = page.getContent().stream()
                .map(this::enrichNoticeDto)
                .collect(Collectors.toList());

        return PagedResponse.<TaxNoticeDto>builder()
                .content(dtos)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TaxNoticeDto getNoticeById(UUID noticeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TaxNoticeEntity entity = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(entity.getClientId());
        return enrichNoticeDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxNoticeDto getNoticeByNumber(String noticeNumber) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TaxNoticeEntity entity = noticeRepository.findByOrganizationIdAndNoticeNumber(organizationId, noticeNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with number: " + noticeNumber));

        validateClientAccess(entity.getClientId());
        return enrichNoticeDto(entity);
    }

    @Override
    @Transactional
    public TaxNoticeDto createNotice(CreateTaxNoticeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        // 1. Verify Client
        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.getClientId()));

        validateClientAccess(client.getId());

        // 1b. Verify Employees if provided
        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned employee not found with id: " + request.getAssignedEmployeeId()));
        }
        if (request.getReviewerEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getReviewerEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reviewer employee not found with id: " + request.getReviewerEmployeeId()));
        }
        if (request.getPartnerEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getPartnerEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Partner employee not found with id: " + request.getPartnerEmployeeId()));
        }

        // 2. Uniqueness check
        if (noticeRepository.existsByOrganizationIdAndNoticeNumber(organizationId, request.getNoticeNumber())) {
            throw new BusinessValidationException("A tax notice with number '" + request.getNoticeNumber() + "' already exists in this organization");
        }

        // 2b. Resolve Organization Operations Configuration
        com.taxoryn.module.notice.dto.TaxNoticeConfigDto config = configurationService.getEffectiveConfiguration(organizationId);

        // 3. Response Due Date Calculation (Official date is authoritative, fallback to config default)
        LocalDate responseDueDate = request.getResponseDueDate();
        if (responseDueDate == null) {
            LocalDate baseDate = request.getNoticeDate() != null ? request.getNoticeDate()
                    : (request.getReceivedDate() != null ? request.getReceivedDate() : LocalDate.now());
            int dueDays = config.getDefaultResponseDueDays() > 0 ? config.getDefaultResponseDueDays() : 30;
            responseDueDate = baseDate.plusDays(dueDays);
        }

        // 3b. Priority calculation
        NoticePriority calculatedPriority = request.getPriority() != null
                ? request.getPriority()
                : (request.getResponseDueDate() != null
                    ? computePriority(request.getResponseDueDate())
                    : (config.getDefaultPriority() != null ? config.getDefaultPriority() : computePriority(responseDueDate)));

        // 4. Build Notice Entity
        TaxNoticeEntity entity = TaxNoticeEntity.builder()
                .clientId(request.getClientId())
                .complianceObligationId(request.getComplianceObligationId())
                .clientServiceId(request.getClientServiceId())
                .noticeNumber(request.getNoticeNumber().trim())
                .dinNumber(StringUtils.hasText(request.getDinNumber()) ? request.getDinNumber().trim() : null)
                .department(request.getDepartment())
                .noticeType(request.getNoticeType().trim())
                .section(StringUtils.hasText(request.getSection()) ? request.getSection().trim() : null)
                .subject(request.getSubject().trim())
                .description(request.getDescription())
                .assessmentYear(request.getAssessmentYear())
                .financialYear(request.getFinancialYear())
                .taxPeriod(request.getTaxPeriod())
                .demandAmount(request.getDemandAmount())
                .noticeDate(request.getNoticeDate())
                .receivedDate(request.getReceivedDate())
                .responseDueDate(responseDueDate)
                .responseRequired(request.getResponseRequired() != null ? request.getResponseRequired() : true)
                .responseStatus(request.getResponseStatus() != null ? request.getResponseStatus() : NoticeResponseStatus.REQUIRED)
                .responseDraft(request.getResponseDraft())
                .hearingRequired(request.getHearingRequired() != null ? request.getHearingRequired() : false)
                .hearingDate(request.getHearingDate())
                .hearingTime(request.getHearingTime())
                .hearingMode(request.getHearingMode())
                .hearingLocation(request.getHearingLocation())
                .hearingReference(request.getHearingReference())
                .hearingNotes(request.getHearingNotes())
                .hearingStatus(request.getHearingStatus())
                .status(NoticeStatus.RECEIVED)
                .priority(calculatedPriority)
                .riskLevel(request.getRiskLevel() != null ? request.getRiskLevel() : NoticeRisk.MEDIUM)
                .followUpDate(request.getFollowUpDate())
                .followUpNotes(request.getFollowUpNotes())
                .assignedEmployeeId(request.getAssignedEmployeeId())
                .reviewerEmployeeId(request.getReviewerEmployeeId())
                .partnerEmployeeId(request.getPartnerEmployeeId())
                .issuingAuthority(request.getIssuingAuthority())
                .issuingOfficerName(request.getIssuingOfficerName())
                .internalNotes(request.getInternalNotes())
                .build();

        TaxNoticeEntity saved = noticeRepository.save(entity);

        // 5. Link original document if provided
        if (request.getOriginalDocumentId() != null) {
            documentRepository.findByIdAndOrganizationId(request.getOriginalDocumentId(), organizationId)
                    .ifPresent(doc -> {
                        doc.setNoticeId(saved.getId());
                        doc.setDocumentType(DocumentType.TAX_NOTICE_ORIGINAL);
                        documentRepository.save(doc);
                    });
        }

        // 6. Auto-create Intake/Response Prep Task if requested or configured
        boolean shouldCreateTask = Boolean.TRUE.equals(request.getCreateIntakeTask())
                || (request.getCreateIntakeTask() == null && config.isAutoCreateResponseTask());
        if (shouldCreateTask) {
            createNoticePreparationTask(saved, client);
        }

        // 7. Record Activities & Audit
        recordActivity(saved.getId(), NoticeActivityType.NOTICE_CREATED, currentUserId,
                "Tax notice " + saved.getNoticeNumber() + " logged for client " + client.getDisplayName(),
                null, saved.getStatus().name(), null);

        auditService.logEvent("NOTICE_CREATED", "TAX_NOTICE", saved.getId().toString(), null, saved);

        // 8. Notifications (Config-driven)
        if (config.isNotifyOnAssignment()) {
            dispatchNoticeAssignmentNotifications(saved, client, "New Tax Notice Logged: " + saved.getNoticeNumber());
        }

        return enrichNoticeDto(saved);
    }

    @Override
    @Transactional
    public TaxNoticeDto updateNotice(UUID noticeId, UpdateTaxNoticeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        TaxNoticeEntity entity = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(entity.getClientId());

        if (StringUtils.hasText(request.getNoticeNumber()) && !request.getNoticeNumber().equals(entity.getNoticeNumber())) {
            if (noticeRepository.existsByOrganizationIdAndNoticeNumberAndIdNot(organizationId, request.getNoticeNumber(), noticeId)) {
                throw new BusinessValidationException("A tax notice with number '" + request.getNoticeNumber() + "' already exists");
            }
            entity.setNoticeNumber(request.getNoticeNumber().trim());
        }

        if (request.getClientId() != null && !request.getClientId().equals(entity.getClientId())) {
            clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.getClientId()));
            validateClientAccess(request.getClientId());
            entity.setClientId(request.getClientId());
        }

        if (request.getDinNumber() != null) entity.setDinNumber(request.getDinNumber());
        if (request.getDepartment() != null) entity.setDepartment(request.getDepartment());
        if (StringUtils.hasText(request.getNoticeType())) entity.setNoticeType(request.getNoticeType().trim());
        if (request.getSection() != null) entity.setSection(request.getSection());
        if (StringUtils.hasText(request.getSubject())) entity.setSubject(request.getSubject().trim());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getAssessmentYear() != null) entity.setAssessmentYear(request.getAssessmentYear());
        if (request.getFinancialYear() != null) entity.setFinancialYear(request.getFinancialYear());
        if (request.getTaxPeriod() != null) entity.setTaxPeriod(request.getTaxPeriod());
        if (request.getDemandAmount() != null) entity.setDemandAmount(request.getDemandAmount());
        if (request.getNoticeDate() != null) entity.setNoticeDate(request.getNoticeDate());
        if (request.getReceivedDate() != null) entity.setReceivedDate(request.getReceivedDate());

        if (request.getResponseDueDate() != null) {
            entity.setResponseDueDate(request.getResponseDueDate());
            if (request.getPriority() == null) {
                entity.setPriority(computePriority(request.getResponseDueDate()));
            }
        }

        if (request.getResponseRequired() != null) entity.setResponseRequired(request.getResponseRequired());
        if (request.getResponseStatus() != null && request.getResponseStatus() != entity.getResponseStatus()) {
            validateResponseStatusTransition(entity.getResponseStatus(), request.getResponseStatus());
            entity.setResponseStatus(request.getResponseStatus());
        }
        if (request.getResponseDraft() != null) entity.setResponseDraft(request.getResponseDraft());
        if (request.getSubmissionReference() != null) entity.setSubmissionReference(request.getSubmissionReference());
        if (request.getSubmissionNotes() != null) entity.setSubmissionNotes(request.getSubmissionNotes());

        if (request.getHearingRequired() != null) entity.setHearingRequired(request.getHearingRequired());
        if (request.getHearingMode() != null) entity.setHearingMode(request.getHearingMode());
        if (request.getHearingLocation() != null) entity.setHearingLocation(request.getHearingLocation());
        if (request.getHearingReference() != null) entity.setHearingReference(request.getHearingReference());
        if (request.getHearingNotes() != null) entity.setHearingNotes(request.getHearingNotes());
        if (request.getHearingOutcome() != null) entity.setHearingOutcome(request.getHearingOutcome());
        if (request.getHearingStatus() != null && request.getHearingStatus() != entity.getHearingStatus()) {
            validateHearingStatusTransition(entity.getHearingStatus(), request.getHearingStatus());
            entity.setHearingStatus(request.getHearingStatus());
        }

        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        if (request.getRiskLevel() != null) entity.setRiskLevel(request.getRiskLevel());

        if (request.getWaitingForClient() != null) entity.setWaitingForClient(request.getWaitingForClient());
        if (request.getWaitingReason() != null) entity.setWaitingReason(request.getWaitingReason());
        if (request.getExpectedResponseDate() != null) entity.setExpectedResponseDate(request.getExpectedResponseDate());

        if (request.getFollowUpDate() != null) entity.setFollowUpDate(request.getFollowUpDate());
        if (request.getFollowUpNotes() != null) entity.setFollowUpNotes(request.getFollowUpNotes());

        if (request.getComplianceObligationId() != null) entity.setComplianceObligationId(request.getComplianceObligationId());
        if (request.getClientServiceId() != null) entity.setClientServiceId(request.getClientServiceId());

        NoticeStatus oldStatus = entity.getStatus();
        if (request.getStatus() != null && request.getStatus() != oldStatus) {
            validateNoticeStatusTransition(oldStatus, request.getStatus());
            entity.setStatus(request.getStatus());
            recordActivity(entity.getId(), NoticeActivityType.STATUS_CHANGED, currentUserId,
                    "Notice status changed from " + oldStatus + " to " + request.getStatus(),
                    oldStatus.name(), request.getStatus().name(), null);
            auditService.logEvent("STATUS_CHANGED", "TAX_NOTICE", entity.getId().toString(), oldStatus.name(), request.getStatus().name());
        }

        UUID oldAssignee = entity.getAssignedEmployeeId();
        if (request.getAssignedEmployeeId() != null && !request.getAssignedEmployeeId().equals(oldAssignee)) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned employee not found with id: " + request.getAssignedEmployeeId()));
            entity.setAssignedEmployeeId(request.getAssignedEmployeeId());
            recordActivity(entity.getId(), NoticeActivityType.ASSIGNMENT_CHANGED, currentUserId,
                    "Assigned employee updated", oldAssignee != null ? oldAssignee.toString() : "None",
                    request.getAssignedEmployeeId().toString(), null);
            auditService.logEvent("NOTICE_ASSIGNED", "TAX_NOTICE", entity.getId().toString(), null, request.getAssignedEmployeeId().toString());
        }

        if (request.getReviewerEmployeeId() != null && !request.getReviewerEmployeeId().equals(entity.getReviewerEmployeeId())) {
            employeeRepository.findByIdAndOrganizationId(request.getReviewerEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reviewer employee not found with id: " + request.getReviewerEmployeeId()));
            entity.setReviewerEmployeeId(request.getReviewerEmployeeId());
        }
        if (request.getPartnerEmployeeId() != null && !request.getPartnerEmployeeId().equals(entity.getPartnerEmployeeId())) {
            employeeRepository.findByIdAndOrganizationId(request.getPartnerEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Partner employee not found with id: " + request.getPartnerEmployeeId()));
            entity.setPartnerEmployeeId(request.getPartnerEmployeeId());
        }
        if (request.getIssuingAuthority() != null) entity.setIssuingAuthority(request.getIssuingAuthority());
        if (request.getIssuingOfficerName() != null) entity.setIssuingOfficerName(request.getIssuingOfficerName());
        if (request.getHearingDate() != null) entity.setHearingDate(request.getHearingDate());
        if (request.getHearingTime() != null) entity.setHearingTime(request.getHearingTime());
        if (request.getInternalNotes() != null) entity.setInternalNotes(request.getInternalNotes());

        TaxNoticeEntity updated = noticeRepository.save(entity);
        auditService.logEvent("NOTICE_UPDATED", "TAX_NOTICE", updated.getId().toString(), null, updated);

        return enrichNoticeDto(updated);
    }

    @Override
    @Transactional
    public void deleteNotice(UUID noticeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TaxNoticeEntity entity = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(entity.getClientId());

        auditService.logEvent("NOTICE_DELETED", "TAX_NOTICE", noticeId.toString(), entity, null);
        noticeRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public NoticeDashboardStatsDto getDashboardStats() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);

        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);

        List<TaxNoticeEntity> notices;
        if (!scope.isFirmAdmin()) {
            if (accessibleClientIds == null || accessibleClientIds.isEmpty()) {
                notices = List.of();
            } else {
                notices = noticeRepository.findAll(buildSpecification(organizationId, null, accessibleClientIds));
            }
        } else {
            notices = noticeRepository.findAll(buildSpecification(organizationId, null, null));
        }

        long totalActive = 0;
        long overdue = 0;
        long dueToday = 0;
        long dueThisWeek = 0;
        long pendingReview = 0;
        long pendingPartner = 0;
        long upcomingHearings = 0;
        long critical = 0;
        long resolvedThisMonth = 0;
        BigDecimal totalDemand = BigDecimal.ZERO;

        Map<NoticeDepartment, Long> byDepartment = new EnumMap<>(NoticeDepartment.class);
        for (NoticeDepartment dept : NoticeDepartment.values()) {
            byDepartment.put(dept, 0L);
        }

        Map<NoticeStatus, Long> byStatus = new EnumMap<>(NoticeStatus.class);
        for (NoticeStatus st : NoticeStatus.values()) {
            byStatus.put(st, 0L);
        }

        for (TaxNoticeEntity n : notices) {
            if (n.getStatus() != null) {
                byStatus.put(n.getStatus(), byStatus.getOrDefault(n.getStatus(), 0L) + 1);
            }
            if (n.getDepartment() != null && !CLOSED_STATUSES.contains(n.getStatus())) {
                byDepartment.put(n.getDepartment(), byDepartment.getOrDefault(n.getDepartment(), 0L) + 1);
            }

            if (!CLOSED_STATUSES.contains(n.getStatus())) {
                totalActive++;
                if (n.getDemandAmount() != null) {
                    totalDemand = totalDemand.add(n.getDemandAmount());
                }
                if (n.getResponseDueDate() != null) {
                    if (n.getResponseDueDate().isBefore(today)) {
                        overdue++;
                    }
                    if (n.getResponseDueDate().isEqual(today)) {
                        dueToday++;
                    }
                    if (!n.getResponseDueDate().isBefore(today) && !n.getResponseDueDate().isAfter(weekEnd)) {
                        dueThisWeek++;
                    }
                }
                if (n.getPriority() == NoticePriority.CRITICAL) {
                    critical++;
                }
                if (n.getHearingDate() != null && !n.getHearingDate().isBefore(today)) {
                    upcomingHearings++;
                }
            }

            if (n.getStatus() == NoticeStatus.INTERNAL_REVIEW) {
                pendingReview++;
            } else if (n.getStatus() == NoticeStatus.PARTNER_APPROVED) {
                pendingPartner++;
            } else if (n.getStatus() == NoticeStatus.RESOLVED) {
                resolvedThisMonth++;
            }
        }

        return NoticeDashboardStatsDto.builder()
                .totalActiveNotices(totalActive)
                .overdueNotices(overdue)
                .dueTodayNotices(dueToday)
                .dueThisWeekNotices(dueThisWeek)
                .pendingReviewNotices(pendingReview)
                .pendingPartnerApprovalNotices(pendingPartner)
                .upcomingHearingsCount(upcomingHearings)
                .criticalPriorityCount(critical)
                .resolvedThisMonthCount(resolvedThisMonth)
                .totalDemandUnderDispute(totalDemand)
                .byDepartment(byDepartment)
                .byStatus(byStatus)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxNoticeDto> getNoticesByClient(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        validateClientAccess(clientId);
        return noticeRepository.findAllByOrganizationIdAndClientId(organizationId, clientId).stream()
                .map(this::enrichNoticeDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ClientNoticeDto> getClientPortalNotices(UUID clientId, PageRequestDto pageRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (SecurityUtils.isClientPortalUser()) {
            UUID callerClientId = SecurityUtils.getCurrentClientId().orElse(null);
            if (!Objects.equals(callerClientId, clientId)) {
                throw new ForbiddenException("Access denied: You can only view notices for your own account");
            }
        } else {
            validateClientAccess(clientId);
        }

        Pageable pageable = pageRequest != null ? pageRequest.toPageable() : PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "receivedDate"));
        Page<TaxNoticeEntity> page = noticeRepository.findAllByOrganizationIdAndClientId(organizationId, clientId, pageable);

        List<ClientNoticeDto> dtos = noticeMapper.toClientDtoList(page.getContent());

        return PagedResponse.<ClientNoticeDto>builder()
                .content(dtos)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    // ==========================================
    // Response Drafting & Maker-Checker Reviews
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<NoticeResponseDto> getResponses(UUID noticeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        List<NoticeResponseEntity> responses = responseRepository.findAllByOrganizationIdAndNoticeIdOrderByResponseVersionDesc(organizationId, noticeId);
        return responses.stream().map(this::enrichResponseDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public NoticeResponseDto getResponseById(UUID noticeId, UUID responseId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        NoticeResponseEntity entity = responseRepository.findByIdAndNoticeIdAndOrganizationId(responseId, noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice response not found with id: " + responseId));
        return enrichResponseDto(entity);
    }

    @Override
    @Transactional
    public NoticeResponseDto createResponse(UUID noticeId, CreateNoticeResponseRequest request) {
        return draftNoticeResponse(noticeId, request);
    }

    @Override
    @Transactional
    public NoticeResponseDto draftNoticeResponse(UUID noticeId, CreateNoticeResponseRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        Integer currentMaxVersion = responseRepository.findMaxVersionByNoticeId(organizationId, noticeId);
        int nextVersion = currentMaxVersion != null ? currentMaxVersion + 1 : 1;

        ReviewStatus initialStatus = Boolean.TRUE.equals(request.getSubmitForReview())
                ? ReviewStatus.PENDING_REVIEW
                : ReviewStatus.DRAFT;

        NoticeResponseEntity entity = NoticeResponseEntity.builder()
                .noticeId(noticeId)
                .responseVersion(nextVersion)
                .responseTitle(request.getResponseTitle().trim())
                .responseSummary(request.getResponseSummary())
                .legalGrounds(request.getLegalGrounds())
                .factsOfCase(request.getFactsOfCase())
                .preparedByUserId(currentUserId)
                .reviewStatus(initialStatus)
                .build();

        NoticeResponseEntity saved = responseRepository.save(entity);

        // Synchronize notice fields
        notice.setResponseDraft(request.getResponseSummary() != null ? request.getResponseSummary() : request.getResponseTitle());
        if (initialStatus == ReviewStatus.PENDING_REVIEW) {
            validateNoticeStatusTransition(notice.getStatus(), NoticeStatus.INTERNAL_REVIEW);
            notice.setStatus(NoticeStatus.INTERNAL_REVIEW);
            notice.setResponseStatus(NoticeResponseStatus.UNDER_REVIEW);
            noticeRepository.save(notice);
            recordActivity(noticeId, NoticeActivityType.RESPONSE_SUBMITTED_FOR_REVIEW, currentUserId,
                    "Response draft v" + nextVersion + " submitted for internal review",
                    NoticeStatus.RESPONSE_DRAFTING.name(), NoticeStatus.INTERNAL_REVIEW.name(), null);
        } else {
            if (notice.getStatus() == NoticeStatus.RECEIVED || notice.getStatus() == NoticeStatus.UNDER_REVIEW || notice.getStatus() == NoticeStatus.NOTICE_REGISTERED) {
                validateNoticeStatusTransition(notice.getStatus(), NoticeStatus.RESPONSE_DRAFTING);
                notice.setStatus(NoticeStatus.RESPONSE_DRAFTING);
            }
            notice.setResponseStatus(NoticeResponseStatus.DRAFT);
            noticeRepository.save(notice);
            recordActivity(noticeId, NoticeActivityType.RESPONSE_DRAFT_CREATED, currentUserId,
                    "Response draft v" + nextVersion + " created (" + saved.getResponseTitle() + ")",
                    null, null, null);
        }

        auditService.logEvent("RESPONSE_DRAFT_CREATED", "TAX_NOTICE", saved.getId().toString(), null, saved);
        return enrichResponseDto(saved);
    }

    @Override
    @Transactional
    public NoticeResponseDto reviewResponse(UUID noticeId, UUID responseId, ReviewNoticeResponseRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        NoticeResponseEntity response = responseRepository.findByIdAndNoticeIdAndOrganizationId(responseId, noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice response not found with id: " + responseId));

        String action = request.getAction().trim().toUpperCase();

        switch (action) {
            case "SUBMIT_FOR_REVIEW" -> {
                validateResponseStatusTransition(notice.getResponseStatus(), NoticeResponseStatus.UNDER_REVIEW);
                response.setReviewStatus(ReviewStatus.PENDING_REVIEW);
                notice.setStatus(NoticeStatus.INTERNAL_REVIEW);
                notice.setResponseStatus(NoticeResponseStatus.UNDER_REVIEW);
                recordActivity(noticeId, NoticeActivityType.RESPONSE_SUBMITTED_FOR_REVIEW, currentUserId,
                    "Response draft v" + response.getResponseVersion() + " submitted for internal review",
                    null, ReviewStatus.PENDING_REVIEW.name(), request.getComments());
            }
            case "APPROVE_REVIEW", "APPROVE" -> {
                if (response.getPreparedByUserId() != null && response.getPreparedByUserId().equals(currentUserId)) {
                    throw new ForbiddenException("Maker-Checker Violation: The preparer of the response draft cannot approve their own draft.");
                }
                response.setReviewedByUserId(currentUserId);
                response.setReviewStatus(ReviewStatus.APPROVED_BY_REVIEWER);
                response.setReviewComments(request.getComments());
                notice.setStatus(NoticeStatus.INTERNAL_REVIEW);
                notice.setResponseStatus(NoticeResponseStatus.UNDER_REVIEW);
                recordActivity(noticeId, NoticeActivityType.RESPONSE_APPROVED, currentUserId,
                        "Response draft v" + response.getResponseVersion() + " approved by reviewer",
                        ReviewStatus.PENDING_REVIEW.name(), ReviewStatus.APPROVED_BY_REVIEWER.name(), request.getComments());
                auditService.logEvent("RESPONSE_APPROVED", "TAX_NOTICE", response.getId().toString(), null, response);
            }
            case "REQUEST_REVISION", "CHANGES_REQUIRED", "REJECT" -> {
                if (!StringUtils.hasText(request.getComments())) {
                    throw new BusinessValidationException("Review comments are mandatory when requesting revision");
                }
                validateResponseStatusTransition(notice.getResponseStatus(), NoticeResponseStatus.DRAFT);
                response.setReviewStatus(ReviewStatus.REVISION_REQUESTED);
                response.setReviewComments(request.getComments());
                notice.setStatus(NoticeStatus.RESPONSE_DRAFTING);
                notice.setResponseStatus(NoticeResponseStatus.DRAFT);
                recordActivity(noticeId, NoticeActivityType.REVISION_REQUESTED, currentUserId,
                        "Revision requested for response draft v" + response.getResponseVersion() + ": " + request.getComments(),
                        null, ReviewStatus.REVISION_REQUESTED.name(), request.getComments());
            }
            case "APPROVE_PARTNER", "PARTNER_SIGN_OFF" -> {
                if (response.getPreparedByUserId() != null && response.getPreparedByUserId().equals(currentUserId)) {
                    throw new ForbiddenException("Maker-Checker Violation: The author cannot sign off as partner on their own draft.");
                }
                validateResponseStatusTransition(notice.getResponseStatus(), NoticeResponseStatus.APPROVED);
                response.setApprovedByUserId(currentUserId);
                response.setReviewStatus(ReviewStatus.APPROVED_BY_PARTNER);
                if (StringUtils.hasText(request.getComments())) {
                    response.setReviewComments(request.getComments());
                }
                notice.setStatus(NoticeStatus.PARTNER_APPROVED);
                notice.setResponseStatus(NoticeResponseStatus.APPROVED);
                recordActivity(noticeId, NoticeActivityType.RESPONSE_PARTNER_APPROVED, currentUserId,
                        "Response draft v" + response.getResponseVersion() + " signed off by Partner",
                        null, NoticeStatus.PARTNER_APPROVED.name(), request.getComments());
                auditService.logEvent("RESPONSE_APPROVED", "TAX_NOTICE", response.getId().toString(), null, response);
            }
            case "CLIENT_CONFIRMATION", "CLIENT_CONFIRM" -> {
                response.setReviewStatus(ReviewStatus.CLIENT_CONFIRMATION);
                if (StringUtils.hasText(request.getComments())) {
                    response.setReviewComments(response.getReviewComments() != null ? response.getReviewComments() + "\nClient Confirmation: " + request.getComments() : "Client Confirmation: " + request.getComments());
                }
                notice.setStatus(NoticeStatus.CLIENT_CONFIRMATION);
                recordActivity(noticeId, NoticeActivityType.CLIENT_CONFIRMED, currentUserId,
                        "Response draft v" + response.getResponseVersion() + " confirmed with client",
                        null, NoticeStatus.CLIENT_CONFIRMATION.name(), request.getComments());
            }
            case "READY_FOR_SUBMISSION", "MARK_READY" -> {
                response.setReviewStatus(ReviewStatus.READY_FOR_SUBMISSION);
                notice.setStatus(NoticeStatus.READY_FOR_SUBMISSION);
                recordActivity(noticeId, NoticeActivityType.RESPONSE_READY_FOR_SUBMISSION, currentUserId,
                        "Response draft v" + response.getResponseVersion() + " marked ready for filing/submission",
                        null, NoticeStatus.READY_FOR_SUBMISSION.name(), request.getComments());
            }
            default -> throw new BusinessValidationException("Invalid review action: " + action);
        }

        NoticeResponseEntity updatedResponse = responseRepository.save(response);
        noticeRepository.save(notice);

        auditService.logEvent("NOTICE_RESPONSE_REVIEWED", "NOTICE_RESPONSE", updatedResponse.getId().toString(), null, updatedResponse);
        return enrichResponseDto(updatedResponse);
    }

    @Override
    @Transactional
    public TaxNoticeDto setWaitingForClient(UUID noticeId, SetWaitingForClientRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        notice.setWaitingForClient(true);
        notice.setWaitingReason(request.getWaitingReason());
        notice.setWaitingRequestedAt(Instant.now());
        notice.setWaitingRequestedBy(currentUserId);
        notice.setExpectedResponseDate(request.getExpectedResponseDate());
        notice.setStatus(NoticeStatus.WAITING_FOR_CLIENT);

        // Optional document request generation
        if (Boolean.TRUE.equals(request.getCreateDocumentRequest())) {
            String reqNum = "DOC-REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String purpose = StringUtils.hasText(request.getDocumentRequestTitle())
                    ? request.getDocumentRequestTitle().trim()
                    : "Information/Documents required for Tax Notice " + notice.getNoticeNumber();
            String message = StringUtils.hasText(request.getDocumentRequestDescription())
                    ? request.getDocumentRequestDescription().trim()
                    : request.getWaitingReason();

            com.taxoryn.module.docrequest.entity.DocumentRequestEntity docReq = com.taxoryn.module.docrequest.entity.DocumentRequestEntity.builder()
                    .clientId(notice.getClientId())
                    .noticeId(notice.getId())
                    .requestNumber(reqNum)
                    .purpose(purpose)
                    .message(message)
                    .dueDate(request.getExpectedResponseDate())
                    .requestedByUserId(currentUserId)
                    .status(RequestStatus.SENT)
                    .exchangeType(com.taxoryn.module.docrequest.entity.DocumentRequestEntity.ExchangeType.DOCUMENT_REQUEST)
                    .direction(com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestDirection.PRACTITIONER_TO_CLIENT)
                    .build();
            documentRequestRepository.save(docReq);
            recordActivity(noticeId, NoticeActivityType.DOCUMENT_REQUESTED, currentUserId,
                    "Client document request created: " + reqNum, null, null, null);
        }

        recordActivity(noticeId, NoticeActivityType.WAITING_FOR_CLIENT, currentUserId,
                "Notice placed on waiting for client: " + request.getWaitingReason(),
                null, NoticeStatus.WAITING_FOR_CLIENT.name(), request.getWaitingReason());

        TaxNoticeEntity saved = noticeRepository.save(notice);
        auditService.logEvent("NOTICE_WAITING_FOR_CLIENT", "TAX_NOTICE", saved.getId().toString(), null, saved);
        return enrichNoticeDto(saved);
    }

    @Override
    @Transactional
    public TaxNoticeDto resumeFromWaiting(UUID noticeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        notice.setWaitingForClient(false);
        notice.setWaitingReason(null);
        notice.setWaitingRequestedAt(null);
        notice.setWaitingRequestedBy(null);
        notice.setExpectedResponseDate(null);

        if (notice.getStatus() == NoticeStatus.WAITING_FOR_CLIENT || notice.getStatus() == NoticeStatus.INFO_REQUESTED) {
            long responses = responseRepository.countByOrganizationIdAndNoticeId(organizationId, noticeId);
            notice.setStatus(responses > 0 ? NoticeStatus.RESPONSE_DRAFTING : NoticeStatus.UNDER_REVIEW);
        }

        recordActivity(noticeId, NoticeActivityType.RESUMED_FROM_WAITING, currentUserId,
                "Notice resumed from waiting for client", null, notice.getStatus().name(), null);

        TaxNoticeEntity saved = noticeRepository.save(notice);
        auditService.logEvent("NOTICE_RESUMED", "TAX_NOTICE", saved.getId().toString(), null, saved);
        return enrichNoticeDto(saved);
    }

    @Override
    @Transactional
    public TaxNoticeDto setFollowUp(UUID noticeId, SetFollowUpRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        notice.setFollowUpDate(request.getFollowUpDate());
        if (request.getFollowUpNotes() != null) {
            notice.setFollowUpNotes(request.getFollowUpNotes());
        }
        if (request.getResponsibleEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getResponsibleEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Responsible employee not found with id: " + request.getResponsibleEmployeeId()));
            notice.setAssignedEmployeeId(request.getResponsibleEmployeeId());
        }

        recordActivity(noticeId, NoticeActivityType.FOLLOW_UP_SCHEDULED, currentUserId,
                "Follow-up scheduled for " + request.getFollowUpDate() + (request.getFollowUpNotes() != null ? " (" + request.getFollowUpNotes() + ")" : ""),
                null, null, request.getFollowUpNotes());

        TaxNoticeEntity saved = noticeRepository.save(notice);
        auditService.logEvent("NOTICE_FOLLOW_UP_SET", "TAX_NOTICE", saved.getId().toString(), null, saved);
        return enrichNoticeDto(saved);
    }

    @Override
    @Transactional
    public NoticeResponseDto clientConfirmResponse(UUID noticeId, UUID responseId, String confirmationNotes) {
        ReviewNoticeResponseRequest req = ReviewNoticeResponseRequest.builder()
                .action("CLIENT_CONFIRMATION")
                .comments(confirmationNotes)
                .build();
        return reviewResponse(noticeId, responseId, req);
    }

    @Override
    @Transactional
    public NoticeResponseDto markResponseReadyForSubmission(UUID noticeId, UUID responseId) {
        ReviewNoticeResponseRequest req = ReviewNoticeResponseRequest.builder()
                .action("READY_FOR_SUBMISSION")
                .build();
        return reviewResponse(noticeId, responseId, req);
    }

    @Override
    @Transactional
    public TaxNoticeDto updateNoticeResponse(UUID noticeId, UpdateNoticeResponseRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        if (request.getResponseRequired() != null) {
            notice.setResponseRequired(request.getResponseRequired());
        }
        if (request.getResponseDueDate() != null) {
            notice.setResponseDueDate(request.getResponseDueDate());
            notice.setPriority(computePriority(request.getResponseDueDate()));
        }
        if (request.getResponseStatus() != null && request.getResponseStatus() != notice.getResponseStatus()) {
            validateResponseStatusTransition(notice.getResponseStatus(), request.getResponseStatus());
            notice.setResponseStatus(request.getResponseStatus());
        }
        if (request.getResponseDraft() != null) {
            notice.setResponseDraft(request.getResponseDraft());
        }
        if (request.getSubmissionReference() != null) {
            notice.setSubmissionReference(request.getSubmissionReference());
        }
        if (request.getSubmissionNotes() != null) {
            notice.setSubmissionNotes(request.getSubmissionNotes());
        }

        // Link response document if specified
        if (request.getResponseDocumentId() != null) {
            documentRepository.findByIdAndOrganizationId(request.getResponseDocumentId(), organizationId)
                    .ifPresent(doc -> {
                        doc.setNoticeId(noticeId);
                        doc.setDocumentType(DocumentType.NOTICE_RESPONSE_DRAFT);
                        documentRepository.save(doc);
                    });
        }

        TaxNoticeEntity saved = noticeRepository.save(notice);
        recordActivity(noticeId, NoticeActivityType.RESPONSE_UPDATED, currentUserId,
                "Notice response details updated", null, null, null);
        auditService.logEvent("RESPONSE_UPDATED", "TAX_NOTICE", saved.getId().toString(), null, saved);

        return enrichNoticeDto(saved);
    }

    @Override
    @Transactional
    public TaxNoticeDto submitNoticeResponse(UUID noticeId, SubmitNoticeRequest request) {
        return submitNotice(noticeId, request);
    }

    // ==========================================
    // Notice Hearings & Proceedings
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<NoticeHearingDto> getHearings(UUID noticeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        List<NoticeHearingEntity> hearings = hearingRepository.findAllByOrganizationIdAndNoticeIdOrderByHearingDateDesc(organizationId, noticeId);
        return hearings.stream().map(this::enrichHearingDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NoticeHearingDto scheduleHearing(UUID noticeId, ScheduleHearingRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        if (request.getDesignatedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getDesignatedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Designated employee not found with id: " + request.getDesignatedEmployeeId()));
        }
        if (request.getDesignatedPartnerId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getDesignatedPartnerId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Designated partner not found with id: " + request.getDesignatedPartnerId()));
        }

        NoticeHearingEntity hearing = NoticeHearingEntity.builder()
                .noticeId(noticeId)
                .hearingDate(request.getHearingDate())
                .hearingTime(request.getHearingTime())
                .hearingMode(request.getHearingMode() != null ? request.getHearingMode() : HearingMode.VIRTUAL_VC)
                .hearingLink(request.getHearingLink())
                .authorityName(request.getAuthorityName())
                .officerName(request.getOfficerName())
                .designatedEmployeeId(request.getDesignatedEmployeeId())
                .designatedPartnerId(request.getDesignatedPartnerId())
                .status(HearingStatus.SCHEDULED)
                .proceedingsSummary(request.getProceedingsSummary())
                .build();

        NoticeHearingEntity saved = hearingRepository.save(hearing);

        // Update notice hearing properties
        notice.setHearingRequired(true);
        notice.setHearingDate(request.getHearingDate());
        notice.setHearingTime(request.getHearingTime());
        notice.setHearingMode(request.getHearingMode() != null ? request.getHearingMode() : HearingMode.VIRTUAL_VC);
        notice.setHearingStatus(HearingStatus.SCHEDULED);
        if (!CLOSED_STATUSES.contains(notice.getStatus())) {
            notice.setStatus(NoticeStatus.HEARING_SCHEDULED);
        }
        noticeRepository.save(notice);

        recordActivity(noticeId, NoticeActivityType.HEARING_SCHEDULED, currentUserId,
                "Hearing scheduled for " + request.getHearingDate() + (StringUtils.hasText(request.getHearingTime()) ? " at " + request.getHearingTime() : ""),
                null, HearingStatus.SCHEDULED.name(), null);

        auditService.logEvent("HEARING_SCHEDULED", "TAX_NOTICE", saved.getId().toString(), null, saved);

        com.taxoryn.module.notice.dto.TaxNoticeConfigDto config = configurationService.getEffectiveConfiguration(organizationId);
        if (config.isNotifyOnHearing()) {
            dispatchNoticeHearingNotifications(saved, notice);
        }

        return enrichHearingDto(saved);
    }

    @Override
    @Transactional
    public NoticeHearingDto recordHearingOutcome(UUID noticeId, UUID hearingId, RecordHearingOutcomeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        NoticeHearingEntity hearing;
        if (hearingId != null) {
            hearing = hearingRepository.findByIdAndNoticeIdAndOrganizationId(hearingId, noticeId, organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hearing not found with id: " + hearingId));
        } else {
            hearing = hearingRepository.findAllByOrganizationIdAndNoticeIdOrderByHearingDateDesc(organizationId, noticeId)
                    .stream().findFirst()
                    .orElseGet(() -> hearingRepository.save(NoticeHearingEntity.builder()
                            .noticeId(noticeId)
                            .hearingDate(notice.getHearingDate() != null ? notice.getHearingDate() : LocalDate.now())
                            .hearingTime(notice.getHearingTime())
                            .hearingMode(notice.getHearingMode() != null ? notice.getHearingMode() : HearingMode.VIRTUAL_VC)
                            .status(HearingStatus.SCHEDULED)
                            .build()));
        }

        HearingStatus oldStatus = hearing.getStatus();
        if (request.getStatus() != null && request.getStatus() != oldStatus) {
            validateHearingStatusTransition(oldStatus, request.getStatus());
            hearing.setStatus(request.getStatus());
            notice.setHearingStatus(request.getStatus());
        }

        if (request.getProceedingsSummary() != null) hearing.setProceedingsSummary(request.getProceedingsSummary());
        if (request.getOutcomeSummary() != null) {
            hearing.setOutcomeSummary(request.getOutcomeSummary());
            notice.setHearingOutcome(request.getOutcomeSummary());
        }
        if (request.getNextAction() != null) hearing.setNextAction(request.getNextAction());
        if (request.getNextHearingDate() != null) {
            hearing.setNextHearingDate(request.getNextHearingDate());
            notice.setHearingDate(request.getNextHearingDate());
        }

        NoticeHearingEntity saved = hearingRepository.save(hearing);
        noticeRepository.save(notice);

        recordActivity(noticeId, NoticeActivityType.HEARING_OUTCOME_RECORDED, currentUserId,
                "Hearing outcome recorded: " + request.getStatus() + (StringUtils.hasText(request.getOutcomeSummary()) ? " - " + request.getOutcomeSummary() : ""),
                oldStatus.name(), request.getStatus() != null ? request.getStatus().name() : oldStatus.name(), null);

        auditService.logEvent("HEARING_OUTCOME_RECORDED", "TAX_NOTICE", saved.getId().toString(), null, saved);

        com.taxoryn.module.notice.dto.TaxNoticeConfigDto config = configurationService.getEffectiveConfiguration(organizationId);
        if (config.isNotifyOnHearing()) {
            dispatchNoticeHearingNotifications(saved, notice);
        }

        return enrichHearingDto(saved);
    }

    @Override
    @Transactional
    public TaxNoticeDto updateNoticeHearing(UUID noticeId, UpdateNoticeHearingRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        if (request.getHearingRequired() != null) notice.setHearingRequired(request.getHearingRequired());
        if (request.getHearingDate() != null) notice.setHearingDate(request.getHearingDate());
        if (request.getHearingTime() != null) notice.setHearingTime(request.getHearingTime());
        if (request.getHearingMode() != null) notice.setHearingMode(request.getHearingMode());
        if (request.getHearingLocation() != null) notice.setHearingLocation(request.getHearingLocation());
        if (request.getHearingReference() != null) notice.setHearingReference(request.getHearingReference());
        if (request.getHearingNotes() != null) notice.setHearingNotes(request.getHearingNotes());
        if (request.getHearingOutcome() != null) notice.setHearingOutcome(request.getHearingOutcome());
        if (request.getHearingStatus() != null && request.getHearingStatus() != notice.getHearingStatus()) {
            validateHearingStatusTransition(notice.getHearingStatus(), request.getHearingStatus());
            notice.setHearingStatus(request.getHearingStatus());
        }

        TaxNoticeEntity saved = noticeRepository.save(notice);
        recordActivity(noticeId, NoticeActivityType.HEARING_UPDATED, currentUserId,
                "Hearing details updated", null, null, null);
        auditService.logEvent("HEARING_UPDATED", "TAX_NOTICE", saved.getId().toString(), null, saved);

        return enrichNoticeDto(saved);
    }

    @Override
    @Transactional
    public NoticeHearingDto completeHearing(UUID noticeId, UUID hearingId, RecordHearingOutcomeRequest request) {
        RecordHearingOutcomeRequest req = request != null ? request : new RecordHearingOutcomeRequest();
        req.setStatus(HearingStatus.COMPLETED);
        NoticeHearingDto dto = recordHearingOutcome(noticeId, hearingId, req);
        auditService.logEvent("HEARING_COMPLETED", "TAX_NOTICE", dto.getId().toString(), null, dto);
        return dto;
    }

    @Override
    @Transactional
    public NoticeHearingDto adjournHearing(UUID noticeId, UUID hearingId, AdjournHearingRequest request) {
        RecordHearingOutcomeRequest outcomeRequest = RecordHearingOutcomeRequest.builder()
                .status(HearingStatus.ADJOURNED)
                .nextHearingDate(request.getNextHearingDate())
                .outcomeSummary(request.getReason())
                .proceedingsSummary(request.getProceedingsSummary())
                .build();
        NoticeHearingDto dto = recordHearingOutcome(noticeId, hearingId, outcomeRequest);
        auditService.logEvent("HEARING_ADJOURNED", "TAX_NOTICE", dto.getId().toString(), null, dto);
        return dto;
    }

    @Override
    @Transactional
    public NoticeHearingDto cancelHearing(UUID noticeId, UUID hearingId, String reason) {
        RecordHearingOutcomeRequest outcomeRequest = RecordHearingOutcomeRequest.builder()
                .status(HearingStatus.CANCELLED)
                .outcomeSummary(reason)
                .build();
        NoticeHearingDto dto = recordHearingOutcome(noticeId, hearingId, outcomeRequest);
        auditService.logEvent("HEARING_CANCELLED", "TAX_NOTICE", dto.getId().toString(), null, dto);
        return dto;
    }

    // ==========================================
    // Filing Submission & Closure
    // ==========================================

    @Override
    @Transactional
    public TaxNoticeDto submitNotice(UUID noticeId, SubmitNoticeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        if (notice.getStatus() == NoticeStatus.SUBMITTED || notice.getResponseStatus() == NoticeResponseStatus.SUBMITTED) {
            throw new BusinessValidationException("Tax notice response has already been submitted and cannot be resubmitted");
        }
        validateNoticeStatusTransition(notice.getStatus(), NoticeStatus.SUBMITTED);

        NoticeStatus oldStatus = notice.getStatus();
        notice.setStatus(NoticeStatus.SUBMITTED);
        notice.setResponseStatus(NoticeResponseStatus.SUBMITTED);
        notice.setSubmissionMode(request.getSubmissionMode());
        notice.setSubmittedAt(Instant.now());
        notice.setResponseSubmittedAt(Instant.now());
        notice.setResponseSubmittedBy(currentUserId);
        if (StringUtils.hasText(request.getPortalAcknowledgementNumber())) {
            notice.setPortalAcknowledgementNumber(request.getPortalAcknowledgementNumber().trim());
            notice.setSubmissionReference(request.getPortalAcknowledgementNumber().trim());
        }
        if (StringUtils.hasText(request.getRemarks())) {
            notice.setSubmissionNotes(request.getRemarks());
        }

        // Link response version if specified - strictly scoped to this notice and organization
        if (request.getResponseId() != null) {
            NoticeResponseEntity resp = responseRepository.findByIdAndNoticeIdAndOrganizationId(request.getResponseId(), noticeId, organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Notice response not found with id: " + request.getResponseId()));
            resp.setReviewStatus(ReviewStatus.SUBMITTED);
            resp.setSubmittedAt(Instant.now());
            resp.setAcknowledgementNumber(request.getPortalAcknowledgementNumber());
            resp.setAcknowledgementDate(request.getAcknowledgementDate() != null ? request.getAcknowledgementDate() : LocalDate.now());
            responseRepository.save(resp);
        }

        // Link document proofs
        if (request.getAcknowledgementDocumentId() != null) {
            documentRepository.findByIdAndOrganizationId(request.getAcknowledgementDocumentId(), organizationId)
                    .ifPresent(doc -> {
                        doc.setNoticeId(noticeId);
                        doc.setDocumentType(DocumentType.NOTICE_ACKNOWLEDGEMENT);
                        documentRepository.save(doc);
                    });
        }
        if (request.getSubmissionProofDocumentId() != null) {
            documentRepository.findByIdAndOrganizationId(request.getSubmissionProofDocumentId(), organizationId)
                    .ifPresent(doc -> {
                        doc.setNoticeId(noticeId);
                        doc.setDocumentType(DocumentType.NOTICE_SUBMISSION_PROOF);
                        documentRepository.save(doc);
                    });
        }

        // Policy Check: If partner approval is required by org configuration, enforce sign-off
        com.taxoryn.module.notice.dto.TaxNoticeConfigDto config = configurationService.getEffectiveConfiguration(organizationId);
        if (config.isPartnerApprovalRequired()) {
            boolean isPartnerApproved = notice.getStatus() == NoticeStatus.PARTNER_APPROVED
                    || notice.getResponseStatus() == NoticeResponseStatus.APPROVED;
            PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
            boolean callerHasPartnerRole = scope.isFirmAdmin() || SecurityUtils.isTaxorynPlatformUser();
            if (!isPartnerApproved && !callerHasPartnerRole) {
                throw new BusinessValidationException("Partner approval is required by organization policy before submitting this notice response.");
            }
        }

        TaxNoticeEntity saved = noticeRepository.save(notice);

        recordActivity(noticeId, NoticeActivityType.RESPONSE_SUBMITTED, currentUserId,
                "Notice response submitted via " + request.getSubmissionMode() + (StringUtils.hasText(request.getPortalAcknowledgementNumber()) ? " (Ack: " + request.getPortalAcknowledgementNumber() + ")" : ""),
                oldStatus.name(), NoticeStatus.SUBMITTED.name(), request.getRemarks());

        auditService.logEvent("RESPONSE_SUBMITTED", "TAX_NOTICE", saved.getId().toString(), null, saved);

        if (config.isNotifyOnSubmission()) {
            dispatchNoticeSubmissionNotifications(saved);
        }

        return enrichNoticeDto(saved);
    }

    @Override
    @Transactional
    public TaxNoticeDto closeNotice(UUID noticeId, CloseNoticeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());
        validateNoticeStatusTransition(notice.getStatus(), request.getClosureStatus());

        NoticeStatus oldStatus = notice.getStatus();
        notice.setStatus(request.getClosureStatus());
        notice.setClosureDate(request.getClosureDate() != null ? request.getClosureDate() : LocalDate.now());
        notice.setClosureRemarks(request.getClosureRemarks());

        if (request.getOrderDocumentId() != null) {
            documentRepository.findByIdAndOrganizationId(request.getOrderDocumentId(), organizationId)
                    .ifPresent(doc -> {
                        doc.setNoticeId(noticeId);
                        doc.setDocumentType(DocumentType.NOTICE_ORDER_COMMUNICATION);
                        documentRepository.save(doc);
                    });
        }

        TaxNoticeEntity saved = noticeRepository.save(notice);

        NoticeActivityType actType = request.getClosureStatus() == NoticeStatus.RESOLVED ? NoticeActivityType.NOTICE_RESOLVED : NoticeActivityType.NOTICE_CLOSED;
        recordActivity(noticeId, actType, currentUserId,
                "Notice closed with status: " + request.getClosureStatus() + (StringUtils.hasText(request.getClosureRemarks()) ? " - Remarks: " + request.getClosureRemarks() : ""),
                oldStatus.name(), request.getClosureStatus().name(), request.getClosureRemarks());

        auditService.logEvent("NOTICE_CLOSED", "TAX_NOTICE", saved.getId().toString(), null, saved);
        return enrichNoticeDto(saved);
    }

    // ==========================================
    // Activities & Timeline
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<NoticeActivityDto> getActivities(UUID noticeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        List<NoticeActivityEntity> activities = activityRepository.findAllByOrganizationIdAndNoticeIdOrderByCreatedAtDesc(organizationId, noticeId);
        return noticeMapper.toActivityDtoList(activities);
    }

    @Override
    @Transactional
    public void addInternalNote(UUID noticeId, String note) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        TaxNoticeEntity notice = noticeRepository.findByIdAndOrganizationId(noticeId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax notice not found with id: " + noticeId));

        validateClientAccess(notice.getClientId());

        if (!StringUtils.hasText(note)) {
            throw new BusinessValidationException("Internal note cannot be empty");
        }

        String existing = notice.getInternalNotes();
        String updatedNotes = StringUtils.hasText(existing) ? existing + "\n\n[" + LocalDate.now() + "]: " + note.trim() : "[" + LocalDate.now() + "]: " + note.trim();
        notice.setInternalNotes(updatedNotes);
        noticeRepository.save(notice);

        recordActivity(noticeId, NoticeActivityType.INTERNAL_NOTE_ADDED, currentUserId,
                "Internal note added: " + note.trim(), null, null, null);
    }

    // ==========================================
    // State Transition Validations
    // ==========================================

    private void validateNoticeStatusTransition(NoticeStatus from, NoticeStatus to) {
        if (from == null || to == null || from == to) {
            return;
        }

        // Closed cases can only transition to UNDER_REVIEW or APPEAL_FILED
        if (CLOSED_STATUSES.contains(from)) {
            if (to != NoticeStatus.UNDER_REVIEW && to != NoticeStatus.APPEAL_FILED && to != NoticeStatus.FOLLOW_UP) {
                throw new BusinessValidationException("Cannot transition closed notice with status " + from + " to " + to);
            }
        }
    }

    private void validateResponseStatusTransition(NoticeResponseStatus from, NoticeResponseStatus to) {
        if (from == null || to == null || from == to) {
            return;
        }

        boolean valid = switch (from) {
            case REQUIRED -> to == NoticeResponseStatus.DRAFT || to == NoticeResponseStatus.NOT_REQUIRED || to == NoticeResponseStatus.UNDER_REVIEW || to == NoticeResponseStatus.APPROVED;
            case DRAFT -> to == NoticeResponseStatus.UNDER_REVIEW || to == NoticeResponseStatus.APPROVED || to == NoticeResponseStatus.NOT_REQUIRED || to == NoticeResponseStatus.REQUIRED;
            case UNDER_REVIEW -> to == NoticeResponseStatus.APPROVED || to == NoticeResponseStatus.DRAFT || to == NoticeResponseStatus.NOT_REQUIRED;
            case APPROVED -> to == NoticeResponseStatus.SUBMITTED || to == NoticeResponseStatus.DRAFT || to == NoticeResponseStatus.UNDER_REVIEW;
            case SUBMITTED -> to == NoticeResponseStatus.SUBMITTED; // Terminal
            case NOT_REQUIRED -> to == NoticeResponseStatus.REQUIRED || to == NoticeResponseStatus.DRAFT;
        };

        if (!valid) {
            throw new BusinessValidationException("Invalid response status transition from " + from + " to " + to);
        }
    }

    private void validateHearingStatusTransition(HearingStatus from, HearingStatus to) {
        if (from == null || to == null || from == to) {
            return;
        }

        boolean fromIsScheduled = from == HearingStatus.SCHEDULED || from == HearingStatus.HEARING_SCHEDULED;
        boolean fromIsAdjourned = from == HearingStatus.ADJOURNED || from == HearingStatus.HEARING_ADJOURNED;
        boolean fromIsCompleted = from == HearingStatus.COMPLETED || from == HearingStatus.HEARING_COMPLETED;
        boolean fromIsCancelled = from == HearingStatus.CANCELLED || from == HearingStatus.HEARING_CANCELLED;

        boolean toIsScheduled = to == HearingStatus.SCHEDULED || to == HearingStatus.HEARING_SCHEDULED;
        boolean toIsAdjourned = to == HearingStatus.ADJOURNED || to == HearingStatus.HEARING_ADJOURNED;
        boolean toIsCompleted = to == HearingStatus.COMPLETED || to == HearingStatus.HEARING_COMPLETED;
        boolean toIsCancelled = to == HearingStatus.CANCELLED || to == HearingStatus.HEARING_CANCELLED;

        boolean valid;
        if (fromIsScheduled) {
            valid = toIsCompleted || toIsAdjourned || toIsCancelled;
        } else if (fromIsAdjourned) {
            valid = toIsScheduled || toIsCompleted || toIsCancelled;
        } else if (fromIsCancelled) {
            valid = toIsScheduled;
        } else if (fromIsCompleted) {
            valid = false; // Terminal
        } else {
            valid = true;
        }

        if (!valid) {
            throw new BusinessValidationException("Invalid hearing status transition from " + from + " to " + to);
        }
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private Specification<TaxNoticeEntity> buildSpecification(UUID organizationId, TaxNoticeFilterRequest filter, Set<UUID> accessibleClientIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (accessibleClientIds != null) {
                predicates.add(root.get("clientId").in(accessibleClientIds));
            }

            if (filter != null) {
                if (filter.getClientId() != null) {
                    predicates.add(cb.equal(root.get("clientId"), filter.getClientId()));
                }
                if (filter.getDepartment() != null) {
                    predicates.add(cb.equal(root.get("department"), filter.getDepartment()));
                }
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }
                if (filter.getPriority() != null) {
                    predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
                }
                if (filter.getRiskLevel() != null) {
                    predicates.add(cb.equal(root.get("riskLevel"), filter.getRiskLevel()));
                }
                if (filter.getWaitingForClient() != null) {
                    predicates.add(cb.equal(root.get("waitingForClient"), filter.getWaitingForClient()));
                }
                if (Boolean.TRUE.equals(filter.getFollowUpDue())) {
                    predicates.add(cb.isNotNull(root.get("followUpDate")));
                    predicates.add(cb.lessThanOrEqualTo(root.get("followUpDate"), LocalDate.now()));
                    predicates.add(root.get("status").in(CLOSED_STATUSES).not());
                }
                if (filter.getAssignedEmployeeId() != null) {
                    predicates.add(cb.equal(root.get("assignedEmployeeId"), filter.getAssignedEmployeeId()));
                }
                if (filter.getReviewerEmployeeId() != null) {
                    predicates.add(cb.equal(root.get("reviewerEmployeeId"), filter.getReviewerEmployeeId()));
                }
                if (filter.getPartnerEmployeeId() != null) {
                    predicates.add(cb.equal(root.get("partnerEmployeeId"), filter.getPartnerEmployeeId()));
                }
                if (filter.getDueDateFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("responseDueDate"), filter.getDueDateFrom()));
                }
                if (filter.getDueDateTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("responseDueDate"), filter.getDueDateTo()));
                }
                if (Boolean.TRUE.equals(filter.getOverdueOnly())) {
                    predicates.add(cb.lessThan(root.get("responseDueDate"), LocalDate.now()));
                    predicates.add(root.get("status").in(CLOSED_STATUSES).not());
                }
                if (Boolean.TRUE.equals(filter.getUpcomingHearing())) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("hearingDate"), LocalDate.now()));
                    predicates.add(root.get("status").in(CLOSED_STATUSES).not());
                }
                if (StringUtils.hasText(filter.getSearch())) {
                    String pattern = "%" + filter.getSearch().trim().toLowerCase() + "%";
                    Predicate noticeNum = cb.like(cb.lower(root.get("noticeNumber")), pattern);
                    Predicate din = cb.like(cb.lower(root.get("dinNumber")), pattern);
                    Predicate sub = cb.like(cb.lower(root.get("subject")), pattern);
                    Predicate sec = cb.like(cb.lower(root.get("section")), pattern);
                    Predicate nType = cb.like(cb.lower(root.get("noticeType")), pattern);
                    predicates.add(cb.or(noticeNum, din, sub, sec, nType));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private NoticePriority computePriority(LocalDate responseDueDate) {
        if (responseDueDate == null) return NoticePriority.MEDIUM;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), responseDueDate);
        if (days <= 3) return NoticePriority.CRITICAL;
        if (days <= 7) return NoticePriority.HIGH;
        if (days <= 15) return NoticePriority.MEDIUM;
        return NoticePriority.LOW;
    }

    private TaxNoticeDto enrichNoticeDto(TaxNoticeEntity entity) {
        TaxNoticeDto dto = noticeMapper.toDto(entity);
        UUID orgId = entity.getOrganizationId();

        // 1. Client Details
        if (entity.getClientId() != null) {
            clientRepository.findByIdAndOrganizationId(entity.getClientId(), orgId).ifPresent(c -> {
                dto.setClientName(c.getDisplayName());
                dto.setClientPan(c.getPan());
                dto.setClientGstin(c.getGstin());
                dto.setClientEmail(c.getEmail());
                dto.setClientPhone(c.getPhone());
            });
        }

        // 2. Employee Names
        if (entity.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(entity.getAssignedEmployeeId(), orgId).ifPresent(e ->
                    dto.setAssignedEmployeeName(e.getFirstName() + (StringUtils.hasText(e.getLastName()) ? " " + e.getLastName() : "")));
        }
        if (entity.getReviewerEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(entity.getReviewerEmployeeId(), orgId).ifPresent(e ->
                    dto.setReviewerEmployeeName(e.getFirstName() + (StringUtils.hasText(e.getLastName()) ? " " + e.getLastName() : "")));
        }
        if (entity.getPartnerEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(entity.getPartnerEmployeeId(), orgId).ifPresent(e ->
                    dto.setPartnerEmployeeName(e.getFirstName() + (StringUtils.hasText(e.getLastName()) ? " " + e.getLastName() : "")));
        }
        if (entity.getResponseSubmittedBy() != null) {
            userRepository.findById(entity.getResponseSubmittedBy()).ifPresent(u ->
                    dto.setResponseSubmittedByName(u.getFullName() != null ? u.getFullName() : u.getEmail()));
        }
        if (entity.getWaitingRequestedBy() != null) {
            userRepository.findById(entity.getWaitingRequestedBy()).ifPresent(u ->
                    dto.setWaitingRequestedByName(u.getFullName() != null ? u.getFullName() : u.getEmail()));
        }

        // 3. Deadline / Countdown
        if (entity.getResponseDueDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), entity.getResponseDueDate());
            dto.setDaysRemaining(days);
            dto.setIsOverdue(days < 0 && !CLOSED_STATUSES.contains(entity.getStatus()));
        }

        // 4. Related entities count
        dto.setResponsesCount((int) responseRepository.countByOrganizationIdAndNoticeId(orgId, entity.getId()));
        dto.setHearingsCount((int) hearingRepository.countByOrganizationIdAndNoticeId(orgId, entity.getId()));
        dto.setOpenTasksCount((int) taskRepository.countByOrganizationIdAndNoticeIdAndStatusNot(orgId, entity.getId(), TaskStatus.COMPLETED));
        dto.setDocumentsCount((int) documentRepository.countByOrganizationIdAndNoticeIdAndStatus(orgId, entity.getId(), DocumentStatus.ACTIVE));
        dto.setPendingRequestsCount((int) documentRequestRepository.countByOrganizationIdAndNoticeIdAndStatusIn(orgId, entity.getId(), List.of(RequestStatus.SENT, RequestStatus.PARTIALLY_COMPLETED, RequestStatus.OVERDUE)));

        return dto;
    }

    private NoticeResponseDto enrichResponseDto(NoticeResponseEntity entity) {
        NoticeResponseDto dto = noticeMapper.toResponseDto(entity);
        if (entity.getPreparedByUserId() != null) {
            userRepository.findById(entity.getPreparedByUserId()).ifPresent(u ->
                    dto.setPreparedByUserName(u.getFullName() != null ? u.getFullName() : u.getEmail()));
        }
        if (entity.getReviewedByUserId() != null) {
            userRepository.findById(entity.getReviewedByUserId()).ifPresent(u ->
                    dto.setReviewedByUserName(u.getFullName() != null ? u.getFullName() : u.getEmail()));
        }
        if (entity.getApprovedByUserId() != null) {
            userRepository.findById(entity.getApprovedByUserId()).ifPresent(u ->
                    dto.setApprovedByUserName(u.getFullName() != null ? u.getFullName() : u.getEmail()));
        }
        return dto;
    }

    private NoticeHearingDto enrichHearingDto(NoticeHearingEntity entity) {
        NoticeHearingDto dto = noticeMapper.toHearingDto(entity);
        UUID orgId = entity.getOrganizationId();
        if (entity.getDesignatedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(entity.getDesignatedEmployeeId(), orgId).ifPresent(e ->
                    dto.setDesignatedEmployeeName(e.getFirstName() + (StringUtils.hasText(e.getLastName()) ? " " + e.getLastName() : "")));
        }
        if (entity.getDesignatedPartnerId() != null) {
            employeeRepository.findByIdAndOrganizationId(entity.getDesignatedPartnerId(), orgId).ifPresent(e ->
                    dto.setDesignatedPartnerName(e.getFirstName() + (StringUtils.hasText(e.getLastName()) ? " " + e.getLastName() : "")));
        }
        return dto;
    }

    private void recordActivity(UUID noticeId, NoticeActivityType type, UUID userId, String desc, String from, String to, String meta) {
        String performerName = "System";
        if (userId != null) {
            Optional<UserEntity> user = userRepository.findById(userId);
            if (user.isPresent()) {
                performerName = user.get().getFullName() != null ? user.get().getFullName() : user.get().getEmail();
            }
        }

        NoticeActivityEntity act = NoticeActivityEntity.builder()
                .noticeId(noticeId)
                .activityType(type)
                .performedByUserId(userId)
                .performerName(performerName)
                .description(desc)
                .fromState(from)
                .toState(to)
                .metadata(meta)
                .build();
        activityRepository.save(act);
    }

    private void createNoticePreparationTask(TaxNoticeEntity notice, ClientEntity client) {
        UUID assignedUserId = null;
        if (notice.getAssignedEmployeeId() != null) {
            assignedUserId = employeeRepository.findById(notice.getAssignedEmployeeId())
                    .map(EmployeeEntity::getUserId)
                    .orElse(null);
        }

        LocalDate taskDueDate = notice.getResponseDueDate() != null
                ? (notice.getResponseDueDate().isAfter(LocalDate.now().plusDays(2)) ? notice.getResponseDueDate().minusDays(2) : notice.getResponseDueDate())
                : LocalDate.now().plusDays(3);

        TaskEntity task = TaskEntity.builder()
                .clientId(notice.getClientId())
                .noticeId(notice.getId())
                .assignedTo(assignedUserId)
                .title("Notice Response Prep: [" + notice.getNoticeNumber() + "] " + notice.getSubject())
                .description("Review notice requirements, collect documents from " + client.getDisplayName() + ", and draft response reply under section " + (notice.getSection() != null ? notice.getSection() : "applicable tax provisions") + ".")
                .taskCategory(TaskCategory.NOTICE)
                .status(TaskStatus.TODO)
                .priority(mapNoticePriorityToTaskPriority(notice.getPriority()))
                .dueDate(taskDueDate)
                .build();

        taskRepository.save(task);
    }

    private TaskPriority mapNoticePriorityToTaskPriority(NoticePriority p) {
        if (p == null) return TaskPriority.MEDIUM;
        return switch (p) {
            case CRITICAL -> TaskPriority.URGENT;
            case HIGH -> TaskPriority.HIGH;
            case MEDIUM -> TaskPriority.MEDIUM;
            case LOW -> TaskPriority.LOW;
        };
    }

    private void dispatchNoticeAssignmentNotifications(TaxNoticeEntity notice, ClientEntity client, String title) {
        try {
            if (notice.getAssignedEmployeeId() != null) {
                employeeRepository.findById(notice.getAssignedEmployeeId()).ifPresent(emp -> {
                    if (emp.getUserId() != null) {
                        notificationService.notify(
                                notice.getOrganizationId(),
                                emp.getUserId(),
                                null,
                                NotificationType.TASK_ASSIGNED,
                                Severity.ACTION_REQUIRED,
                                Category.TASK,
                                "TAX_NOTICE",
                                notice.getId().toString(),
                                title,
                                "You have been assigned to handle tax notice " + notice.getNoticeNumber() + " for " + client.getDisplayName() + ". Response due date is " + notice.getResponseDueDate(),
                                Set.of(NotificationChannel.IN_APP),
                                "/notices/" + notice.getId(),
                                null,
                                null
                        );
                    }
                });
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch notice assignment notification: {}", ex.getMessage());
        }
    }

    private void dispatchNoticeSubmissionNotifications(TaxNoticeEntity notice) {
        try {
            if (notice.getAssignedEmployeeId() != null) {
                employeeRepository.findById(notice.getAssignedEmployeeId()).ifPresent(emp -> {
                    if (emp.getUserId() != null) {
                        notificationService.notify(
                                notice.getOrganizationId(),
                                emp.getUserId(),
                                null,
                                NotificationType.COMPLIANCE_COMPLETED,
                                Severity.INFO,
                                Category.COMPLIANCE,
                                "TAX_NOTICE",
                                notice.getId().toString(),
                                "Notice Response Submitted: " + notice.getNoticeNumber(),
                                "Response for tax notice " + notice.getNoticeNumber() + " has been successfully submitted.",
                                Set.of(NotificationChannel.IN_APP),
                                "/notices/" + notice.getId(),
                                null,
                                null
                        );
                    }
                });
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch notice submission notification: {}", ex.getMessage());
        }
    }

    private void dispatchNoticeHearingNotifications(NoticeHearingEntity hearing, TaxNoticeEntity notice) {
        try {
            UUID targetEmployeeId = hearing.getDesignatedEmployeeId() != null ? hearing.getDesignatedEmployeeId() : notice.getAssignedEmployeeId();
            if (targetEmployeeId != null) {
                employeeRepository.findById(targetEmployeeId).ifPresent(emp -> {
                    if (emp.getUserId() != null) {
                        notificationService.notify(
                                notice.getOrganizationId(),
                                emp.getUserId(),
                                null,
                                NotificationType.TASK_ASSIGNED,
                                Severity.ACTION_REQUIRED,
                                Category.TASK,
                                "TAX_NOTICE",
                                notice.getId().toString(),
                                "Hearing Scheduled/Updated: " + notice.getNoticeNumber(),
                                "Hearing for notice " + notice.getNoticeNumber() + " is scheduled for " + hearing.getHearingDate(),
                                Set.of(NotificationChannel.IN_APP),
                                "/notices/" + notice.getId(),
                                null,
                                null
                        );
                    }
                });
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch notice hearing notification: {}", ex.getMessage());
        }
    }

    private void validateClientAccess(UUID clientId) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
        if (accessibleClientIds != null && !accessibleClientIds.contains(clientId)) {
            throw new ForbiddenException("You do not have access to notices for this client");
        }
    }
}
