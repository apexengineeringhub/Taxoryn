package com.taxoryn.module.udin;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.service.repository.ServiceRepository;
import com.taxoryn.module.udin.dto.CancelUdinRequest;
import com.taxoryn.module.udin.dto.CreateUdinRequest;
import com.taxoryn.module.udin.dto.UdinDto;
import com.taxoryn.module.udin.dto.UdinSummaryDto;
import com.taxoryn.module.udin.dto.UpdateUdinRequest;
import com.taxoryn.module.udin.dto.UpdateUdinVerificationRequest;
import com.taxoryn.module.udin.entity.UdinEntity;
import com.taxoryn.module.udin.model.UdinDocumentType;
import com.taxoryn.module.udin.model.UdinStatus;
import com.taxoryn.module.udin.model.UdinVerificationStatus;
import com.taxoryn.module.udin.repository.UdinRepository;
import com.taxoryn.module.udin.service.UdinServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UdinServiceTest {

    @Mock
    private UdinRepository udinRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EngagementRepository engagementRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UdinServiceImpl udinService;

    private UUID organizationId;
    private UUID clientId;
    private ClientEntity clientEntity;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        TenantContext.setTenantId(organizationId);

        clientEntity = ClientEntity.builder()
                .displayName("Acme Global Corp")
                .pan("AACCA1234F")
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Create UDIN record successfully saves and triggers audit event")
    void testCreateUdin_Success() {
        String testUdin = "24123456AAAAAA1234";
        CreateUdinRequest request = CreateUdinRequest.builder()
                .udin(testUdin)
                .clientId(clientId)
                .documentType(UdinDocumentType.TAX_AUDIT_REPORT_3CA_3CD)
                .documentTitle("Tax Audit Report FY 2023-24")
                .signatoryName("CA Rajesh Sharma")
                .signatoryMembershipNo("123456")
                .generationDate(LocalDate.now())
                .notes("Generated on ICAI UDIN portal")
                .build();

        when(udinRepository.existsByOrganizationIdAndUdin(organizationId, testUdin)).thenReturn(false);
        when(clientRepository.findByIdAndOrganizationId(clientId, organizationId)).thenReturn(Optional.of(clientEntity));

        when(udinRepository.save(any(UdinEntity.class))).thenAnswer(inv -> {
            UdinEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        UdinDto result = udinService.createUdin(request);

        assertThat(result).isNotNull();
        assertThat(result.getUdin()).isEqualTo(testUdin);
        assertThat(result.getClientName()).isEqualTo("Acme Global Corp");
        assertThat(result.getClientPan()).isEqualTo("AACCA1234F");
        assertThat(result.getStatus()).isEqualTo(UdinStatus.ACTIVE);
        assertThat(result.getVerificationStatus()).isEqualTo(UdinVerificationStatus.NOT_VERIFIED);

        verify(auditService).logEvent(eq("UDIN_REGISTERED"), eq("UDIN"), any(), any(), any());
    }

    @Test
    @DisplayName("Create UDIN with invalid length throws BusinessValidationException")
    void testCreateUdin_InvalidLength_ThrowsException() {
        CreateUdinRequest request = CreateUdinRequest.builder()
                .udin("12345") // Invalid length
                .documentType(UdinDocumentType.NET_WORTH_CERTIFICATE)
                .documentTitle("Net Worth Certificate")
                .signatoryName("CA Priya")
                .generationDate(LocalDate.now())
                .build();

        assertThatThrownBy(() -> udinService.createUdin(request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("must be exactly 18 characters long");
    }

    @Test
    @DisplayName("Create duplicate UDIN in same organization throws BusinessValidationException")
    void testCreateUdin_Duplicate_ThrowsException() {
        String testUdin = "24123456AAAAAA1234";
        CreateUdinRequest request = CreateUdinRequest.builder()
                .udin(testUdin)
                .documentType(UdinDocumentType.GST_AUDIT_CERTIFICATE)
                .documentTitle("GST Audit Certificate")
                .signatoryName("CA Amit")
                .generationDate(LocalDate.now())
                .build();

        when(udinRepository.existsByOrganizationIdAndUdin(organizationId, testUdin)).thenReturn(true);

        assertThatThrownBy(() -> udinService.createUdin(request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Update UDIN details modifies fields and persists audit log")
    void testUpdateUdin_Success() {
        UUID udinId = UUID.randomUUID();
        UdinEntity existing = UdinEntity.builder()
                .udin("24123456AAAAAA1234")
                .documentType(UdinDocumentType.TURNOVER_CERTIFICATE)
                .documentTitle("Turnover Cert 2024")
                .signatoryName("CA Rakesh")
                .generationDate(LocalDate.now())
                .status(UdinStatus.ACTIVE)
                .verificationStatus(UdinVerificationStatus.NOT_VERIFIED)
                .build();
        existing.setId(udinId);
        existing.setOrganizationId(organizationId);

        when(udinRepository.findByIdAndOrganizationId(udinId, organizationId)).thenReturn(Optional.of(existing));
        when(udinRepository.save(any(UdinEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUdinRequest updateReq = UpdateUdinRequest.builder()
                .documentType(UdinDocumentType.TURNOVER_CERTIFICATE)
                .documentTitle("Turnover Cert 2024 - Revised")
                .signatoryName("CA Rakesh Kumar")
                .generationDate(LocalDate.now())
                .notes("Updated notes")
                .build();

        UdinDto updated = udinService.updateUdin(udinId, updateReq);

        assertThat(updated.getDocumentTitle()).isEqualTo("Turnover Cert 2024 - Revised");
        assertThat(updated.getSignatoryName()).isEqualTo("CA Rakesh Kumar");
        verify(auditService).logEvent(eq("UDIN_UPDATED"), eq("UDIN"), eq(udinId.toString()), any(), any());
    }

    @Test
    @DisplayName("Update Verification Status sets status, timestamp, and audit record")
    void testUpdateVerification_Success() {
        UUID udinId = UUID.randomUUID();
        UdinEntity existing = UdinEntity.builder()
                .udin("24123456AAAAAA1234")
                .documentType(UdinDocumentType.FORM_15CB_CERTIFICATION)
                .documentTitle("Form 15CB Cert")
                .signatoryName("CA Rakesh")
                .generationDate(LocalDate.now())
                .status(UdinStatus.ACTIVE)
                .verificationStatus(UdinVerificationStatus.NOT_VERIFIED)
                .build();
        existing.setId(udinId);
        existing.setOrganizationId(organizationId);

        when(udinRepository.findByIdAndOrganizationId(udinId, organizationId)).thenReturn(Optional.of(existing));
        when(udinRepository.save(any(UdinEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUdinVerificationRequest request = UpdateUdinVerificationRequest.builder()
                .verificationStatus(UdinVerificationStatus.VERIFIED)
                .verificationSource("ICAI_PORTAL_CHECK")
                .verificationRemarks("Verified against ICAI portal successfully")
                .build();

        UdinDto verified = udinService.updateVerification(udinId, request);

        assertThat(verified.getVerificationStatus()).isEqualTo(UdinVerificationStatus.VERIFIED);
        assertThat(verified.getVerificationRemarks()).isEqualTo("Verified against ICAI portal successfully");
        verify(auditService).logEvent(eq("UDIN_VERIFICATION_STATUS_CHANGED"), eq("UDIN"), eq(udinId.toString()), eq("NOT_VERIFIED"), any());
    }

    @Test
    @DisplayName("Cancel UDIN marks status as CANCELLED with reason")
    void testCancelUdin_Success() {
        UUID udinId = UUID.randomUUID();
        UdinEntity existing = UdinEntity.builder()
                .udin("24123456AAAAAA1234")
                .documentType(UdinDocumentType.OTHER)
                .documentTitle("Draft Report")
                .signatoryName("CA Rakesh")
                .generationDate(LocalDate.now())
                .status(UdinStatus.ACTIVE)
                .build();
        existing.setId(udinId);
        existing.setOrganizationId(organizationId);

        when(udinRepository.findByIdAndOrganizationId(udinId, organizationId)).thenReturn(Optional.of(existing));
        when(udinRepository.save(any(UdinEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        CancelUdinRequest request = CancelUdinRequest.builder()
                .reason("Generated with typo in turnover figure")
                .build();

        UdinDto cancelled = udinService.cancelUdin(udinId, request);

        assertThat(cancelled.getStatus()).isEqualTo(UdinStatus.CANCELLED);
        assertThat(cancelled.getNotes()).contains("Generated with typo in turnover figure");
        verify(auditService).logEvent(eq("UDIN_CANCELLED"), eq("UDIN"), eq(udinId.toString()), any(), any());
    }

    @Test
    @DisplayName("Get UDIN Summary computes correct metrics")
    void testGetUdinSummary() {
        when(udinRepository.countByOrganizationId(organizationId)).thenReturn(30L);
        when(udinRepository.countByOrganizationIdAndStatus(organizationId, UdinStatus.ACTIVE)).thenReturn(25L);
        when(udinRepository.countByOrganizationIdAndStatus(organizationId, UdinStatus.CANCELLED)).thenReturn(5L);
        when(udinRepository.countByOrganizationIdAndVerificationStatus(organizationId, UdinVerificationStatus.VERIFIED)).thenReturn(20L);
        when(udinRepository.countByOrganizationIdAndVerificationStatus(organizationId, UdinVerificationStatus.NOT_VERIFIED)).thenReturn(8L);
        when(udinRepository.countByOrganizationIdAndVerificationStatus(organizationId, UdinVerificationStatus.FAILED)).thenReturn(2L);

        UdinSummaryDto summary = udinService.getUdinSummary();

        assertThat(summary.getTotalCount()).isEqualTo(30);
        assertThat(summary.getActiveCount()).isEqualTo(25);
        assertThat(summary.getCancelledCount()).isEqualTo(5);
        assertThat(summary.getVerifiedCount()).isEqualTo(20);
        assertThat(summary.getUnverifiedCount()).isEqualTo(8);
        assertThat(summary.getFailedVerificationCount()).isEqualTo(2);
    }
}
