package com.taxoryn.module.organization.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.TenantAccessDeniedException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.organization.dto.CreateOrganizationRequest;
import com.taxoryn.module.organization.dto.OrganizationDto;
import com.taxoryn.module.organization.dto.OrganizationSettingsDto;
import com.taxoryn.module.organization.dto.UpdateOrganizationRequest;
import com.taxoryn.module.organization.dto.UpdateOrganizationSettingsRequest;
import com.taxoryn.module.organization.dto.UpdateOrganizationStatusRequest;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationSettingsEntity;
import com.taxoryn.module.organization.mapper.OrganizationMapper;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.organization.repository.OrganizationSettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationSettingsRepository settingsRepository;

    @Mock
    private OrganizationMapper organizationMapper;

    @InjectMocks
    private OrganizationServiceImpl organizationService;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .email("admin@test.com")
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("ORG_READ", "ORG_WRITE"))
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
    @DisplayName("Create Organization successfully and auto-provisions default settings")
    void testCreateOrganizationSuccess() {
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .name("Apex Advisors")
                .legalName("Apex Advisors LLP")
                .email("contact@apex.com")
                .phone("+919876543210")
                .city("Mumbai")
                .state("Maharashtra")
                .country("India")
                .pincode("400001")
                .pan("ABCDE1234F")
                .gstin("27ABCDE1234F1Z5")
                .taxRegistrationNumber("LLPIN-1234")
                .build();

        when(organizationRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(false);

        OrganizationEntity savedOrg = OrganizationEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
        savedOrg.setId(tenantId);

        when(organizationRepository.save(any(OrganizationEntity.class))).thenReturn(savedOrg);
        when(settingsRepository.save(any(OrganizationSettingsEntity.class)))
                .thenReturn(OrganizationSettingsEntity.createDefault(tenantId));
        when(organizationMapper.toDto(any(OrganizationEntity.class)))
                .thenReturn(OrganizationDto.builder().id(tenantId).name(request.getName()).build());

        OrganizationDto result = organizationService.createOrganization(request);

        assertNotNull(result);
        assertEquals(tenantId, result.getId());
        verify(settingsRepository).save(any(OrganizationSettingsEntity.class));
    }

    @Test
    @DisplayName("Create Organization fails when email already exists")
    void testCreateOrganizationDuplicateEmailThrows() {
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .name("Apex Advisors")
                .email("duplicate@apex.com")
                .build();

        when(organizationRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> organizationService.createOrganization(request));
    }

    @Test
    @DisplayName("Get Organization by ID succeeds when accessing own tenant")
    void testGetOrganizationByIdSameTenantSuccess() {
        OrganizationEntity entity = OrganizationEntity.builder().name("Test Org").build();
        entity.setId(tenantId);

        when(organizationRepository.findById(tenantId)).thenReturn(Optional.of(entity));
        when(organizationMapper.toDto(entity)).thenReturn(OrganizationDto.builder().id(tenantId).name("Test Org").build());

        OrganizationDto result = organizationService.getOrganizationById(tenantId);

        assertNotNull(result);
        assertEquals(tenantId, result.getId());
    }

    @Test
    @DisplayName("Get Organization by ID throws TenantAccessDeniedException on cross-tenant access attempt")
    void testGetOrganizationByIdCrossTenantThrows() {
        UUID otherTenantId = UUID.randomUUID();

        assertThrows(TenantAccessDeniedException.class, () -> organizationService.getOrganizationById(otherTenantId));
    }

    @Test
    @DisplayName("Get Organization by ID allows SUPER_ADMIN to view any tenant")
    void testGetOrganizationByIdSuperAdminAllowed() {
        UUID otherTenantId = UUID.randomUUID();

        SecurityUser superAdmin = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .email("superadmin@taxoryn.com")
                .roles(Set.of("SUPER_ADMIN"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(superAdmin, null, superAdmin.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        OrganizationEntity entity = OrganizationEntity.builder().name("Other Org").build();
        entity.setId(otherTenantId);

        when(organizationRepository.findById(otherTenantId)).thenReturn(Optional.of(entity));
        when(organizationMapper.toDto(entity)).thenReturn(OrganizationDto.builder().id(otherTenantId).name("Other Org").build());

        OrganizationDto result = organizationService.getOrganizationById(otherTenantId);

        assertNotNull(result);
        assertEquals(otherTenantId, result.getId());
    }

    @Test
    @DisplayName("Update Organization details successfully")
    void testUpdateOrganizationSuccess() {
        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .name("Updated Name")
                .legalName("Updated Legal Name")
                .city("Pune")
                .state("Maharashtra")
                .pincode("411001")
                .pan("ABCDE1234F")
                .build();

        OrganizationEntity existing = OrganizationEntity.builder().name("Old Name").build();
        existing.setId(tenantId);

        when(organizationRepository.findById(tenantId)).thenReturn(Optional.of(existing));
        when(organizationRepository.save(existing)).thenReturn(existing);
        when(organizationMapper.toDto(existing)).thenReturn(OrganizationDto.builder().id(tenantId).name("Updated Name").build());

        OrganizationDto result = organizationService.updateOrganization(tenantId, request);

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
    }

    @Test
    @DisplayName("Update Organization Status to INACTIVE or SUSPENDED")
    void testUpdateOrganizationStatus() {
        UpdateOrganizationStatusRequest request = UpdateOrganizationStatusRequest.builder()
                .status(OrganizationStatus.SUSPENDED)
                .reason("Non-payment")
                .build();

        OrganizationEntity existing = OrganizationEntity.builder().name("Test Org").status(OrganizationStatus.ACTIVE).build();
        existing.setId(tenantId);

        when(organizationRepository.findById(tenantId)).thenReturn(Optional.of(existing));
        when(organizationRepository.save(existing)).thenReturn(existing);
        when(organizationMapper.toDto(existing)).thenReturn(OrganizationDto.builder().id(tenantId).status(OrganizationStatus.SUSPENDED).build());

        OrganizationDto result = organizationService.updateOrganizationStatus(tenantId, request);

        assertNotNull(result);
        assertEquals(OrganizationStatus.SUSPENDED, result.getStatus());
    }

    @Test
    @DisplayName("Get Organization Settings provisions defaults if not present")
    void testGetOrganizationSettingsProvisionsDefault() {
        when(settingsRepository.findByOrganizationId(tenantId)).thenReturn(Optional.empty());
        OrganizationSettingsEntity defaultSettings = OrganizationSettingsEntity.createDefault(tenantId);
        when(settingsRepository.save(any(OrganizationSettingsEntity.class))).thenReturn(defaultSettings);
        when(organizationMapper.toSettingsDto(defaultSettings))
                .thenReturn(OrganizationSettingsDto.builder().organizationId(tenantId).timezone("Asia/Kolkata").build());

        OrganizationSettingsDto result = organizationService.getOrganizationSettings(tenantId);

        assertNotNull(result);
        assertEquals("Asia/Kolkata", result.getTimezone());
    }

    @Test
    @DisplayName("Update Organization Settings successfully")
    void testUpdateOrganizationSettingsSuccess() {
        UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
                .timezone("Asia/Dubai")
                .dateFormat("YYYY-MM-DD")
                .currency("AED")
                .financialYearStartMonth(1)
                .enableEmailNotifications(true)
                .enableSmsNotifications(true)
                .invoicePrefix("AE/")
                .build();

        OrganizationSettingsEntity existing = OrganizationSettingsEntity.createDefault(tenantId);
        when(settingsRepository.findByOrganizationId(tenantId)).thenReturn(Optional.of(existing));
        when(settingsRepository.save(existing)).thenReturn(existing);
        when(organizationMapper.toSettingsDto(existing))
                .thenReturn(OrganizationSettingsDto.builder().organizationId(tenantId).timezone("Asia/Dubai").currency("AED").build());

        OrganizationSettingsDto result = organizationService.updateOrganizationSettings(tenantId, request);

        assertNotNull(result);
        assertEquals("Asia/Dubai", result.getTimezone());
        assertEquals("AED", result.getCurrency());
    }
}
