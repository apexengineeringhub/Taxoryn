package com.taxoryn.module.notification.whatsapp.template;

public enum WhatsAppTemplateType {
    WELCOME_PRACTITIONER("welcome_practitioner"),
    WELCOME_INDIVIDUAL("welcome_individual"),
    INVOICE_ISSUED("invoice_issued"),
    PAYMENT_RECEIVED("payment_received"),
    INVOICE_REMINDER("invoice_reminder");

    private final String defaultTemplateName;

    WhatsAppTemplateType(String defaultTemplateName) {
        this.defaultTemplateName = defaultTemplateName;
    }

    public String getDefaultTemplateName() {
        return defaultTemplateName;
    }
}
