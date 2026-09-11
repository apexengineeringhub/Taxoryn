package com.taxoryn.module.notification.email.template;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailTemplateType {

    WELCOME_PRACTITIONER("Welcome to Taxoryn — Your Tax Practice Workspace is Ready!"),
    WELCOME_INDIVIDUAL("Welcome to Taxoryn — Your Account is Ready!"),
    INVOICE_ISSUED("New Invoice from {{organizationName}}"),
    PAYMENT_RECEIVED("Payment Receipt for Invoice {{invoiceNumber}}"),
    INVOICE_REMINDER("Reminder: Pending Payment for Invoice {{invoiceNumber}}"),
    PASSWORD_RESET("Reset Your Taxoryn Account Password"),
    DOCUMENT_REQUEST("Documents Required — {{purpose}}"),
    DOCUMENT_REMINDER("Reminder: Documents Required — {{purpose}}"),
    DOCUMENT_REJECTED("Action Required: Document Needs Correction — {{purpose}}"),
    ORGANIZATION_ACTIVATION("Activate Your Taxoryn Practice Account"),
    EMPLOYEE_INVITATION("You've been invited to join {{practiceName}} on Taxoryn"),
    CLIENT_PORTAL_INVITATION("Activate Your Taxoryn Client Portal — {{practiceName}}"),
    CLIENT_PORTAL_SUSPENDED("Your Taxoryn Portal Access Has Been Suspended"),
    CLIENT_PORTAL_RESTORED("Your Taxoryn Portal Access Has Been Restored"),
    CLIENT_PORTAL_DEACTIVATED("Your Taxoryn Portal Access Is No Longer Active"),
    CUSTOMER_EMAIL_VERIFICATION("Welcome to Taxoryn — Verify Your Email");

    private final String defaultSubject;
}
