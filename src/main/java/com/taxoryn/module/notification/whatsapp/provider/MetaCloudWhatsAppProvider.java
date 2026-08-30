package com.taxoryn.module.notification.whatsapp.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.notification.whatsapp.config.WhatsAppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component("metaWhatsAppProvider")
@RequiredArgsConstructor
public class MetaCloudWhatsAppProvider implements WhatsAppProvider {

    private final WhatsAppProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    @Override
    public String getProviderName() {
        return "META";
    }

    @Override
    public WhatsAppSendResult sendTemplate(String phoneNumber, String templateName, Map<String, String> variables) {
        if (!StringUtils.hasText(properties.getPhoneNumberId()) || !StringUtils.hasText(properties.getAccessToken())) {
            log.warn("Meta Cloud WhatsApp provider is selected but phoneNumberId or accessToken is not configured");
            return WhatsAppSendResult.failure(getProviderName(), "Meta WhatsApp credentials not configured");
        }

        String url = String.format("%s/%s/messages",
                StringUtils.hasText(properties.getBaseUrl()) ? properties.getBaseUrl().replaceAll("/+$", "") : "https://graph.facebook.com/v19.0",
                properties.getPhoneNumberId());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getAccessToken());

            // Build Meta WhatsApp Cloud API Template Payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("recipient_type", "individual");
            payload.put("to", phoneNumber.replace("+", ""));
            payload.put("type", "template");

            String lang = StringUtils.hasText(properties.getLanguageCode()) ? properties.getLanguageCode() : "en_US";

            Map<String, Object> templateObj = new HashMap<>();
            templateObj.put("name", templateName);
            templateObj.put("language", Map.of("code", lang));

            // hello_world does not accept parameters in Meta API
            if (!"hello_world".equalsIgnoreCase(templateName) && variables != null && !variables.isEmpty()) {
                List<Map<String, String>> parameters = new ArrayList<>();
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    parameters.add(Map.of("type", "text", "text", entry.getValue() != null ? entry.getValue() : ""));
                }
                templateObj.put("components", List.of(Map.of("type", "body", "parameters", parameters)));
            }

            payload.put("template", templateObj);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                String messageId = null;
                if (json.has("messages") && json.get("messages").isArray() && json.get("messages").size() > 0) {
                    messageId = json.get("messages").get(0).path("id").asText(null);
                }
                return WhatsAppSendResult.success(getProviderName(), messageId != null ? messageId : "META-" + UUID.randomUUID());
            }

            return WhatsAppSendResult.failure(getProviderName(), "Non-2xx response from Meta API: " + response.getStatusCode());
        } catch (HttpStatusCodeException ex) {
            log.error("Meta WhatsApp API error: Status={}, Body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return WhatsAppSendResult.failure(getProviderName(), "HTTP " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            log.error("Meta WhatsApp API network/timeout error: {}", ex.getMessage());
            return WhatsAppSendResult.failure(getProviderName(), "Network/Timeout error: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error invoking Meta WhatsApp API: {}", ex.getMessage(), ex);
            return WhatsAppSendResult.failure(getProviderName(), "Unexpected error: " + ex.getMessage());
        }
    }

    @Override
    public WhatsAppSendResult sendTextMessage(String phoneNumber, String messageText) {
        if (!StringUtils.hasText(properties.getPhoneNumberId()) || !StringUtils.hasText(properties.getAccessToken())) {
            log.warn("Meta Cloud WhatsApp provider is selected but phoneNumberId or accessToken is not configured");
            return WhatsAppSendResult.failure(getProviderName(), "Meta WhatsApp credentials not configured");
        }

        String url = String.format("%s/%s/messages",
                StringUtils.hasText(properties.getBaseUrl()) ? properties.getBaseUrl().replaceAll("/+$", "") : "https://graph.facebook.com/v19.0",
                properties.getPhoneNumberId());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getAccessToken());

            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("recipient_type", "individual");
            payload.put("to", phoneNumber.replace("+", ""));
            payload.put("type", "text");
            payload.put("text", Map.of("body", messageText));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                String messageId = null;
                if (json.has("messages") && json.get("messages").isArray() && json.get("messages").size() > 0) {
                    messageId = json.get("messages").get(0).path("id").asText(null);
                }
                return WhatsAppSendResult.success(getProviderName(), messageId != null ? messageId : "META-" + UUID.randomUUID());
            }

            return WhatsAppSendResult.failure(getProviderName(), "Non-2xx response from Meta API: " + response.getStatusCode());
        } catch (Exception ex) {
            log.error("Meta WhatsApp API text message error: {}", ex.getMessage());
            return WhatsAppSendResult.failure(getProviderName(), ex.getMessage());
        }
    }

    @Override
    public WhatsAppSendResult sendDocument(String phoneNumber, String documentUrl, String filename, String caption) {
        if (!StringUtils.hasText(properties.getPhoneNumberId()) || !StringUtils.hasText(properties.getAccessToken())) {
            log.warn("Meta Cloud WhatsApp provider is selected but phoneNumberId or accessToken is not configured");
            return WhatsAppSendResult.failure(getProviderName(), "Meta WhatsApp credentials not configured");
        }

        String url = String.format("%s/%s/messages",
                StringUtils.hasText(properties.getBaseUrl()) ? properties.getBaseUrl().replaceAll("/+$", "") : "https://graph.facebook.com/v19.0",
                properties.getPhoneNumberId());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getAccessToken());

            Map<String, Object> documentObj = new HashMap<>();
            documentObj.put("link", documentUrl);
            if (StringUtils.hasText(filename)) {
                documentObj.put("filename", filename);
            }
            if (StringUtils.hasText(caption)) {
                documentObj.put("caption", caption);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("recipient_type", "individual");
            payload.put("to", phoneNumber.replace("+", ""));
            payload.put("type", "document");
            payload.put("document", documentObj);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                String messageId = null;
                if (json.has("messages") && json.get("messages").isArray() && json.get("messages").size() > 0) {
                    messageId = json.get("messages").get(0).path("id").asText(null);
                }
                return WhatsAppSendResult.success(getProviderName(), messageId != null ? messageId : "META-DOC-" + UUID.randomUUID());
            }

            return WhatsAppSendResult.failure(getProviderName(), "Non-2xx response from Meta API: " + response.getStatusCode());
        } catch (Exception ex) {
            log.error("Meta WhatsApp API document message error: {}", ex.getMessage());
            return WhatsAppSendResult.failure(getProviderName(), ex.getMessage());
        }
    }
}
