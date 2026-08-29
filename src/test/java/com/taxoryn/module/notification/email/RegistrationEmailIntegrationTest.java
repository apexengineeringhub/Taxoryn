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
        assertThat(html).contains("Practitioner Suite");
        assertThat(html).contains("Client 360° Management");
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
}
