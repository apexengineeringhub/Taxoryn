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

    public static final String CANONICAL_PRODUCTION_FRONTEND_URL = "https://app.taxoryn.com";
    public static final String CANONICAL_PRODUCTION_LOGIN_URL = "https://app.taxoryn.com/login";
    public static final String CANONICAL_PRODUCTION_ACTIVATION_URL = "https://app.taxoryn.com/activate";
    public static final String CANONICAL_PRODUCTION_RESET_PASSWORD_URL = "https://app.taxoryn.com/reset-password";

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

    /**
     * Application password reset URL.
     */
    private String resetPasswordUrl;

    public String getFrontendUrl() {
        if (org.springframework.util.StringUtils.hasText(frontendUrl)) {
            String trimmed = frontendUrl.trim().replaceAll("/+$", "");
            if (isApexOrTenantDomain(trimmed)) {
                return CANONICAL_PRODUCTION_FRONTEND_URL;
            }
            return trimmed;
        }
        return "http://localhost:5173";
    }

    public String getLoginUrl() {
        if (org.springframework.util.StringUtils.hasText(loginUrl)) {
            String trimmed = loginUrl.trim();
            if (isApexOrTenantDomain(trimmed)) {
                return CANONICAL_PRODUCTION_LOGIN_URL;
            }
            return trimmed;
        }
        return getFrontendUrl() + "/login";
    }

    public String getActivationUrl() {
        if (org.springframework.util.StringUtils.hasText(activationUrl)) {
            String trimmed = activationUrl.trim();
            if (isApexOrTenantDomain(trimmed)) {
                return CANONICAL_PRODUCTION_ACTIVATION_URL;
            }
            return trimmed;
        }
        return getFrontendUrl() + "/activate";
    }

    public String getResetPasswordUrl() {
        if (org.springframework.util.StringUtils.hasText(resetPasswordUrl)) {
            String trimmed = resetPasswordUrl.trim();
            if (isApexOrTenantDomain(trimmed)) {
                return CANONICAL_PRODUCTION_RESET_PASSWORD_URL;
            }
            return trimmed;
        }
        return getFrontendUrl() + "/reset-password";
    }

    private boolean isApexOrTenantDomain(String url) {
        if (!org.springframework.util.StringUtils.hasText(url)) return false;
        String lower = url.toLowerCase();
        if (lower.startsWith("https://taxoryn.com") || lower.startsWith("http://taxoryn.com")) {
            return true;
        }
        if (lower.contains(".taxoryn.com") && !lower.contains("app.taxoryn.com")) {
            return true;
        }
        return false;
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
