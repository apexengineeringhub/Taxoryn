package com.taxoryn.module.dsc;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.dsc.dto.CreateDscRequest;
import com.taxoryn.module.dsc.dto.DscDto;
import com.taxoryn.module.dsc.dto.DscSummaryDto;
import com.taxoryn.module.dsc.dto.UpdateDscRequest;
import com.taxoryn.module.dsc.entity.DscEntity;
import com.taxoryn.module.dsc.model.DscCertificateType;
import com.taxoryn.module.dsc.model.DscStatus;
import com.taxoryn.module.dsc.repository.DscRepository;
import com.taxoryn.module.dsc.service.DscServiceImpl;
import com.taxoryn.module.notification.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DscServiceTest {

    @Mock
    private DscRepository dscRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DscServiceImpl dscService;

    private UUID organizationId;
    private UUID clientId;
    private ClientEntity clientEntity;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        TenantContext.setTenantId(organizationId);

        clientEntity = ClientEntity.builder()
                .displayName("ABC Logistics Pvt Ltd")
                .pan("AABCL1234A")
                .gstin("27AABCL1234A1Z5")
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Create DSC successfully creates active certificate entry and logs audit")
    void testCreateDsc_Success() {
        LocalDate today = LocalDate.now();
        CreateDscRequest request = CreateDscRequest.builder()
                .clientId(clientId)
                .holderName("Rajesh Kumar")
                .certificateIdentifier("SN-998877")
                .certificateType(DscCertificateType.CLASS_3)
                .issuer("eMudhra")
                .issuedDate(today.minusMonths(1))
                .expiryDate(today.plusYears(2))
                .applicableServices("GST, ITR, MCA")
                .notes("Key token in locker #1")
                .build();

        when(clientRepository.findByIdAndOrganizationId(clientId, organizationId))
                .thenReturn(Optional.of(clientEntity));

        when(dscRepository.save(any(DscEntity.class))).thenAnswer(invocation -> {
            DscEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        DscDto result = dscService.createDsc(request);

        assertThat(result).isNotNull();
        assertThat(result.getHolderName()).isEqualTo("Rajesh Kumar");
        assertThat(result.getCertificateIdentifier()).isEqualTo("SN-998877");
        assertThat(result.getStatus()).isEqualTo(DscStatus.ACTIVE);
        assertThat(result.getClientName()).isEqualTo("ABC Logistics Pvt Ltd");
        assertThat(result.getClientPan()).isEqualTo("AABCL1234A");

        verify(auditService).logEvent(eq("DSC_CREATED"), eq("DSC"), any(), any(), any());
    }

    @Test
    @DisplayName("Create DSC with invalid date range throws BusinessValidationException")
    void testCreateDsc_InvalidDates_ThrowsException() {
        LocalDate today = LocalDate.now();
        CreateDscRequest request = CreateDscRequest.builder()
                .holderName("Invalid Date Holder")
                .issuedDate(today.plusMonths(6))
                .expiryDate(today.minusDays(5)) // Issue after expiry
                .build();

        assertThatThrownBy(() -> dscService.createDsc(request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("cannot be after expiry date");
    }

    @Test
    @DisplayName("Create DSC expiring within 30 days is derived as EXPIRING status")
    void testCreateDsc_ExpiringSoon() {
        LocalDate today = LocalDate.now();
        CreateDscRequest request = CreateDscRequest.builder()
                .holderName("Priya Sharma")
                .issuedDate(today.minusYears(2))
                .expiryDate(today.plusDays(15)) // Expiring in 15 days
                .build();

        when(dscRepository.save(any(DscEntity.class))).thenAnswer(invocation -> {
            DscEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        DscDto result = dscService.createDsc(request);

        assertThat(result.getStatus()).isEqualTo(DscStatus.EXPIRING);
        assertThat(result.getDaysUntilExpiry()).isEqualTo(15);
    }

    @Test
    @DisplayName("Update DSC successfully modifies fields and updates audit trail")
    void testUpdateDsc_Success() {
        UUID dscId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        DscEntity existing = DscEntity.builder()
                .holderName("Aarav Mehta")
                .certificateType(DscCertificateType.CLASS_3)
                .issuedDate(today.minusMonths(6))
                .expiryDate(today.plusYears(1))
                .status(DscStatus.ACTIVE)
                .build();
        existing.setId(dscId);
        existing.setOrganizationId(organizationId);

        when(dscRepository.findByIdAndOrganizationId(dscId, organizationId))
                .thenReturn(Optional.of(existing));
        when(dscRepository.save(any(DscEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateDscRequest updateReq = UpdateDscRequest.builder()
                .holderName("Aarav K. Mehta")
                .issuer("Capricorn")
                .applicableServices("GST, TDS")
                .notes("Updated notes")
                .build();

        DscDto result = dscService.updateDsc(dscId, updateReq);

        assertThat(result.getHolderName()).isEqualTo("Aarav K. Mehta");
        assertThat(result.getIssuer()).isEqualTo("Capricorn");
        assertThat(result.getApplicableServices()).isEqualTo("GST, TDS");
        assertThat(result.getNotes()).isEqualTo("Updated notes");

        verify(auditService).logEvent(eq("DSC_UPDATED"), eq("DSC"), eq(dscId.toString()), any(), any());
    }

    @Test
    @DisplayName("Activate, Deactivate and Revoke lifecycle state transitions")
    void testLifecycleTransitions() {
        UUID dscId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        DscEntity entity = DscEntity.builder()
                .holderName("Kiran Rao")
                .issuedDate(today.minusMonths(1))
                .expiryDate(today.plusYears(1))
                .status(DscStatus.ACTIVE)
                .build();
        entity.setId(dscId);
        entity.setOrganizationId(organizationId);

        when(dscRepository.findByIdAndOrganizationId(dscId, organizationId))
                .thenReturn(Optional.of(entity));
        when(dscRepository.save(any(DscEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Deactivate
        DscDto deactivated = dscService.deactivateDsc(dscId);
        assertThat(deactivated.getStatus()).isEqualTo(DscStatus.INACTIVE);
        verify(auditService).logEvent(eq("DSC_DEACTIVATED"), eq("DSC"), eq(dscId.toString()), any(), any());

        // 2. Activate
        DscDto activated = dscService.activateDsc(dscId);
        assertThat(activated.getStatus()).isEqualTo(DscStatus.ACTIVE);
        verify(auditService).logEvent(eq("DSC_ACTIVATED"), eq("DSC"), eq(dscId.toString()), any(), any());

        // 3. Revoke
        DscDto revoked = dscService.revokeDsc(dscId, "Token corrupted");
        assertThat(revoked.getStatus()).isEqualTo(DscStatus.REVOKED);
        assertThat(revoked.getNotes()).contains("Token corrupted");
        verify(auditService).logEvent(eq("DSC_REVOKED"), eq("DSC"), eq(dscId.toString()), any(), any());
    }

    @Test
    @DisplayName("Get DSC Summary returns aggregated metrics breakdown")
    void testGetDscSummary() {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(30);

        when(dscRepository.countByOrganizationId(organizationId)).thenReturn(25L);
        when(dscRepository.countActiveByOrganizationId(organizationId, cutoff)).thenReturn(18L);
        when(dscRepository.countExpiringSoonByOrganizationId(organizationId, today, cutoff)).thenReturn(4L);
        when(dscRepository.countExpiredByOrganizationId(organizationId, today)).thenReturn(2L);
        when(dscRepository.countByOrganizationIdAndStatus(organizationId, DscStatus.REVOKED)).thenReturn(1L);
        when(dscRepository.countByOrganizationIdAndStatus(organizationId, DscStatus.INACTIVE)).thenReturn(0L);

        DscSummaryDto summary = dscService.getDscSummary();

        assertThat(summary.getTotal()).isEqualTo(25);
        assertThat(summary.getActive()).isEqualTo(18);
        assertThat(summary.getExpiringSoon()).isEqualTo(4);
        assertThat(summary.getExpired()).isEqualTo(2);
        assertThat(summary.getRevoked()).isEqualTo(1);
        assertThat(summary.getInactive()).isEqualTo(0);
    }

    @Test
    @DisplayName("Trigger DSC Expiry Reminders dispatches notifications for expiring certificates")
    void testCheckAndTriggerExpiryReminders() {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(30);

        DscEntity exp1 = DscEntity.builder()
                .holderName("Suresh Gupta")
                .expiryDate(today.plusDays(10))
                .build();
        exp1.setId(UUID.randomUUID());
        exp1.setOrganizationId(organizationId);

        when(dscRepository.findExpiringSoon(eq(organizationId), eq(today), eq(cutoff)))
                .thenReturn(List.of(exp1));

        int count = dscService.checkAndTriggerExpiryReminders();

        assertThat(count).isEqualTo(1);
        verify(notificationService).notify(
                eq(organizationId),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                eq("/dsc-register"),
                any()
        );
    }
}
