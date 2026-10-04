package com.taxoryn.module.dashboard.service;

import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.InvoiceItemEntity;
import com.taxoryn.module.billing.entity.InvoicePaymentEntity;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.dashboard.dto.BillingDashboardSummaryDto;
import com.taxoryn.module.dashboard.dto.BillingStatsDto;
import com.taxoryn.module.dashboard.dto.ClientStatsDto;
import com.taxoryn.module.dashboard.dto.ClientSummaryDashboardDto;
import com.taxoryn.module.dashboard.dto.ComplianceDashboardDto;
import com.taxoryn.module.dashboard.dto.DashboardFilterRequest;
import com.taxoryn.module.dashboard.dto.DocumentDashboardDto;
import com.taxoryn.module.dashboard.dto.EmployeeStatsDto;
import com.taxoryn.module.dashboard.dto.EmployeeWorkloadItemDto;
import com.taxoryn.module.dashboard.dto.GstStatsDto;
import com.taxoryn.module.dashboard.dto.ItrStatsDto;
import com.taxoryn.module.dashboard.dto.NoticeDashboardDto;
import com.taxoryn.module.dashboard.dto.OrganizationDashboardDto;
import com.taxoryn.module.dashboard.dto.PracticeDashboardOverviewDto;
import com.taxoryn.module.dashboard.dto.TaskStatsDto;
import com.taxoryn.module.dashboard.dto.TdsStatsDto;
import com.taxoryn.module.dashboard.dto.WorkDashboardDto;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.notice.entity.TaxNoticeEntity;
import com.taxoryn.module.notice.enums.NoticeStatus;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.entity.WorkItemEntity;
import com.taxoryn.module.task.model.WorkItemStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.repository.WorkItemRepository;
import com.taxoryn.module.tds.entity.TdsReturnEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.dashboard.dto.RecentActivityDto;
import com.taxoryn.module.dashboard.dto.ReminderSummaryDto;
import com.taxoryn.module.dsc.dto.DscSummaryDto;
import com.taxoryn.module.dsc.service.DscService;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import com.taxoryn.module.reminder.repository.ReminderRepository;
import com.taxoryn.module.udin.dto.UdinSummaryDto;
import com.taxoryn.module.udin.service.UdinService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final WorkItemRepository workItemRepository;
    private final EngagementRepository engagementRepository;
    private final ComplianceObligationRepository complianceObligationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final TaxNoticeRepository taxNoticeRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final GstProfileRepository gstProfileRepository;
    private final GstReturnFilingRepository gstReturnFilingRepository;
    private final ItrProfileRepository itrProfileRepository;
    private final ItrReturnRepository itrReturnRepository;
    private final TdsProfileRepository tdsProfileRepository;
    private final TdsReturnRepository tdsReturnRepository;
    private final LocationRepository locationRepository;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final DscService dscService;
    private final UdinService udinService;
    private final ReminderRepository reminderRepository;
    private final AuditLogRepository auditLogRepository;

    private static final Set<NoticeStatus> CLOSED_NOTICE_STATUSES = Set.of(
            NoticeStatus.CLOSED,
            NoticeStatus.RESOLVED,
            NoticeStatus.DEMAND_DROPPED,
            NoticeStatus.APPEAL_FILED
    );

    // =========================================================================
    // 1. Practice Operations Overview
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PracticeDashboardOverviewDto getOverview(DashboardFilterRequest filter) {
        DashboardFilterRequest safeFilter = filter != null ? filter : new DashboardFilterRequest();
        ScopingContext ctx = resolveScoping(safeFilter);
        if (ctx.hasZeroAccess()) {
            return PracticeDashboardOverviewDto.builder().build();
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = safeFilter.resolveStartDate();
        LocalDate endDate = safeFilter.resolveEndDate();

        // 1. Clients
        long totalClients = clientRepository.count(createClientSpec(ctx, safeFilter));
        long activeClients = clientRepository.count(createActiveClientSpec(ctx, safeFilter));

        // 2. Active Engagements
        long activeEngagements = engagementRepository.count(createEngagementSpec(ctx, safeFilter, EngagementStatus.ACTIVE));

        // 3. Compliance Obligations
        long openCompliance = complianceObligationRepository.count(createOpenComplianceSpec(ctx, safeFilter));
        long overdueCompliance = complianceObligationRepository.count(createOverdueComplianceSpec(ctx, safeFilter, today));

        // 4. Work Items
        long openWorkItems = workItemRepository.count(createOpenWorkItemSpec(ctx, safeFilter));
        long overdueWorkItems = workItemRepository.count(createOverdueWorkItemSpec(ctx, safeFilter, today));

        // 5. Tasks
        long pendingTasks = taskRepository.count(createPendingTaskSpec(ctx, safeFilter));
        long overdueTasks = taskRepository.count(createOverdueTaskSpec(ctx, safeFilter, today));
        long completedTasks = taskRepository.count(createCompletedTaskSpec(ctx, safeFilter));

        // 6. Document Requests
        long pendingDocRequests = documentRequestRepository.count(createPendingDocRequestSpec(ctx, safeFilter));

        // 7. Tax Notices
        long openNotices = taxNoticeRepository.count(createOpenNoticeSpec(ctx, safeFilter));

        // 8. Billing Metrics (Restricted to users with billing access)
        BigDecimal outstandingBilling = BigDecimal.ZERO;
        BigDecimal periodInvoiced = BigDecimal.ZERO;
        BigDecimal periodCollected = BigDecimal.ZERO;

        if (ctx.hasBillingAccess()) {
            List<InvoiceEntity> invoices = invoiceRepository.findAll(createInvoiceSpec(ctx, safeFilter));
            for (InvoiceEntity inv : invoices) {
                if (inv.getStatus() != InvoiceStatus.CANCELLED) {
                    outstandingBilling = outstandingBilling.add(inv.getBalanceDue() != null ? inv.getBalanceDue() : BigDecimal.ZERO);
                    if (startDate == null && endDate == null) {
                        periodInvoiced = periodInvoiced.add(inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO);
                        periodCollected = periodCollected.add(inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO);
                    } else {
                        if (inv.getInvoiceDate() != null
                                && (startDate == null || !inv.getInvoiceDate().isBefore(startDate))
                                && (endDate == null || !inv.getInvoiceDate().isAfter(endDate))) {
                            periodInvoiced = periodInvoiced.add(inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO);
                        }
                    }
                }
            }

            if (startDate != null || endDate != null) {
                List<InvoicePaymentEntity> payments = invoicePaymentRepository.findAll(createPaymentSpec(ctx, safeFilter, startDate, endDate));
                for (InvoicePaymentEntity pmt : payments) {
                    periodCollected = periodCollected.add(pmt.getAmount() != null ? pmt.getAmount() : BigDecimal.ZERO);
                }
            }
        }

        // 9. DSC Health
        DscSummaryDto dscSummary = null;
        try {
            dscSummary = dscService.getDscSummary();
        } catch (Exception e) {
            log.warn("Could not retrieve DSC summary for dashboard: {}", e.getMessage());
        }

        // 10. UDIN Status
        UdinSummaryDto udinSummary = null;
        try {
            udinSummary = udinService.getUdinSummary();
        } catch (Exception e) {
            log.warn("Could not retrieve UDIN summary for dashboard: {}", e.getMessage());
        }

        // 11. Reminders
        ReminderSummaryDto reminderSummary = null;
        try {
            Instant now = Instant.now();
            Instant sevenDaysAhead = now.plus(7, java.time.temporal.ChronoUnit.DAYS);
            long pendingReminders = reminderRepository.countByOrganizationIdAndStatus(ctx.organizationId(), ReminderStatus.PENDING);
            long overdueReminders = reminderRepository.countByOrganizationIdAndStatusAndScheduledAtBefore(ctx.organizationId(), ReminderStatus.PENDING, now);
            long upcomingReminders = reminderRepository.countByOrganizationIdAndStatusAndScheduledAtBetween(ctx.organizationId(), ReminderStatus.PENDING, now, sevenDaysAhead);
            long triggeredReminders = reminderRepository.countByOrganizationIdAndStatus(ctx.organizationId(), ReminderStatus.TRIGGERED);
            reminderSummary = ReminderSummaryDto.builder()
                    .pending(pendingReminders)
                    .overdue(overdueReminders)
                    .upcoming(upcomingReminders)
                    .triggered(triggeredReminders)
                    .build();
        } catch (Exception e) {
            log.warn("Could not retrieve reminder summary for dashboard: {}", e.getMessage());
        }

        // 12. Sub-dashboards
        ComplianceDashboardDto compliance = getComplianceDashboard(safeFilter);
        WorkDashboardDto work = getWorkDashboard(safeFilter);
        BillingDashboardSummaryDto billing = ctx.hasBillingAccess() ? getBillingDashboard(safeFilter) : null;
        List<EmployeeWorkloadItemDto> workload = getEmployeeWorkload(ctx.organizationId(), today, ctx.scope());

        // 13. Recent Activities (from AuditLog)
        List<RecentActivityDto> recentActivities = new ArrayList<>();
        try {
            List<AuditLogEntity> logEntities = auditLogRepository
                    .findAllByOrganizationId(ctx.organizationId(), org.springframework.data.domain.PageRequest.of(0, 8, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                    .getContent();
            for (AuditLogEntity logItem : logEntities) {
                String desc = logItem.getAction() + (logItem.getEntityName() != null ? " on " + logItem.getEntityName() : (logItem.getEntityType() != null ? " on " + logItem.getEntityType() : ""));
                recentActivities.add(RecentActivityDto.builder()
                        .id(logItem.getId())
                        .action(logItem.getAction())
                        .entityType(logItem.getEntityType())
                        .entityName(logItem.getEntityName())
                        .entityId(logItem.getEntityId())
                        .description(desc)
                        .userId(logItem.getUserId())
                        .userName(null)
                        .createdAt(logItem.getCreatedAt())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Could not retrieve audit log activity for dashboard: {}", e.getMessage());
        }

        return PracticeDashboardOverviewDto.builder()
                .totalClients(totalClients)
                .activeClients(activeClients)
                .activeEngagements(activeEngagements)
                .openComplianceObligations(openCompliance)
                .overdueComplianceObligations(overdueCompliance)
                .openWorkItems(openWorkItems)
                .overdueWorkItems(overdueWorkItems)
                .pendingTasks(pendingTasks)
                .overdueTasks(overdueTasks)
                .completedTasks(completedTasks)
                .pendingDocumentRequests(pendingDocRequests)
                .openTaxNotices(openNotices)
                .outstandingBillingAmount(outstandingBilling)
                .periodInvoicedAmount(periodInvoiced)
                .periodCollectedAmount(periodCollected)
                .dsc(dscSummary)
                .udin(udinSummary)
                .reminders(reminderSummary)
                .compliance(compliance)
                .work(work)
                .billing(billing)
                .employeeWorkload(workload)
                .recentActivity(recentActivities)
                .generatedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // 2. Compliance Dashboard
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ComplianceDashboardDto getComplianceDashboard(DashboardFilterRequest filter) {
        DashboardFilterRequest safeFilter = filter != null ? filter : new DashboardFilterRequest();
        ScopingContext ctx = resolveScoping(safeFilter);
        if (ctx.hasZeroAccess()) {
            return ComplianceDashboardDto.builder().build();
        }

        LocalDate today = LocalDate.now();
        LocalDate deadlineWindow = today.plusDays(15);

        List<ComplianceObligationEntity> obligations = complianceObligationRepository.findAll(createComplianceSpec(ctx, safeFilter));

        long total = obligations.size();
        long pending = 0;
        long inProgress = 0;
        long completed = 0;
        long overdue = 0;
        long upcomingDeadlines = 0;

        ComplianceDashboardDto.CategoryComplianceSummary gstSummary = ComplianceDashboardDto.CategoryComplianceSummary.builder().category("GST").build();
        ComplianceDashboardDto.CategoryComplianceSummary itrSummary = ComplianceDashboardDto.CategoryComplianceSummary.builder().category("ITR").build();
        ComplianceDashboardDto.CategoryComplianceSummary tdsSummary = ComplianceDashboardDto.CategoryComplianceSummary.builder().category("TDS").build();
        ComplianceDashboardDto.CategoryComplianceSummary noticeSummary = ComplianceDashboardDto.CategoryComplianceSummary.builder().category("NOTICES").build();

        for (ComplianceObligationEntity ob : obligations) {
            boolean isCompleted = ob.getStatus() == ComplianceObligationStatus.COMPLETED;
            boolean isOverdue = ob.getStatus() == ComplianceObligationStatus.OVERDUE
                    || (!isCompleted && ob.getStatus() != ComplianceObligationStatus.CANCELLED && ob.getStatutoryDueDate() != null && ob.getStatutoryDueDate().isBefore(today));
            boolean isInProg = ob.getStatus() == ComplianceObligationStatus.IN_PROGRESS || ob.getStatus() == ComplianceObligationStatus.READY_FOR_FILING || ob.getStatus() == ComplianceObligationStatus.WAITING_FOR_CLIENT || ob.getStatus() == ComplianceObligationStatus.FILED;
            boolean isPend = ob.getStatus() == ComplianceObligationStatus.UPCOMING || ob.getStatus() == ComplianceObligationStatus.READY;

            if (isCompleted) {
                completed++;
            } else if (isOverdue) {
                overdue++;
            } else if (isInProg) {
                inProgress++;
            } else if (isPend) {
                pending++;
            }

            if (!isCompleted && ob.getStatutoryDueDate() != null && !ob.getStatutoryDueDate().isBefore(today) && !ob.getStatutoryDueDate().isAfter(deadlineWindow)) {
                upcomingDeadlines++;
            }

            // Category breakdown
            ComplianceDashboardDto.CategoryComplianceSummary targetCat = resolveCategorySummary(ob.getObligationType(), gstSummary, itrSummary, tdsSummary, noticeSummary);
            if (targetCat != null) {
                targetCat.setTotal(targetCat.getTotal() + 1);
                if (isCompleted) targetCat.setCompleted(targetCat.getCompleted() + 1);
                else if (isOverdue) targetCat.setOverdue(targetCat.getOverdue() + 1);
                else if (isInProg) targetCat.setInProgress(targetCat.getInProgress() + 1);
                else if (isPend) targetCat.setPending(targetCat.getPending() + 1);
            }
        }

        // Include Tax Notice module count into notices summary if not covered by obligations
        List<TaxNoticeEntity> notices = taxNoticeRepository.findAll(createNoticeSpec(ctx, safeFilter));
        for (TaxNoticeEntity n : notices) {
            noticeSummary.setTotal(noticeSummary.getTotal() + 1);
            if (CLOSED_NOTICE_STATUSES.contains(n.getStatus())) {
                noticeSummary.setCompleted(noticeSummary.getCompleted() + 1);
            } else if (n.getResponseDueDate() != null && n.getResponseDueDate().isBefore(today)) {
                noticeSummary.setOverdue(noticeSummary.getOverdue() + 1);
            } else {
                noticeSummary.setPending(noticeSummary.getPending() + 1);
            }
        }

        return ComplianceDashboardDto.builder()
                .totalObligations(total)
                .pending(pending)
                .inProgress(inProgress)
                .completed(completed)
                .overdue(overdue)
                .upcomingDeadlines(upcomingDeadlines)
                .gst(gstSummary)
                .itr(itrSummary)
                .tds(tdsSummary)
                .notices(noticeSummary)
                .generatedAt(Instant.now())
                .build();
    }

    private ComplianceDashboardDto.CategoryComplianceSummary resolveCategorySummary(
            ComplianceObligationType type,
            ComplianceDashboardDto.CategoryComplianceSummary gst,
            ComplianceDashboardDto.CategoryComplianceSummary itr,
            ComplianceDashboardDto.CategoryComplianceSummary tds,
            ComplianceDashboardDto.CategoryComplianceSummary notice
    ) {
        if (type == null) return null;
        String name = type.name().toUpperCase();
        if (name.contains("GST") || name.contains("GSTR")) return gst;
        if (name.contains("ITR") || name.contains("INCOME_TAX")) return itr;
        if (name.contains("TDS") || name.contains("TCS")) return tds;
        if (name.contains("NOTICE")) return notice;
        return null;
    }

    // =========================================================================
    // 3. Work Management Dashboard
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public WorkDashboardDto getWorkDashboard(DashboardFilterRequest filter) {
        DashboardFilterRequest safeFilter = filter != null ? filter : new DashboardFilterRequest();
        ScopingContext ctx = resolveScoping(safeFilter);
        if (ctx.hasZeroAccess()) {
            return WorkDashboardDto.builder().build();
        }

        LocalDate today = LocalDate.now();

        // 1. Work Items
        List<WorkItemEntity> workItems = workItemRepository.findAll(createWorkItemSpec(ctx, safeFilter));
        long todoWI = 0, inProgWI = 0, blockedWI = 0, completedWI = 0, cancelledWI = 0, overdueWI = 0;

        Map<UUID, Long> wiAssignedMap = new HashMap<>();
        Map<UUID, Long> wiOpenMap = new HashMap<>();

        for (WorkItemEntity wi : workItems) {
            switch (wi.getStatus()) {
                case TODO -> todoWI++;
                case IN_PROGRESS -> inProgWI++;
                case BLOCKED -> blockedWI++;
                case COMPLETED -> completedWI++;
                case CANCELLED -> cancelledWI++;
            }
            if ((wi.getStatus() == WorkItemStatus.TODO || wi.getStatus() == WorkItemStatus.IN_PROGRESS || wi.getStatus() == WorkItemStatus.BLOCKED)
                    && wi.getDueDate() != null && wi.getDueDate().isBefore(today)) {
                overdueWI++;
            }

            if (wi.getAssignedUserId() != null) {
                wiAssignedMap.merge(wi.getAssignedUserId(), 1L, Long::sum);
                if (wi.getStatus() != WorkItemStatus.COMPLETED && wi.getStatus() != WorkItemStatus.CANCELLED) {
                    wiOpenMap.merge(wi.getAssignedUserId(), 1L, Long::sum);
                }
            }
        }

        // 2. Tasks
        List<TaskEntity> tasks = taskRepository.findAll(createTaskSpec(ctx, safeFilter));
        long pendingTasks = 0, inProgTasks = 0, completedTasks = 0, overdueTasks = 0;

        Map<UUID, Long> taskAssignedMap = new HashMap<>();
        Map<UUID, Long> taskPendingMap = new HashMap<>();
        Map<UUID, Long> taskOverdueMap = new HashMap<>();

        for (TaskEntity t : tasks) {
            boolean isCompleted = t.getStatus() == TaskStatus.COMPLETED;
            boolean isOverdue = !isCompleted && t.getStatus() != TaskStatus.CANCELLED && t.getDueDate() != null && t.getDueDate().isBefore(today);

            if (isCompleted) {
                completedTasks++;
            } else if (t.getStatus() == TaskStatus.IN_PROGRESS) {
                inProgTasks++;
            } else if (t.getStatus() == TaskStatus.TODO || t.getStatus() == TaskStatus.UNDER_REVIEW) {
                pendingTasks++;
            }

            if (isOverdue) {
                overdueTasks++;
            }

            if (t.getAssignedUserId() != null) {
                taskAssignedMap.merge(t.getAssignedUserId(), 1L, Long::sum);
                if (!isCompleted && t.getStatus() != TaskStatus.CANCELLED) {
                    taskPendingMap.merge(t.getAssignedUserId(), 1L, Long::sum);
                }
                if (isOverdue) {
                    taskOverdueMap.merge(t.getAssignedUserId(), 1L, Long::sum);
                }
            }
        }

        // 3. User Workload Distribution
        List<EmployeeEntity> employees = resolveAccessibleEmployees(ctx);
        List<WorkDashboardDto.UserWorkloadSummaryDto> workloads = new ArrayList<>();

        for (EmployeeEntity emp : employees) {
            UUID userId = emp.getUserId() != null ? emp.getUserId() : emp.getId();
            long assignedWi = wiAssignedMap.getOrDefault(userId, 0L) + (!userId.equals(emp.getId()) ? wiAssignedMap.getOrDefault(emp.getId(), 0L) : 0L);
            long openWi = wiOpenMap.getOrDefault(userId, 0L) + (!userId.equals(emp.getId()) ? wiOpenMap.getOrDefault(emp.getId(), 0L) : 0L);
            long assignedT = taskAssignedMap.getOrDefault(userId, 0L) + (!userId.equals(emp.getId()) ? taskAssignedMap.getOrDefault(emp.getId(), 0L) : 0L);
            long pendT = taskPendingMap.getOrDefault(userId, 0L) + (!userId.equals(emp.getId()) ? taskPendingMap.getOrDefault(emp.getId(), 0L) : 0L);
            long overT = taskOverdueMap.getOrDefault(userId, 0L) + (!userId.equals(emp.getId()) ? taskOverdueMap.getOrDefault(emp.getId(), 0L) : 0L);

            workloads.add(WorkDashboardDto.UserWorkloadSummaryDto.builder()
                    .userId(userId)
                    .userName(emp.getFullName())
                    .email(emp.getEmail())
                    .department(emp.getDepartment())
                    .designation(emp.getDesignation())
                    .assignedWorkItems(assignedWi)
                    .openWorkItems(openWi)
                    .assignedTasks(assignedT)
                    .pendingTasks(pendT)
                    .overdueTasks(overT)
                    .build());
        }

        return WorkDashboardDto.builder()
                .totalWorkItems(workItems.size())
                .workItemsTodo(todoWI)
                .workItemsInProgress(inProgWI)
                .workItemsBlocked(blockedWI)
                .workItemsCompleted(completedWI)
                .workItemsCancelled(cancelledWI)
                .overdueWorkItems(overdueWI)
                .totalTasks(tasks.size())
                .pendingTasks(pendingTasks)
                .inProgressTasks(inProgTasks)
                .completedTasks(completedTasks)
                .overdueTasks(overdueTasks)
                .userWorkloads(workloads)
                .generatedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // 4. Document & Request Dashboard
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public DocumentDashboardDto getDocumentDashboard(DashboardFilterRequest filter) {
        DashboardFilterRequest safeFilter = filter != null ? filter : new DashboardFilterRequest();
        ScopingContext ctx = resolveScoping(safeFilter);
        if (ctx.hasZeroAccess()) {
            return DocumentDashboardDto.builder().build();
        }

        LocalDate today = LocalDate.now();

        // 1. Documents
        List<DocumentEntity> docs = documentRepository.findAll(createDocumentSpec(ctx, safeFilter));
        long totalDocs = docs.size();
        long activeWorkflowDocs = docs.stream().filter(d -> d.getWorkflowId() != null || d.getTaskId() != null || d.getNoticeId() != null).count();

        // 2. Document Requests
        List<DocumentRequestEntity> requests = documentRequestRepository.findAll(createDocRequestSpec(ctx, safeFilter));
        long totalReqs = requests.size();
        long pendingReqs = requests.stream().filter(r -> r.getStatus() != RequestStatus.COMPLETED && r.getStatus() != RequestStatus.CANCELLED && r.getStatus() != RequestStatus.DECLINED).count();
        long fulfilledReqs = requests.stream().filter(r -> r.getStatus() == RequestStatus.COMPLETED).count();
        long overdueReqs = requests.stream().filter(r -> r.getStatus() != RequestStatus.COMPLETED && r.getStatus() != RequestStatus.CANCELLED && r.getStatus() != RequestStatus.DECLINED
                && ((r.getStatus() == RequestStatus.OVERDUE) || (r.getDueDate() != null && r.getDueDate().isBefore(today)))).count();

        return DocumentDashboardDto.builder()
                .totalDocuments(totalDocs)
                .activeWorkflowDocuments(activeWorkflowDocs)
                .totalRequests(totalReqs)
                .pendingRequests(pendingReqs)
                .fulfilledRequests(fulfilledReqs)
                .overdueRequests(overdueReqs)
                .generatedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // 5. Notice Dashboard
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public NoticeDashboardDto getNoticeDashboard(DashboardFilterRequest filter) {
        DashboardFilterRequest safeFilter = filter != null ? filter : new DashboardFilterRequest();
        ScopingContext ctx = resolveScoping(safeFilter);
        if (ctx.hasZeroAccess()) {
            return NoticeDashboardDto.builder().build();
        }

        LocalDate today = LocalDate.now();
        LocalDate hearingWindow = today.plusDays(30);

        List<TaxNoticeEntity> notices = taxNoticeRepository.findAll(createNoticeSpec(ctx, safeFilter));

        long totalOpen = 0;
        long requiringResponse = 0;
        long awaitingResponse = 0;
        long requiringHearing = 0;
        long upcomingHearings = 0;
        long overdueResponseDeadlines = 0;
        BigDecimal totalDemand = BigDecimal.ZERO;

        for (TaxNoticeEntity n : notices) {
            boolean isClosed = CLOSED_NOTICE_STATUSES.contains(n.getStatus());
            if (!isClosed) {
                totalOpen++;
                if (n.getDemandAmount() != null) {
                    totalDemand = totalDemand.add(n.getDemandAmount());
                }

                if (Boolean.TRUE.equals(n.getResponseRequired()) && n.getStatus() != NoticeStatus.SUBMITTED && n.getStatus() != NoticeStatus.AWAITING_ORDER) {
                    requiringResponse++;
                }

                if (n.getStatus() == NoticeStatus.SUBMITTED || n.getStatus() == NoticeStatus.AWAITING_ORDER) {
                    awaitingResponse++;
                }

                if (Boolean.TRUE.equals(n.getHearingRequired()) || n.getHearingDate() != null || n.getStatus() == NoticeStatus.HEARING || n.getStatus() == NoticeStatus.HEARING_SCHEDULED) {
                    requiringHearing++;
                }

                if (n.getHearingDate() != null && !n.getHearingDate().isBefore(today) && !n.getHearingDate().isAfter(hearingWindow)) {
                    upcomingHearings++;
                }

                if (n.getResponseDueDate() != null && n.getResponseDueDate().isBefore(today) && n.getStatus() != NoticeStatus.SUBMITTED && n.getStatus() != NoticeStatus.AWAITING_ORDER) {
                    overdueResponseDeadlines++;
                }
            }
        }

        return NoticeDashboardDto.builder()
                .totalOpenNotices(totalOpen)
                .noticesRequiringResponse(requiringResponse)
                .noticesAwaitingResponse(awaitingResponse)
                .noticesRequiringHearing(requiringHearing)
                .upcomingHearings(upcomingHearings)
                .overdueResponseDeadlines(overdueResponseDeadlines)
                .totalDemandAmount(totalDemand)
                .generatedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // 6. Billing Dashboard Summary
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public BillingDashboardSummaryDto getBillingDashboard(DashboardFilterRequest filter) {
        DashboardFilterRequest safeFilter = filter != null ? filter : new DashboardFilterRequest();
        ScopingContext ctx = resolveScoping(safeFilter);

        if (ctx.hasZeroAccess() || !ctx.hasBillingAccess()) {
            return BillingDashboardSummaryDto.builder().build();
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = safeFilter.resolveStartDate();
        LocalDate endDate = safeFilter.resolveEndDate();

        List<InvoiceEntity> invoices = invoiceRepository.findAll(createInvoiceSpec(ctx, safeFilter));

        long total = invoices.size();
        long draft = 0, issued = 0, partiallyPaid = 0, paid = 0, overdue = 0, cancelled = 0;
        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalOverdue = BigDecimal.ZERO;
        BigDecimal periodInvoiced = BigDecimal.ZERO;

        Map<String, BigDecimal> revenueByService = new HashMap<>();

        for (InvoiceEntity inv : invoices) {
            if (inv.getStatus() == InvoiceStatus.DRAFT) {
                draft++;
            } else if (inv.getStatus() == InvoiceStatus.CANCELLED) {
                cancelled++;
            } else {
                totalInvoiced = totalInvoiced.add(inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO);
                totalCollected = totalCollected.add(inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO);
                totalOutstanding = totalOutstanding.add(inv.getBalanceDue() != null ? inv.getBalanceDue() : BigDecimal.ZERO);

                boolean isOverdue = inv.getBalanceDue() != null && inv.getBalanceDue().compareTo(BigDecimal.ZERO) > 0
                        && inv.getDueDate() != null && inv.getDueDate().isBefore(today);

                if (isOverdue) {
                    overdue++;
                    totalOverdue = totalOverdue.add(inv.getBalanceDue());
                }

                if (inv.getStatus() == InvoiceStatus.ISSUED) issued++;
                else if (inv.getStatus() == InvoiceStatus.PARTIALLY_PAID) partiallyPaid++;
                else if (inv.getStatus() == InvoiceStatus.PAID) paid++;

                if (startDate == null && endDate == null) {
                    periodInvoiced = periodInvoiced.add(inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO);
                } else if (inv.getInvoiceDate() != null
                        && (startDate == null || !inv.getInvoiceDate().isBefore(startDate))
                        && (endDate == null || !inv.getInvoiceDate().isAfter(endDate))) {
                    periodInvoiced = periodInvoiced.add(inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO);
                }

                if (inv.getItems() != null) {
                    for (InvoiceItemEntity item : inv.getItems()) {
                        String svc = item.getService() != null ? item.getService().name() : "OTHER";
                        revenueByService.merge(svc, item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO, BigDecimal::add);
                    }
                }
            }
        }

        BigDecimal periodCollected = BigDecimal.ZERO;
        List<InvoicePaymentEntity> payments = invoicePaymentRepository.findAll(createPaymentSpec(ctx, safeFilter, startDate, endDate));
        for (InvoicePaymentEntity pmt : payments) {
            periodCollected = periodCollected.add(pmt.getAmount() != null ? pmt.getAmount() : BigDecimal.ZERO);
        }

        return BillingDashboardSummaryDto.builder()
                .totalInvoices(total)
                .draftInvoices(draft)
                .issuedInvoices(issued)
                .partiallyPaidInvoices(partiallyPaid)
                .paidInvoices(paid)
                .overdueInvoices(overdue)
                .cancelledInvoices(cancelled)
                .totalInvoicedAmount(totalInvoiced)
                .totalCollectedAmount(totalCollected)
                .totalOutstandingAmount(totalOutstanding)
                .totalOverdueAmount(totalOverdue)
                .periodInvoicedAmount(periodInvoiced)
                .periodCollectedAmount(periodCollected)
                .revenueByService(revenueByService)
                .generatedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // 7. Client Summary & Operational Attention Dashboard
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ClientSummaryDashboardDto getClientDashboard(DashboardFilterRequest filter) {
        DashboardFilterRequest safeFilter = filter != null ? filter : new DashboardFilterRequest();
        ScopingContext ctx = resolveScoping(safeFilter);
        if (ctx.hasZeroAccess()) {
            return ClientSummaryDashboardDto.builder().build();
        }

        LocalDate today = LocalDate.now();

        List<ClientEntity> clients = clientRepository.findAll(createClientSpec(ctx, safeFilter));
        long totalClients = clients.size();
        long activeClients = clients.stream().filter(c -> c.getStatus() == ClientStatus.ACTIVE).count();
        long inactiveClients = totalClients - activeClients;

        // Fetch scoped domain entities to build cross-client operational metrics
        List<ComplianceObligationEntity> obligations = complianceObligationRepository.findAll(createComplianceSpec(ctx, safeFilter));
        List<WorkItemEntity> workItems = workItemRepository.findAll(createWorkItemSpec(ctx, safeFilter));
        List<DocumentRequestEntity> docRequests = documentRequestRepository.findAll(createDocRequestSpec(ctx, safeFilter));
        List<TaxNoticeEntity> notices = taxNoticeRepository.findAll(createNoticeSpec(ctx, safeFilter));
        List<InvoiceEntity> invoices = ctx.hasBillingAccess() ? invoiceRepository.findAll(createInvoiceSpec(ctx, safeFilter)) : List.of();

        Map<UUID, Long> clientPendingCompliance = new HashMap<>();
        for (ComplianceObligationEntity o : obligations) {
            if (o.getClientId() != null && o.getStatus() != ComplianceObligationStatus.COMPLETED && o.getStatus() != ComplianceObligationStatus.CANCELLED) {
                clientPendingCompliance.merge(o.getClientId(), 1L, Long::sum);
            }
        }

        Map<UUID, Long> clientOverdueWork = new HashMap<>();
        for (WorkItemEntity w : workItems) {
            if (w.getClientId() != null && (w.getStatus() == WorkItemStatus.TODO || w.getStatus() == WorkItemStatus.IN_PROGRESS || w.getStatus() == WorkItemStatus.BLOCKED)
                    && w.getDueDate() != null && w.getDueDate().isBefore(today)) {
                clientOverdueWork.merge(w.getClientId(), 1L, Long::sum);
            }
        }

        Map<UUID, Long> clientPendingDocs = new HashMap<>();
        for (DocumentRequestEntity r : docRequests) {
            if (r.getClientId() != null && r.getStatus() != RequestStatus.COMPLETED && r.getStatus() != RequestStatus.CANCELLED && r.getStatus() != RequestStatus.DECLINED) {
                clientPendingDocs.merge(r.getClientId(), 1L, Long::sum);
            }
        }

        Map<UUID, Long> clientOpenNotices = new HashMap<>();
        for (TaxNoticeEntity n : notices) {
            if (n.getClientId() != null && !CLOSED_NOTICE_STATUSES.contains(n.getStatus())) {
                clientOpenNotices.merge(n.getClientId(), 1L, Long::sum);
            }
        }

        Map<UUID, BigDecimal> clientOutstandingBilling = new HashMap<>();
        for (InvoiceEntity inv : invoices) {
            if (inv.getClientId() != null && inv.getStatus() != InvoiceStatus.CANCELLED && inv.getStatus() != InvoiceStatus.DRAFT) {
                clientOutstandingBilling.merge(inv.getClientId(), inv.getBalanceDue() != null ? inv.getBalanceDue() : BigDecimal.ZERO, BigDecimal::add);
            }
        }

        long clientsWithPendingCompliance = clientPendingCompliance.size();
        long clientsWithOverdueWork = clientOverdueWork.size();
        long clientsWithPendingDocRequests = clientPendingDocs.size();
        long clientsWithOpenNotices = clientOpenNotices.size();
        long clientsWithOutstandingBilling = clientOutstandingBilling.values().stream().filter(amt -> amt.compareTo(BigDecimal.ZERO) > 0).count();

        List<ClientSummaryDashboardDto.ClientAttentionItemDto> attentionList = new ArrayList<>();
        for (ClientEntity c : clients) {
            long pendComp = clientPendingCompliance.getOrDefault(c.getId(), 0L);
            long overWork = clientOverdueWork.getOrDefault(c.getId(), 0L);
            long pendDocs = clientPendingDocs.getOrDefault(c.getId(), 0L);
            long openNot = clientOpenNotices.getOrDefault(c.getId(), 0L);
            BigDecimal balance = clientOutstandingBilling.getOrDefault(c.getId(), BigDecimal.ZERO);

            if (pendComp > 0 || overWork > 0 || pendDocs > 0 || openNot > 0 || balance.compareTo(BigDecimal.ZERO) > 0) {
                attentionList.add(ClientSummaryDashboardDto.ClientAttentionItemDto.builder()
                        .clientId(c.getId())
                        .clientName(c.getDisplayName())
                        .pan(c.getPan())
                        .gstin(c.getGstin())
                        .pendingComplianceCount(pendComp)
                        .overdueWorkCount(overWork)
                        .pendingDocRequestsCount(pendDocs)
                        .openNoticesCount(openNot)
                        .outstandingBalance(balance)
                        .build());
            }
        }

        // Sort attention list by urgency: open notices DESC, overdue work DESC, pending docs DESC
        attentionList.sort((a, b) -> {
            int cmp = Long.compare(b.getOpenNoticesCount(), a.getOpenNoticesCount());
            if (cmp != 0) return cmp;
            cmp = Long.compare(b.getOverdueWorkCount(), a.getOverdueWorkCount());
            if (cmp != 0) return cmp;
            cmp = Long.compare(b.getPendingDocRequestsCount(), a.getPendingDocRequestsCount());
            if (cmp != 0) return cmp;
            return b.getOutstandingBalance().compareTo(a.getOutstandingBalance());
        });

        return ClientSummaryDashboardDto.builder()
                .totalClients(totalClients)
                .activeClients(activeClients)
                .inactiveClients(inactiveClients)
                .clientsWithPendingCompliance(clientsWithPendingCompliance)
                .clientsWithOverdueWork(clientsWithOverdueWork)
                .clientsWithPendingDocRequests(clientsWithPendingDocRequests)
                .clientsWithOpenNotices(clientsWithOpenNotices)
                .clientsWithOutstandingBilling(clientsWithOutstandingBilling)
                .attentionList(attentionList)
                .generatedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // 8. Legacy Organization Dashboard (Backward Compatibility)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public OrganizationDashboardDto getOrganizationDashboard() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        ClientStatsDto clientStats = getClientStats(organizationId, scope);
        EmployeeStatsDto employeeStats = getEmployeeStats(organizationId, scope);
        TaskStatsDto taskStats = getTaskStats(organizationId, today, scope);
        GstStatsDto gstStats = getGstStats(organizationId, today, scope);
        ItrStatsDto itrStats = getItrStats(organizationId, today, scope);
        TdsStatsDto tdsStats = getTdsStats(organizationId, today, scope);
        BillingStatsDto billingStats = securityScopeEvaluator.hasBillingAccess(scope) ? getBillingStats(organizationId) : null;
        List<EmployeeWorkloadItemDto> employeeWorkload = getEmployeeWorkload(organizationId, today, scope);

        return OrganizationDashboardDto.builder()
                .clients(clientStats)
                .employees(employeeStats)
                .tasks(taskStats)
                .gst(gstStats)
                .itr(itrStats)
                .tds(tdsStats)
                .billing(billingStats)
                .employeeWorkload(employeeWorkload)
                .build();
    }

    // =========================================================================
    // Scoping & Dynamic Specification Helpers
    // =========================================================================

    private record ScopingContext(
            UUID organizationId,
            boolean isFirmAdmin,
            boolean isDepartmentManager,
            Set<UUID> accessibleLocationIds,
            Set<UUID> accessibleClientIds,
            Set<UUID> accessibleAssigneeIds,
            boolean hasBillingAccess,
            PracticeSecurityScope scope
    ) {
        public boolean hasZeroAccess() {
            return !isFirmAdmin && accessibleClientIds != null && accessibleClientIds.isEmpty();
        }
    }

    private ScopingContext resolveScoping(DashboardFilterRequest filter) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        Set<UUID> clientIds;
        if (filter.getClientId() != null) {
            UUID reqClientId = filter.getClientId();
            if (!scope.isFirmAdmin()) {
                Set<UUID> permitted = securityScopeEvaluator.getAccessibleClientIds(scope);
                if (permitted == null || !permitted.contains(reqClientId)) {
                    throw new ForbiddenException("Access denied: You do not have permission for client " + reqClientId);
                }
            } else {
                if (clientRepository.findByIdAndOrganizationId(reqClientId, organizationId).isEmpty()) {
                    throw new ResourceNotFoundException("Client", "id", reqClientId);
                }
            }
            clientIds = Set.of(reqClientId);
        } else {
            clientIds = scope.isFirmAdmin() ? null : securityScopeEvaluator.getAccessibleClientIds(scope);
        }

        Set<UUID> locationIds;
        if (filter.getLocationId() != null) {
            UUID reqLocId = filter.getLocationId();
            if (locationRepository.findByIdAndOrganizationId(reqLocId, organizationId).isEmpty()) {
                throw new ResourceNotFoundException("Location", "id", reqLocId);
            }
            if (!scope.isFirmAdmin()) {
                Set<UUID> permittedLocs = scope.getAccessibleLocationIds();
                if (permittedLocs == null || (!permittedLocs.isEmpty() && !permittedLocs.contains(reqLocId))) {
                    throw new ForbiddenException("Access denied: You do not have permission for location " + reqLocId);
                }
            }
            locationIds = Set.of(reqLocId);
        } else {
            locationIds = scope.isFirmAdmin() ? null : scope.getAccessibleLocationIds();
        }

        boolean hasBilling = securityScopeEvaluator.hasBillingAccess(scope);

        return new ScopingContext(
                organizationId,
                scope.isFirmAdmin(),
                scope.isDepartmentManager(),
                locationIds,
                clientIds,
                scope.getAccessibleAssigneeIds(),
                hasBilling,
                scope
        );
    }

    private Specification<ClientEntity> createClientSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), ctx.organizationId()));

            if (ctx.accessibleClientIds() != null) {
                if (ctx.accessibleClientIds().isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("id").in(ctx.accessibleClientIds()));
                }
            }

            if (ctx.accessibleLocationIds() != null && !ctx.accessibleLocationIds().isEmpty()) {
                predicates.add(root.get("locationId").in(ctx.accessibleLocationIds()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<EngagementEntity> createEngagementSpec(ScopingContext ctx, DashboardFilterRequest filter, EngagementStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), ctx.organizationId()));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (ctx.accessibleClientIds() != null) {
                if (ctx.accessibleClientIds().isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(ctx.accessibleClientIds()));
                }
            }

            if (ctx.accessibleLocationIds() != null && !ctx.accessibleLocationIds().isEmpty()) {
                predicates.add(root.get("locationId").in(ctx.accessibleLocationIds()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<ComplianceObligationEntity> createComplianceSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), ctx.organizationId()));

            if (ctx.accessibleClientIds() != null) {
                if (ctx.accessibleClientIds().isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(ctx.accessibleClientIds()));
                }
            }

            if (ctx.accessibleLocationIds() != null && !ctx.accessibleLocationIds().isEmpty()) {
                predicates.add(root.get("locationId").in(ctx.accessibleLocationIds()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<WorkItemEntity> createWorkItemSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), ctx.organizationId()));

            if (ctx.accessibleClientIds() != null) {
                if (ctx.accessibleClientIds().isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(ctx.accessibleClientIds()));
                }
            }

            if (ctx.accessibleLocationIds() != null && !ctx.accessibleLocationIds().isEmpty()) {
                predicates.add(root.get("locationId").in(ctx.accessibleLocationIds()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<TaskEntity> createTaskSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), ctx.organizationId()));

            if (ctx.accessibleClientIds() != null) {
                if (ctx.accessibleClientIds().isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(ctx.accessibleClientIds()));
                }
            }

            if (ctx.accessibleLocationIds() != null && !ctx.accessibleLocationIds().isEmpty()) {
                predicates.add(root.get("locationId").in(ctx.accessibleLocationIds()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<DocumentEntity> createDocumentSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), ctx.organizationId()));

            if (ctx.accessibleClientIds() != null) {
                if (ctx.accessibleClientIds().isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(ctx.accessibleClientIds()));
                }
            }

            if (ctx.accessibleLocationIds() != null && !ctx.accessibleLocationIds().isEmpty()) {
                predicates.add(root.get("locationId").in(ctx.accessibleLocationIds()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<DocumentRequestEntity> createDocRequestSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), ctx.organizationId()));

            if (ctx.accessibleClientIds() != null) {
                if (ctx.accessibleClientIds().isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(ctx.accessibleClientIds()));
                }
            }

            if (ctx.accessibleLocationIds() != null && !ctx.accessibleLocationIds().isEmpty()) {
                predicates.add(root.get("locationId").in(ctx.accessibleLocationIds()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<TaxNoticeEntity> createNoticeSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), ctx.organizationId()));

            if (ctx.accessibleClientIds() != null) {
                if (ctx.accessibleClientIds().isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(ctx.accessibleClientIds()));
                }
            }

            if (ctx.accessibleLocationIds() != null && !ctx.accessibleLocationIds().isEmpty()) {
                predicates.add(root.get("locationId").in(ctx.accessibleLocationIds()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<InvoiceEntity> createInvoiceSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), ctx.organizationId()));

            if (ctx.accessibleClientIds() != null) {
                if (ctx.accessibleClientIds().isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(ctx.accessibleClientIds()));
                }
            }

            if (ctx.accessibleLocationIds() != null && !ctx.accessibleLocationIds().isEmpty()) {
                predicates.add(root.get("locationId").in(ctx.accessibleLocationIds()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<InvoicePaymentEntity> createPaymentSpec(ScopingContext ctx, DashboardFilterRequest filter, LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), ctx.organizationId()));

            if (ctx.accessibleClientIds() != null) {
                if (ctx.accessibleClientIds().isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(ctx.accessibleClientIds()));
                }
            }

            if (ctx.accessibleLocationIds() != null && !ctx.accessibleLocationIds().isEmpty()) {
                predicates.add(root.get("locationId").in(ctx.accessibleLocationIds()));
            }

            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("paymentDate"), end));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<ClientEntity> createActiveClientSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return Specification.where(createClientSpec(ctx, filter))
                .and((root, query, cb) -> cb.equal(root.get("status"), ClientStatus.ACTIVE));
    }

    private Specification<ComplianceObligationEntity> createOpenComplianceSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return Specification.where(createComplianceSpec(ctx, filter))
                .and((root, query, cb) -> cb.not(root.get("status").in(ComplianceObligationStatus.COMPLETED, ComplianceObligationStatus.CANCELLED)));
    }

    private Specification<ComplianceObligationEntity> createOverdueComplianceSpec(ScopingContext ctx, DashboardFilterRequest filter, LocalDate today) {
        return Specification.where(createComplianceSpec(ctx, filter))
                .and((root, query, cb) -> cb.or(
                        cb.equal(root.get("status"), ComplianceObligationStatus.OVERDUE),
                        cb.and(
                                cb.not(root.get("status").in(ComplianceObligationStatus.COMPLETED, ComplianceObligationStatus.CANCELLED)),
                                cb.isNotNull(root.get("statutoryDueDate")),
                                cb.lessThan(root.get("statutoryDueDate"), today)
                        )
                ));
    }

    private Specification<WorkItemEntity> createOpenWorkItemSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return Specification.where(createWorkItemSpec(ctx, filter))
                .and((root, query, cb) -> root.get("status").in(WorkItemStatus.TODO, WorkItemStatus.IN_PROGRESS, WorkItemStatus.BLOCKED));
    }

    private Specification<WorkItemEntity> createOverdueWorkItemSpec(ScopingContext ctx, DashboardFilterRequest filter, LocalDate today) {
        return Specification.where(createOpenWorkItemSpec(ctx, filter))
                .and((root, query, cb) -> cb.and(
                        cb.isNotNull(root.get("dueDate")),
                        cb.lessThan(root.get("dueDate"), today)
                ));
    }

    private Specification<TaskEntity> createPendingTaskSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return Specification.where(createTaskSpec(ctx, filter))
                .and((root, query, cb) -> root.get("status").in(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.UNDER_REVIEW));
    }

    private Specification<TaskEntity> createCompletedTaskSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return Specification.where(createTaskSpec(ctx, filter))
                .and((root, query, cb) -> cb.equal(root.get("status"), TaskStatus.COMPLETED));
    }

    private Specification<TaskEntity> createOverdueTaskSpec(ScopingContext ctx, DashboardFilterRequest filter, LocalDate today) {
        return Specification.where(createPendingTaskSpec(ctx, filter))
                .and((root, query, cb) -> cb.and(
                        cb.isNotNull(root.get("dueDate")),
                        cb.lessThan(root.get("dueDate"), today)
                ));
    }

    private Specification<DocumentRequestEntity> createPendingDocRequestSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return Specification.where(createDocRequestSpec(ctx, filter))
                .and((root, query, cb) -> cb.not(root.get("status").in(RequestStatus.COMPLETED, RequestStatus.CANCELLED, RequestStatus.DECLINED)));
    }

    private Specification<TaxNoticeEntity> createOpenNoticeSpec(ScopingContext ctx, DashboardFilterRequest filter) {
        return Specification.where(createNoticeSpec(ctx, filter))
                .and((root, query, cb) -> cb.not(root.get("status").in(CLOSED_NOTICE_STATUSES)));
    }

    private List<EmployeeEntity> resolveAccessibleEmployees(ScopingContext ctx) {
        if (ctx.isFirmAdmin()) {
            List<EmployeeEntity> active = employeeRepository.findAllByOrganizationIdAndStatus(ctx.organizationId(), EmployeeStatus.ACTIVE);
            return active.isEmpty() ? employeeRepository.findAllByOrganizationId(ctx.organizationId()) : active;
        }

        if (ctx.isDepartmentManager()) {
            PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
            String dept = scope.getDepartment();
            if (StringUtils.hasText(dept)) {
                return employeeRepository.findAllByOrganizationIdAndDepartmentIgnoreCase(ctx.organizationId(), dept);
            }
        }

        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        if (scope.getEmployee() != null) {
            return List.of(scope.getEmployee());
        }

        return List.of();
    }

    // =========================================================================
    // Legacy internal helpers
    // =========================================================================

    private ClientStatsDto getClientStats(UUID organizationId, PracticeSecurityScope scope) {
        if (scope.isFirmAdmin()) {
            List<Object[]> results = clientRepository.getClientDashboardStats(organizationId);
            long total = 0, active = 0, inactive = 0;
            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Object[] row = results.get(0);
                total = toLong(row[0]);
                active = toLong(row[1]);
                inactive = toLong(row[2]);
            }
            return ClientStatsDto.builder().total(total).active(active).inactive(inactive).build();
        }

        Set<UUID> accessibleIds = securityScopeEvaluator.getAccessibleClientIds(scope);
        if (accessibleIds == null || accessibleIds.isEmpty()) {
            return ClientStatsDto.builder().total(0).active(0).inactive(0).build();
        }

        List<ClientEntity> accessibleClients = clientRepository.findAllById(accessibleIds);
        long total = accessibleClients.size();
        long active = accessibleClients.stream().filter(c -> c.getStatus() == ClientEntity.ClientStatus.ACTIVE).count();
        long inactive = total - active;

        return ClientStatsDto.builder().total(total).active(active).inactive(inactive).build();
    }

    private EmployeeStatsDto getEmployeeStats(UUID organizationId, PracticeSecurityScope scope) {
        if (scope.isFirmAdmin()) {
            List<Object[]> results = employeeRepository.getEmployeeDashboardStats(organizationId);
            long total = 0, active = 0;
            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Object[] row = results.get(0);
                total = toLong(row[0]);
                active = toLong(row[1]);
            }
            return EmployeeStatsDto.builder().total(total).active(active).build();
        }
        if (scope.isStaff()) {
            return EmployeeStatsDto.builder().total(1).active(1).build();
        }
        String dept = scope.getDepartment();
        if (StringUtils.hasText(dept)) {
            long total = employeeRepository.countByOrganizationIdAndDepartmentIgnoreCase(organizationId, dept);
            long active = employeeRepository.countByOrganizationIdAndDepartmentIgnoreCaseAndStatus(organizationId, dept, EmployeeStatus.ACTIVE);
            return EmployeeStatsDto.builder().total(total).active(active).build();
        }
        return EmployeeStatsDto.builder().total(1).active(1).build();
    }

    private TaskStatsDto getTaskStats(UUID organizationId, LocalDate today, PracticeSecurityScope scope) {
        if (scope.isFirmAdmin()) {
            List<Object[]> results = taskRepository.getTaskDashboardStats(organizationId, today);
            long total = 0, pending = 0, overdue = 0, completed = 0;
            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Object[] row = results.get(0);
                total = toLong(row[0]);
                pending = toLong(row[1]);
                overdue = toLong(row[2]);
                completed = toLong(row[3]);
            }
            return TaskStatsDto.builder().total(total).pending(pending).overdue(overdue).completed(completed).build();
        }

        Set<UUID> assigneeIds = scope.getAccessibleAssigneeIds();
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            return TaskStatsDto.builder().total(0).pending(0).overdue(0).completed(0).build();
        }

        long total = taskRepository.countAssignedTasks(organizationId, assigneeIds);
        long completed = taskRepository.countByStatuses(organizationId, assigneeIds, Set.of(TaskStatus.COMPLETED));
        Set<TaskStatus> pendingStatuses = Set.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.UNDER_REVIEW);
        long pending = taskRepository.countByStatuses(organizationId, assigneeIds, pendingStatuses);
        long overdue = taskRepository.countOverdueTasks(organizationId, assigneeIds, pendingStatuses, today);

        return TaskStatsDto.builder().total(total).pending(pending).overdue(overdue).completed(completed).build();
    }

    private GstStatsDto getGstStats(UUID organizationId, LocalDate today, PracticeSecurityScope scope) {
        if (scope.isFirmAdmin()) {
            long totalGstClients = gstProfileRepository.countDistinctClientsByOrganizationId(organizationId);
            List<Object[]> results = gstReturnFilingRepository.getGstDashboardStats(organizationId, today);
            long returnsDue = 0, returnsOverdue = 0, returnsFiled = 0;
            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Object[] row = results.get(0);
                returnsDue = toLong(row[0]);
                returnsOverdue = toLong(row[1]);
                returnsFiled = toLong(row[2]);
            }
            return GstStatsDto.builder().totalGstClients(totalGstClients).returnsDue(returnsDue).returnsOverdue(returnsOverdue).returnsFiled(returnsFiled).build();
        }

        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
        if (accessibleClientIds == null || accessibleClientIds.isEmpty()) {
            return GstStatsDto.builder().totalGstClients(0).returnsDue(0).returnsOverdue(0).returnsFiled(0).build();
        }

        List<GstReturnFilingEntity> allFilings = gstReturnFilingRepository.findAllByOrganizationIdAndClientIdIn(organizationId, accessibleClientIds);
        long totalGstClients = allFilings.stream().map(GstReturnFilingEntity::getClientId).filter(Objects::nonNull).distinct().count();
        long returnsFiled = allFilings.stream().filter(f -> f.getFilingStatus() == GstFilingStatus.FILED).count();
        long returnsOverdue = allFilings.stream().filter(f -> f.getFilingStatus() != GstFilingStatus.FILED && f.getDueDate() != null && f.getDueDate().isBefore(today)).count();
        long returnsDue = allFilings.stream().filter(f -> f.getFilingStatus() != GstFilingStatus.FILED && (f.getDueDate() == null || !f.getDueDate().isBefore(today))).count();

        return GstStatsDto.builder().totalGstClients(totalGstClients).returnsDue(returnsDue).returnsOverdue(returnsOverdue).returnsFiled(returnsFiled).build();
    }

    private ItrStatsDto getItrStats(UUID organizationId, LocalDate today, PracticeSecurityScope scope) {
        if (scope.isFirmAdmin()) {
            long totalItrClients = itrProfileRepository.countDistinctClientsByOrganizationId(organizationId);
            List<Object[]> results = itrReturnRepository.getItrDashboardStats(organizationId, today);
            long pending = 0, filed = 0, overdue = 0;
            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Object[] row = results.get(0);
                pending = toLong(row[0]);
                filed = toLong(row[1]);
                overdue = toLong(row[2]);
            }
            return ItrStatsDto.builder().totalItrClients(totalItrClients).pending(pending).filed(filed).overdue(overdue).build();
        }

        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
        if (accessibleClientIds == null || accessibleClientIds.isEmpty()) {
            return ItrStatsDto.builder().totalItrClients(0).pending(0).filed(0).overdue(0).build();
        }

        List<ItrReturnEntity> returns = itrReturnRepository.findAllByOrganizationIdAndClientIdIn(organizationId, accessibleClientIds);
        long totalItrClients = returns.stream().map(ItrReturnEntity::getClientId).filter(Objects::nonNull).distinct().count();
        long filed = returns.stream().filter(r -> r.getStatus() == ItrStatus.FILED || r.getStatus() == ItrStatus.COMPLETED).count();
        long overdue = returns.stream().filter(r -> r.getStatus() != ItrStatus.FILED && r.getStatus() != ItrStatus.COMPLETED && r.getDueDate() != null && r.getDueDate().isBefore(today)).count();
        long pending = returns.stream().filter(r -> r.getStatus() != ItrStatus.FILED && r.getStatus() != ItrStatus.COMPLETED && (r.getDueDate() == null || !r.getDueDate().isBefore(today))).count();

        return ItrStatsDto.builder().totalItrClients(totalItrClients).pending(pending).filed(filed).overdue(overdue).build();
    }

    private TdsStatsDto getTdsStats(UUID organizationId, LocalDate today, PracticeSecurityScope scope) {
        if (scope.isFirmAdmin()) {
            long totalTdsClients = tdsProfileRepository.countDistinctClientsByOrganizationId(organizationId);
            List<Object[]> results = tdsReturnRepository.getTdsDashboardStats(organizationId, today);
            long pending = 0, filed = 0, overdue = 0;
            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Object[] row = results.get(0);
                pending = toLong(row[0]);
                filed = toLong(row[1]);
                overdue = toLong(row[2]);
            }
            return TdsStatsDto.builder().totalTdsClients(totalTdsClients).pending(pending).filed(filed).overdue(overdue).build();
        }

        Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
        if (accessibleClientIds == null || accessibleClientIds.isEmpty()) {
            return TdsStatsDto.builder().totalTdsClients(0).pending(0).filed(0).overdue(0).build();
        }

        List<TdsReturnEntity> returns = tdsReturnRepository.findAllByOrganizationIdAndClientIdIn(organizationId, accessibleClientIds);
        long totalTdsClients = returns.stream().map(TdsReturnEntity::getClientId).filter(Objects::nonNull).distinct().count();
        long filed = returns.stream().filter(r -> r.getFilingStatus() == TdsFilingStatus.FILED).count();
        long overdue = returns.stream().filter(r -> r.getFilingStatus() != TdsFilingStatus.FILED && r.getDueDate() != null && r.getDueDate().isBefore(today)).count();
        long pending = returns.stream().filter(r -> r.getFilingStatus() != TdsFilingStatus.FILED && (r.getDueDate() == null || !r.getDueDate().isBefore(today))).count();

        return TdsStatsDto.builder().totalTdsClients(totalTdsClients).pending(pending).filed(filed).overdue(overdue).build();
    }

    private BillingStatsDto getBillingStats(UUID organizationId) {
        List<Object[]> results = invoiceRepository.getBillingDashboardStatsSummary(organizationId);
        BigDecimal total = BigDecimal.ZERO, paid = BigDecimal.ZERO, outstanding = BigDecimal.ZERO;
        if (results != null && !results.isEmpty() && results.get(0) != null) {
            Object[] row = results.get(0);
            total = toBigDecimal(row[0]);
            paid = toBigDecimal(row[1]);
            outstanding = toBigDecimal(row[2]);
        }
        return BillingStatsDto.builder().totalInvoiceAmount(total).paidAmount(paid).outstandingAmount(outstanding).build();
    }

    private List<EmployeeWorkloadItemDto> getEmployeeWorkload(UUID organizationId, LocalDate today, PracticeSecurityScope scope) {
        List<EmployeeEntity> employees;
        if (scope.isFirmAdmin()) {
            employees = employeeRepository.findAllByOrganizationIdAndStatus(organizationId, EmployeeStatus.ACTIVE);
            if (employees.isEmpty()) {
                employees = employeeRepository.findAllByOrganizationId(organizationId);
            }
        } else if (scope.isDepartmentManager() && StringUtils.hasText(scope.getDepartment())) {
            employees = employeeRepository.findAllByOrganizationIdAndDepartmentIgnoreCase(organizationId, scope.getDepartment());
        } else {
            employees = scope.getEmployee() != null ? List.of(scope.getEmployee()) : List.of();
        }

        List<Object[]> taskGroupResults = taskRepository.getEmployeeTaskWorkloadStats(organizationId, today);
        Map<UUID, TaskAgg> taskStatsMap = new HashMap<>();
        if (taskGroupResults != null) {
            for (Object[] row : taskGroupResults) {
                if (row[0] instanceof UUID assigneeId) {
                    taskStatsMap.put(assigneeId, new TaskAgg(toLong(row[1]), toLong(row[2]), toLong(row[3])));
                }
            }
        }

        List<EmployeeWorkloadItemDto> workloadList = new ArrayList<>();
        for (EmployeeEntity emp : employees) {
            long totalAssigned = 0, pending = 0, overdue = 0;
            TaskAgg direct = taskStatsMap.get(emp.getId());
            if (direct != null) {
                totalAssigned += direct.assigned;
                pending += direct.pending;
                overdue += direct.overdue;
            }
            if (emp.getUserId() != null && !emp.getUserId().equals(emp.getId())) {
                TaskAgg userAssigned = taskStatsMap.get(emp.getUserId());
                if (userAssigned != null) {
                    totalAssigned += userAssigned.assigned;
                    pending += userAssigned.pending;
                    overdue += userAssigned.overdue;
                }
            }
            workloadList.add(EmployeeWorkloadItemDto.builder()
                    .employeeId(emp.getId())
                    .employeeCode(emp.getEmployeeCode())
                    .employeeName(emp.getFullName())
                    .email(emp.getEmail())
                    .department(emp.getDepartment())
                    .designation(emp.getDesignation())
                    .assignedTasks(totalAssigned)
                    .pendingTasks(pending)
                    .overdueTasks(overdue)
                    .build());
        }
        return workloadList;
    }

    private static long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private record TaskAgg(long assigned, long pending, long overdue) {}
}
