package com.taxoryn.module.notification.whatsapp;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.notification.whatsapp.config.WhatsAppProperties;
import com.taxoryn.module.notification.whatsapp.dto.WhatsAppIntegrationStatusDto;
import com.taxoryn.module.notification.whatsapp.dto.WhatsAppMessageDto;
import com.taxoryn.module.notification.whatsapp.entity.WhatsAppMessageEntity;
import com.taxoryn.module.notification.whatsapp.entity.WhatsAppMessageStatus;
import com.taxoryn.module.notification.whatsapp.event.UserRegisteredEvent;
import com.taxoryn.module.notification.whatsapp.event.UserRegistrationType;
import com.taxoryn.module.notification.whatsapp.provider.MockWhatsAppProvider;
import com.taxoryn.module.notification.whatsapp.repository.WhatsAppMessageRepository;
import com.taxoryn.module.notification.whatsapp.service.WhatsAppNotificationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationEntity.SubscriptionPlan;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WhatsAppNotificationIntegrationTest {

    @Autowired
    private WhatsAppNotificationService whatsAppNotificationService;

    @Autowired
    private WhatsAppMessageRepository messageRepository;

    @Autowired
    private MockWhatsAppProvider mockWhatsAppProvider;

    @Autowired
    private WhatsAppProperties whatsAppProperties;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private OrganizationEntity testOrg;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        mockWhatsAppProvider.clear();
        whatsAppProperties.setEnabled(true);
        whatsAppProperties.setProvider("MOCK");

        testOrg = organizationRepository.findAll().stream().findFirst().orElseGet(() ->
                organizationRepository.save(OrganizationEntity.builder()
                        .name("Test Tax Practice")
                        .email("test-practice-" + UUID.randomUUID() + "@taxoryn.com")
                        .phone("9876543210")
                        .status(OrganizationStatus.ACTIVE)
                        .subscriptionPlan(SubscriptionPlan.STARTER)
                        .build())
        );

        testUser = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            UserEntity user = UserEntity.builder()
                    .email("test-user-" + UUID.randomUUID() + "@taxoryn.com")
                    .passwordHash("hashed")
                    .firstName("Test")
                    .lastName("User")
                    .phone("9876543210")
                    .status(UserStatus.ACTIVE)
                    .build();
            user.setOrganizationId(testOrg.getId());
            return userRepository.save(user);
        });
    }

    @Test
    @DisplayName("Should format and send welcome message for practitioner successfully")
    void testSendWelcomeMessagePractitioner() {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(testUser.getId())
                .organizationId(testOrg.getId())
                .registrationType(UserRegistrationType.PRACTITIONER)
                .firstName("Rajesh")
                .lastName("Sharma")
                .organizationName("Sharma & Associates")
                .email("rajesh@sharmatax.com")
                .phone("9876543210")
                .build();

        WhatsAppMessageEntity entity = whatsAppNotificationService.sendWelcomeMessage(event);

        assertThat(entity).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(WhatsAppMessageStatus.SENT);
        assertThat(entity.getRecipientPhone()).isEqualTo("+919876543210");
        assertThat(entity.getTemplateType()).isEqualTo("WELCOME_PRACTITIONER");
        assertThat(entity.getMessageContent()).contains("Rajesh Sharma");
        assertThat(entity.getMessageContent()).contains("Sharma & Associates");
        assertThat(entity.getProvider()).isEqualTo("MOCK");
        assertThat(entity.getProviderMessageId()).startsWith("MOCK-WA-");

        assertThat(mockWhatsAppProvider.getSentRecords()).hasSize(1);
        MockWhatsAppProvider.SentRecord record = mockWhatsAppProvider.getSentRecords().get(0);
        assertThat(record.phoneNumber()).isEqualTo("+919876543210");
        assertThat(record.variables().get("name")).isEqualTo("Rajesh Sharma");
    }

    @Test
    @DisplayName("Should format and send welcome message for individual customer successfully")
    void testSendWelcomeMessageIndividual() {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(testUser.getId())
                .organizationId(null)
                .registrationType(UserRegistrationType.INDIVIDUAL)
                .firstName("Priya")
                .lastName("Patel")
                .email("priya.patel@gmail.com")
                .phone("+919123456789")
                .build();

        WhatsAppMessageEntity entity = whatsAppNotificationService.sendWelcomeMessage(event);

        assertThat(entity).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(WhatsAppMessageStatus.SENT);
        assertThat(entity.getRecipientPhone()).isEqualTo("+919123456789");
        assertThat(entity.getTemplateType()).isEqualTo("WELCOME_INDIVIDUAL");
        assertThat(entity.getMessageContent()).contains("Priya Patel");
        assertThat(entity.getOrganizationId()).isNull();

        assertThat(mockWhatsAppProvider.getSentRecords()).hasSize(1);
    }

    @Test
    @DisplayName("Should mark message as FAILED when provider fails without throwing unhandled exception")
    void testProviderFailureHandling() {
        mockWhatsAppProvider.setShouldFail(true);
        mockWhatsAppProvider.setFailureReason("Meta API Rate Limit Exceeded");

        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(testUser.getId())
                .organizationId(testOrg.getId())
                .registrationType(UserRegistrationType.PRACTITIONER)
                .firstName("Amit")
                .lastName("Kumar")
                .organizationName("Kumar Tax")
                .email("amit@kumartax.com")
                .phone("9988776655")
                .build();

        WhatsAppMessageEntity entity = whatsAppNotificationService.sendWelcomeMessage(event);

        assertThat(entity).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(WhatsAppMessageStatus.FAILED);
        assertThat(entity.getErrorMessage()).contains("Meta API Rate Limit Exceeded");
    }

    @Test
    @DisplayName("Should skip sending when WhatsApp is globally disabled")
    void testWhatsAppDisabled() {
        whatsAppProperties.setEnabled(false);

        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(testUser.getId())
                .registrationType(UserRegistrationType.INDIVIDUAL)
                .firstName("Sneha")
                .lastName("Reddy")
                .email("sneha@test.com")
                .phone("9876500000")
                .build();

        WhatsAppMessageEntity entity = whatsAppNotificationService.sendWelcomeMessage(event);

        assertThat(entity).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(WhatsAppMessageStatus.PENDING);
        assertThat(entity.getErrorMessage()).contains("disabled in configuration");
        assertThat(mockWhatsAppProvider.getSentRecords()).isEmpty();
    }

    @Test
    @DisplayName("Should return accurate integration status and message history")
    void testIntegrationStatusAndHistory() {
        WhatsAppIntegrationStatusDto status = whatsAppNotificationService.getIntegrationStatus();
        assertThat(status).isNotNull();
        assertThat(status.getProvider()).isEqualTo("MOCK");

        PagedResponse<WhatsAppMessageDto> messages = whatsAppNotificationService.getMessages(PageRequest.of(0, 10));
        assertThat(messages).isNotNull();
        assertThat(messages.getContent()).isNotNull();
    }

    @Test
    @DisplayName("Should format and send InvoiceIssuedEvent notification")
    void testSendInvoiceIssuedMessage() {
        com.taxoryn.module.notification.whatsapp.event.InvoiceIssuedEvent event = com.taxoryn.module.notification.whatsapp.event.InvoiceIssuedEvent.builder()
                .organizationId(testOrg.getId())
                .invoiceId(UUID.randomUUID())
                .clientId(UUID.randomUUID())
                .clientName("Acrobat Tech Ltd")
                .clientPhone("9876543210")
                .organizationName("Sharma & Associates")
                .invoiceNumber("INV-2026-001")
                .totalAmount(new java.math.BigDecimal("15000.00"))
                .balanceAmount(new java.math.BigDecimal("15000.00"))
                .currency("INR")
                .issueDate(java.time.LocalDate.now())
                .dueDate(java.time.LocalDate.now().plusDays(15))
                .build();

        WhatsAppMessageEntity entity = whatsAppNotificationService.sendInvoiceIssuedMessage(event);

        assertThat(entity).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(WhatsAppMessageStatus.SENT);
        assertThat(entity.getRecipientPhone()).isEqualTo("+919876543210");
        assertThat(entity.getMessageContent()).contains("INV-2026-001");
        assertThat(entity.getMessageContent()).contains("15000.00");
        assertThat(mockWhatsAppProvider.getSentRecords()).hasSize(1);
    }

    @Test
    @DisplayName("Should format and send PaymentReceivedEvent notification")
    void testSendPaymentReceivedMessage() {
        com.taxoryn.module.notification.whatsapp.event.PaymentReceivedEvent event = com.taxoryn.module.notification.whatsapp.event.PaymentReceivedEvent.builder()
                .organizationId(testOrg.getId())
                .invoiceId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .clientId(UUID.randomUUID())
                .clientName("Acrobat Tech Ltd")
                .clientPhone("9876543210")
                .organizationName("Sharma & Associates")
                .invoiceNumber("INV-2026-001")
                .paymentReference("UPI-123456789")
                .amountPaid(new java.math.BigDecimal("10000.00"))
                .remainingBalance(new java.math.BigDecimal("5000.00"))
                .currency("INR")
                .paymentDate(java.time.LocalDate.now())
                .build();

        WhatsAppMessageEntity entity = whatsAppNotificationService.sendPaymentReceivedMessage(event);

        assertThat(entity).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(WhatsAppMessageStatus.SENT);
        assertThat(entity.getMessageContent()).contains("10000.00");
        assertThat(entity.getMessageContent()).contains("5000.00");
        assertThat(entity.getMessageContent()).contains("UPI-123456789");
    }

    @Test
    @DisplayName("Should process webhook delivery receipts and update message status")
    void testWebhookStatusUpdate() {
        WhatsAppMessageEntity entity = WhatsAppMessageEntity.builder()
                .organizationId(testOrg.getId())
                .recipientPhone("919876543210")
                .templateType("INVOICE_ISSUED")
                .templateName("invoice_issued")
                .messageContent("Invoice test")
                .provider("META")
                .providerMessageId("wamid.HBgLMjAyNjA4Mjk=")
                .status(WhatsAppMessageStatus.SENT)
                .build();
        entity = messageRepository.saveAndFlush(entity);

        String webhookPayload = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "1430976512178761",
                    "changes": [{
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": { "display_phone_number": "15556581244", "phone_number_id": "1345984618587263" },
                        "statuses": [{
                          "id": "wamid.HBgLMjAyNjA4Mjk=",
                          "status": "delivered",
                          "timestamp": "1724930000",
                          "recipient_id": "919876543210"
                        }]
                      },
                      "field": "messages"
                    }]
                  }]
                }
                """;

        whatsAppNotificationService.processWebhookPayload(webhookPayload, null);

        entityManager.clear();
        WhatsAppMessageEntity updated = messageRepository.findById(entity.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(WhatsAppMessageStatus.DELIVERED);
        assertThat(updated.getDeliveredAt()).isNotNull();
    }

    @Test
    @DisplayName("SECURITY: rejects webhook payload with invalid X-Hub-Signature-256 when app secret is configured")
    void testWebhookRejectsInvalidSignature() {
        whatsAppProperties.setAppSecret("test-meta-app-secret");
        try {
            WhatsAppMessageEntity entity = WhatsAppMessageEntity.builder()
                    .organizationId(testOrg.getId())
                    .recipientPhone("919876543210")
                    .templateType("INVOICE_ISSUED")
                    .templateName("invoice_issued")
                    .messageContent("Invoice test")
                    .provider("META")
                    .providerMessageId("wamid.SIGTEST01")
                    .status(WhatsAppMessageStatus.SENT)
                    .build();
            entity = messageRepository.saveAndFlush(entity);

            String webhookPayload = """
                    {"entry":[{"changes":[{"value":{"statuses":[
                      {"id":"wamid.SIGTEST01","status":"delivered","timestamp":"1724930000"}
                    ]}}]}]}
                    """;

            // Forged/mismatched signature - must be rejected, message status must NOT change.
            whatsAppNotificationService.processWebhookPayload(webhookPayload, "sha256=deadbeef00112233");

            entityManager.clear();
            WhatsAppMessageEntity unchanged = messageRepository.findById(entity.getId()).orElseThrow();
            assertThat(unchanged.getStatus()).isEqualTo(WhatsAppMessageStatus.SENT);
        } finally {
            whatsAppProperties.setAppSecret(null);
        }
    }

    @Test
    @DisplayName("SECURITY: accepts webhook payload with a correctly computed HMAC-SHA256 signature")
    void testWebhookAcceptsValidSignature() throws Exception {
        String appSecret = "test-meta-app-secret";
        whatsAppProperties.setAppSecret(appSecret);
        try {
            WhatsAppMessageEntity entity = WhatsAppMessageEntity.builder()
                    .organizationId(testOrg.getId())
                    .recipientPhone("919876543210")
                    .templateType("INVOICE_ISSUED")
                    .templateName("invoice_issued")
                    .messageContent("Invoice test")
                    .provider("META")
                    .providerMessageId("wamid.SIGTEST02")
                    .status(WhatsAppMessageStatus.SENT)
                    .build();
            entity = messageRepository.saveAndFlush(entity);

            String webhookPayload = "{\"entry\":[{\"changes\":[{\"value\":{\"statuses\":[" +
                    "{\"id\":\"wamid.SIGTEST02\",\"status\":\"delivered\",\"timestamp\":\"1724930000\"}" +
                    "]}}]}]}";

            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    appSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(webhookPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : raw) hex.append(String.format("%02x", b));
            String validSignature = "sha256=" + hex;

            whatsAppNotificationService.processWebhookPayload(webhookPayload, validSignature);

            entityManager.clear();
            WhatsAppMessageEntity updated = messageRepository.findById(entity.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(WhatsAppMessageStatus.DELIVERED);
        } finally {
            whatsAppProperties.setAppSecret(null);
        }
    }
}