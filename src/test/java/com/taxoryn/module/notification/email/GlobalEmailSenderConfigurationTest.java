package com.taxoryn.module.notification.email;

import com.taxoryn.module.notification.channel.SmtpEmailNotificationSender;
import com.taxoryn.module.notification.email.config.EmailProperties;
import com.taxoryn.module.notification.email.service.EmailNotificationService;
import com.taxoryn.module.notification.email.service.EmailNotificationServiceImpl;
import com.taxoryn.module.notification.email.template.EmailTemplateRenderer;
import com.taxoryn.module.notification.whatsapp.event.UserRegisteredEvent;
import com.taxoryn.module.notification.whatsapp.event.UserRegistrationType;
import jakarta.mail.Address;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalEmailSenderConfigurationTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    @Mock
    private com.taxoryn.module.audit.service.AuditService auditService;

    private EmailProperties emailProperties;
    private SmtpEmailNotificationSender sender;
    private EmailTemplateRenderer templateRenderer;
    private EmailNotificationServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setEnabled(true);
        emailProperties.setProvider("SMTP");
        emailProperties.setFromEmail("info@taxoryn.com");
        emailProperties.setFromName("Taxoryn");
        emailProperties.setReplyTo("info@taxoryn.com");

        sender = new SmtpEmailNotificationSender(emailProperties);
        ReflectionTestUtils.setField(sender, "javaMailSender", javaMailSender);
        sender.setHttpClient(mockHttpClient);

        templateRenderer = new EmailTemplateRenderer();
        emailService = new EmailNotificationServiceImpl(sender, templateRenderer, emailProperties, auditService);
    }

    @Test
    @DisplayName("Centralized SMTP Sender: Generates MimeMessage with From: Taxoryn <info@taxoryn.com> and Reply-To: info@taxoryn.com")
    void testJavaMailMimeMessageSenderConfiguration() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        boolean result = sender.sendEmail(
                "user@example.com",
                "Anita Desai",
                "Taxoryn Test Notification",
                "<p>Test content</p>",
                Map.of()
        );

        assertTrue(result);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        try {
            Address[] fromAddresses = sentMessage.getFrom();
            assertThat(fromAddresses).isNotNull().hasSize(1);
            InternetAddress from = (InternetAddress) fromAddresses[0];
            assertThat(from.getAddress()).isEqualTo("info@taxoryn.com");
            assertThat(from.getPersonal()).isEqualTo("Taxoryn");

            Address[] replyToAddresses = sentMessage.getReplyTo();
            assertThat(replyToAddresses).isNotNull().hasSize(1);
            InternetAddress replyTo = (InternetAddress) replyToAddresses[0];
            assertThat(replyTo.getAddress()).isEqualTo("info@taxoryn.com");

            Address[] toAddresses = sentMessage.getRecipients(MimeMessage.RecipientType.TO);
            assertThat(toAddresses).isNotNull().hasSize(1);
            assertThat(((InternetAddress) toAddresses[0]).getAddress()).isEqualTo("user@example.com");

            assertThat(sentMessage.getSubject()).isEqualTo("Taxoryn Test Notification");
        } catch (Exception ex) {
            org.junit.jupiter.api.Assertions.fail("MimeMessage address parsing failed: " + ex.getMessage());
        }
    }

    @Test
    @DisplayName("Centralized Resend Sender: Generates payload with from: 'Taxoryn <info@taxoryn.com>' and reply_to: 'info@taxoryn.com'")
    void testResendSenderConfiguration() throws Exception {
        emailProperties.setProvider("RESEND");
        emailProperties.setResendApiKey("re_prod_key_12345");

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        boolean result = sender.sendEmail(
                "ca.verma@example.com",
                "CA Verma",
                "Welcome to Taxoryn",
                "<p>Welcome to Taxoryn</p>",
                Map.of()
        );

        assertTrue(result);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        HttpRequest request = requestCaptor.getValue();
        assertThat(request.uri().toString()).isEqualTo("https://api.resend.com/emails");
        assertThat(request.headers().firstValue("Authorization")).contains("Bearer re_prod_key_12345");
    }

    @Test
    @DisplayName("Centralized Brevo Sender: Generates payload with sender: { name: 'Taxoryn', email: 'info@taxoryn.com' }")
    void testBrevoSenderConfiguration() throws Exception {
        emailProperties.setProvider("BREVO");
        emailProperties.setBrevoApiKey("xkeysib-prod-key-12345");

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        boolean result = sender.sendEmail(
                "client@example.com",
                "Client Sharma",
                "Invoice from Sharma & Associates",
                "<p>Invoice content</p>",
                Map.of()
        );

        assertTrue(result);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        HttpRequest request = requestCaptor.getValue();
        assertThat(request.uri().toString()).isEqualTo("https://api.brevo.com/v3/smtp/email");
        assertThat(request.headers().firstValue("api-key")).contains("xkeysib-prod-key-12345");
    }

    @Test
    @DisplayName("Transactional Flow: Organization Activation Email dispatches with configured global sender")
    void testOrganizationActivationEmailDispatchesWithGlobalSender() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOrganizationActivationEmail(
                "principal@apexca.in",
                "CA Rajesh Sharma",
                "Apex CA Associates",
                "https://app.taxoryn.com/activate?token=tok-123",
                24
        );

        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Transactional Flow: Employee Invitation Email dispatches with configured global sender")
    void testEmployeeInvitationEmailDispatchesWithGlobalSender() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendEmployeeInvitationEmail(
                "staff@apexca.in",
                "Anita Roy",
                "Apex CA Associates",
                "Senior Tax Associate",
                "https://app.taxoryn.com/activate?token=emp-tok-456",
                48
        );

        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Transactional Flow: Password Reset Email dispatches with configured global sender")
    void testPasswordResetEmailDispatchesWithGlobalSender() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordResetEmail(
                "user@apexca.in",
                "CA Rajesh Sharma",
                "https://app.taxoryn.com/reset-password?token=pwd-reset-789",
                30
        );

        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Transactional Flow: Document Request, Reminder, and Rejection Emails dispatch with configured global sender")
    void testDocumentWorkflowEmailsDispatchWithGlobalSender() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // 1. Document Request
        emailService.sendDocumentRequestEmail(
                "client@example.com",
                "Apex Corp Ltd",
                "FY 2025-26 Tax Audit",
                "Apex CA Associates",
                LocalDate.now().plusDays(7),
                "Please upload GSTR-2B & bank statements",
                List.of("GSTR-2B", "Bank Statements Q4")
        );

        // 2. Document Reminder
        emailService.sendDocumentReminderEmail(
                "client@example.com",
                "Apex Corp Ltd",
                "FY 2025-26 Tax Audit",
                "Apex CA Associates",
                LocalDate.now().plusDays(2),
                List.of("GSTR-2B")
        );

        // 3. Document Rejection
        emailService.sendDocumentRejectedEmail(
                "client@example.com",
                "Apex Corp Ltd",
                "FY 2025-26 Tax Audit",
                "Bank Statement Q4.pdf",
                "Statement password protected; please provide unencrypted PDF",
                "Apex CA Associates"
        );

        verify(javaMailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Configuration Override: MAIL_FROM_ADDRESS and MAIL_FROM_NAME override defaults dynamically")
    void testConfigurationOverrideSupport() {
        emailProperties.setFromAddress("custom-notifications@taxoryn.com");
        emailProperties.setFromName("Taxoryn Compliance Engine");
        emailProperties.setReplyTo("custom-reply@taxoryn.com");

        assertThat(emailProperties.getFromEmail()).isEqualTo("custom-notifications@taxoryn.com");
        assertThat(emailProperties.getFromAddress()).isEqualTo("custom-notifications@taxoryn.com");
        assertThat(emailProperties.getFromName()).isEqualTo("Taxoryn Compliance Engine");
        assertThat(emailProperties.getReplyTo()).isEqualTo("custom-reply@taxoryn.com");
    }

    @Test
    @DisplayName("Environment Fallback: Null/empty fromAddress falls back safely to info@taxoryn.com")
    void testEnvironmentFallbackSafety() {
        emailProperties.setFromAddress("");
        emailProperties.setFromEmail("");
        emailProperties.setFromName("");
        emailProperties.setReplyTo("");

        assertThat(emailProperties.getFromEmail()).isEqualTo("info@taxoryn.com");
        assertThat(emailProperties.getFromAddress()).isEqualTo("info@taxoryn.com");
        assertThat(emailProperties.getFromName()).isEqualTo("Taxoryn");
        assertThat(emailProperties.getReplyTo()).isEqualTo("info@taxoryn.com");
    }
}
