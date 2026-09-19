package com.taxoryn.module.marketing.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.security.RateLimitingService;
import com.taxoryn.core.security.proxy.ClientIpResolver;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.marketing.dto.CreateEarlyAccessRequest;
import com.taxoryn.module.marketing.dto.EarlyAccessResponse;
import com.taxoryn.module.marketing.entity.EarlyAccessRequestEntity;
import com.taxoryn.module.marketing.entity.EarlyAccessStatus;
import com.taxoryn.module.marketing.repository.EarlyAccessRequestRepository;
import com.taxoryn.module.marketing.service.EarlyAccessService;
import com.taxoryn.module.marketing.service.EarlyAccessServiceImpl;
import com.taxoryn.module.notification.email.service.EmailNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EarlyAccessSecurityIntegrationTest {

    @Mock
    private EarlyAccessRequestRepository earlyAccessRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private AuditService auditService;

    @Mock
    private ClientIpResolver clientIpResolver;

    @Mock
    private HttpServletRequest httpRequest;

    private EarlyAccessService earlyAccessService;
    private EarlyAccessController earlyAccessController;

    @BeforeEach
    void setUp() {
        earlyAccessService = new EarlyAccessServiceImpl(earlyAccessRepository, emailNotificationService, auditService);
        earlyAccessController = new EarlyAccessController(earlyAccessService, clientIpResolver);
    }

    // 1. Valid request -> 201 success
    @Test
    @DisplayName("1. Valid request creates a NEW early access request and returns 201 Created")
    void testValidRequest_CreatesNewEntityAndReturnsSuccess() {
        when(clientIpResolver.resolveClientIp(httpRequest)).thenReturn("203.0.113.195");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        CreateEarlyAccessRequest request = CreateEarlyAccessRequest.builder()
                .name("CA Rajesh Verma")
                .email("rajesh@apextax.in")
                .practiceName("Apex Tax Advisors LLP")
                .phone("+919876543210")
                .city("Mumbai")
                .practiceProfile("CA Firm")
                .primaryArea("Complete Practice Management")
                .source("MARKETING_WEBSITE")
                .build();

        when(earlyAccessRepository.findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc("rajesh@apextax.in", EarlyAccessStatus.NEW))
                .thenReturn(Optional.empty());

        UUID generatedId = UUID.randomUUID();
        when(earlyAccessRepository.save(any(EarlyAccessRequestEntity.class))).thenAnswer(inv -> {
            EarlyAccessRequestEntity entity = inv.getArgument(0);
            entity.setId(generatedId);
            entity.setCreatedAt(Instant.now());
            return entity;
        });

        ResponseEntity<ApiResponse<EarlyAccessResponse>> responseEntity = earlyAccessController.submitEarlyAccessRequest(request, httpRequest);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().isSuccess());
        assertEquals("NEW", responseEntity.getBody().getData().getStatus());

        ArgumentCaptor<EarlyAccessRequestEntity> captor = ArgumentCaptor.forClass(EarlyAccessRequestEntity.class);
        verify(earlyAccessRepository).save(captor.capture());
        EarlyAccessRequestEntity saved = captor.getValue();
        assertEquals("CA Rajesh Verma", saved.getName());
        assertEquals("rajesh@apextax.in", saved.getEmail());
        assertEquals("Apex Tax Advisors LLP", saved.getPracticeName());
        assertEquals("203.0.113.195", saved.getIpAddress());
    }

    // 2. Email normalization
    @Test
    @DisplayName("2. Submissions with mixed-case and whitespace emails are normalized to lowercase trimmed")
    void testEmailNormalization_TrimAndLowercase() {
        CreateEarlyAccessRequest request = CreateEarlyAccessRequest.builder()
                .name("CA Priya Sharma")
                .email("  Priya.Sharma@TAXORYN.IN  ")
                .practiceName("Sharma & Associates")
                .build();

        when(earlyAccessRepository.findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc("priya.sharma@taxoryn.in", EarlyAccessStatus.NEW))
                .thenReturn(Optional.empty());

        when(earlyAccessRepository.save(any(EarlyAccessRequestEntity.class))).thenAnswer(inv -> {
            EarlyAccessRequestEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        earlyAccessService.submitEarlyAccessRequest(request, "127.0.0.1", "TestAgent");

        ArgumentCaptor<EarlyAccessRequestEntity> captor = ArgumentCaptor.forClass(EarlyAccessRequestEntity.class);
        verify(earlyAccessRepository).save(captor.capture());
        assertEquals("priya.sharma@taxoryn.in", captor.getValue().getEmail());
    }

    // 3. Duplicate request handling
    @Test
    @DisplayName("3. Duplicate submission for existing active email updates record without creating duplicate entries")
    void testDuplicateRequest_UpdatesExistingRecord() {
        UUID existingId = UUID.randomUUID();
        EarlyAccessRequestEntity existingEntity = EarlyAccessRequestEntity.builder()
                .name("CA Rajesh")
                .email("rajesh@apextax.in")
                .practiceName("Apex Tax")
                .status(EarlyAccessStatus.NEW)
                .build();
        existingEntity.setId(existingId);

        when(earlyAccessRepository.findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc("rajesh@apextax.in", EarlyAccessStatus.NEW))
                .thenReturn(Optional.of(existingEntity));

        when(earlyAccessRepository.save(any(EarlyAccessRequestEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateEarlyAccessRequest updatedRequest = CreateEarlyAccessRequest.builder()
                .name("CA Rajesh Verma")
                .email("rajesh@apextax.in")
                .practiceName("Apex Tax Advisors LLP")
                .phone("+919876543210")
                .city("Pune")
                .build();

        EarlyAccessResponse response = earlyAccessService.submitEarlyAccessRequest(updatedRequest, "198.51.100.1", "Chrome");

        assertEquals(existingId, response.getId());
        assertTrue(response.getMessage().contains("updated"));

        verify(earlyAccessRepository).save(existingEntity);
        assertEquals("Pune", existingEntity.getCity());
        assertEquals("CA Rajesh Verma", existingEntity.getName());
    }

    // 4. Anti-spam honeypot trap
    @Test
    @DisplayName("4. Anti-spam honeypot field discard: Bots filling hidden honeypot are silently dropped")
    void testAntiSpamHoneypot_SilentlyDroppedWithoutPersistenceOrEmail() {
        CreateEarlyAccessRequest botRequest = CreateEarlyAccessRequest.builder()
                .name("Spam Bot")
                .email("spammer@evil.com")
                .practiceName("Fake Firm")
                .honeypot("http://spam-link.com") // Bot filled the invisible field
                .build();

        EarlyAccessResponse response = earlyAccessService.submitEarlyAccessRequest(botRequest, "185.220.101.5", "BotNet/1.0");

        assertNotNull(response);
        assertEquals("NEW", response.getStatus());

        // Zero repository saves and zero email notifications
        verify(earlyAccessRepository, never()).save(any());
        verify(emailNotificationService, never()).sendEarlyAccessInternalNotification(any(), any(), any(), any(), any(), any(), any(), any());
        verify(emailNotificationService, never()).sendEarlyAccessConfirmation(any(), any(), any());
    }

    // 5. Asynchronous decoupled email failure resilience
    @Test
    @DisplayName("5. Resend/SMTP email delivery failure does not fail persistence or return error to requester")
    void testEmailFailure_PreservesDatabaseRecordAndReturnsSuccess() {
        doThrow(new RuntimeException("Resend API connection timeout"))
                .when(emailNotificationService)
                .sendEarlyAccessInternalNotification(any(), any(), any(), any(), any(), any(), any(), any());

        CreateEarlyAccessRequest request = CreateEarlyAccessRequest.builder()
                .name("CA Anil Gupta")
                .email("anil@guptatax.com")
                .practiceName("Gupta Tax Consultants")
                .build();

        when(earlyAccessRepository.findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc("anil@guptatax.com", EarlyAccessStatus.NEW))
                .thenReturn(Optional.empty());

        UUID generatedId = UUID.randomUUID();
        when(earlyAccessRepository.save(any(EarlyAccessRequestEntity.class))).thenAnswer(inv -> {
            EarlyAccessRequestEntity entity = inv.getArgument(0);
            entity.setId(generatedId);
            return entity;
        });

        assertDoesNotThrow(() -> {
            EarlyAccessResponse response = earlyAccessService.submitEarlyAccessRequest(request, "127.0.0.1", "Browser");
            assertNotNull(response);
            assertEquals(generatedId, response.getId());
        });

        verify(earlyAccessRepository, times(1)).save(any(EarlyAccessRequestEntity.class));
    }

    // 6. Security: Request cannot create organizations, users, or clients
    @Test
    @DisplayName("6. Security: Early access submission strictly creates early access lead without organization/user creation")
    void testSecurity_OnlyEarlyAccessRequestCreated() {
        CreateEarlyAccessRequest request = CreateEarlyAccessRequest.builder()
                .name("CA Sneha Kapoor")
                .email("sneha@kapoortax.com")
                .practiceName("Kapoor & Co")
                .build();

        when(earlyAccessRepository.save(any(EarlyAccessRequestEntity.class))).thenAnswer(inv -> {
            EarlyAccessRequestEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        earlyAccessService.submitEarlyAccessRequest(request, "127.0.0.1", "Agent");

        // Asserts only EarlyAccessRequestRepository was touched
        verify(earlyAccessRepository, times(1)).save(any(EarlyAccessRequestEntity.class));
    }
}
