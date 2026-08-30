package com.taxoryn.module.notification.email.service;

import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.notification.channel.EmailNotificationSender;
import com.taxoryn.module.notification.email.config.EmailProperties;
import com.taxoryn.module.notification.email.template.EmailTemplateRenderer;
import com.taxoryn.module.notification.email.template.EmailTemplateType;
import com.taxoryn.module.notification.whatsapp.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final EmailNotificationSender emailSender;
    private final EmailTemplateRenderer templateRenderer;
    private final EmailProperties emailProperties;
    private final AuditService auditService;

    @Override
    public void sendWelcomeEmail(UserRegisteredEvent event) {
        if (event == null || !StringUtils.hasText(event.getEmail())) {
            log.warn("Cannot send welcome email: event or recipient email is empty");
            return;
        }

        String recipientEmail = event.getEmail().trim();
        String fullName = buildFullName(event.getFirstName(), event.getLastName());
        boolean isPractitioner = event.getRegistrationType() == UserRegistrationType.PRACTITIONER;

        EmailTemplateType templateType = isPractitioner
                ? EmailTemplateType.WELCOME_PRACTITIONER
                : EmailTemplateType.WELCOME_INDIVIDUAL;

        Map<String, Object> data = new HashMap<>();
        data.put("name", fullName);
        data.put("email", recipientEmail);
        data.put("mobile", StringUtils.hasText(event.getPhone()) ? event.getPhone() : "Not provided");
        data.put("loginUrl", emailProperties.getLoginUrl());

        if (isPractitioner) {
            data.put("practiceName", StringUtils.hasText(event.getOrganizationName()) ? event.getOrganizationName() : "Your Practice");
        }

        String subject = templateRenderer.renderSubject(templateType, data);
        String htmlBody = templateRenderer.renderHtml(templateType, data);

        boolean success = emailSender.sendEmail(recipientEmail, fullName, subject, htmlBody, data);
        log.info("Welcome email dispatch result for {}: success={}, provider={}",
                maskEmail(recipientEmail), success, emailSender.getProviderName());

        auditService.logEvent(
                success ? "WELCOME_EMAIL_SENT" : "WELCOME_EMAIL_FAILED",
                "USER",
                event.getUserId() != null ? event.getUserId().toString() : "UNKNOWN",
                event.getOrganizationId(),
                "Dispatched welcome email to " + maskEmail(recipientEmail) + " via " + emailSender.getProviderName()
        );
    }

    @Override
    public void sendInvoiceIssuedEmail(InvoiceIssuedEvent event) {
        if (event == null || !StringUtils.hasText(event.getClientEmail())) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("clientName", event.getClientName());
        data.put("invoiceNumber", event.getInvoiceNumber());
        data.put("organizationName", event.getOrganizationName());
        data.put("totalAmount", event.getTotalAmount() != null ? event.getTotalAmount().toPlainString() : "0.00");
        data.put("dueDate", event.getDueDate() != null ? event.getDueDate().toString() : "");
        data.put("invoiceUrl", emailProperties.getLoginUrl());

        String subject = templateRenderer.renderSubject(EmailTemplateType.INVOICE_ISSUED, data);
        String htmlBody = templateRenderer.renderHtml(EmailTemplateType.INVOICE_ISSUED, data);

        emailSender.sendEmail(event.getClientEmail(), event.getClientName(), subject, htmlBody, data);
    }

    @Override
    public void sendPaymentReceivedEmail(PaymentReceivedEvent event) {
        if (event == null || !StringUtils.hasText(event.getClientEmail())) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("clientName", event.getClientName());
        data.put("invoiceNumber", event.getInvoiceNumber());
        data.put("organizationName", event.getOrganizationName());
        data.put("amountPaid", event.getAmountPaid() != null ? event.getAmountPaid().toPlainString() : "0.00");
        data.put("remainingBalance", event.getRemainingBalance() != null ? event.getRemainingBalance().toPlainString() : "0.00");
        data.put("paymentReference", event.getPaymentReference() != null ? event.getPaymentReference() : "Direct Payment");

        String subject = templateRenderer.renderSubject(EmailTemplateType.PAYMENT_RECEIVED, data);
        String htmlBody = templateRenderer.renderHtml(EmailTemplateType.PAYMENT_RECEIVED, data);

        emailSender.sendEmail(event.getClientEmail(), event.getClientName(), subject, htmlBody, data);
    }

    @Override
    public void sendInvoiceReminderEmail(InvoiceReminderEvent event) {
        if (event == null || !StringUtils.hasText(event.getClientEmail())) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("clientName", event.getClientName());
        data.put("invoiceNumber", event.getInvoiceNumber());
        data.put("organizationName", event.getOrganizationName());
        data.put("balanceAmount", event.getBalanceAmount() != null ? event.getBalanceAmount().toPlainString() : "0.00");
        data.put("dueDate", event.getDueDate() != null ? event.getDueDate().toString() : "Immediate");
        data.put("invoiceUrl", emailProperties.getLoginUrl());

        String subject = templateRenderer.renderSubject(EmailTemplateType.INVOICE_REMINDER, data);
        String htmlBody = templateRenderer.renderHtml(EmailTemplateType.INVOICE_REMINDER, data);

        emailSender.sendEmail(event.getClientEmail(), event.getClientName(), subject, htmlBody, data);
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetUrl, long expiryMinutes) {
        if (!StringUtils.hasText(recipientEmail)) {
            log.warn("Cannot send password reset email: recipient email is empty");
            return;
        }

        String displayName = StringUtils.hasText(recipientName) ? recipientName.trim() : "Valued Member";
        Map<String, Object> data = new HashMap<>();
        data.put("name", displayName);
        data.put("email", recipientEmail.trim());
        data.put("resetUrl", resetUrl);
        data.put("expiryMinutes", String.valueOf(expiryMinutes));

        String subject = templateRenderer.renderSubject(EmailTemplateType.PASSWORD_RESET, data);
        String htmlBody = templateRenderer.renderHtml(EmailTemplateType.PASSWORD_RESET, data);

        boolean success = emailSender.sendEmail(recipientEmail.trim(), displayName, subject, htmlBody, data);
        log.info("Password reset email dispatch for {}: success={}, provider={}",
                maskEmail(recipientEmail), success, emailSender.getProviderName());
    }

    @Override
    public void sendDocumentRequestEmail(String recipientEmail, String clientName, String purpose, String practiceName, java.time.LocalDate dueDate, String message, java.util.List<String> itemTitles) {
        if (!StringUtils.hasText(recipientEmail)) {
            return;
        }

        StringBuilder itemsListHtml = new StringBuilder();
        if (itemTitles != null && !itemTitles.isEmpty()) {
            for (String t : itemTitles) {
                itemsListHtml.append("<li>").append(t).append("</li>");
            }
        } else {
            itemsListHtml.append("<li>Standard tax compliance documents</li>");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", StringUtils.hasText(clientName) ? clientName.trim() : "Valued Client");
        data.put("purpose", StringUtils.hasText(purpose) ? purpose.trim() : "Tax Preparation");
        data.put("practiceName", StringUtils.hasText(practiceName) ? practiceName.trim() : "Your Tax Consultant");
        data.put("dueDate", dueDate != null ? dueDate.toString() : "Promptly");
        data.put("message", message != null ? message : "");
        data.put("itemsListHtml", itemsListHtml.toString());
        data.put("uploadUrl", emailProperties.getLoginUrl() != null ? emailProperties.getLoginUrl() : "http://localhost:5173/login");

        String subject = templateRenderer.renderSubject(EmailTemplateType.DOCUMENT_REQUEST, data);
        String htmlBody = templateRenderer.renderHtml(EmailTemplateType.DOCUMENT_REQUEST, data);

        emailSender.sendEmail(recipientEmail.trim(), clientName, subject, htmlBody, data);
    }

    @Override
    public void sendDocumentReminderEmail(String recipientEmail, String clientName, String purpose, String practiceName, java.time.LocalDate dueDate, java.util.List<String> pendingItemTitles) {
        if (!StringUtils.hasText(recipientEmail)) {
            return;
        }

        StringBuilder itemsListHtml = new StringBuilder();
        if (pendingItemTitles != null && !pendingItemTitles.isEmpty()) {
            for (String t : pendingItemTitles) {
                itemsListHtml.append("<li>").append(t).append("</li>");
            }
        } else {
            itemsListHtml.append("<li>Pending compliance documents</li>");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", StringUtils.hasText(clientName) ? clientName.trim() : "Valued Client");
        data.put("purpose", StringUtils.hasText(purpose) ? purpose.trim() : "Tax Preparation");
        data.put("practiceName", StringUtils.hasText(practiceName) ? practiceName.trim() : "Your Tax Consultant");
        data.put("dueDate", dueDate != null ? dueDate.toString() : "Immediate");
        data.put("itemsListHtml", itemsListHtml.toString());
        data.put("uploadUrl", emailProperties.getLoginUrl() != null ? emailProperties.getLoginUrl() : "http://localhost:5173/login");

        String subject = templateRenderer.renderSubject(EmailTemplateType.DOCUMENT_REMINDER, data);
        String htmlBody = templateRenderer.renderHtml(EmailTemplateType.DOCUMENT_REMINDER, data);

        emailSender.sendEmail(recipientEmail.trim(), clientName, subject, htmlBody, data);
    }

    @Override
    public void sendDocumentRejectedEmail(String recipientEmail, String clientName, String purpose, String documentTitle, String reason, String practiceName) {
        if (!StringUtils.hasText(recipientEmail)) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", StringUtils.hasText(clientName) ? clientName.trim() : "Valued Client");
        data.put("purpose", StringUtils.hasText(purpose) ? purpose.trim() : "Tax Preparation");
        data.put("documentTitle", StringUtils.hasText(documentTitle) ? documentTitle.trim() : "Document");
        data.put("reason", StringUtils.hasText(reason) ? reason.trim() : "Correction required by practitioner");
        data.put("practiceName", StringUtils.hasText(practiceName) ? practiceName.trim() : "Your Tax Consultant");
        data.put("uploadUrl", emailProperties.getLoginUrl() != null ? emailProperties.getLoginUrl() : "http://localhost:5173/login");

        String subject = templateRenderer.renderSubject(EmailTemplateType.DOCUMENT_REJECTED, data);
        String htmlBody = templateRenderer.renderHtml(EmailTemplateType.DOCUMENT_REJECTED, data);

        emailSender.sendEmail(recipientEmail.trim(), clientName, subject, htmlBody, data);
    }

    private String buildFullName(String firstName, String lastName) {
        if (StringUtils.hasText(firstName) && StringUtils.hasText(lastName)) {
            return firstName.trim() + " " + lastName.trim();
        } else if (StringUtils.hasText(firstName)) {
            return firstName.trim();
        } else if (StringUtils.hasText(lastName)) {
            return lastName.trim();
        }
        return "Valued Member";
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String maskedLocal = local.length() <= 2 ? local : local.substring(0, 2) + "****";
        return maskedLocal + "@" + parts[1];
    }
}
