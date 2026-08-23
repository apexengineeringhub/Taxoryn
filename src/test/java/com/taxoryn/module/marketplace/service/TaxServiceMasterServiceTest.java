package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.mapper.TaxServiceMapper;
import com.taxoryn.module.marketplace.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaxServiceMasterServiceTest {

    @Mock
    private TaxServiceCategoryRepository categoryRepository;
    @Mock
    private TaxServiceRepository taxServiceRepository;
    @Mock
    private TaxServiceAliasRepository aliasRepository;
    @Mock
    private PracticeServiceRepository practiceServiceRepository;
    @Mock
    private MarketplaceProfileRepository profileRepository;
    @Mock
    private AuditService auditService;

    private TaxServiceMapper mapper = Mappers.getMapper(TaxServiceMapper.class);
    private TaxServiceMasterServiceImpl masterService;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    private final UUID orgId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID profileId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID categoryId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final UUID serviceId = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @BeforeEach
    void setUp() {
        masterService = new TaxServiceMasterServiceImpl(
                categoryRepository,
                taxServiceRepository,
                aliasRepository,
                practiceServiceRepository,
                profileRepository,
                mapper,
                auditService
        );

        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentOrganizationId).thenReturn(orgId);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Test
    @DisplayName("Should create tax service category and enforce unique code")
    void testCreateCategory_Success() {
        CreateTaxServiceCategoryRequest req = CreateTaxServiceCategoryRequest.builder()
                .code("INCOME_TAX")
                .name("Income Tax")
                .description("Direct tax returns")
                .icon("FileText")
                .sortOrder(1)
                .isActive(true)
                .build();

        when(categoryRepository.existsByCodeIgnoreCase("INCOME_TAX")).thenReturn(false);
        when(categoryRepository.save(any(TaxServiceCategoryEntity.class))).thenAnswer(inv -> {
            TaxServiceCategoryEntity entity = inv.getArgument(0);
            entity.setId(categoryId);
            return entity;
        });

        TaxServiceCategoryDto result = masterService.createCategory(req);

        assertNotNull(result);
        assertEquals("INCOME_TAX", result.getCode());
        assertEquals("Income Tax", result.getName());
        verify(auditService).logEvent(eq("TAX_SERVICE_CATEGORY_CREATED"), eq("TAX_SERVICE_CATEGORY"), anyString(), isNull(), anyString());
    }

    @Test
    @DisplayName("Should throw ConflictException when category code already exists")
    void testCreateCategory_DuplicateCode_ThrowsConflict() {
        CreateTaxServiceCategoryRequest req = CreateTaxServiceCategoryRequest.builder()
                .code("INCOME_TAX")
                .name("Income Tax")
                .build();

        when(categoryRepository.existsByCodeIgnoreCase("INCOME_TAX")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> masterService.createCategory(req));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create master tax service with initial search aliases")
    void testCreateTaxService_WithAliases_Success() {
        CreateTaxServiceRequest req = CreateTaxServiceRequest.builder()
                .categoryId(categoryId)
                .code("INCOME_TAX_RETURN")
                .name("Income Tax Return Filing")
                .description("Annual ITR preparation")
                .sortOrder(1)
                .isActive(true)
                .aliases(List.of("ITR", "IT Return", "Income Tax Filing"))
                .build();

        TaxServiceCategoryEntity category = TaxServiceCategoryEntity.builder()
                .code("INCOME_TAX")
                .name("Income Tax")
                .build();
        category.setId(categoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(taxServiceRepository.existsByCodeIgnoreCase("INCOME_TAX_RETURN")).thenReturn(false);
        when(taxServiceRepository.save(any(TaxServiceEntity.class))).thenAnswer(inv -> {
            TaxServiceEntity e = inv.getArgument(0);
            e.setId(serviceId);
            e.setCategory(category);
            return e;
        });
        when(taxServiceRepository.findById(serviceId)).thenAnswer(inv -> {
            TaxServiceEntity e = TaxServiceEntity.builder()
                    .categoryId(categoryId)
                    .category(category)
                    .code("INCOME_TAX_RETURN")
                    .name("Income Tax Return Filing")
                    .description("Annual ITR preparation")
                    .sortOrder(1)
                    .isActive(true)
                    .build();
            e.setId(serviceId);
            return Optional.of(e);
        });

        TaxServiceDto result = masterService.createTaxService(req);

        assertNotNull(result);
        assertEquals("INCOME_TAX_RETURN", result.getCode());
        verify(aliasRepository, times(3)).save(any(TaxServiceAliasEntity.class));
        verify(auditService).logEvent(eq("TAX_SERVICE_CREATED"), eq("TAX_SERVICE"), eq(serviceId.toString()), isNull(), anyString());
    }

    @Test
    @DisplayName("Should resolve search query 'itr' to INCOME_TAX_RETURN via alias")
    void testResolveQueryToService_AliasMatch() {
        TaxServiceCategoryEntity cat = TaxServiceCategoryEntity.builder()
                .code("INCOME_TAX")
                .name("Income Tax")
                .build();
        cat.setId(categoryId);

        TaxServiceEntity service = TaxServiceEntity.builder()
                .categoryId(categoryId)
                .category(cat)
                .code("INCOME_TAX_RETURN")
                .name("Income Tax Return Filing")
                .isActive(true)
                .build();
        service.setId(serviceId);

        TaxServiceAliasEntity aliasEntity = TaxServiceAliasEntity.builder()
                .taxServiceId(serviceId)
                .taxService(service)
                .alias("ITR")
                .normalizedAlias("itr")
                .isActive(true)
                .build();

        when(taxServiceRepository.findByCodeIgnoreCase("ITR")).thenReturn(Optional.empty());
        when(aliasRepository.findActiveMatchingAliases("itr")).thenReturn(List.of(aliasEntity));

        Optional<PublicTaxServiceDto> resolved = masterService.resolveQueryToService("itr");

        assertTrue(resolved.isPresent());
        assertEquals("INCOME_TAX_RETURN", resolved.get().getCode());
        assertEquals("Income Tax Return Filing", resolved.get().getName());
    }

    @Test
    @DisplayName("Should update practice services and reject inactive services with BadRequestException")
    void testUpdatePracticeServices_InactiveService_ThrowsBadRequest() {
        MarketplaceProfileEntity profile = MarketplaceProfileEntity.builder()
                .organizationId(orgId)
                .displayName("ABC Tax")
                .build();
        profile.setId(profileId);

        when(profileRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(profile));

        UUID activeSvcId = UUID.randomUUID();
        UUID inactiveSvcId = UUID.randomUUID();

        TaxServiceEntity activeSvc = TaxServiceEntity.builder().code("GST_RETURN_FILING").isActive(true).build();
        activeSvc.setId(activeSvcId);

        // Only activeSvcId is returned by findByIdInAndIsActiveTrue
        when(taxServiceRepository.findByIdInAndIsActiveTrue(List.of(activeSvcId, inactiveSvcId)))
                .thenReturn(List.of(activeSvc));

        UpdatePracticeServicesRequest req = UpdatePracticeServicesRequest.builder()
                .taxServiceIds(List.of(activeSvcId, inactiveSvcId))
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> masterService.updateMyPracticeServices(req));
        assertTrue(ex.getMessage().contains("inactive or do not exist"));
    }

    @Test
    @DisplayName("Should synchronize active practice services on valid update request")
    void testUpdatePracticeServices_Success() {
        MarketplaceProfileEntity profile = MarketplaceProfileEntity.builder()
                .organizationId(orgId)
                .displayName("ABC Tax")
                .build();
        profile.setId(profileId);

        when(profileRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(profile));

        UUID svc1 = UUID.randomUUID();
        UUID svc2 = UUID.randomUUID();

        TaxServiceEntity s1 = TaxServiceEntity.builder().code("INCOME_TAX_RETURN").isActive(true).build();
        s1.setId(svc1);
        TaxServiceEntity s2 = TaxServiceEntity.builder().code("GST_RETURN_FILING").isActive(true).build();
        s2.setId(svc2);

        when(taxServiceRepository.findByIdInAndIsActiveTrue(List.of(svc1, svc2))).thenReturn(List.of(s1, s2));
        when(practiceServiceRepository.findByMarketplaceProfileId(profileId)).thenReturn(Collections.emptyList());

        UpdatePracticeServicesRequest req = UpdatePracticeServicesRequest.builder()
                .taxServiceIds(List.of(svc1, svc2))
                .build();

        masterService.updateMyPracticeServices(req);

        ArgumentCaptor<PracticeServiceEntity> captor = ArgumentCaptor.forClass(PracticeServiceEntity.class);
        verify(practiceServiceRepository, times(2)).save(captor.capture());

        List<PracticeServiceEntity> saved = captor.getAllValues();
        assertEquals(2, saved.size());
        assertTrue(saved.stream().allMatch(ps -> Boolean.TRUE.equals(ps.getIsActive())));
        verify(auditService).logEvent(eq("PRACTICE_SERVICES_UPDATED"), eq("PRACTICE_SERVICE"), eq(profileId.toString()), isNull(), anyString());
    }
}
