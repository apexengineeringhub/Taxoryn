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
    PASSWORD_RESET("Reset Your Taxoryn Account Password");

    private final String defaultSubject;
}
