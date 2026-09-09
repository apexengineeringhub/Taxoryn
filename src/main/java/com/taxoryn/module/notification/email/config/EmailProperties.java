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
    private boolean enabled = true;

    /**
     * Email provider: AUTO, SMTP, RESEND, BREVO, or LOG.
     */
    private String provider = "AUTO";

    /**
     * From email address.
     */
    private String fromEmail = "info@taxoryn.com";

    /**
     * Optional alias for from email address (supports MAIL_FROM_ADDRESS / taxoryn.mail.from-address).
     */
    private String fromAddress;

    /**
     * Reply-To email address.
     */
    private String replyTo = "info@taxoryn.com";

    /**
     * From display name.
     */
    private String fromName = "Taxoryn";

    /**
     * Explicit development email mode for testing unverified domains.
     * When true and devRecipient is configured, emails are redirected to devRecipient.
     * Must never be enabled in production.
     */
    private boolean devMode = false;

    /**
     * Target recipient in development testing mode (e.g. verified Resend account owner email).
     */
    private String devRecipient;

    public String getFromEmail() {
        if (org.springframework.util.StringUtils.hasText(fromAddress)) {
            return fromAddress.trim();
        }
        if (org.springframework.util.StringUtils.hasText(fromEmail)) {
            return fromEmail.trim();
        }
        return "info@taxoryn.com";
    }

    public String getFromAddress() {
        return getFromEmail();
    }

    public String getReplyTo() {
        if (org.springframework.util.StringUtils.hasText(replyTo)) {
            return replyTo.trim();
        }
        return getFromEmail();
    }

    public String getFromName() {
        if (org.springframework.util.StringUtils.hasText(fromName)) {
            return fromName.trim();
        }
        return "Taxoryn";
    }

    /**
     * Centralized Frontend base URL.
     */
    private String frontendUrl = "http://localhost:5173";

    /**
     * Application login portal URL.
     */
    private String loginUrl;

    /**
     * Application activation URL.
     */
    private String activationUrl;

    public String getFrontendUrl() {
        if (org.springframework.util.StringUtils.hasText(frontendUrl)) {
            return frontendUrl.trim().replaceAll("/+$", "");
        }
        return "http://localhost:5173";
    }

    public String getLoginUrl() {
        if (org.springframework.util.StringUtils.hasText(loginUrl)) {
            return loginUrl.trim();
        }
        return getFrontendUrl() + "/login";
    }

    public String getActivationUrl() {
        if (org.springframework.util.StringUtils.hasText(activationUrl)) {
            return activationUrl.trim();
        }
        return getFrontendUrl() + "/activate";
    }

    /**
     * Organization activation token expiration in hours.
     */
    private long activationExpirationHours = 24;

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
