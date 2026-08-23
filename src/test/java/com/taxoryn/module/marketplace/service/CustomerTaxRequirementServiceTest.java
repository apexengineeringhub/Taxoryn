package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.marketplace.dto.CreateTaxRequirementRequest;
import com.taxoryn.module.marketplace.dto.CustomerTaxRequirementDto;
import com.taxoryn.module.marketplace.dto.UpdateTaxRequirementRequest;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.mapper.CustomerTaxRequirementMapper;
import com.taxoryn.module.marketplace.mapper.TaxServiceMapper;
import com.taxoryn.module.marketplace.repository.CustomerTaxRequirementRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceCustomerProfileRepository;
import com.taxoryn.module.marketplace.repository.TaxServiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerTaxRequirementServiceTest {

    @Mock
    private CustomerTaxRequirementRepository requirementRepository;

    @Mock
    private MarketplaceCustomerProfileRepository customerProfileRepository;

    @Mock
    private TaxServiceRepository taxServiceRepository;

    @Mock
    private AuditService auditService;

    private CustomerTaxRequirementMapper mapper;
    private CustomerTaxRequirementServiceImpl service;

    private UUID userId;
    private UUID customerId;
    private UUID taxServiceId;
    private MarketplaceCustomerProfileEntity customerProfile;
    private TaxServiceCategoryEntity category;
    private TaxServiceEntity taxService;

    @BeforeEach
    void setUp() {
        TaxServiceMapper taxServiceMapper = Mappers.getMapper(TaxServiceMapper.class);
        mapper = Mappers.getMapper(CustomerTaxRequirementMapper.class);
        // Inject TaxServiceMapper into CustomerTaxRequirementMapper via reflection if needed
        ReflectionTestUtils.setField(mapper, "taxServiceMapper", taxServiceMapper);

        service = new CustomerTaxRequirementServiceImpl(
                requirementRepository,
                customerProfileRepository,
                taxServiceRepository,
                mapper,
                auditService
        );

        userId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        taxServiceId = UUID.randomUUID();

        // Setup mock security context
        SecurityUser securityUser = SecurityUser.builder()
                .userId(userId)
                .email("customer@taxoryn.com")
                .password("Password@123")
                .enabled(true)
                .roles(Set.of("ROLE_MARKETPLACE_CUSTOMER"))
                .permissions(Set.of())
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        customerProfile = MarketplaceCustomerProfileEntity.builder()
                .userId(userId)
                .firstName("Rahul")
                .lastName("Sharma")
                .displayName("Rahul Sharma")
                .email("customer@taxoryn.com")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .build();
        customerProfile.setId(customerId);

        category = TaxServiceCategoryEntity.builder()
                .code("DIRECT_TAX")
                .name("Income Tax")
                .build();
        category.setId(UUID.randomUUID());

        taxService = TaxServiceEntity.builder()
                .category(category)
                .code("INCOME_TAX_RETURN")
                .name("Income Tax Return (ITR) Filing")
                .isActive(true)
                .build();
        taxService.setId(taxServiceId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully create a customer tax requirement in DRAFT state")
    void shouldCreateRequirementInDraft() {
        when(customerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(customerProfile));
        when(taxServiceRepository.findById(taxServiceId)).thenReturn(Optional.of(taxService));
        when(requirementRepository.existsByCustomerIdAndTaxServiceIdAndFinancialYearAndStatusIn(any(), any(), any(), any()))
                .thenReturn(false);

        when(requirementRepository.save(any(CustomerTaxRequirementEntity.class))).thenAnswer(inv -> {
            CustomerTaxRequirementEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        CreateTaxRequirementRequest request = CreateTaxRequirementRequest.builder()
                .taxServiceId(taxServiceId)
                .customerType(CustomerTaxpayerType.SALARIED)
                .financialYear("FY 2025-26")
                .description("I have Form 16 from two employers this year.")
                .build();

        CustomerTaxRequirementDto result = service.createRequirement(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TaxRequirementStatus.DRAFT);
        assertThat(result.getFinancialYear()).isEqualTo("2025-26");
        assertThat(result.getCustomerType()).isEqualTo(CustomerTaxpayerType.SALARIED);
        assertThat(result.isEditable()).isTrue();
        assertThat(result.isCancellable()).isTrue();

        verify(auditService).logEvent(eq("CUSTOMER_REQUIREMENT_CREATED"), eq("CUSTOMER_TAX_REQUIREMENT"), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject requirement creation when referenced tax service is inactive")
    void shouldRejectInactiveTaxService() {
        taxService.setIsActive(false);
        when(customerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(customerProfile));
        when(taxServiceRepository.findById(taxServiceId)).thenReturn(Optional.of(taxService));

        CreateTaxRequirementRequest request = CreateTaxRequirementRequest.builder()
                .taxServiceId(taxServiceId)
                .financialYear("2025-26")
                .build();

        assertThatThrownBy(() -> service.createRequirement(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot select inactive tax service");
    }

    @Test
    @DisplayName("Should reject requirement creation when active duplicate already exists for customer + service + FY")
    void shouldRejectDuplicateActiveRequirement() {
        when(customerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(customerProfile));
        when(taxServiceRepository.findById(taxServiceId)).thenReturn(Optional.of(taxService));
        when(requirementRepository.existsByCustomerIdAndTaxServiceIdAndFinancialYearAndStatusIn(
                eq(customerId), eq(taxServiceId), eq("2025-26"), any()
        )).thenReturn(true);

        CreateTaxRequirementRequest request = CreateTaxRequirementRequest.builder()
                .taxServiceId(taxServiceId)
                .financialYear("2025-26")
                .build();

        assertThatThrownBy(() -> service.createRequirement(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("An active tax requirement already exists");
    }

    @Test
    @DisplayName("Should sanitize HTML and scripts from customer description")
    void shouldSanitizeHtmlFromDescription() {
        when(customerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(customerProfile));
        when(taxServiceRepository.findById(taxServiceId)).thenReturn(Optional.of(taxService));
        when(requirementRepository.existsByCustomerIdAndTaxServiceIdAndFinancialYearAndStatusIn(any(), any(), any(), any()))
                .thenReturn(false);

        ArgumentCaptor<CustomerTaxRequirementEntity> captor = ArgumentCaptor.forClass(CustomerTaxRequirementEntity.class);
        when(requirementRepository.save(captor.capture())).thenAnswer(inv -> {
            CustomerTaxRequirementEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        CreateTaxRequirementRequest request = CreateTaxRequirementRequest.builder()
                .taxServiceId(taxServiceId)
                .financialYear("2025-26")
                .description("<script>alert('xss')</script><b>Need help with deductions</b>")
                .build();

        service.createRequirement(request);

        CustomerTaxRequirementEntity saved = captor.getValue();
        assertThat(saved.getDescription()).doesNotContain("<script>").doesNotContain("<b>");
        assertThat(saved.getDescription()).contains("Need help with deductions");
    }

    @Test
    @DisplayName("Should update requirement only when in DRAFT state")
    void shouldUpdateDraftRequirement() {
        UUID reqId = UUID.randomUUID();
        CustomerTaxRequirementEntity draftEntity = CustomerTaxRequirementEntity.builder()
                .customerId(customerId)
                .taxServiceId(taxServiceId)
                .taxService(taxService)
                .status(TaxRequirementStatus.DRAFT)
                .financialYear("2024-25")
                .build();
        draftEntity.setId(reqId);

        when(customerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(customerProfile));
        when(requirementRepository.findByIdAndCustomerId(reqId, customerId)).thenReturn(Optional.of(draftEntity));
        when(requirementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateTaxRequirementRequest updateReq = UpdateTaxRequirementRequest.builder()
                .financialYear("2025-26")
                .description("Updated requirement description")
                .build();

        CustomerTaxRequirementDto updated = service.updateRequirement(reqId, updateReq);
        assertThat(updated.getFinancialYear()).isEqualTo("2025-26");
        assertThat(updated.getDescription()).isEqualTo("Updated requirement description");
    }

    @Test
    @DisplayName("Should reject updating when requirement is not in DRAFT status")
    void shouldRejectUpdateWhenSubmitted() {
        UUID reqId = UUID.randomUUID();
        CustomerTaxRequirementEntity submittedEntity = CustomerTaxRequirementEntity.builder()
                .customerId(customerId)
                .taxServiceId(taxServiceId)
                .taxService(taxService)
                .status(TaxRequirementStatus.SUBMITTED)
                .build();
        submittedEntity.setId(reqId);

        when(customerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(customerProfile));
        when(requirementRepository.findByIdAndCustomerId(reqId, customerId)).thenReturn(Optional.of(submittedEntity));

        UpdateTaxRequirementRequest updateReq = UpdateTaxRequirementRequest.builder()
                .description("Trying to modify submitted requirement")
                .build();

        assertThatThrownBy(() -> service.updateRequirement(reqId, updateReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only requirements in DRAFT status can be modified");
    }

    @Test
    @DisplayName("Should transition requirement status from DRAFT to SUBMITTED")
    void shouldSubmitDraftRequirement() {
        UUID reqId = UUID.randomUUID();
        CustomerTaxRequirementEntity draftEntity = CustomerTaxRequirementEntity.builder()
                .customerId(customerId)
                .taxServiceId(taxServiceId)
                .taxService(taxService)
                .status(TaxRequirementStatus.DRAFT)
                .build();
        draftEntity.setId(reqId);

        when(customerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(customerProfile));
        when(requirementRepository.findByIdAndCustomerId(reqId, customerId)).thenReturn(Optional.of(draftEntity));
        when(requirementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerTaxRequirementDto submitted = service.submitRequirement(reqId);

        assertThat(submitted.getStatus()).isEqualTo(TaxRequirementStatus.SUBMITTED);
        assertThat(submitted.isEditable()).isFalse();
        assertThat(submitted.isCancellable()).isTrue();

        verify(auditService).logEvent(eq("CUSTOMER_REQUIREMENT_SUBMITTED"), eq("CUSTOMER_TAX_REQUIREMENT"), any(), any(), any());
    }

    @Test
    @DisplayName("Should cancel requirement from DRAFT or SUBMITTED state")
    void shouldCancelRequirement() {
        UUID reqId = UUID.randomUUID();
        CustomerTaxRequirementEntity entity = CustomerTaxRequirementEntity.builder()
                .customerId(customerId)
                .taxServiceId(taxServiceId)
                .taxService(taxService)
                .status(TaxRequirementStatus.SUBMITTED)
                .build();
        entity.setId(reqId);

        when(customerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(customerProfile));
        when(requirementRepository.findByIdAndCustomerId(reqId, customerId)).thenReturn(Optional.of(entity));
        when(requirementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerTaxRequirementDto cancelled = service.cancelRequirement(reqId);

        assertThat(cancelled.getStatus()).isEqualTo(TaxRequirementStatus.CANCELLED);
        assertThat(cancelled.isEditable()).isFalse();
        assertThat(cancelled.isCancellable()).isFalse();

        verify(auditService).logEvent(eq("CUSTOMER_REQUIREMENT_CANCELLED"), eq("CUSTOMER_TAX_REQUIREMENT"), any(), any(), any());
    }

    @Test
    @DisplayName("Should prevent IDOR by throwing ResourceNotFoundException when accessing unowned requirement")
    void shouldPreventIdorOnRequirementAccess() {
        UUID otherReqId = UUID.randomUUID();
        when(customerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(customerProfile));
        when(requirementRepository.findByIdAndCustomerId(otherReqId, customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRequirementById(otherReqId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
