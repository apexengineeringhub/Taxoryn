package com.taxoryn.module.notification.whatsapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "taxoryn.whatsapp")
public class WhatsAppProperties {

    /**
     * Whether WhatsApp notification dispatch is globally enabled.
     */
    private boolean enabled = false;

    /**
     * Provider implementation: LOG, MOCK, META. Default is LOG.
     */
    private String provider = "LOG";

    /**
     * Meta Cloud / Provider API Base URL.
     */
    private String baseUrl = "https://graph.facebook.com/v19.0";

    /**
     * WhatsApp Business Account ID.
     */
    private String businessAccountId;

    /**
     * WhatsApp Phone Number ID (from Meta Developer portal).
     */
    private String phoneNumberId;

    /**
     * Permanent system user access token (or Bearer token).
     */
    private String accessToken;

    /**
     * Application login portal URL used in welcome messages.
     */
    private String loginUrl = "http://localhost:5173/login";

    /**
     * Template language code (e.g. en_US or en). Default is en_US.
     */
    private String languageCode = "en_US";

    /**
     * Optional custom template names mapping.
     */
    private Map<String, String> templates = new HashMap<>();
}
