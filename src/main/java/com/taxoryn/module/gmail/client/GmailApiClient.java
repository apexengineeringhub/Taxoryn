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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmailApiClient {

    private final GmailProperties properties;

    protected RestTemplate getRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int connectTimeout = (properties != null && properties.getApi() != null)
                ? properties.getApi().getConnectTimeoutMs()
                : 8000;
        int readTimeout = (properties != null && properties.getApi() != null)
                ? properties.getApi().getReadTimeoutMs()
                : 15000;
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
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
            ResponseEntity<GoogleOAuthTokenResponse> response = getRestTemplate().postForEntity(
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
            ResponseEntity<GoogleOAuthTokenResponse> response = getRestTemplate().postForEntity(
                    properties.getTokenUrl(),
                    request,
                    GoogleOAuthTokenResponse.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.error("Google OAuth token refresh failed: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BadRequestException("Failed to refresh Google OAuth token: " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Error refreshing Google OAuth token: {}", ex.getMessage());
            throw new BadRequestException("Failed to reach Google OAuth service: " + ex.getMessage());
        }
    }

    public GoogleUserInfoResponse getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        return executeWithRetry(() -> {
            ResponseEntity<GoogleUserInfoResponse> response = getRestTemplate().exchange(
                    properties.getUserInfoUrl(),
                    HttpMethod.GET,
                    entity,
                    GoogleUserInfoResponse.class
            );
            return response.getBody();
        }, "getUserInfo");
    }

    public GmailThreadModels.ThreadListResponse listThreads(String accessToken, String query, String pageToken, int maxResults) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        int effectiveMax = maxResults > 0 ? maxResults : (properties.getApi() != null ? properties.getApi().getMaxResults() : 50);

        StringBuilder url = new StringBuilder(properties.getApiBaseUrl())
                .append("/users/me/threads?maxResults=")
                .append(effectiveMax);

        if (StringUtils.hasText(query)) {
            url.append("&q=").append(query);
        }
        if (StringUtils.hasText(pageToken)) {
            url.append("&pageToken=").append(pageToken);
        }

        return executeWithRetry(() -> {
            ResponseEntity<GmailThreadModels.ThreadListResponse> response = getRestTemplate().exchange(
                    url.toString(),
                    HttpMethod.GET,
                    entity,
                    GmailThreadModels.ThreadListResponse.class
            );
            return response.getBody();
        }, "listThreads");
    }

    public GmailThreadModels.ThreadDetail getThreadMetadata(String accessToken, String threadId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        String url = String.format("%s/users/me/threads/%s?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=To&metadataHeaders=Date",
                properties.getApiBaseUrl(),
                threadId);

        return executeWithRetry(() -> {
            ResponseEntity<GmailThreadModels.ThreadDetail> response = getRestTemplate().exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    GmailThreadModels.ThreadDetail.class
            );
            return response.getBody();
        }, "getThreadMetadata");
    }

    public GmailThreadModels.ThreadDetail getThreadFull(String accessToken, String threadId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        String url = String.format("%s/users/me/threads/%s?format=full",
                properties.getApiBaseUrl(),
                threadId);

        return executeWithRetry(() -> {
            ResponseEntity<GmailThreadModels.ThreadDetail> response = getRestTemplate().exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    GmailThreadModels.ThreadDetail.class
            );
            return response.getBody();
        }, "getThreadFull");
    }

    public GmailThreadModels.SendMessageResponse sendMessage(String accessToken, String rawBase64Url, String threadId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        GmailThreadModels.SendMessageRequest sendRequest = GmailThreadModels.SendMessageRequest.builder()
                .raw(rawBase64Url)
                .threadId(threadId)
                .build();

        HttpEntity<GmailThreadModels.SendMessageRequest> entity = new HttpEntity<>(sendRequest, headers);
        String url = String.format("%s/users/me/messages/send", properties.getApiBaseUrl());

        return executeWithRetry(() -> {
            ResponseEntity<GmailThreadModels.SendMessageResponse> response = getRestTemplate().exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    GmailThreadModels.SendMessageResponse.class
            );
            return response.getBody();
        }, "sendMessage");
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
            ResponseEntity<GmailThreadModels.HistoryResponse> response = getRestTemplate().exchange(
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

    private <T> T executeWithRetry(Supplier<T> action, String operationName) {
        boolean retryEnabled = properties.getRetry() != null && properties.getRetry().isEnabled();
        int maxAttempts = retryEnabled ? Math.max(1, properties.getRetry().getMaxAttempts()) : 1;
        long backoffMs = properties.getRetry() != null ? properties.getRetry().getBackoffMs() : 1000L;

        int attempt = 0;
        while (attempt < maxAttempts) {
            attempt++;
            try {
                return action.get();
            } catch (HttpStatusCodeException ex) {
                // Do not retry 4xx errors (e.g. 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found),
                // EXCEPT 429 Too Many Requests (Rate Limit).
                if (ex.getStatusCode().is4xxClientError() && ex.getStatusCode() != HttpStatus.TOO_MANY_REQUESTS) {
                    log.error("Non-retryable client error on {} (HTTP {}): {}", operationName, ex.getStatusCode(), ex.getResponseBodyAsString());
                    throw new BadRequestException("Gmail API call failed (" + operationName + "): " + ex.getStatusCode());
                }

                if (attempt >= maxAttempts) {
                    log.error("Max retry attempts reached for {}. HTTP {}: {}", operationName, ex.getStatusCode(), ex.getResponseBodyAsString());
                    throw new BadRequestException("Gmail API call failed after retries: " + ex.getStatusCode());
                }

                log.warn("Transient error on {} (attempt {}/{}). Retrying in {}ms... Error: {}", operationName, attempt, maxAttempts, backoffMs * attempt, ex.getStatusCode());
                sleep(backoffMs * attempt);

            } catch (ResourceAccessException ex) {
                // Connection or Read timeout
                if (attempt >= maxAttempts) {
                    log.error("Network/timeout error on {} after {} attempts: {}", operationName, attempt, ex.getMessage());
                    throw new BadRequestException("Gmail API connection timed out: " + ex.getMessage());
                }
                log.warn("Network timeout on {} (attempt {}/{}). Retrying in {}ms... Error: {}", operationName, attempt, maxAttempts, backoffMs * attempt, ex.getMessage());
                sleep(backoffMs * attempt);

            } catch (Exception ex) {
                log.error("Unexpected error on {}: {}", operationName, ex.getMessage());
                throw new BadRequestException("Gmail API operation failed: " + ex.getMessage());
            }
        }
        throw new BadRequestException("Gmail API operation exhausted attempts without result: " + operationName);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
