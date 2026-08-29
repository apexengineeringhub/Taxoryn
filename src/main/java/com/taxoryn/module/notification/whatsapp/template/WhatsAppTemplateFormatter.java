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
