package com.taxoryn.module.notification.scheduler;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Background jobs that scan open work items nearing or past their due date and raise the
 * corresponding notification (TASK_DUE / TASK_OVERDUE / GST_DUE / ITR_DUE / PAYMENT_DUE).
 * <p>
 * Follows the same per-tenant iteration pattern as {@code ComplianceScheduler}: each active
 * organization is processed under its own {@code TenantContext} so every downstream repository
 * call and notification stays correctly tenant-scoped.
 */
import com.taxoryn.module.tds.entity.TdsReturnEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.repository.TdsReturnRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final Set<TaskStatus> CLOSED_TASK_STATUSES = Set.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED);
    private static final Set<GstFilingStatus> CLOSED_GST_STATUSES = Set.of(GstFilingStatus.FILED, GstFilingStatus.CANCELLED);
    private static final Set<ItrStatus> CLOSED_ITR_STATUSES = Set.of(ItrStatus.FILED, ItrStatus.COMPLETED, ItrStatus.CANCELLED);
    private static final Set<TdsFilingStatus> CLOSED_TDS_STATUSES = Set.of(TdsFilingStatus.FILED, TdsFilingStatus.CANCELLED);
    private static final int DUE_SOON_WINDOW_DAYS = 3;

    private final OrganizationRepository organizationRepository;
    private final TaskRepository taskRepository;
    private final GstReturnFilingRepository gstReturnFilingRepository;
    private final ItrReturnRepository itrReturnRepository;
    private final TdsReturnRepository tdsReturnRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;
    private final com.taxoryn.module.docrequest.repository.DocumentRequestRepository documentRequestRepository;
    private final com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository documentRequestItemRepository;
    private final com.taxoryn.module.notification.repository.NotificationRepository notificationRepository;
    private final com.taxoryn.module.notification.email.service.EmailNotificationService emailNotificationService;
    private final com.taxoryn.module.employee.repository.EmployeeRepository employeeRepository;
    private final com.taxoryn.module.user.repository.UserRepository userRepository;

    /**
     * Runs daily at 07:00 AM, ahead of the working day, covering tasks due today, tasks already
     * overdue, GST/ITR filings due within the reminder window, overdue invoices, and client document follow-ups.
     */
    @Scheduled(cron = "0 0 7 * * ?")
    public void runDailyReminders() {
        log.info("Running daily notification reminder scan");
        List<OrganizationEntity> activeOrgs = organizationRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrganizationStatus.ACTIVE)
                .toList();

        for (OrganizationEntity org : activeOrgs) {
            try {
                TenantContext.setTenantId(org.getId());
                remindDueTasks(org);
                remindOverdueTasks(org);
                remindDueGstFilings(org);
                remindDueItrReturns(org);
                remindDueTdsReturns(org);
                remindOverdueInvoices(org);
                remindClientDocumentRequests(org);
            } catch (Exception ex) {
                log.error("Notification reminder scan failed for organization {}: {}", org.getId(), ex.getMessage(), ex);
            } finally {
                TenantContext.clear();
            }
        }
        log.info("Daily notification reminder scan completed for {} organization(s)", activeOrgs.size());
    }

    private void remindDueTasks(OrganizationEntity org) {
        List<TaskEntity> dueToday = taskRepository.findAllByOrganizationIdAndDueDateAndStatusNotIn(
                org.getId(), LocalDate.now(), CLOSED_TASK_STATUSES);

        for (TaskEntity task : dueToday) {
            if (task.getAssignedTo() == null) {
                continue;
            }
            notificationService.notify(
                    org.getId(), task.getAssignedTo(), null,
                    NotificationType.TASK_DUE,
                    "Task Due Today: " + task.getTitle(),
                    "The task \"" + task.getTitle() + "\" is due today.",
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    "/tasks/" + task.getId(),
                    "{\"taskId\":\"" + task.getId() + "\"}"
            );
        }
    }

    private void remindOverdueTasks(OrganizationEntity org) {
        List<TaskEntity> overdue = taskRepository.findAllByOrganizationIdAndDueDateBeforeAndStatusNotIn(
                org.getId(), LocalDate.now(), CLOSED_TASK_STATUSES);

        for (TaskEntity task : overdue) {
            if (task.getAssignedTo() == null) {
                continue;
            }
            notificationService.notify(
                    org.getId(), task.getAssignedTo(), null,
                    NotificationType.TASK_OVERDUE,
                    "Task Overdue: " + task.getTitle(),
                    "The task \"" + task.getTitle() + "\" was due on " + task.getDueDate() + " and is now overdue.",
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    "/tasks/" + task.getId(),
                    "{\"taskId\":\"" + task.getId() + "\"}"
            );
        }
    }

    private void remindDueGstFilings(OrganizationEntity org) {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(DUE_SOON_WINDOW_DAYS);

        List<GstReturnFilingEntity> dueSoon = gstReturnFilingRepository.findAllByOrganizationIdAndDueDateBetweenAndFilingStatusNotIn(
                org.getId(), from, to, CLOSED_GST_STATUSES);

        for (GstReturnFilingEntity filing : dueSoon) {
            if (filing.getAssignedEmployeeId() == null) {
                continue;
            }
            notificationService.notify(
                    org.getId(), filing.getAssignedEmployeeId(), null,
                    NotificationType.GST_DUE,
                    "GST Filing Due: " + filing.getReturnType() + " (" + filing.getReturnPeriod() + ")",
                    "The " + filing.getReturnType() + " return for period " + filing.getReturnPeriod() +
                            " is due on " + filing.getDueDate() + ".",
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    "/gst/filings/" + filing.getId(),
                    "{\"gstFilingId\":\"" + filing.getId() + "\"}"
            );
        }
    }

    private void remindDueItrReturns(OrganizationEntity org) {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(DUE_SOON_WINDOW_DAYS);

        List<ItrReturnEntity> dueSoon = itrReturnRepository.findAllByOrganizationIdAndDueDateBetweenAndStatusNotIn(
                org.getId(), from, to, CLOSED_ITR_STATUSES);

        for (ItrReturnEntity itr : dueSoon) {
            if (itr.getAssignedEmployeeId() == null) {
                continue;
            }
            notificationService.notify(
                    org.getId(), itr.getAssignedEmployeeId(), null,
                    NotificationType.ITR_DUE,
                    "ITR Filing Due: " + itr.getItrType() + " (" + itr.getAssessmentYear() + ")",
                    "The " + itr.getItrType() + " return for assessment year " + itr.getAssessmentYear() +
                            " is due on " + itr.getDueDate() + ".",
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    "/itr/returns/" + itr.getId(),
                    "{\"itrReturnId\":\"" + itr.getId() + "\"}"
            );
        }
    }

    private void remindDueTdsReturns(OrganizationEntity org) {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(DUE_SOON_WINDOW_DAYS);

        List<TdsReturnEntity> dueSoon = tdsReturnRepository.findAllByOrganizationIdAndDueDateBetweenAndFilingStatusNotIn(
                org.getId(), from, to, CLOSED_TDS_STATUSES);

        for (TdsReturnEntity tds : dueSoon) {
            if (tds.getAssignedEmployeeId() == null) {
                continue;
            }
            notificationService.notify(
                    org.getId(), tds.getAssignedEmployeeId(), null,
                    NotificationType.TDS_DUE,
                    "TDS Return Due: " + tds.getFormType() + " (" + tds.getQuarter() + " FY " + tds.getFinancialYear() + ")",
                    "The " + tds.getFormType() + " return for " + tds.getQuarter() + " FY " + tds.getFinancialYear() +
                            " is due on " + tds.getDueDate() + ".",
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    "/tds/returns/" + tds.getId(),
                    "{\"tdsReturnId\":\"" + tds.getId() + "\"}"
            );
        }
    }

    private void remindOverdueInvoices(OrganizationEntity org) {
        List<InvoiceEntity> overdueInvoices = invoiceRepository.findOverdueIssuedInvoices(org.getId(), LocalDate.now());

        for (InvoiceEntity invoice : overdueInvoices) {
            // Nudge the client directly.
            notificationService.notify(
                    org.getId(), null, invoice.getClientId(),
                    NotificationType.PAYMENT_DUE,
                    "Payment Overdue: Invoice " + invoice.getInvoiceNumber(),
                    "Invoice " + invoice.getInvoiceNumber() + " was due on " + invoice.getDueDate() +
                            " and has a balance of " + invoice.getBalanceDue() + " outstanding.",
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    "/invoices/" + invoice.getId(),
                    "{\"invoiceId\":\"" + invoice.getId() + "\"}"
            );

            // Also flag it internally to whoever manages the client relationship, if assigned.
            clientRepository.findByIdAndOrganizationId(invoice.getClientId(), org.getId())
                    .map(ClientEntity::getAssignedEmployeeId)
                    .ifPresent(assignedEmployeeId -> notificationService.notify(
                            org.getId(), assignedEmployeeId, null,
                            NotificationType.PAYMENT_DUE,
                            "Client Payment Overdue: Invoice " + invoice.getInvoiceNumber(),
                            "Invoice " + invoice.getInvoiceNumber() + " for your client is overdue since " + invoice.getDueDate() + ".",
                            Set.of(NotificationChannel.IN_APP),
                            "/invoices/" + invoice.getId(),
                            "{\"invoiceId\":\"" + invoice.getId() + "\"}"
                    ));
        }
    }

    private void remindClientDocumentRequests(OrganizationEntity org) {
        LocalDate today = LocalDate.now();
        java.time.Instant todayStart = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

        List<com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus> activeStatuses = List.of(
                com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.SENT,
                com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.PARTIALLY_COMPLETED
        );

        List<com.taxoryn.module.docrequest.entity.DocumentRequestEntity> activeRequests =
                documentRequestRepository.findAllByOrganizationIdAndStatusIn(org.getId(), activeStatuses);

        for (com.taxoryn.module.docrequest.entity.DocumentRequestEntity req : activeRequests) {
            if (req.getDueDate() == null) {
                continue;
            }

            long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, req.getDueDate());
            ClientEntity client = clientRepository.findByIdAndOrganizationId(req.getClientId(), org.getId()).orElse(null);
            if (client == null) {
                continue;
            }

            List<String> pendingItemTitles = documentRequestItemRepository.findAllByRequestIdOrderByCreatedAtAsc(req.getId()).stream()
                    .filter(i -> i.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.PENDING
                            || i.getStatus() == com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.REJECTED)
                    .map(com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity::getTitle)
                    .toList();

            // Tier 1: Due - 3 days
            if (daysUntilDue == 3) {
                boolean alreadyNotified = notificationRepository.existsByOrganizationIdAndEntityTypeAndEntityIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                        org.getId(), "DOCUMENT_REQUEST", req.getId().toString(), NotificationType.DOCUMENT_REMINDER, todayStart);

                if (!alreadyNotified) {
                    notificationService.notify(
                            org.getId(), null, client.getId(),
                            NotificationType.DOCUMENT_REMINDER,
                            "Reminder: Documents Required for " + req.getPurpose(),
                            "Please upload the required documents for " + req.getPurpose() + " (Due on " + req.getDueDate() + " - in 3 days).",
                            Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                            "/portal?tab=documents",
                            "{\"requestId\":\"" + req.getId() + "\",\"requestNumber\":\"" + req.getRequestNumber() + "\"}"
                    );
                    if (StringUtils.hasText(client.getEmail())) {
                        try {
                            emailNotificationService.sendDocumentReminderEmail(
                                    client.getEmail(), client.getDisplayName(), req.getPurpose(), org.getName(), req.getDueDate(), pendingItemTitles);
                        } catch (Exception e) {
                            log.warn("Failed to send 3-day reminder email to {}: {}", client.getEmail(), e.getMessage());
                        }
                    }
                }
            }
            // Tier 2: Due - 1 day (Tomorrow)
            else if (daysUntilDue == 1) {
                boolean alreadyNotified = notificationRepository.existsByOrganizationIdAndEntityTypeAndEntityIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                        org.getId(), "DOCUMENT_REQUEST", req.getId().toString(), NotificationType.DOCUMENT_REMINDER, todayStart);

                if (!alreadyNotified) {
                    notificationService.notify(
                            org.getId(), null, client.getId(),
                            NotificationType.DOCUMENT_REMINDER,
                            "Urgent Reminder: Documents Due Tomorrow for " + req.getPurpose(),
                            "Please upload your documents for " + req.getPurpose() + " (Due tomorrow, " + req.getDueDate() + ").",
                            Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                            "/portal?tab=documents",
                            "{\"requestId\":\"" + req.getId() + "\",\"requestNumber\":\"" + req.getRequestNumber() + "\"}"
                    );
                    if (StringUtils.hasText(client.getEmail())) {
                        try {
                            emailNotificationService.sendDocumentReminderEmail(
                                    client.getEmail(), client.getDisplayName(), req.getPurpose(), org.getName(), req.getDueDate(), pendingItemTitles);
                        } catch (Exception e) {
                            log.warn("Failed to send 1-day reminder email to {}: {}", client.getEmail(), e.getMessage());
                        }
                    }
                }
            }
            // Tier 3: Due Today
            else if (daysUntilDue == 0) {
                boolean alreadyNotified = notificationRepository.existsByOrganizationIdAndEntityTypeAndEntityIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                        org.getId(), "DOCUMENT_REQUEST", req.getId().toString(), NotificationType.DOCUMENT_DUE_TODAY, todayStart);

                if (!alreadyNotified) {
                    notificationService.notify(
                            org.getId(), null, client.getId(),
                            NotificationType.DOCUMENT_DUE_TODAY,
                            "Due Today: Documents Required for " + req.getPurpose(),
                            "Required documents for " + req.getPurpose() + " are due today (" + req.getDueDate() + "). Please upload as soon as possible.",
                            Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                            "/portal?tab=documents",
                            "{\"requestId\":\"" + req.getId() + "\",\"requestNumber\":\"" + req.getRequestNumber() + "\"}"
                    );
                    if (StringUtils.hasText(client.getEmail())) {
                        try {
                            emailNotificationService.sendDocumentReminderEmail(
                                    client.getEmail(), client.getDisplayName(), req.getPurpose(), org.getName(), req.getDueDate(), pendingItemTitles);
                        } catch (Exception e) {
                            log.warn("Failed to send due-today reminder email to {}: {}", client.getEmail(), e.getMessage());
                        }
                    }
                }
            }
            // Tier 4: Overdue
            else if (daysUntilDue < 0) {
                boolean alreadyNotified = notificationRepository.existsByOrganizationIdAndEntityTypeAndEntityIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                        org.getId(), "DOCUMENT_REQUEST", req.getId().toString(), NotificationType.DOCUMENT_OVERDUE, todayStart);

                if (!alreadyNotified) {
                    // 1. Client notification
                    notificationService.notify(
                            org.getId(), null, client.getId(),
                            NotificationType.DOCUMENT_OVERDUE,
                            "Overdue: Documents Required for " + req.getPurpose(),
                            "The document submission for " + req.getPurpose() + " was due on " + req.getDueDate() + " and is now overdue. Please upload the remaining files.",
                            Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                            "/portal?tab=documents",
                            "{\"requestId\":\"" + req.getId() + "\",\"requestNumber\":\"" + req.getRequestNumber() + "\"}"
                    );
                    if (StringUtils.hasText(client.getEmail())) {
                        try {
                            emailNotificationService.sendDocumentReminderEmail(
                                    client.getEmail(), client.getDisplayName(), req.getPurpose() + " (OVERDUE)", org.getName(), req.getDueDate(), pendingItemTitles);
                        } catch (Exception e) {
                            log.warn("Failed to send overdue reminder email to {}: {}", client.getEmail(), e.getMessage());
                        }
                    }

                    // 2. Practitioner Alert
                    UUID assignedUserId = req.getRequestedByUserId();
                    if (assignedUserId == null && client.getAssignedEmployeeId() != null) {
                        assignedUserId = employeeRepository.findByIdAndOrganizationId(client.getAssignedEmployeeId(), org.getId())
                                .map(com.taxoryn.module.employee.entity.EmployeeEntity::getUserId)
                                .orElse(null);
                    }

                    if (assignedUserId != null) {
                        notificationService.notify(
                                org.getId(), assignedUserId, null,
                                NotificationType.DOCUMENT_OVERDUE,
                                "Client Action Overdue: " + client.getDisplayName(),
                                "Client " + client.getDisplayName() + " has overdue document submission for " + req.getPurpose() + " (Due: " + req.getDueDate() + ").",
                                Set.of(NotificationChannel.IN_APP),
                                "/documents",
                                "{\"requestId\":\"" + req.getId() + "\",\"clientId\":\"" + client.getId() + "\"}"
                        );
                    }

                    // 3. Manager Escalation if overdue > 3 days
                    if (daysUntilDue <= -3) {
                        List<com.taxoryn.module.user.entity.UserEntity> orgAdmins = userRepository.findAllByOrganizationId(org.getId()).stream()
                                .filter(u -> u.getRoles() != null && u.getRoles().stream().anyMatch(r -> "ORG_ADMIN".equals(r.getCode()) || "PARTNER".equals(r.getCode())))
                                .toList();

                        for (com.taxoryn.module.user.entity.UserEntity admin : orgAdmins) {
                            notificationService.notify(
                                    org.getId(), admin.getId(), null,
                                    NotificationType.DOCUMENT_OVERDUE,
                                    "Escalation: Client Documents Overdue > 3 Days (" + client.getDisplayName() + ")",
                                    "Client " + client.getDisplayName() + " has pending document submission for " + req.getPurpose() + " overdue since " + req.getDueDate() + ".",
                                    Set.of(NotificationChannel.IN_APP),
                                    "/documents",
                                    "{\"requestId\":\"" + req.getId() + "\",\"clientId\":\"" + client.getId() + "\"}"
                            );
                        }
                    }
                }
            }
        }
    }
}
