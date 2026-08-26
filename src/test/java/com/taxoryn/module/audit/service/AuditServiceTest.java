package com.taxoryn.module.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.module.audit.dto.AuditLogDto;
import com.taxoryn.module.audit.dto.AuditLogFilterRequest;
import com.taxoryn.module.audit.dto.AuditRecordRequest;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuditServiceImpl auditService;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .email("admin@taxoryn.com")
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("AUDIT_READ"))
                .enabled(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully record audit log with explicit context")
    void testRecordAuditExplicit() {
        UUID entityId = UUID.randomUUID();
        AuditRecordRequest request = AuditRecordRequest.builder()
                .organizationId(tenantId)
                .userId(userId)
                .action("CLIENT_CREATED")
                .entityType("CLIENT")
                .entityId(entityId.toString())
                .oldValue(null)
                .newValue("{\"displayName\":\"Test Client\"}")
                .ipAddress("192.168.1.100")
                .requestId("req-trace-123")
                .userAgent("Taxoryn-Agent/1.0")
                .build();

        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(invocation -> {
            AuditLogEntity saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        AuditLogDto result = auditService.recordAudit(request);

        assertNotNull(result);
        assertEquals("CLIENT_CREATED", result.getAction());
        assertEquals("CLIENT", result.getEntityType());
        assertEquals(entityId.toString(), result.getEntityId());
        assertEquals("req-trace-123", result.getRequestId());
        assertEquals("192.168.1.100", result.getIpAddress());
        assertEquals(tenantId, result.getOrganizationId());
        assertEquals(userId, result.getUserId());

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntity entity = captor.getValue();
        assertEquals(tenantId, entity.getOrganizationId());
        assertEquals("CLIENT_CREATED", entity.getAction());
    }

    @Test
    @DisplayName("Should automatically resolve tenant and user context when logging events")
    void testLogEventAutoContext() {
        UUID entityId = UUID.randomUUID();

        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(invocation -> {
            AuditLogEntity saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        AuditLogDto result = auditService.logEvent(
                "INVOICE_STATUS_UPDATED",
                "INVOICE",
                entityId.toString(),
                "DRAFT",
                "ISSUED"
        );

        assertNotNull(result);
        assertEquals("INVOICE_STATUS_UPDATED", result.getAction());
        assertEquals("INVOICE", result.getEntityType());
        assertEquals(tenantId, result.getOrganizationId());
        assertEquals(userId, result.getUserId());
        assertEquals("DRAFT", result.getOldValue());
        assertEquals("ISSUED", result.getNewValue());
        assertNotNull(result.getRequestId());
    }

    @Test
    @DisplayName("Should query audit logs with filters, pagination and tenant isolation")
    void testGetAuditLogsWithFilters() {
        AuditLogEntity log1 = AuditLogEntity.builder()
                .organizationId(tenantId)
                .userId(userId)
                .action("GST_PROFILE_CREATED")
                .entityType("GST_PROFILE")
                .entityName("GST_PROFILE")
                .entityId(UUID.randomUUID().toString())
                .ipAddress("10.0.0.1")
                .requestId("trace-gst-1")
                .createdAt(Instant.now())
                .build();
        log1.setId(UUID.randomUUID());

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log1)));
        when(organizationRepository.findAllById(any())).thenReturn(Collections.emptyList());
        when(userRepository.findAllById(any())).thenReturn(Collections.emptyList());

        AuditLogFilterRequest filter = AuditLogFilterRequest.builder()
                .entityType("GST_PROFILE")
                .action("GST_PROFILE_CREATED")
                .userId(userId)
                .requestId("trace-gst-1")
                .startDate(Instant.now().minus(7, ChronoUnit.DAYS))
                .endDate(Instant.now())
                .search("GST")
                .page(0)
                .size(10)
                .sortBy("createdAt")
                .sortDirection("DESC")
                .build();

        PagedResponse<AuditLogDto> response = auditService.getAuditLogs(filter);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("GST_PROFILE_CREATED", response.getContent().get(0).getAction());
        assertEquals("GST_PROFILE", response.getContent().get(0).getEntityType());
        assertEquals("trace-gst-1", response.getContent().get(0).getRequestId());
    }
}
