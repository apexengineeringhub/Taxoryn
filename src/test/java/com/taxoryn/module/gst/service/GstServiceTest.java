package com.taxoryn.module.gst.service;

import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.dto.BatchGenerateFilingsRequest;
import com.taxoryn.module.gst.dto.CreateGstProfileRequest;
import com.taxoryn.module.gst.dto.CreateGstReturnFilingRequest;
import com.taxoryn.module.gst.dto.GstMonthlySummaryDto;
import com.taxoryn.module.gst.dto.GstProfileDto;
import com.taxoryn.module.gst.dto.GstReturnFilingDto;
import com.taxoryn.module.gst.dto.GstWorkloadDashboardDto;
import com.taxoryn.module.gst.dto.SaveGstMonthlySummaryRequest;
import com.taxoryn.module.gst.entity.GstMonthlySummaryEntity;
import com.taxoryn.module.gst.entity.GstProfileEntity;
import com.taxoryn.module.gst.entity.GstProfileEntity.FilingFrequency;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstProfileStatus;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstType;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import com.taxoryn.module.gst.mapper.GstMapper;
import com.taxoryn.module.gst.repository.GstMonthlySummaryRepository;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GstServiceTest {

    @Mock
    private GstProfileRepository gstProfileRepository;

    @Mock
    private GstReturnFilingRepository gstReturnFilingRepository;

    @Mock
    private GstMonthlySummaryRepository gstMonthlySummaryRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private GstMapper gstMapper;

    @Mock
    private com.taxoryn.module.audit.service.AuditService auditService;

    @InjectMocks
    private GstServiceImpl gstService;

    private UUID tenantId;
    private UUID clientId;
    private UUID profileId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        profileId = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(tenantId)
                .email("admin@taxpractice.com")
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("GST_CREATE", "GST_VIEW", "GST_UPDATE"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Create GST Profile successfully")
    void testCreateProfileSuccess() {
        CreateGstProfileRequest request = CreateGstProfileRequest.builder()
                .clientId(clientId)
                .gstin("27AAACZ1234D1Z8")
                .legalName("ABC Traders Pvt Ltd")
                .tradeName("ABC Traders")
                .gstType(GstType.REGULAR)
                .filingFrequency(FilingFrequency.MONTHLY)
                .build();

        ClientEntity client = ClientEntity.builder().displayName("ABC Traders").build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(gstProfileRepository.existsByOrganizationIdAndGstin(tenantId, "27AAACZ1234D1Z8")).thenReturn(false);

        GstProfileEntity saved = GstProfileEntity.builder()
                .clientId(clientId)
                .gstin("27AAACZ1234D1Z8")
                .legalName("ABC Traders Pvt Ltd")
                .tradeName("ABC Traders")
                .gstType(GstType.REGULAR)
                .filingFrequency(FilingFrequency.MONTHLY)
                .status(GstProfileStatus.ACTIVE)
                .build();
        saved.setId(profileId);
        saved.setOrganizationId(tenantId);

        when(gstProfileRepository.save(any(GstProfileEntity.class))).thenReturn(saved);
        when(gstMapper.toDto(saved)).thenReturn(GstProfileDto.builder()
                .id(profileId)
                .clientId(clientId)
                .gstin("27AAACZ1234D1Z8")
                .tradeName("ABC Traders")
                .build());

        GstProfileDto result = gstService.createProfile(request);

        assertNotNull(result);
        assertEquals("27AAACZ1234D1Z8", result.getGstin());
        assertEquals("ABC Traders", result.getTradeName());
    }

    @Test
    @DisplayName("Create GST Profile with duplicate GSTIN throws exception")
    void testCreateProfileDuplicateGstinThrows() {
        CreateGstProfileRequest request = CreateGstProfileRequest.builder()
                .clientId(clientId)
                .gstin("27AAACZ1234D1Z8")
                .filingFrequency(FilingFrequency.MONTHLY)
                .build();

        when(gstProfileRepository.existsByOrganizationIdAndGstin(tenantId, "27AAACZ1234D1Z8")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> gstService.createProfile(request));
    }

    @Test
    @DisplayName("Save monthly summary and calculate net liability")
    void testSaveMonthlySummary() {
        SaveGstMonthlySummaryRequest request = SaveGstMonthlySummaryRequest.builder()
                .gstProfileId(profileId)
                .period("2026-08")
                .financialYear("2026-27")
                .totalSalesTaxable(new BigDecimal("1000000.00"))
                .igstSales(new BigDecimal("80000.00"))
                .cgstSales(new BigDecimal("50000.00"))
                .sgstSales(new BigDecimal("50000.00"))
                .itcEligible(new BigDecimal("125000.00"))
                .itcNetClaimed(new BigDecimal("125000.00"))
                .build();

        GstProfileEntity profile = GstProfileEntity.builder().clientId(clientId).gstin("27AAACZ1234D1Z8").build();
        profile.setId(profileId);
        profile.setOrganizationId(tenantId);

        when(gstProfileRepository.findByIdAndOrganizationId(profileId, tenantId)).thenReturn(Optional.of(profile));
        when(gstMonthlySummaryRepository.findByOrganizationIdAndGstProfileIdAndPeriod(tenantId, profileId, "2026-08"))
                .thenReturn(Optional.empty());

        GstMonthlySummaryEntity saved = GstMonthlySummaryEntity.builder()
                .gstProfileId(profileId)
                .clientId(clientId)
                .period("2026-08")
                .financialYear("2026-27")
                .totalSalesTaxable(new BigDecimal("1000000.00"))
                .itcNetClaimed(new BigDecimal("125000.00"))
                .netTaxLiability(new BigDecimal("55000.00"))
                .build();
        saved.setId(UUID.randomUUID());
        saved.setOrganizationId(tenantId);

        when(gstMonthlySummaryRepository.save(any(GstMonthlySummaryEntity.class))).thenReturn(saved);
        when(gstMapper.toSummaryDto(saved)).thenReturn(GstMonthlySummaryDto.builder()
                .period("2026-08")
                .itcNetClaimed(new BigDecimal("125000.00"))
                .netTaxLiability(new BigDecimal("55000.00"))
                .build());

        GstMonthlySummaryDto result = gstService.saveMonthlySummary(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("125000.00"), result.getItcNetClaimed());
        assertEquals(new BigDecimal("55000.00"), result.getNetTaxLiability());
    }

    @Test
    @DisplayName("Get GST Workload Dashboard aggregated for period")
    void testGetWorkloadDashboard() {
        GstProfileEntity profile = GstProfileEntity.builder()
                .clientId(clientId)
                .gstin("27AAACZ1234D1Z8")
                .tradeName("ABC Traders")
                .assignedEmployeeId(employeeId)
                .gstType(GstType.REGULAR)
                .status(GstProfileStatus.ACTIVE)
                .build();
        profile.setId(profileId);
        profile.setOrganizationId(tenantId);

        when(gstProfileRepository.findAllByOrganizationIdAndStatus(tenantId, GstProfileStatus.ACTIVE))
                .thenReturn(List.of(profile));

        ClientEntity client = ClientEntity.builder().displayName("ABC Traders").build();
        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));

        EmployeeEntity emp = EmployeeEntity.builder().firstName("Rahul").lastName("Sharma").build();
        when(employeeRepository.findByIdAndOrganizationId(employeeId, tenantId)).thenReturn(Optional.of(emp));

        GstReturnFilingEntity gstr1 = GstReturnFilingEntity.builder()
                .filingStatus(GstFilingStatus.PENDING)
                .dueDate(LocalDate.of(2026, 9, 11))
                .build();
        GstReturnFilingEntity gstr3b = GstReturnFilingEntity.builder()
                .filingStatus(GstFilingStatus.PENDING)
                .dueDate(LocalDate.of(2026, 9, 20))
                .build();

        when(gstReturnFilingRepository.findByOrganizationIdAndGstProfileIdAndReturnTypeAndReturnPeriod(
                tenantId, profileId, GstReturnType.GSTR1, "2026-08")).thenReturn(Optional.of(gstr1));
        when(gstReturnFilingRepository.findByOrganizationIdAndGstProfileIdAndReturnTypeAndReturnPeriod(
                tenantId, profileId, GstReturnType.GSTR3B, "2026-08")).thenReturn(Optional.of(gstr3b));

        GstMonthlySummaryEntity summary = GstMonthlySummaryEntity.builder()
                .itcNetClaimed(new BigDecimal("125000.00"))
                .netTaxLiability(new BigDecimal("82000.00"))
                .build();
        when(gstMonthlySummaryRepository.findByOrganizationIdAndGstProfileIdAndPeriod(tenantId, profileId, "2026-08"))
                .thenReturn(Optional.of(summary));

        GstWorkloadDashboardDto dashboard = gstService.getWorkloadDashboard("2026-08", null);

        assertNotNull(dashboard);
        assertEquals(1, dashboard.getTotalGstClients());
        assertEquals(1, dashboard.getGstr1PendingCount());
        assertEquals(1, dashboard.getGstr3bPendingCount());
        assertEquals(new BigDecimal("125000.00"), dashboard.getTotalItcTracked());
        assertEquals(new BigDecimal("82000.00"), dashboard.getTotalTaxLiability());
        assertEquals(1, dashboard.getClients().size());
        assertEquals("ABC Traders", dashboard.getClients().get(0).getClientName());
        assertEquals("Rahul Sharma", dashboard.getClients().get(0).getAssignedTo());
        assertEquals(new BigDecimal("125000.00"), dashboard.getClients().get(0).getItc());
        assertEquals(new BigDecimal("82000.00"), dashboard.getClients().get(0).getTaxLiability());
    }
}
