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

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Background jobs that scan open work items nearing or past their due date and raise the
 * corresponding notification (TASK_DUE / TASK_OVERDUE / GST_DUE / ITR_DUE / PAYMENT_DUE).
 * <p>
 * Follows the same per-tenant iteration pattern as {@code ComplianceScheduler}: each active
 * organization is processed under its own {@code TenantContext} so every downstream repository
 * call and notification stays correctly tenant-scoped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final Set<TaskStatus> CLOSED_TASK_STATUSES = Set.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED);
    private static final Set<GstFilingStatus> CLOSED_GST_STATUSES = Set.of(GstFilingStatus.FILED, GstFilingStatus.CANCELLED);
    private static final Set<ItrStatus> CLOSED_ITR_STATUSES = Set.of(ItrStatus.FILED, ItrStatus.COMPLETED, ItrStatus.CANCELLED);
    private static final int DUE_SOON_WINDOW_DAYS = 3;

    private final OrganizationRepository organizationRepository;
    private final TaskRepository taskRepository;
    private final GstReturnFilingRepository gstReturnFilingRepository;
    private final ItrReturnRepository itrReturnRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;

    /**
     * Runs daily at 07:00 AM, ahead of the working day, covering tasks due today, tasks already
     * overdue, GST/ITR filings due within the reminder window, and overdue invoices.
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
                remindOverdueInvoices(org);
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
}
