package com.taxoryn.module.tds.service;

import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.tds.dto.*;
import com.taxoryn.module.tds.entity.*;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFormType;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import com.taxoryn.module.tds.mapper.TdsMapper;
import com.taxoryn.module.tds.repository.*;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TdsServiceTest {

    @Mock
    private TdsProfileRepository tdsProfileRepository;

    @Mock
    private TdsReturnRepository tdsReturnRepository;

    @Mock
    private TdsChallanRepository tdsChallanRepository;

    @Mock
    private TdsDeducteeEntryRepository tdsDeducteeEntryRepository;

    @Mock
    private TdsCertificateRepository tdsCertificateRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TdsMapper tdsMapper;

    @Mock
    private TdsCalculatorService tdsCalculatorService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TdsServiceImpl tdsService;

    private UUID organizationId;
    private UUID clientId;
    private UUID profileId;
    private UUID returnId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        profileId = UUID.randomUUID();
        returnId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        TenantContext.setTenantId(organizationId);
        SecurityUser user = SecurityUser.builder()
                .userId(userId)
                .organizationId(organizationId)
                .email("admin@apextax.com")
                .password("hash")
                .enabled(true)
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("TDS_VIEW", "TDS_CREATE", "TDS_UPDATE", "TDS_DELETE"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully create a TDS profile")
    void testCreateProfile() {
        CreateTdsProfileRequest request = CreateTdsProfileRequest.builder()
                .clientId(clientId)
                .tan("BLRP12345A")
                .deductorType(TdsProfileEntity.DeductorType.COMPANY)
                .responsiblePersonName("Rajesh Sharma")
                .build();

        ClientEntity client = ClientEntity.builder()
                .displayName("Acme Corp")
                .legalName("Acme Corporation Pvt Ltd")
                .build();
        client.setId(clientId);

        TdsProfileEntity savedEntity = TdsProfileEntity.builder()
                .clientId(clientId)
                .tan("BLRP12345A")
                .deductorType(TdsProfileEntity.DeductorType.COMPANY)
                .responsiblePersonName("Rajesh Sharma")
                .build();
        savedEntity.setId(profileId);
        savedEntity.setOrganizationId(organizationId);

        TdsProfileDto dto = TdsProfileDto.builder()
                .id(profileId)
                .tan("BLRP12345A")
                .responsiblePersonName("Rajesh Sharma")
                .build();

        when(tdsProfileRepository.findByOrganizationIdAndTan(organizationId, "BLRP12345A")).thenReturn(Optional.empty());
        when(clientRepository.findByIdAndOrganizationId(clientId, organizationId)).thenReturn(Optional.of(client));
        when(tdsProfileRepository.save(any(TdsProfileEntity.class))).thenReturn(savedEntity);
        when(tdsMapper.toProfileDto(savedEntity)).thenReturn(dto);

        TdsProfileDto result = tdsService.createProfile(request);

        assertNotNull(result);
        assertEquals("BLRP12345A", result.getTan());
        verify(tdsProfileRepository).save(any(TdsProfileEntity.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException if TAN already exists")
    void testCreateProfileDuplicate() {
        CreateTdsProfileRequest request = CreateTdsProfileRequest.builder()
                .clientId(clientId)
                .tan("BLRP12345A")
                .build();

        TdsProfileEntity existing = new TdsProfileEntity();
        when(tdsProfileRepository.findByOrganizationIdAndTan(organizationId, "BLRP12345A")).thenReturn(Optional.of(existing));

        assertThrows(DuplicateResourceException.class, () -> tdsService.createProfile(request));
    }

    @Test
    @DisplayName("Should successfully create a TDS return")
    void testCreateReturn() {
        CreateTdsReturnRequest request = CreateTdsReturnRequest.builder()
                .clientId(clientId)
                .tdsProfileId(profileId)
                .formType(TdsFormType.FORM_26Q)
                .quarter(TdsQuarter.Q1)
                .financialYear("2026-27")
                .build();

        TdsProfileEntity profile = TdsProfileEntity.builder().tan("BLRP12345A").build();
        profile.setId(profileId);

        TdsReturnEntity savedReturn = TdsReturnEntity.builder()
                .clientId(clientId)
                .tdsProfileId(profileId)
                .formType(TdsFormType.FORM_26Q)
                .quarter(TdsQuarter.Q1)
                .financialYear("2026-27")
                .filingStatus(TdsFilingStatus.PENDING)
                .build();
        savedReturn.setId(returnId);
        savedReturn.setOrganizationId(organizationId);

        TdsReturnDto dto = TdsReturnDto.builder()
                .id(returnId)
                .formType(TdsFormType.FORM_26Q)
                .quarter(TdsQuarter.Q1)
                .financialYear("2026-27")
                .filingStatus(TdsFilingStatus.PENDING)
                .build();

        when(tdsReturnRepository.findByOrganizationIdAndTdsProfileIdAndFormTypeAndQuarterAndFinancialYear(
                organizationId, profileId, TdsFormType.FORM_26Q, TdsQuarter.Q1, "2026-27"
        )).thenReturn(Optional.empty());
        when(tdsProfileRepository.findByIdAndOrganizationId(profileId, organizationId)).thenReturn(Optional.of(profile));
        when(tdsReturnRepository.save(any(TdsReturnEntity.class))).thenReturn(savedReturn);
        when(tdsMapper.toReturnDto(savedReturn)).thenReturn(dto);

        TdsReturnDto result = tdsService.createReturn(request);

        assertNotNull(result);
        assertEquals(TdsFormType.FORM_26Q, result.getFormType());
        assertEquals(TdsQuarter.Q1, result.getQuarter());
    }

    @Test
    @DisplayName("Should record filing token and mark return as FILED")
    void testRecordFiling() {
        RecordTdsFilingRequest request = RecordTdsFilingRequest.builder()
                .filingDate(LocalDate.of(2026, 7, 28))
                .tokenNumber("010020304050607")
                .build();

        TdsReturnEntity existing = TdsReturnEntity.builder()
                .clientId(clientId)
                .tdsProfileId(profileId)
                .formType(TdsFormType.FORM_26Q)
                .quarter(TdsQuarter.Q1)
                .financialYear("2026-27")
                .filingStatus(TdsFilingStatus.PENDING)
                .build();
        existing.setId(returnId);
        existing.setOrganizationId(organizationId);

        TdsReturnEntity updated = TdsReturnEntity.builder()
                .clientId(clientId)
                .tdsProfileId(profileId)
                .formType(TdsFormType.FORM_26Q)
                .quarter(TdsQuarter.Q1)
                .financialYear("2026-27")
                .filingStatus(TdsFilingStatus.FILED)
                .tokenNumber("010020304050607")
                .build();
        updated.setId(returnId);

        TdsReturnDto dto = TdsReturnDto.builder()
                .id(returnId)
                .filingStatus(TdsFilingStatus.FILED)
                .tokenNumber("010020304050607")
                .build();

        when(tdsReturnRepository.findByIdAndOrganizationId(returnId, organizationId)).thenReturn(Optional.of(existing));
        when(tdsReturnRepository.save(any(TdsReturnEntity.class))).thenReturn(updated);
        when(tdsMapper.toReturnDto(updated)).thenReturn(dto);

        TdsReturnDto result = tdsService.recordFiling(returnId, request);

        assertNotNull(result);
        assertEquals(TdsFilingStatus.FILED, result.getFilingStatus());
        assertEquals("010020304050607", result.getTokenNumber());
    }
}
