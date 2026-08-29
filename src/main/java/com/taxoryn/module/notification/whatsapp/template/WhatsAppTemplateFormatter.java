package com.taxoryn.module.notification.whatsapp.template;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WhatsAppTemplateFormatter {

    public String format(WhatsAppTemplateType type, Map<String, String> variables) {
        String template = switch (type) {
            case WELCOME_PRACTITIONER -> """
                    Welcome to Taxoryn, {{name}}!

                    Your Taxoryn account has been created successfully.

                    Practice: {{practiceName}}
                    Email: {{email}}
                    Mobile: {{mobile}}

                    You can now access Taxoryn and start managing your tax practice.
                    Login here: {{loginUrl}}

                    Thank you,
                    Taxoryn Team
                    """.stripIndent().trim();

            case WELCOME_INDIVIDUAL -> """
                    Welcome to Taxoryn, {{name}}!

                    Your Taxoryn account has been created successfully.

                    Email: {{email}}
                    Mobile: {{mobile}}

                    You can now use Taxoryn to manage your tax-related information and services.
                    Login here: {{loginUrl}}

                    Thank you,
                    Taxoryn Team
                    """.stripIndent().trim();

            case INVOICE_ISSUED -> """
                    Hello {{clientName}},

                    A new invoice #{{invoiceNumber}} has been issued by {{organizationName}}.

                    Amount Due: {{currency}} {{totalAmount}}
                    Due Date: {{dueDate}}

                    View / Pay your invoice: {{invoiceUrl}}

                    Thank you,
                    {{organizationName}}
                    """.stripIndent().trim();

            case PAYMENT_RECEIVED -> """
                    Hello {{clientName}},

                    We have received your payment of {{currency}} {{amountPaid}} for invoice #{{invoiceNumber}}.

                    Receipt Reference: {{paymentReference}}
                    Remaining Balance: {{currency}} {{remainingBalance}}

                    Thank you for your business!
                    {{organizationName}}
                    """.stripIndent().trim();

            case INVOICE_REMINDER -> """
                    Gentle Reminder: Invoice #{{invoiceNumber}}

                    Hello {{clientName}},

                    This is a reminder that invoice #{{invoiceNumber}} from {{organizationName}} for {{currency}} {{balanceAmount}} was due on {{dueDate}}.

                    Please settle the outstanding balance at your earliest convenience:
                    Payment Link: {{invoiceUrl}}

                    Thank you,
                    {{organizationName}}
                    """.stripIndent().trim();
        };

        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String key = "{{" + entry.getKey() + "}}";
                String val = entry.getValue() != null ? entry.getValue() : "";
                template = template.replace(key, val);
            }
        }

        return template;
    }
}
