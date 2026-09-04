package com.taxoryn.module.notification.channel;

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

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class SmtpEmailNotificationSender implements EmailNotificationSender {

    private final EmailProperties emailProperties;

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Override
    public boolean sendEmail(String recipientEmail, String recipientName, String subject, String content, Map<String, Object> templateData) {
        if (!emailProperties.isEnabled() || "LOG".equalsIgnoreCase(emailProperties.getProvider())) {
            log.info("[EMAIL_LOG] To: '{}' <{}> | Subject: '{}' | Provider: LOG (Simulated dispatch)",
                    recipientName != null ? recipientName : "Recipient", recipientEmail, subject);
            return true;
        }

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
            helper.setText(content, true); // HTML content

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
        return "SMTP";
    }
}
