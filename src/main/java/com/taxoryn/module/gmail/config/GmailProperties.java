package com.taxoryn.module.gmail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "taxoryn.gmail")
public class GmailProperties {

    private boolean enabled = true;
    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "";
    private int defaultSlaHours = 24;

    private SyncConfig sync = new SyncConfig();
    private ApiConfig api = new ApiConfig();
    private OAuthConfig oauth = new OAuthConfig();
    private RetryConfig retry = new RetryConfig();
    private HealthConfig health = new HealthConfig();
    private ChannelConfig channels = new ChannelConfig();

    @Data
    public static class SyncConfig {
        private boolean enabled = true;
        private String cron = "0 */15 * * * ?";
        private int batchSize = 50;
    }

    @Data
    public static class ApiConfig {
        private String baseUrl = "https://gmail.googleapis.com/gmail/v1";
        private int connectTimeoutMs = 8000;
        private int readTimeoutMs = 15000;
        private int maxResults = 50;
    }

    @Data
    public static class OAuthConfig {
        private String authUrl = "https://accounts.google.com/o/oauth2/v2/auth";
        private String tokenUrl = "https://oauth2.googleapis.com/token";
        private String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
        private String scope = "https://www.googleapis.com/auth/gmail.metadata https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile";
    }

    @Data
    public static class RetryConfig {
        private boolean enabled = true;
        private int maxAttempts = 3;
        private long backoffMs = 1000;
    }

    @Data
    public static class HealthConfig {
        private boolean enabled = true;
    }

    @Data
    public static class ChannelConfig {
        private ChannelInfo info = new ChannelInfo(true, "info@taxoryn.com");
        private ChannelInfo support = new ChannelInfo(true, "support@taxoryn.com");
        private ChannelInfo admin = new ChannelInfo(true, "admin@taxoryn.com");
    }

    @Data
    public static class ChannelInfo {
        private boolean enabled;
        private String email;

        public ChannelInfo() {
            this.enabled = true;
            this.email = "";
        }

        public ChannelInfo(boolean enabled, String email) {
            this.enabled = enabled;
            this.email = email;
        }
    }

    // Convenience delegates for backward compatibility
    public String getAuthUrl() { return oauth != null && oauth.getAuthUrl() != null ? oauth.getAuthUrl() : "https://accounts.google.com/o/oauth2/v2/auth"; }
    public String getTokenUrl() { return oauth != null && oauth.getTokenUrl() != null ? oauth.getTokenUrl() : "https://oauth2.googleapis.com/token"; }
    public String getUserInfoUrl() { return oauth != null && oauth.getUserInfoUrl() != null ? oauth.getUserInfoUrl() : "https://www.googleapis.com/oauth2/v2/userinfo"; }
    public String getApiBaseUrl() { return api != null && api.getBaseUrl() != null ? api.getBaseUrl() : "https://gmail.googleapis.com/gmail/v1"; }
    public String getScope() { return oauth != null && oauth.getScope() != null ? oauth.getScope() : "https://www.googleapis.com/auth/gmail.metadata https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile"; }
    public int getSyncBatchSize() { return sync != null ? sync.getBatchSize() : 50; }
}
