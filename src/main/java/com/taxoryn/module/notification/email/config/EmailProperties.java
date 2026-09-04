package com.taxoryn.module.notification.email.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "taxoryn.mail")
public class EmailProperties {

    /**
     * Whether real email dispatch is enabled.
     */
    private boolean enabled = false;

    /**
     * Email provider: LOG or SMTP.
     */
    private String provider = "LOG";

    /**
     * From email address.
     */
    private String fromEmail = "taxoryn@gmail.com";

    /**
     * From display name.
     */
    private String fromName = "Taxoryn";

    /**
     * Application login portal URL.
     */
    private String loginUrl = "https://taxoryn.com/login";

    /**
     * Resend API Key for Port 443 HTTPS email dispatch (Render/Cloud compatible).
     */
    private String resendApiKey;

    /**
     * Brevo (Sendinblue) API Key for Port 443 HTTPS email dispatch.
     */
    private String brevoApiKey;

    /**
     * Generic API key if using custom HTTP relay.
     */
    private String apiKey;
}
