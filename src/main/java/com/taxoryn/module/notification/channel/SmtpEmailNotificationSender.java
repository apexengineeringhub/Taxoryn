package com.taxoryn.module.notification.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.notification.email.config.EmailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class SmtpEmailNotificationSender implements EmailNotificationSender {

    private final EmailProperties emailProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Override
    public boolean sendEmail(String recipientEmail, String recipientName, String subject, String content, Map<String, Object> templateData) {
        if (!emailProperties.isEnabled() || "LOG".equalsIgnoreCase(emailProperties.getProvider())) {
            log.info("[EMAIL_LOG] To: '{}' <{}> | Subject: '{}' | Provider: LOG (Simulated dispatch)",
                    recipientName != null ? recipientName : "Recipient", recipientEmail, subject);
            return true;
        }

        String provider = resolveProvider();

        // 1. Resend HTTPS API (Port 443 — Unblocked on Render & Cloud)
        if ("RESEND".equalsIgnoreCase(provider)) {
            return sendViaResend(recipientEmail, recipientName, subject, content);
        }

        // 2. Brevo HTTPS API (Port 443 — Unblocked on Render & Cloud)
        if ("BREVO".equalsIgnoreCase(provider) || "BREVO_API".equalsIgnoreCase(provider)) {
            return sendViaBrevo(recipientEmail, recipientName, subject, content);
        }

        // 3. SMTP Transport (Standard SMTP over TLS/SSL)
        if ("SMTP".equalsIgnoreCase(provider)) {
            return sendViaSmtp(recipientEmail, recipientName, subject, content);
        }

        // Fallback: Log email
        log.info("[EMAIL_LOG_FALLBACK] Provider '{}' not configured. To: '{}' <{}> | Subject: '{}'",
                provider, recipientName != null ? recipientName : "Recipient", recipientEmail, subject);
        return true;
    }

    private String resolveProvider() {
        String configured = emailProperties.getProvider();
        if (StringUtils.hasText(configured) && !"AUTO".equalsIgnoreCase(configured)) {
            return configured.trim().toUpperCase();
        }
        if (StringUtils.hasText(emailProperties.getResendApiKey())) {
            return "RESEND";
        }
        if (StringUtils.hasText(emailProperties.getBrevoApiKey())) {
            return "BREVO";
        }
        return "SMTP";
    }

    private boolean sendViaResend(String recipientEmail, String recipientName, String subject, String htmlContent) {
        try {
            String apiKey = emailProperties.getResendApiKey();
            if (!StringUtils.hasText(apiKey)) {
                apiKey = emailProperties.getApiKey();
            }

            if (!StringUtils.hasText(apiKey)) {
                log.warn("Resend provider selected but RESEND_API_KEY is missing. Logging email instead.");
                log.info("[EMAIL_LOG_FALLBACK] To: '{}' <{}> | Subject: '{}'", recipientName, recipientEmail, subject);
                return true;
            }

            String fromAddress = StringUtils.hasText(emailProperties.getFromEmail()) ? emailProperties.getFromEmail() : "taxoryn@gmail.com";
            String fromName = StringUtils.hasText(emailProperties.getFromName()) ? emailProperties.getFromName() : "Taxoryn";
            String formattedFrom = String.format("%s <%s>", fromName, fromAddress);

            Map<String, Object> payload = new HashMap<>();
            payload.put("from", formattedFrom);
            payload.put("to", Collections.singletonList(recipientEmail));
            payload.put("subject", subject);
            payload.put("html", htmlContent);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[EMAIL_SENT_RESEND] Successfully dispatched email via Resend API to '{}' <{}> (status={})",
                        recipientName != null ? recipientName : "Recipient", recipientEmail, response.statusCode());
                return true;
            } else {
                log.warn("Resend API rejected dispatch (HTTP {}): {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception ex) {
            log.error("Failed sending email via Resend HTTPS API to {}: {}", recipientEmail, ex.getMessage(), ex);
            return false;
        }
    }

    private boolean sendViaBrevo(String recipientEmail, String recipientName, String subject, String htmlContent) {
        try {
            String apiKey = emailProperties.getBrevoApiKey();
            if (!StringUtils.hasText(apiKey)) {
                apiKey = emailProperties.getApiKey();
            }

            if (!StringUtils.hasText(apiKey)) {
                log.warn("Brevo provider selected but BREVO_API_KEY is missing. Logging email instead.");
                log.info("[EMAIL_LOG_FALLBACK] To: '{}' <{}> | Subject: '{}'", recipientName, recipientEmail, subject);
                return true;
            }

            String fromAddress = StringUtils.hasText(emailProperties.getFromEmail()) ? emailProperties.getFromEmail() : "taxoryn@gmail.com";
            String fromName = StringUtils.hasText(emailProperties.getFromName()) ? emailProperties.getFromName() : "Taxoryn";

            Map<String, Object> sender = new HashMap<>();
            sender.put("name", fromName);
            sender.put("email", fromAddress);

            Map<String, Object> recipient = new HashMap<>();
            recipient.put("email", recipientEmail);
            if (StringUtils.hasText(recipientName)) {
                recipient.put("name", recipientName);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", sender);
            payload.put("to", Collections.singletonList(recipient));
            payload.put("subject", subject);
            payload.put("htmlContent", htmlContent);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", apiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[EMAIL_SENT_BREVO] Successfully dispatched email via Brevo HTTPS API to '{}' <{}> (status={})",
                        recipientName != null ? recipientName : "Recipient", recipientEmail, response.statusCode());
                return true;
            } else {
                log.warn("Brevo API rejected dispatch (HTTP {}): {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception ex) {
            log.error("Failed sending email via Brevo HTTPS API to {}: {}", recipientEmail, ex.getMessage(), ex);
            return false;
        }
    }

    private boolean sendViaSmtp(String recipientEmail, String recipientName, String subject, String content) {
        if (javaMailSender == null) {
            log.info("[EMAIL_LOG_FALLBACK] SMTP selected but no mail host configured. Dispatched to logs. To: '{}' <{}> | Subject: '{}'",
                    recipientName != null ? recipientName : "Recipient", recipientEmail, subject);
            return true;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            String fromAddress = StringUtils.hasText(emailProperties.getFromEmail()) ? emailProperties.getFromEmail() : "taxoryn@gmail.com";
            String fromName = StringUtils.hasText(emailProperties.getFromName()) ? emailProperties.getFromName() : "Taxoryn";

            helper.setFrom(fromAddress, fromName);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            javaMailSender.send(message);
            log.info("[EMAIL_SENT] Successfully dispatched SMTP email to '{}' <{}> | Subject: '{}'",
                    recipientName != null ? recipientName : "Recipient", recipientEmail, subject);
            return true;
        } catch (Exception ex) {
            log.warn("Failed sending live SMTP email to '{}' <{}> (Reason: {}). Recording email payload to log.",
                    recipientName != null ? recipientName : "Recipient", recipientEmail, ex.getMessage());
            log.info("[EMAIL_LOG_FALLBACK] To: '{}' <{}> | Subject: '{}'",
                    recipientName != null ? recipientName : "Recipient", recipientEmail, subject);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return resolveProvider();
    }
}
