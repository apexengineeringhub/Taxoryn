package com.taxoryn.module.report.service;

import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.InvoicePaymentEntity;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.report.dto.ClientReportDto;
import com.taxoryn.module.report.dto.ClientReportDto.ClientAttentionItemDto;
import com.taxoryn.module.report.dto.FinancialReportDto;
import com.taxoryn.module.report.dto.FinancialReportDto.OutstandingInvoiceDto;
import com.taxoryn.module.report.dto.PracticeOverviewReportDto;
import com.taxoryn.module.report.dto.TaxWorkReportDto;
import com.taxoryn.module.report.dto.TaxWorkReportDto.TaxTypeSummaryDto;
import com.taxoryn.module.report.dto.WorkManagementReportDto;
import com.taxoryn.module.report.dto.WorkManagementReportDto.EmployeeProductivityDto;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.tds.entity.TdsReturnEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final ComplianceObligationRepository complianceObligationRepository;
    private final GstProfileRepository gstProfileRepository;
    private final GstReturnFilingRepository gstReturnFilingRepository;
    private final ItrProfileRepository itrProfileRepository;
    private final ItrReturnRepository itrReturnRepository;
    private final TdsProfileRepository tdsProfileRepository;
    private final TdsReturnRepository tdsReturnRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final DocumentRequestItemRepository documentRequestItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;

    @Override
    @Transactional(readOnly = true)
    public PracticeOverviewReportDto getPracticeOverviewReport(LocalDate fromDate, LocalDate toDate) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.plusDays(7);
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        // 1. Clients
        long totalClients;
        long activeClients;
        long inactiveClients;

        if (scope.isFirmAdmin()) {
            List<Object[]> clientStats = clientRepository.getClientDashboardStats(organizationId);
            totalClients = 0;
            activeClients = 0;
            inactiveClients = 0;
            if (clientStats != null && !clientStats.isEmpty() && clientStats.get(0) != null) {
                Object[] row = clientStats.get(0);
                totalClients = toLong(row[0]);
                activeClients = toLong(row[1]);
                inactiveClients = toLong(row[2]);
            }
        } else {
            Set<UUID> accessibleIds = securityScopeEvaluator.getAccessibleClientIds(scope);
            if (accessibleIds == null || accessibleIds.isEmpty()) {
                totalClients = 0;
                activeClients = 0;
                inactiveClients = 0;
            } else {
                List<ClientEntity> clients = clientRepository.findAllById(accessibleIds);
                totalClients = clients.size();
                activeClients = clients.stream().filter(c -> c.getStatus() == ClientStatus.ACTIVE).count();
                inactiveClients = totalClients - activeClients;
            }
        }

        // 2. Active Tax Jobs
        List<GstReturnFilingEntity> gstFilings = gstReturnFilingRepository.findAllByOrganizationId(organizationId);
        List<ItrReturnEntity> itrReturns = itrReturnRepository.findAllByOrganizationId(organizationId);
        List<TdsReturnEntity> tdsReturns = tdsReturnRepository.findAllByOrganizationId(organizationId);

        long pendingGst = gstFilings.stream().filter(f -> f.getFilingStatus() != GstFilingStatus.FILED && f.getFilingStatus() != GstFilingStatus.CANCELLED).count();
        long pendingItr = itrReturns.stream().filter(r -> r.getStatus() != ItrStatus.COMPLETED && r.getStatus() != ItrStatus.FILED && r.getStatus() != ItrStatus.CANCELLED).count();
        long pendingTds = tdsReturns.stream().filter(r -> r.getFilingStatus() != TdsFilingStatus.FILED && r.getFilingStatus() != TdsFilingStatus.CANCELLED).count();
        long activeTaxJobs = pendingGst + pendingItr + pendingTds;

        // 3. Tasks
        List<TaskEntity> tasks = taskRepository.findAllByOrganizationId(organizationId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        long openTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO || t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long reviewTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.UNDER_REVIEW).count();
        long completedTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long overdueTasks = tasks.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED && t.getDueDate() != null && t.getDueDate().isBefore(today)).count();

        // 4. Compliance Obligations
        List<ComplianceObligationEntity> obligations = complianceObligationRepository.findAllByOrganizationIdAndDueDateBetween(
                organizationId, today.minusYears(1), today.plusYears(1));

        long complianceDueToday = obligations.stream().filter(o -> o.getStatus() != ComplianceStatus.COMPLETED && o.getStatus() != ComplianceStatus.CANCELLED && today.equals(o.getDueDate())).count();
        long complianceDueThisWeek = obligations.stream().filter(o -> o.getStatus() != ComplianceStatus.COMPLETED && o.getStatus() != ComplianceStatus.CANCELLED && o.getDueDate() != null && !o.getDueDate().isBefore(today) && !o.getDueDate().isAfter(endOfWeek)).count();
        long complianceOverdue = obligations.stream().filter(o -> o.getStatus() != ComplianceStatus.COMPLETED && o.getStatus() != ComplianceStatus.CANCELLED && o.getDueDate() != null && o.getDueDate().isBefore(today)).count();
        long complianceCompleted = obligations.stream().filter(o -> o.getStatus() == ComplianceStatus.COMPLETED).count();

        // 5. Document Requests
        List<DocumentRequestEntity> docRequests = documentRequestRepository.findAllByOrganizationId(organizationId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        long documentRequestsPending = docRequests.stream().filter(d -> d.getStatus() == RequestStatus.SENT || d.getStatus() == RequestStatus.PARTIALLY_COMPLETED).count();
        long documentRequestsOpen = docRequests.stream().filter(d -> d.getStatus() != RequestStatus.COMPLETED && d.getStatus() != RequestStatus.CANCELLED).count();

        // 6. Financial Summary
        boolean hasBilling = securityScopeEvaluator.hasBillingAccess(scope);
        BigDecimal totalInvoiced = null;
        BigDecimal totalCollected = null;
        BigDecimal totalOutstanding = null;

        if (hasBilling) {
            List<Object[]> billingResults = invoiceRepository.getBillingDashboardStatsSummary(organizationId);
            if (billingResults != null && !billingResults.isEmpty() && billingResults.get(0) != null) {
                Object[] row = billingResults.get(0);
                totalInvoiced = toBigDecimal(row[0]);
                totalCollected = toBigDecimal(row[1]);
                totalOutstanding = toBigDecimal(row[2]);
            } else {
                totalInvoiced = BigDecimal.ZERO;
                totalCollected = BigDecimal.ZERO;
                totalOutstanding = BigDecimal.ZERO;
            }
        }

        return PracticeOverviewReportDto.builder()
                .totalClients(totalClients)
                .activeClients(activeClients)
                .inactiveClients(inactiveClients)
                .activeTaxJobs(activeTaxJobs)
                .openTasks(openTasks)
                .reviewTasks(reviewTasks)
                .overdueTasks(overdueTasks)
                .completedTasks(completedTasks)
                .complianceDueToday(complianceDueToday)
                .complianceDueThisWeek(complianceDueThisWeek)
                .complianceOverdue(complianceOverdue)
                .complianceCompleted(complianceCompleted)
                .documentRequestsPending(documentRequestsPending)
                .documentRequestsOpen(documentRequestsOpen)
                .totalInvoiced(totalInvoiced)
                .totalCollected(totalCollected)
                .totalOutstanding(totalOutstanding)
                .hasBillingAccess(hasBilling)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TaxWorkReportDto getTaxWorkReport(String financialYear, String assessmentYear, String quarter, LocalDate fromDate, LocalDate toDate) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.plusDays(7);

        // 1. GST Filings
        List<GstReturnFilingEntity> allGst = gstReturnFilingRepository.findAllByOrganizationId(organizationId);
        if (StringUtils.hasText(financialYear)) {
            allGst = allGst.stream().filter(g -> financialYear.equalsIgnoreCase(g.getFinancialYear())).toList();
        }

        long gstTotalClients = gstProfileRepository.countDistinctClientsByOrganizationId(organizationId);
        long gstFiled = allGst.stream().filter(f -> f.getFilingStatus() == GstFilingStatus.FILED).count();
        long gstReview = allGst.stream().filter(f -> f.getFilingStatus() == GstFilingStatus.UNDER_REVIEW).count();
        long gstOverdue = allGst.stream().filter(f -> f.getFilingStatus() != GstFilingStatus.FILED && f.getFilingStatus() != GstFilingStatus.CANCELLED && f.getDueDate() != null && f.getDueDate().isBefore(today)).count();
        long gstPending = allGst.stream().filter(f -> f.getFilingStatus() != GstFilingStatus.FILED && f.getFilingStatus() != GstFilingStatus.CANCELLED && (f.getDueDate() == null || !f.getDueDate().isBefore(today))).count();

        Map<String, Long> gstByReturnType = new LinkedHashMap<>();
        for (GstReturnFilingEntity f : allGst) {
            String typeName = f.getReturnType() != null ? f.getReturnType().name() : "OTHER";
            gstByReturnType.put(typeName, gstByReturnType.getOrDefault(typeName, 0L) + 1);
        }

        // 2. ITR Returns
        List<ItrReturnEntity> allItr = itrReturnRepository.findAllByOrganizationId(organizationId);
        if (StringUtils.hasText(assessmentYear)) {
            allItr = allItr.stream().filter(i -> assessmentYear.equalsIgnoreCase(i.getAssessmentYear())).toList();
        } else if (StringUtils.hasText(financialYear)) {
            allItr = allItr.stream().filter(i -> financialYear.equalsIgnoreCase(i.getFinancialYear())).toList();
        }

        long itrTotalClients = itrProfileRepository.countDistinctClientsByOrganizationId(organizationId);
        long itrPending = allItr.stream().filter(r -> r.getStatus() == ItrStatus.DOCUMENTS_PENDING).count();
        long itrPreparation = allItr.stream().filter(r -> r.getStatus() == ItrStatus.DATA_ENTRY || r.getStatus() == ItrStatus.READY_TO_FILE).count();
        long itrReview = allItr.stream().filter(r -> r.getStatus() == ItrStatus.UNDER_REVIEW).count();
        long itrFiled = allItr.stream().filter(r -> r.getStatus() == ItrStatus.FILED || r.getStatus() == ItrStatus.VERIFICATION_PENDING).count();
        long itrCompleted = allItr.stream().filter(r -> r.getStatus() == ItrStatus.COMPLETED).count();
        long itrOverdue = allItr.stream().filter(r -> r.getStatus() != ItrStatus.FILED && r.getStatus() != ItrStatus.COMPLETED && r.getStatus() != ItrStatus.CANCELLED && r.getDueDate() != null && r.getDueDate().isBefore(today)).count();

        Map<String, Long> itrByFormType = new LinkedHashMap<>();
        for (ItrReturnEntity r : allItr) {
            String form = r.getItrType() != null ? r.getItrType().name() : "OTHER";
            itrByFormType.put(form, itrByFormType.getOrDefault(form, 0L) + 1);
        }

        // 3. TDS Returns
        List<TdsReturnEntity> allTds = tdsReturnRepository.findAllByOrganizationId(organizationId);
        if (StringUtils.hasText(financialYear)) {
            allTds = allTds.stream().filter(t -> financialYear.equalsIgnoreCase(t.getFinancialYear())).toList();
        }
        if (StringUtils.hasText(quarter)) {
            allTds = allTds.stream().filter(t -> t.getQuarter() != null && quarter.equalsIgnoreCase(t.getQuarter().name())).toList();
        }

        long tdsTotalClients = tdsProfileRepository.countDistinctClientsByOrganizationId(organizationId);
        long tdsPending = allTds.stream().filter(r -> r.getFilingStatus() == TdsFilingStatus.PENDING || r.getFilingStatus() == TdsFilingStatus.DRAFT).count();
        long tdsChallansAttached = allTds.stream().filter(r -> r.getFilingStatus() == TdsFilingStatus.CHALLANS_ATTACHED).count();
        long tdsReview = allTds.stream().filter(r -> r.getFilingStatus() == TdsFilingStatus.UNDER_REVIEW).count();
        long tdsFiled = allTds.stream().filter(r -> r.getFilingStatus() == TdsFilingStatus.FILED).count();
        long tdsOverdue = allTds.stream().filter(r -> r.getFilingStatus() != TdsFilingStatus.FILED && r.getFilingStatus() != TdsFilingStatus.CANCELLED && r.getDueDate() != null && r.getDueDate().isBefore(today)).count();

        Map<String, Long> tdsByQuarter = new LinkedHashMap<>();
        Map<String, Long> tdsByFormType = new LinkedHashMap<>();
        for (TdsReturnEntity t : allTds) {
            String q = t.getQuarter() != null ? t.getQuarter().name() : "Q1";
            tdsByQuarter.put(q, tdsByQuarter.getOrDefault(q, 0L) + 1);

            String form = t.getFormType() != null ? t.getFormType().name() : "FORM_26Q";
            tdsByFormType.put(form, tdsByFormType.getOrDefault(form, 0L) + 1);
        }

        // 4. Statutory Compliance
        List<ComplianceObligationEntity> obligations = complianceObligationRepository.findAllByOrganizationIdAndDueDateBetween(
                organizationId,
                fromDate != null ? fromDate : today.minusYears(1),
                toDate != null ? toDate : today.plusYears(1)
        );

        long complianceTotal = obligations.size();
        long complianceDueToday = obligations.stream().filter(o -> o.getStatus() != ComplianceStatus.COMPLETED && o.getStatus() != ComplianceStatus.CANCELLED && today.equals(o.getDueDate())).count();
        long complianceDueThisWeek = obligations.stream().filter(o -> o.getStatus() != ComplianceStatus.COMPLETED && o.getStatus() != ComplianceStatus.CANCELLED && o.getDueDate() != null && !o.getDueDate().isBefore(today) && !o.getDueDate().isAfter(endOfWeek)).count();
        long complianceUpcoming = obligations.stream().filter(o -> o.getStatus() != ComplianceStatus.COMPLETED && o.getStatus() != ComplianceStatus.CANCELLED && o.getDueDate() != null && o.getDueDate().isAfter(endOfWeek)).count();
        long complianceOverdue = obligations.stream().filter(o -> o.getStatus() != ComplianceStatus.COMPLETED && o.getStatus() != ComplianceStatus.CANCELLED && o.getDueDate() != null && o.getDueDate().isBefore(today)).count();
        long complianceCompleted = obligations.stream().filter(o -> o.getStatus() == ComplianceStatus.COMPLETED).count();

        Map<String, Long> complianceByType = new LinkedHashMap<>();
        for (ComplianceObligationEntity ob : obligations) {
            String typeStr = ob.getComplianceType() != null ? ob.getComplianceType().name() : "OTHER";
            complianceByType.put(typeStr, complianceByType.getOrDefault(typeStr, 0L) + 1);
        }

        // Consolidated Summary Matrix
        List<TaxTypeSummaryDto> summaryRows = List.of(
                TaxTypeSummaryDto.builder()
                        .taxType("GST")
                        .pending(gstPending)
                        .review(gstReview)
                        .filed(gstFiled)
                        .overdue(gstOverdue)
                        .total(allGst.size())
                        .build(),
                TaxTypeSummaryDto.builder()
                        .taxType("ITR")
                        .pending(itrPending + itrPreparation)
                        .review(itrReview)
                        .filed(itrFiled + itrCompleted)
                        .overdue(itrOverdue)
                        .total(allItr.size())
                        .build(),
                TaxTypeSummaryDto.builder()
                        .taxType("TDS")
                        .pending(tdsPending + tdsChallansAttached)
                        .review(tdsReview)
                        .filed(tdsFiled)
                        .overdue(tdsOverdue)
                        .total(allTds.size())
                        .build()
        );

        return TaxWorkReportDto.builder()
                .taxWorkSummary(summaryRows)
                .gstTotalClients(gstTotalClients)
                .gstPending(gstPending)
                .gstReview(gstReview)
                .gstFiled(gstFiled)
                .gstOverdue(gstOverdue)
                .gstByReturnType(gstByReturnType)
                .itrTotalClients(itrTotalClients)
                .itrPending(itrPending)
                .itrPreparation(itrPreparation)
                .itrReview(itrReview)
                .itrFiled(itrFiled)
                .itrCompleted(itrCompleted)
                .itrOverdue(itrOverdue)
                .itrByFormType(itrByFormType)
                .tdsTotalClients(tdsTotalClients)
                .tdsPending(tdsPending)
                .tdsChallansAttached(tdsChallansAttached)
                .tdsReview(tdsReview)
                .tdsFiled(tdsFiled)
                .tdsOverdue(tdsOverdue)
                .tdsByQuarter(tdsByQuarter)
                .tdsByFormType(tdsByFormType)
                .complianceTotal(complianceTotal)
                .complianceDueToday(complianceDueToday)
                .complianceDueThisWeek(complianceDueThisWeek)
                .complianceUpcoming(complianceUpcoming)
                .complianceOverdue(complianceOverdue)
                .complianceCompleted(complianceCompleted)
                .complianceByType(complianceByType)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientReportDto getClientReport(LocalDate fromDate, LocalDate toDate) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();

        List<ClientEntity> allClients = clientRepository.findAllByOrganizationId(organizationId);
        long totalClients = allClients.size();
        long activeClients = allClients.stream().filter(c -> c.getStatus() == ClientStatus.ACTIVE).count();
        long inactiveClients = totalClients - activeClients;

        // Fetch Tasks, Filings, Document Requests, and Compliance
        List<TaskEntity> tasks = taskRepository.findAllByOrganizationId(organizationId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        List<DocumentRequestEntity> docRequests = documentRequestRepository.findAllByOrganizationId(organizationId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        List<ComplianceObligationEntity> obligations = complianceObligationRepository.findAllByOrganizationIdAndDueDateBetween(
                organizationId, today.minusYears(1), today.plusYears(1));

        Set<UUID> clientsWithOpenTasks = tasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED && t.getClientId() != null)
                .map(TaskEntity::getClientId)
                .collect(Collectors.toSet());

        Set<UUID> clientsWithOverdueTasks = tasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED && t.getClientId() != null && t.getDueDate() != null && t.getDueDate().isBefore(today))
                .map(TaskEntity::getClientId)
                .collect(Collectors.toSet());

        Set<UUID> clientsWithPendingDocs = docRequests.stream()
                .filter(d -> (d.getStatus() == RequestStatus.SENT || d.getStatus() == RequestStatus.PARTIALLY_COMPLETED) && d.getClientId() != null)
                .map(DocumentRequestEntity::getClientId)
                .collect(Collectors.toSet());

        Set<UUID> clientsWithOverdueCompliance = obligations.stream()
                .filter(o -> o.getStatus() != ComplianceStatus.COMPLETED && o.getStatus() != ComplianceStatus.CANCELLED && o.getClientId() != null && o.getDueDate() != null && o.getDueDate().isBefore(today))
                .map(ComplianceObligationEntity::getClientId)
                .collect(Collectors.toSet());

        // Follow-up Summary
        long pendingClientActions = docRequests.stream().filter(d -> d.getStatus() == RequestStatus.SENT || d.getStatus() == RequestStatus.PARTIALLY_COMPLETED).count();
        long clientActionsDueToday = docRequests.stream().filter(d -> d.getDueDate() != null && today.equals(d.getDueDate()) && d.getStatus() != RequestStatus.COMPLETED).count();
        long clientActionsOverdue = docRequests.stream().filter(d -> d.getDueDate() != null && d.getDueDate().isBefore(today) && d.getStatus() != RequestStatus.COMPLETED).count();

        // Document Requests Item Pipeline
        long totalDocRequests = docRequests.size();
        long docRequestsAwaitingUpload = documentRequestItemRepository.countByOrganizationIdAndStatus(organizationId, ItemStatus.PENDING);
        long docRequestsUploaded = documentRequestItemRepository.countByOrganizationIdAndStatus(organizationId, ItemStatus.UPLOADED);
        long docRequestsAccepted = documentRequestItemRepository.countByOrganizationIdAndStatus(organizationId, ItemStatus.ACCEPTED);
        long docRequestsRejected = documentRequestItemRepository.countByOrganizationIdAndStatus(organizationId, ItemStatus.REJECTED);

        // Build Attention Items list
        List<ClientAttentionItemDto> attentionItems = new ArrayList<>();
        Map<UUID, EmployeeEntity> employeeMap = employeeRepository.findAllByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(EmployeeEntity::getId, e -> e, (a, b) -> a));

        for (ClientEntity client : allClients) {
            long openT = tasks.stream().filter(t -> client.getId().equals(t.getClientId()) && t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED).count();
            long overdueT = tasks.stream().filter(t -> client.getId().equals(t.getClientId()) && t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED && t.getDueDate() != null && t.getDueDate().isBefore(today)).count();
            long pendingD = docRequests.stream().filter(d -> client.getId().equals(d.getClientId()) && (d.getStatus() == RequestStatus.SENT || d.getStatus() == RequestStatus.PARTIALLY_COMPLETED)).count();
            boolean hasOverdueComp = clientsWithOverdueCompliance.contains(client.getId());

            if (openT > 0 || overdueT > 0 || pendingD > 0 || hasOverdueComp) {
                String staffName = client.getAssignedEmployeeId() != null && employeeMap.containsKey(client.getAssignedEmployeeId())
                        ? employeeMap.get(client.getAssignedEmployeeId()).getFullName()
                        : "Unassigned";

                attentionItems.add(ClientAttentionItemDto.builder()
                        .clientId(client.getId())
                        .displayName(client.getDisplayName())
                        .pan(client.getPan())
                        .clientType(client.getClientType() != null ? client.getClientType().name() : "INDIVIDUAL")
                        .assignedStaffName(staffName)
                        .openTasks(openT)
                        .overdueTasks(overdueT)
                        .pendingDocRequests(pendingD)
                        .hasOverdueCompliance(hasOverdueComp)
                        .build());
            }
        }

        // Sort by overdue tasks desc, then open tasks desc
        attentionItems.sort((a, b) -> {
            int cmp = Long.compare(b.getOverdueTasks(), a.getOverdueTasks());
            if (cmp != 0) return cmp;
            return Long.compare(b.getOpenTasks(), a.getOpenTasks());
        });

        return ClientReportDto.builder()
                .totalClients(totalClients)
                .activeClients(activeClients)
                .inactiveClients(inactiveClients)
                .clientsWithPendingWork(clientsWithOpenTasks.size())
                .clientsWithOverdueWork(clientsWithOverdueTasks.size() + clientsWithOverdueCompliance.size())
                .clientsWithPendingDocs(clientsWithPendingDocs.size())
                .pendingClientActions(pendingClientActions)
                .clientActionsDueToday(clientActionsDueToday)
                .clientActionsOverdue(clientActionsOverdue)
                .totalDocRequests(totalDocRequests)
                .docRequestsAwaitingUpload(docRequestsAwaitingUpload)
                .docRequestsUploaded(docRequestsUploaded)
                .docRequestsAccepted(docRequestsAccepted)
                .docRequestsRejected(docRequestsRejected)
                .clientsRequiringAttention(attentionItems.stream().limit(50).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkManagementReportDto getWorkManagementReport(LocalDate fromDate, LocalDate toDate) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();

        List<TaskEntity> tasks = taskRepository.findAllByOrganizationId(organizationId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        if (fromDate != null && toDate != null) {
            tasks = tasks.stream()
                    .filter(t -> t.getDueDate() == null || (!t.getDueDate().isBefore(fromDate) && !t.getDueDate().isAfter(toDate)))
                    .toList();
        }

        long totalTasks = tasks.size();
        long openTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        long inProgressTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long underReviewTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.UNDER_REVIEW).count();
        long blockedTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.BLOCKED).count();
        long completedTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long overdueTasks = tasks.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED && t.getDueDate() != null && t.getDueDate().isBefore(today)).count();

        Map<String, Long> tasksByCategory = new LinkedHashMap<>();
        Map<String, Long> tasksByPriority = new LinkedHashMap<>();

        for (TaskEntity t : tasks) {
            String cat = t.getTaskCategory() != null ? t.getTaskCategory().name() : "OTHER";
            tasksByCategory.put(cat, tasksByCategory.getOrDefault(cat, 0L) + 1);

            String pri = t.getPriority() != null ? t.getPriority().name() : "MEDIUM";
            tasksByPriority.put(pri, tasksByPriority.getOrDefault(pri, 0L) + 1);
        }

        // Employee Productivity Breakdown
        List<EmployeeEntity> employees = employeeRepository.findAllByOrganizationId(organizationId);
        List<EmployeeProductivityDto> productivityList = new ArrayList<>();

        for (EmployeeEntity emp : employees) {
            List<TaskEntity> empTasks = tasks.stream()
                    .filter(t -> emp.getId().equals(t.getAssignedTo()) || (emp.getUserId() != null && emp.getUserId().equals(t.getAssignedTo())))
                    .toList();

            long assigned = empTasks.size();
            long open = empTasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
            long inProg = empTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
            long review = empTasks.stream().filter(t -> t.getStatus() == TaskStatus.UNDER_REVIEW).count();
            long overdue = empTasks.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED && t.getDueDate() != null && t.getDueDate().isBefore(today)).count();
            long done = empTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();

            double rate = assigned > 0 ? ((double) done / (double) assigned) * 100.0 : 100.0;
            rate = BigDecimal.valueOf(rate).setScale(1, RoundingMode.HALF_UP).doubleValue();

            productivityList.add(EmployeeProductivityDto.builder()
                    .employeeId(emp.getId())
                    .employeeCode(emp.getEmployeeCode())
                    .employeeName(emp.getFullName())
                    .email(emp.getEmail())
                    .department(emp.getDepartment())
                    .designation(emp.getDesignation())
                    .assignedTasks(assigned)
                    .openTasks(open)
                    .inProgressTasks(inProg)
                    .underReviewTasks(review)
                    .overdueTasks(overdue)
                    .completedTasks(done)
                    .completionRate(rate)
                    .build());
        }

        productivityList.sort((a, b) -> Long.compare(b.getAssignedTasks(), a.getAssignedTasks()));

        return WorkManagementReportDto.builder()
                .totalTasks(totalTasks)
                .openTasks(openTasks)
                .inProgressTasks(inProgressTasks)
                .underReviewTasks(underReviewTasks)
                .blockedTasks(blockedTasks)
                .overdueTasks(overdueTasks)
                .completedTasks(completedTasks)
                .tasksByCategory(tasksByCategory)
                .tasksByPriority(tasksByPriority)
                .employeeProductivity(productivityList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialReportDto getFinancialReport(LocalDate fromDate, LocalDate toDate) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate dueSoonThreshold = today.plusDays(15);
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        boolean hasBilling = securityScopeEvaluator.hasBillingAccess(scope);
        if (!hasBilling) {
            return FinancialReportDto.builder()
                    .hasBillingAccess(false)
                    .totalInvoiced(BigDecimal.ZERO)
                    .totalCollected(BigDecimal.ZERO)
                    .totalOutstanding(BigDecimal.ZERO)
                    .outstandingDueSoon(BigDecimal.ZERO)
                    .outstandingOverdue(BigDecimal.ZERO)
                    .totalInvoices(0)
                    .draftInvoices(0)
                    .issuedInvoices(0)
                    .partiallyPaidInvoices(0)
                    .paidInvoices(0)
                    .overdueInvoices(0)
                    .cancelledInvoices(0)
                    .invoicesByStatus(Collections.emptyMap())
                    .totalPaymentsCount(0)
                    .collectedThisMonth(BigDecimal.ZERO)
                    .collectedThisQuarter(BigDecimal.ZERO)
                    .outstandingInvoices(Collections.emptyList())
                    .build();
        }

        List<InvoiceEntity> allInvoices = invoiceRepository.findAllByOrganizationId(organizationId);
        if (fromDate != null && toDate != null) {
            allInvoices = allInvoices.stream()
                    .filter(i -> i.getInvoiceDate() == null || (!i.getInvoiceDate().isBefore(fromDate) && !i.getInvoiceDate().isAfter(toDate)))
                    .toList();
        }

        long totalInvoices = allInvoices.size();
        long draftInvoices = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.DRAFT).count();
        long issuedInvoices = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.ISSUED).count();
        long partiallyPaidInvoices = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PARTIALLY_PAID).count();
        long paidInvoices = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count();
        long overdueInvoices = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.OVERDUE || ((i.getStatus() == InvoiceStatus.ISSUED || i.getStatus() == InvoiceStatus.PARTIALLY_PAID) && i.getDueDate() != null && i.getDueDate().isBefore(today))).count();
        long cancelledInvoices = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.CANCELLED).count();

        Map<String, Long> invoicesByStatus = new LinkedHashMap<>();
        for (InvoiceEntity inv : allInvoices) {
            String s = inv.getStatus() != null ? inv.getStatus().name() : "ISSUED";
            invoicesByStatus.put(s, invoicesByStatus.getOrDefault(s, 0L) + 1);
        }

        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal outstandingDueSoon = BigDecimal.ZERO;
        BigDecimal outstandingOverdue = BigDecimal.ZERO;

        List<OutstandingInvoiceDto> outstandingList = new ArrayList<>();
        Map<UUID, ClientEntity> clientMap = clientRepository.findAllByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(ClientEntity::getId, c -> c, (a, b) -> a));

        for (InvoiceEntity inv : allInvoices) {
            if (inv.getStatus() == InvoiceStatus.CANCELLED) {
                continue;
            }

            BigDecimal total = inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO;
            BigDecimal paid = inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal balance = inv.getBalanceDue() != null ? inv.getBalanceDue() : total.subtract(paid);

            totalInvoiced = totalInvoiced.add(total);
            totalCollected = totalCollected.add(paid);

            if (balance.compareTo(BigDecimal.ZERO) > 0 && inv.getStatus() != InvoiceStatus.PAID) {
                totalOutstanding = totalOutstanding.add(balance);

                boolean isOverdue = inv.getDueDate() != null && inv.getDueDate().isBefore(today);
                long days = 0;
                if (inv.getDueDate() != null) {
                    days = Math.abs(ChronoUnit.DAYS.between(today, inv.getDueDate()));
                }

                if (isOverdue) {
                    outstandingOverdue = outstandingOverdue.add(balance);
                } else if (inv.getDueDate() != null && !inv.getDueDate().isAfter(dueSoonThreshold)) {
                    outstandingDueSoon = outstandingDueSoon.add(balance);
                }

                String clientName = inv.getClientId() != null && clientMap.containsKey(inv.getClientId())
                        ? clientMap.get(inv.getClientId()).getDisplayName()
                        : "Unknown Client";

                outstandingList.add(OutstandingInvoiceDto.builder()
                        .invoiceId(inv.getId())
                        .invoiceNumber(inv.getInvoiceNumber())
                        .clientId(inv.getClientId())
                        .clientName(clientName)
                        .invoiceDate(inv.getInvoiceDate())
                        .dueDate(inv.getDueDate())
                        .totalAmount(total)
                        .paidAmount(paid)
                        .balanceDue(balance)
                        .status(inv.getStatus() != null ? inv.getStatus().name() : "ISSUED")
                        .daysDueOrOverdue(days)
                        .isOverdue(isOverdue)
                        .build());
            }
        }

        // Sort outstanding invoices by overdue first, then largest balance due
        outstandingList.sort((a, b) -> {
            if (a.isOverdue() != b.isOverdue()) {
                return a.isOverdue() ? -1 : 1;
            }
            return b.getBalanceDue().compareTo(a.getBalanceDue());
        });

        // Collections this month and this quarter
        LocalDate startOfMonth = today.withDayOfMonth(1);
        int currentMonth = today.getMonthValue();
        int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
        LocalDate startOfQuarter = today.withMonth(quarterStartMonth).withDayOfMonth(1);

        BigDecimal collectedThisMonth = BigDecimal.ZERO;
        BigDecimal collectedThisQuarter = BigDecimal.ZERO;
        long totalPaymentsCount = 0;

        List<InvoicePaymentEntity> payments = invoicePaymentRepository.findAll();
        for (InvoicePaymentEntity pay : payments) {
            if (organizationId.equals(pay.getOrganizationId()) && pay.getPaymentDate() != null && pay.getAmount() != null) {
                totalPaymentsCount++;
                if (!pay.getPaymentDate().isBefore(startOfMonth)) {
                    collectedThisMonth = collectedThisMonth.add(pay.getAmount());
                }
                if (!pay.getPaymentDate().isBefore(startOfQuarter)) {
                    collectedThisQuarter = collectedThisQuarter.add(pay.getAmount());
                }
            }
        }

        return FinancialReportDto.builder()
                .hasBillingAccess(true)
                .totalInvoiced(totalInvoiced)
                .totalCollected(totalCollected)
                .totalOutstanding(totalOutstanding)
                .outstandingDueSoon(outstandingDueSoon)
                .outstandingOverdue(outstandingOverdue)
                .totalInvoices(totalInvoices)
                .draftInvoices(draftInvoices)
                .issuedInvoices(issuedInvoices)
                .partiallyPaidInvoices(partiallyPaidInvoices)
                .paidInvoices(paidInvoices)
                .overdueInvoices(overdueInvoices)
                .cancelledInvoices(cancelledInvoices)
                .invoicesByStatus(invoicesByStatus)
                .totalPaymentsCount(totalPaymentsCount)
                .collectedThisMonth(collectedThisMonth)
                .collectedThisQuarter(collectedThisQuarter)
                .outstandingInvoices(outstandingList)
                .build();
    }

    private static long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
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
}
