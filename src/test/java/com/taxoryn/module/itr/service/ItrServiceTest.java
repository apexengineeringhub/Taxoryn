package com.taxoryn.module.itr.service;

import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.itr.dto.CreateItrProfileRequest;
import com.taxoryn.module.itr.dto.CreateItrReturnRequest;
import com.taxoryn.module.itr.dto.ItrProfileDto;
import com.taxoryn.module.itr.dto.ItrReturnDto;
import com.taxoryn.module.itr.dto.ItrWorkloadDashboardDto;
import com.taxoryn.module.itr.dto.RecordItrFilingRequest;
import com.taxoryn.module.itr.dto.UpdateItrStatusRequest;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrProfileStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.mapper.ItrMapper;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
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
class ItrServiceTest {

    @Mock
    private ItrProfileRepository itrProfileRepository;

    @Mock
    private ItrReturnRepository itrReturnRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ItrMapper itrMapper;

    @Mock
    private com.taxoryn.module.audit.service.AuditService auditService;

    @InjectMocks
    private ItrServiceImpl itrService;

    private UUID tenantId;
    private UUID clientId;
    private UUID profileId;
    private UUID returnId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        profileId = UUID.randomUUID();
        returnId = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(tenantId)
                .email("admin@taxpractice.com")
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("ITR_CREATE", "ITR_VIEW", "ITR_UPDATE"))
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
    @DisplayName("Create ITR Profile successfully")
    void testCreateProfileSuccess() {
        CreateItrProfileRequest request = CreateItrProfileRequest.builder()
                .clientId(clientId)
                .pan("ABCPJ9876M")
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .defaultItrType(ItrType.ITR_1)
                .build();

        ClientEntity client = ClientEntity.builder().displayName("Anand Joshi").build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(itrProfileRepository.existsByOrganizationIdAndPan(tenantId, "ABCPJ9876M")).thenReturn(false);
        when(itrProfileRepository.existsByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(false);

        ItrProfileEntity saved = ItrProfileEntity.builder()
                .clientId(clientId)
                .pan("ABCPJ9876M")
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .defaultItrType(ItrType.ITR_1)
                .status(ItrProfileStatus.ACTIVE)
                .build();
        saved.setId(profileId);
        saved.setOrganizationId(tenantId);

        when(itrProfileRepository.save(any(ItrProfileEntity.class))).thenReturn(saved);
        when(itrMapper.toProfileDto(saved)).thenReturn(ItrProfileDto.builder()
                .id(profileId)
                .pan("ABCPJ9876M")
                .clientName("Anand Joshi")
                .build());

        ItrProfileDto result = itrService.createProfile(request);

        assertNotNull(result);
        assertEquals("ABCPJ9876M", result.getPan());
    }

    @Test
    @DisplayName("Create ITR Profile duplicate PAN throws DuplicateResourceException")
    void testCreateProfileDuplicatePanThrows() {
        CreateItrProfileRequest request = CreateItrProfileRequest.builder()
                .clientId(clientId)
                .pan("ABCPJ9876M")
                .build();

        when(itrProfileRepository.existsByOrganizationIdAndPan(tenantId, "ABCPJ9876M")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> itrService.createProfile(request));
    }

    @Test
    @DisplayName("Create ITR Return successfully")
    void testCreateReturnSuccess() {
        CreateItrReturnRequest request = CreateItrReturnRequest.builder()
                .clientId(clientId)
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .dueDate(LocalDate.of(2026, 7, 31))
                .build();

        ClientEntity client = ClientEntity.builder().displayName("Anand Joshi").build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(itrReturnRepository.existsByOrganizationIdAndClientIdAndAssessmentYear(tenantId, clientId, "2026-27")).thenReturn(false);
        when(itrProfileRepository.findByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(Optional.empty());

        ItrReturnEntity saved = ItrReturnEntity.builder()
                .clientId(clientId)
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .dueDate(LocalDate.of(2026, 7, 31))
                .status(ItrStatus.DOCUMENTS_PENDING)
                .build();
        saved.setId(returnId);
        saved.setOrganizationId(tenantId);

        when(itrReturnRepository.save(any(ItrReturnEntity.class))).thenReturn(saved);
        when(itrMapper.toReturnDto(saved)).thenReturn(ItrReturnDto.builder()
                .id(returnId)
                .assessmentYear("2026-27")
                .status(ItrStatus.DOCUMENTS_PENDING)
                .build());

        ItrReturnDto result = itrService.createReturn(request);

        assertNotNull(result);
        assertEquals("2026-27", result.getAssessmentYear());
        assertEquals(ItrStatus.DOCUMENTS_PENDING, result.getStatus());
    }

    @Test
    @DisplayName("Update ITR status workflow transition")
    void testUpdateStatusWorkflow() {
        ItrReturnEntity ret = ItrReturnEntity.builder()
                .clientId(clientId)
                .assessmentYear("2026-27")
                .status(ItrStatus.DOCUMENTS_PENDING)
                .build();
        ret.setId(returnId);
        ret.setOrganizationId(tenantId);

        when(itrReturnRepository.findByIdAndOrganizationId(returnId, tenantId)).thenReturn(Optional.of(ret));
        when(itrReturnRepository.save(ret)).thenReturn(ret);
        when(itrMapper.toReturnDto(ret)).thenReturn(ItrReturnDto.builder().id(returnId).status(ItrStatus.UNDER_REVIEW).build());

        ItrReturnDto result = itrService.updateStatus(returnId, new UpdateItrStatusRequest(ItrStatus.UNDER_REVIEW, "Documents verified"));

        assertNotNull(result);
        assertEquals(ItrStatus.UNDER_REVIEW, result.getStatus());
    }

    @Test
    @DisplayName("Record ITR filing details & acknowledgment number")
    void testRecordFilingDetails() {
        ItrReturnEntity ret = ItrReturnEntity.builder()
                .clientId(clientId)
                .assessmentYear("2026-27")
                .status(ItrStatus.READY_TO_FILE)
                .build();
        ret.setId(returnId);
        ret.setOrganizationId(tenantId);

        RecordItrFilingRequest filingReq = RecordItrFilingRequest.builder()
                .filingDate(LocalDate.of(2026, 7, 28))
                .acknowledgementNumber("123456789012345")
                .verificationDate(LocalDate.of(2026, 7, 28))
                .notes("Filed and e-verified with Aadhaar OTP")
                .build();

        when(itrReturnRepository.findByIdAndOrganizationId(returnId, tenantId)).thenReturn(Optional.of(ret));
        when(itrReturnRepository.save(ret)).thenReturn(ret);
        when(itrMapper.toReturnDto(ret)).thenReturn(ItrReturnDto.builder()
                .id(returnId)
                .status(ItrStatus.COMPLETED)
                .acknowledgementNumber("123456789012345")
                .build());

        ItrReturnDto result = itrService.recordFilingDetails(returnId, filingReq);

        assertNotNull(result);
        assertEquals("123456789012345", result.getAcknowledgementNumber());
        assertEquals(ItrStatus.COMPLETED, result.getStatus());
    }

    @Test
    @DisplayName("Get ITR Workload Dashboard")
    void testGetWorkloadDashboard() {
        ItrReturnEntity ret = ItrReturnEntity.builder()
                .clientId(clientId)
                .assessmentYear("2026-27")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .status(ItrStatus.DATA_ENTRY)
                .dueDate(LocalDate.of(2026, 7, 31))
                .assignedEmployeeId(employeeId)
                .build();
        ret.setId(returnId);
        ret.setOrganizationId(tenantId);

        when(itrReturnRepository.findAllByOrganizationIdAndAssessmentYear(tenantId, "2026-27"))
                .thenReturn(List.of(ret));

        ClientEntity client = ClientEntity.builder().displayName("Anand Joshi").pan("ABCPJ9876M").build();
        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));

        EmployeeEntity emp = EmployeeEntity.builder().firstName("Vikram").lastName("Sharma").build();
        when(employeeRepository.findByIdAndOrganizationId(employeeId, tenantId)).thenReturn(Optional.of(emp));

        ItrWorkloadDashboardDto dashboard = itrService.getWorkloadDashboard("2026-27", null);

        assertNotNull(dashboard);
        assertEquals("2026-27", dashboard.getAssessmentYear());
        assertEquals(1, dashboard.getTotalReturns());
        assertEquals(1, dashboard.getDataEntryCount());
        assertEquals(1, dashboard.getReturns().size());
        assertEquals("Anand Joshi", dashboard.getReturns().get(0).getClientName());
        assertEquals("Vikram Sharma", dashboard.getReturns().get(0).getAssignedTo());
    }
}
