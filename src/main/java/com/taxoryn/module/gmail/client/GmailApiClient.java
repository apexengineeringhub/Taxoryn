package com.taxoryn.module.gmail.client;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.module.gmail.config.GmailProperties;
import com.taxoryn.module.gmail.dto.GmailThreadModels;
import com.taxoryn.module.gmail.dto.GoogleOAuthTokenResponse;
import com.taxoryn.module.gmail.dto.GoogleUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmailApiClient {

    private final GmailProperties properties;
    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }

    public GoogleOAuthTokenResponse exchangeAuthCode(String authCode, String redirectUri) {
        String effectiveRedirectUri = StringUtils.hasText(redirectUri) ? redirectUri : properties.getRedirectUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", authCode);
        params.add("client_id", properties.getClientId());
        params.add("client_secret", properties.getClientSecret());
        params.add("redirect_uri", effectiveRedirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<GoogleOAuthTokenResponse> response = restTemplate.postForEntity(
                    properties.getTokenUrl(),
                    request,
                    GoogleOAuthTokenResponse.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.error("Google OAuth token exchange failed: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BadRequestException("Failed to authenticate with Google OAuth: " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Error connecting to Google OAuth endpoint: {}", ex.getMessage());
            throw new BadRequestException("Failed to reach Google OAuth service: " + ex.getMessage());
        }
    }

    public GoogleOAuthTokenResponse refreshAccessToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("refresh_token", refreshToken);
        params.add("client_id", properties.getClientId());
        params.add("client_secret", properties.getClientSecret());
        params.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<GoogleOAuthTokenResponse> response = restTemplate.postForEntity(
                    properties.getTokenUrl(),
                    request,
                    GoogleOAuthTokenResponse.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.error("Google OAuth token refresh failed: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BadRequestException("Failed to refresh Google OAuth token: " + ex.getStatusCode());
        }
    }

    public GoogleUserInfoResponse getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GoogleUserInfoResponse> response = restTemplate.exchange(
                    properties.getUserInfoUrl(),
                    HttpMethod.GET,
                    entity,
                    GoogleUserInfoResponse.class
            );
            return response.getBody();
        } catch (Exception ex) {
            log.error("Failed to fetch Google user info: {}", ex.getMessage());
            throw new BadRequestException("Failed to fetch Google profile information");
        }
    }

    public GmailThreadModels.ThreadListResponse listThreads(String accessToken, String query, String pageToken, int maxResults) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        StringBuilder url = new StringBuilder(properties.getApiBaseUrl())
                .append("/users/me/threads?maxResults=")
                .append(maxResults > 0 ? maxResults : properties.getSyncBatchSize());

        if (StringUtils.hasText(query)) {
            url.append("&q=").append(query);
        }
        if (StringUtils.hasText(pageToken)) {
            url.append("&pageToken=").append(pageToken);
        }

        try {
            ResponseEntity<GmailThreadModels.ThreadListResponse> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    entity,
                    GmailThreadModels.ThreadListResponse.class
            );
            return response.getBody();
        } catch (Exception ex) {
            log.error("Failed to list threads from Gmail API: {}", ex.getMessage());
            throw new BadRequestException("Failed to list Gmail threads: " + ex.getMessage());
        }
    }

    public GmailThreadModels.ThreadDetail getThreadMetadata(String accessToken, String threadId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        String url = String.format("%s/users/me/threads/%s?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=To&metadataHeaders=Date",
                properties.getApiBaseUrl(),
                threadId);

        try {
            ResponseEntity<GmailThreadModels.ThreadDetail> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    GmailThreadModels.ThreadDetail.class
            );
            return response.getBody();
        } catch (Exception ex) {
            log.error("Failed to get thread metadata for threadId={}: {}", threadId, ex.getMessage());
            throw new BadRequestException("Failed to retrieve thread metadata: " + ex.getMessage());
        }
    }

    public GmailThreadModels.HistoryResponse listHistory(String accessToken, String startHistoryId, String pageToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        StringBuilder url = new StringBuilder(properties.getApiBaseUrl())
                .append("/users/me/history?startHistoryId=")
                .append(startHistoryId);

        if (StringUtils.hasText(pageToken)) {
            url.append("&pageToken=").append(pageToken);
        }

        try {
            ResponseEntity<GmailThreadModels.HistoryResponse> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    entity,
                    GmailThreadModels.HistoryResponse.class
            );
            return response.getBody();
        } catch (Exception ex) {
            log.warn("Failed to retrieve history for startHistoryId={}: {}", startHistoryId, ex.getMessage());
            return null;
        }
    }
}
