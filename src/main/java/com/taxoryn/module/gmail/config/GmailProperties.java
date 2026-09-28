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
    private String authUrl = "https://accounts.google.com/o/oauth2/v2/auth";
    private String tokenUrl = "https://oauth2.googleapis.com/token";
    private String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
    private String apiBaseUrl = "https://gmail.googleapis.com/gmail/v1";
    private String scope = "https://www.googleapis.com/auth/gmail.metadata https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile";
    private int syncBatchSize = 50;
    private int defaultSlaHours = 24;
}
