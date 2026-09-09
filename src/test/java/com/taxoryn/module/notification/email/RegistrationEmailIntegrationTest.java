package com.taxoryn.module.notification.email;

import com.taxoryn.module.notification.email.service.EmailNotificationService;
import com.taxoryn.module.notification.email.template.EmailTemplateRenderer;
import com.taxoryn.module.notification.email.template.EmailTemplateType;
import com.taxoryn.module.notification.whatsapp.event.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RegistrationEmailIntegrationTest {

    @Autowired
    private EmailTemplateRenderer templateRenderer;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Test
    @DisplayName("Practitioner welcome email renders full practice details and login CTA")
    void testPractitionerWelcomeEmailRendering() {
        Map<String, Object> data = Map.of(
                "name", "CA Rajesh Sharma",
                "practiceName", "Sharma & Associates LLP",
                "email", "rajesh@sharmatax.com",
                "mobile", "+919876543210",
                "loginUrl", "https://app.taxoryn.com/login"
        );

        String subject = templateRenderer.renderSubject(EmailTemplateType.WELCOME_PRACTITIONER, data);
        String html = templateRenderer.renderHtml(EmailTemplateType.WELCOME_PRACTITIONER, data);

        assertThat(subject).contains("Welcome to Taxoryn");
        assertThat(html).contains("CA Rajesh Sharma");
        assertThat(html).contains("Sharma &amp; Associates LLP");
        assertThat(html).contains("rajesh@sharmatax.com");
        assertThat(html).contains("+919876543210");
        assertThat(html).contains("https://app.taxoryn.com/login");
        assertThat(html).doesNotContain("https://taxoryn.com/login");
        assertThat(html).contains("Practitioner Suite");
        assertThat(html).contains("Client 360° Management");
        assertThat(html).contains("Access Practice Dashboard");
        assertThat(html).contains("Direct link:");
    }

    @Test
    @DisplayName("Individual welcome email renders customer greeting and portal features")
    void testIndividualWelcomeEmailRendering() {
        Map<String, Object> data = Map.of(
                "name", "Pooja Verma",
                "email", "pooja.verma@example.com",
                "mobile", "+919123456789",
                "loginUrl", "https://app.taxoryn.com/login"
        );

        String subject = templateRenderer.renderSubject(EmailTemplateType.WELCOME_INDIVIDUAL, data);
        String html = templateRenderer.renderHtml(EmailTemplateType.WELCOME_INDIVIDUAL, data);

        assertThat(subject).contains("Welcome to Taxoryn");
        assertThat(html).contains("Pooja Verma");
        assertThat(html).contains("pooja.verma@example.com");
        assertThat(html).contains("+919123456789");
        assertThat(html).contains("Find Verified CAs & CSs");
        assertThat(html).contains("Customer Portal");
    }

    @Test
    @DisplayName("Invoice issued email renders invoice number and total amount")
    void testInvoiceIssuedEmailRendering() {
        Map<String, Object> data = Map.of(
                "clientName", "Apex Corp",
                "invoiceNumber", "INV-2026-0042",
                "organizationName", "Prime Tax Advisors",
                "totalAmount", "14,500.00",
                "dueDate", "2026-09-15",
                "invoiceUrl", "https://app.taxoryn.com/login"
        );

        String subject = templateRenderer.renderSubject(EmailTemplateType.INVOICE_ISSUED, data);
        String html = templateRenderer.renderHtml(EmailTemplateType.INVOICE_ISSUED, data);

        assertThat(subject).contains("Prime Tax Advisors");
        assertThat(html).contains("INV-2026-0042");
        assertThat(html).contains("₹14,500.00");
        assertThat(html).contains("2026-09-15");
    }

    @Test
    @DisplayName("Payment received email renders receipt amount and remaining balance")
    void testPaymentReceivedEmailRendering() {
        Map<String, Object> data = Map.of(
                "clientName", "Apex Corp",
                "invoiceNumber", "INV-2026-0042",
                "amountPaid", "10,000.00",
                "remainingBalance", "4,500.00",
                "paymentReference", "UPI-REF-998811"
        );

        String subject = templateRenderer.renderSubject(EmailTemplateType.PAYMENT_RECEIVED, data);
        String html = templateRenderer.renderHtml(EmailTemplateType.PAYMENT_RECEIVED, data);

        assertThat(subject).contains("INV-2026-0042");
        assertThat(html).contains("₹10,000.00");
        assertThat(html).contains("₹4,500.00");
        assertThat(html).contains("UPI-REF-998811");
    }

    @Test
    @DisplayName("EmailNotificationService executes welcome email dispatch without exceptions")
    void testEmailDispatchOnRegistrationEvent() {
        UserRegisteredEvent practitionerEvent = UserRegisteredEvent.builder()
                .userId(UUID.randomUUID())
                .organizationId(UUID.randomUUID())
                .registrationType(UserRegistrationType.PRACTITIONER)
                .firstName("Sneha")
                .lastName("Patel")
                .organizationName("Patel & Co")
                .email("sneha.patel@example.com")
                .phone("9876500000")
                .build();

        emailNotificationService.sendWelcomeEmail(practitionerEvent);

        UserRegisteredEvent individualEvent = UserRegisteredEvent.builder()
                .userId(UUID.randomUUID())
                .organizationId(null)
                .registrationType(UserRegistrationType.INDIVIDUAL)
                .firstName("Amit")
                .lastName("Kumar")
                .organizationName(null)
                .email("amit.kumar@example.com")
                .phone("9876501111")
                .build();

        emailNotificationService.sendWelcomeEmail(individualEvent);

        InvoiceIssuedEvent invoiceEvent = InvoiceIssuedEvent.builder()
                .invoiceId(UUID.randomUUID())
                .organizationId(UUID.randomUUID())
                .organizationName("Patel & Co")
                .invoiceNumber("INV-101")
                .clientName("Client ABC")
                .clientEmail("client@abc.com")
                .totalAmount(new BigDecimal("5000.00"))
                .dueDate(LocalDate.now().plusDays(10))
                .build();

        emailNotificationService.sendInvoiceIssuedEmail(invoiceEvent);
    }

    @Test
    @DisplayName("Document request, reminder, and rejected emails render correct upload URL")
    void testDocumentEmailsUploadUrlRendering() {
        Map<String, Object> reqData = Map.of(
                "name", "Vikas Patel",
                "purpose", "GST Filing Q2",
                "practiceName", "Prime Tax",
                "dueDate", "2026-09-30",
                "uploadUrl", "https://app.taxoryn.com/login"
        );
        String reqHtml = templateRenderer.renderHtml(EmailTemplateType.DOCUMENT_REQUEST, reqData);
        assertThat(reqHtml).contains("https://app.taxoryn.com/login");
        assertThat(reqHtml).doesNotContain("https://taxoryn.com/login");

        Map<String, Object> remData = Map.of(
                "name", "Vikas Patel",
                "purpose", "GST Filing Q2",
                "practiceName", "Prime Tax",
                "dueDate", "2026-09-30",
                "uploadUrl", "https://app.taxoryn.com/login"
        );
        String remHtml = templateRenderer.renderHtml(EmailTemplateType.DOCUMENT_REMINDER, remData);
        assertThat(remHtml).contains("https://app.taxoryn.com/login");
        assertThat(remHtml).doesNotContain("https://taxoryn.com/login");

        Map<String, Object> rejData = Map.of(
                "name", "Vikas Patel",
                "purpose", "GST Filing Q2",
                "documentTitle", "Form 26AS",
                "reason", "Incorrect financial year",
                "practiceName", "Prime Tax",
                "uploadUrl", "https://app.taxoryn.com/login"
        );
        String rejHtml = templateRenderer.renderHtml(EmailTemplateType.DOCUMENT_REJECTED, rejData);
        assertThat(rejHtml).contains("https://app.taxoryn.com/login");
        assertThat(rejHtml).doesNotContain("https://taxoryn.com/login");
    }

    @Test
    @DisplayName("Client portal invitation email renders activation link on app.taxoryn.com")
    void testClientPortalInvitationEmailRendering() {
        String token = "client-portal-token-998877";
        String activationUrl = "https://app.taxoryn.com/activate?token=" + token;

        Map<String, Object> data = Map.of(
                "name", "Deepak Chopra",
                "clientName", "Chopra Enterprises",
                "practiceName", "Prime Tax Consultants",
                "email", "deepak@chopraent.com",
                "activationUrl", activationUrl,
                "expiryHours", "24"
        );

        String subject = templateRenderer.renderSubject(EmailTemplateType.CLIENT_PORTAL_INVITATION, data);
        String html = templateRenderer.renderHtml(EmailTemplateType.CLIENT_PORTAL_INVITATION, data);

        assertThat(subject).contains("Taxoryn Client Portal");
        assertThat(html).contains("Deepak Chopra");
        assertThat(html).contains("Chopra Enterprises");
        assertThat(html).contains("Prime Tax Consultants");
        assertThat(html).contains("deepak@chopraent.com");
        assertThat(html).contains("https://app.taxoryn.com/activate?token=" + token);
        assertThat(html).doesNotContain("https://taxoryn.com/activate");
        assertThat(html).doesNotContain("vercel.app");
        assertThat(html).contains("Activate Portal & Set Password");
        assertThat(html).contains("Track Filings");
        assertThat(html).contains("Document Vault");
    }

    @Test
    @DisplayName("Client portal restored email renders login URL on app.taxoryn.com")
    void testClientPortalRestoredEmailRendering() {
        Map<String, Object> data = Map.of(
                "name", "Deepak Chopra",
                "loginUrl", "https://app.taxoryn.com/login"
        );

        String subject = templateRenderer.renderSubject(EmailTemplateType.CLIENT_PORTAL_RESTORED, data);
        String html = templateRenderer.renderHtml(EmailTemplateType.CLIENT_PORTAL_RESTORED, data);

        assertThat(subject).contains("Portal Access Has Been Restored");
        assertThat(html).contains("Deepak Chopra");
        assertThat(html).contains("https://app.taxoryn.com/login");
        assertThat(html).doesNotContain("https://taxoryn.com/login");
        assertThat(html).doesNotContain("vercel.app");
        assertThat(html).contains("Sign In to Client Portal");
    }

    @Test
    @DisplayName("Password reset email renders reset URL on app.taxoryn.com")
    void testPasswordResetEmailRendering() {
        String token = "reset-tok-xyz";
        String resetUrl = "https://app.taxoryn.com/reset-password?token=" + token;

        Map<String, Object> data = Map.of(
                "name", "Deepak Chopra",
                "email", "deepak@chopraent.com",
                "resetUrl", resetUrl,
                "expiryMinutes", "30"
        );

        String subject = templateRenderer.renderSubject(EmailTemplateType.PASSWORD_RESET, data);
        String html = templateRenderer.renderHtml(EmailTemplateType.PASSWORD_RESET, data);

        assertThat(subject).contains("Reset Your Taxoryn Account Password");
        assertThat(html).contains("Deepak Chopra");
        assertThat(html).contains(resetUrl);
        assertThat(html).doesNotContain("https://taxoryn.com/reset-password");
        assertThat(html).doesNotContain("vercel.app");
        assertThat(html).contains("Reset My Password");
    }
}
