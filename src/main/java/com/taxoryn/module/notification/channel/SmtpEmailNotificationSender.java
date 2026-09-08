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
    private HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Autowired(required = false)
    private org.springframework.core.env.Environment environment;

    @jakarta.annotation.PostConstruct
    public void logEmailStartupConfiguration() {
        String activeProfiles = environment != null && environment.getActiveProfiles().length > 0
                ? String.join(",", environment.getActiveProfiles())
                : "default";
        String mailHost = environment != null ? environment.getProperty("spring.mail.host", "not configured") : "not configured";
        String mailUsername = environment != null ? environment.getProperty("spring.mail.username", "not configured") : "not configured";
        String fromAddress = emailProperties != null ? emailProperties.getFromEmail() : "info@taxoryn.com";
        String fromName = emailProperties != null ? emailProperties.getFromName() : "Taxoryn";
        String replyTo = emailProperties != null ? emailProperties.getReplyTo() : "info@taxoryn.com";
        String provider = emailProperties != null ? emailProperties.getProvider() : "AUTO";

        String maskedUsername = maskEmailOrUser(mailUsername);

        log.info("==================================================");
        log.info("TAXORYN EMAIL CONFIGURATION INITIALIZATION");
        log.info("Active profile     : {}", activeProfiles);
        log.info("Mail Provider      : {} (resolved: {})", provider, resolveProvider());
        log.info("Mail Host          : {}", mailHost);
        log.info("Mail Username      : {}", maskedUsername);
        log.info("Effective From     : {} <{}>", fromName, fromAddress);
        log.info("Effective Reply-To : {}", replyTo);
        log.info("JavaMailSender     : {}", (javaMailSender != null ? "INITIALIZED (ready)" : "NOT CONFIGURED"));
        log.info("==================================================");
    }

    private String maskEmailOrUser(String val) {
        if (val == null || val.isBlank() || "not configured".equalsIgnoreCase(val)) {
            return "not configured";
        }
        int atIndex = val.indexOf('@');
        if (atIndex > 2) {
            return val.substring(0, 2) + "****" + val.substring(atIndex);
        } else if (val.length() > 4) {
            return val.substring(0, 2) + "****" + val.substring(val.length() - 2);
        }
        return "****";
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setJavaMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

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

        // 4. Fallback: If no real provider configured, check if LOG mode was intended
        if ("LOG".equalsIgnoreCase(provider)) {
            log.info("[EMAIL_LOG] To: '{}' <{}> | Subject: '{}' | Provider: LOG (No live email transport configured)",
                    recipientName != null ? recipientName : "Recipient", recipientEmail, subject);
            return true;
        }

        log.warn("[EMAIL_DELIVERY_FAILED] No active email transport configured. To: '{}' <{}> | Subject: '{}'",
                recipientName != null ? recipientName : "Recipient", recipientEmail, subject);
        return false;
    }

    private String resolveProvider() {
        String configured = emailProperties.getProvider();
        if (StringUtils.hasText(configured) && !"AUTO".equalsIgnoreCase(configured)) {
            String upper = configured.trim().toUpperCase();
            if ("RESEND".equals(upper)) {
                String apiKey = StringUtils.hasText(emailProperties.getResendApiKey()) ? emailProperties.getResendApiKey() : emailProperties.getApiKey();
                if (!StringUtils.hasText(apiKey) && javaMailSender != null) {
                    log.info("RESEND provider requested but RESEND_API_KEY is not set; falling back to SMTP");
                    return "SMTP";
                }
            } else if ("BREVO".equals(upper) || "BREVO_API".equals(upper)) {
                String apiKey = StringUtils.hasText(emailProperties.getBrevoApiKey()) ? emailProperties.getBrevoApiKey() : emailProperties.getApiKey();
                if (!StringUtils.hasText(apiKey) && javaMailSender != null) {
                    log.info("BREVO provider requested but BREVO_API_KEY is not set; falling back to SMTP");
                    return "SMTP";
                }
            }
            return upper;
        }

        // AUTO detection: check available transports in priority order
        String resendKey = StringUtils.hasText(emailProperties.getResendApiKey()) ? emailProperties.getResendApiKey() : emailProperties.getApiKey();
        if (StringUtils.hasText(resendKey)) {
            return "RESEND";
        }
        String brevoKey = StringUtils.hasText(emailProperties.getBrevoApiKey()) ? emailProperties.getBrevoApiKey() : emailProperties.getApiKey();
        if (StringUtils.hasText(brevoKey)) {
            return "BREVO";
        }
        if (javaMailSender != null) {
            return "SMTP";
        }
        return "LOG";
    }

    private boolean sendViaResend(String recipientEmail, String recipientName, String subject, String htmlContent) {
        try {
            String apiKey = emailProperties.getResendApiKey();
            if (!StringUtils.hasText(apiKey)) {
                apiKey = emailProperties.getApiKey();
            }

            if (!StringUtils.hasText(apiKey)) {
                if (javaMailSender != null) {
                    log.info("Resend API key missing; falling back to SMTP for recipient {}", recipientEmail);
                    return sendViaSmtp(recipientEmail, recipientName, subject, htmlContent);
                }
                log.warn("Resend provider selected but RESEND_API_KEY is missing. Falling back to log dispatch.");
                log.info("[EMAIL_LOG_FALLBACK] To: '{}' <{}> | Subject: '{}'", recipientName, recipientEmail, subject);
                return true;
            }

            String fromAddress = StringUtils.hasText(emailProperties.getFromEmail()) ? emailProperties.getFromEmail() : "info@taxoryn.com";
            String fromName = StringUtils.hasText(emailProperties.getFromName()) ? emailProperties.getFromName() : "Taxoryn";
            String formattedFrom = String.format("%s <%s>", fromName, fromAddress);

            Map<String, Object> payload = new HashMap<>();
            payload.put("from", formattedFrom);
            payload.put("to", Collections.singletonList(recipientEmail));
            payload.put("subject", subject);
            payload.put("html", htmlContent);

            if (StringUtils.hasText(emailProperties.getReplyTo())) {
                payload.put("reply_to", emailProperties.getReplyTo().trim());
            }

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
                if (javaMailSender != null) {
                    log.info("Brevo API key missing; falling back to SMTP for recipient {}", recipientEmail);
                    return sendViaSmtp(recipientEmail, recipientName, subject, htmlContent);
                }
                log.warn("Brevo provider selected but BREVO_API_KEY is missing. Falling back to log dispatch.");
                log.info("[EMAIL_LOG_FALLBACK] To: '{}' <{}> | Subject: '{}'", recipientName, recipientEmail, subject);
                return true;
            }

            String fromAddress = StringUtils.hasText(emailProperties.getFromEmail()) ? emailProperties.getFromEmail() : "info@taxoryn.com";
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

            if (StringUtils.hasText(emailProperties.getReplyTo())) {
                Map<String, Object> replyToMap = new HashMap<>();
                replyToMap.put("email", emailProperties.getReplyTo().trim());
                replyToMap.put("name", fromName);
                payload.put("replyTo", replyToMap);
            }

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
            if ("LOG".equalsIgnoreCase(emailProperties.getProvider())) {
                log.info("[EMAIL_LOG] To: '{}' <{}> | Subject: '{}' | Provider: LOG (Simulated dispatch)",
                        recipientName != null ? recipientName : "Recipient", recipientEmail, subject);
                return true;
            }
            log.warn("[EMAIL_DELIVERY_FAILED] SMTP selected but no JavaMailSender is configured. To: '{}' <{}> | Subject: '{}'",
                    recipientName != null ? recipientName : "Recipient", recipientEmail, subject);
            return false;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            String fromAddress = StringUtils.hasText(emailProperties.getFromEmail()) ? emailProperties.getFromEmail() : "info@taxoryn.com";
            String fromName = StringUtils.hasText(emailProperties.getFromName()) ? emailProperties.getFromName() : "Taxoryn";

            helper.setFrom(fromAddress, fromName);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            if (StringUtils.hasText(emailProperties.getReplyTo())) {
                helper.setReplyTo(emailProperties.getReplyTo().trim());
            }

            String activeProfiles = environment != null && environment.getActiveProfiles().length > 0
                    ? String.join(",", environment.getActiveProfiles())
                    : "default";
            String mailHost = environment != null ? environment.getProperty("spring.mail.host", "not configured") : "not configured";
            String mailUsername = environment != null ? environment.getProperty("spring.mail.username", "not configured") : "not configured";

            log.info("========== EMAIL DIAGNOSTIC DEBUG ==========");
            log.info("profile        : {}", activeProfiles);
            log.info("smtpHost       : {}", mailHost);
            log.info("smtpUsername   : {}", maskEmailOrUser(mailUsername));
            log.info("from           : {} <{}>", fromName, fromAddress);
            log.info("replyTo        : {}", (emailProperties.getReplyTo() != null ? emailProperties.getReplyTo() : "none"));
            log.info("ACTUAL MIME FROM = {}", Arrays.toString(message.getFrom()));
            log.info("ACTUAL MIME REPLY-TO = {}", Arrays.toString(message.getReplyTo()));
            log.info("============================================");

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
