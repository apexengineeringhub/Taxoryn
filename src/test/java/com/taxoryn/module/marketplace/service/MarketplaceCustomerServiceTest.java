package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.entity.MarketplaceCustomerProfileEntity.CustomerType;
import com.taxoryn.module.marketplace.mapper.CustomerTaxRequirementMapper;
import com.taxoryn.module.marketplace.mapper.MarketplaceMapper;
import com.taxoryn.module.marketplace.repository.*;
import com.taxoryn.module.marketplace.repository.CustomerTaxRequirementRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceCustomerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MarketplaceCustomerProfileRepository customerProfileRepository;

    @Mock
    private MarketplaceLeadRepository leadRepository;

    @Mock
    private MarketplaceConsultationRepository consultationRepository;

    @Mock
    private MarketplaceProposalRepository proposalRepository;

    @Mock
    private MarketplaceReviewRepository reviewRepository;

    @Mock
    private MarketplaceEnquiryMessageRepository enquiryMessageRepository;

    @Mock
    private com.taxoryn.module.employee.repository.EmployeeRepository employeeRepository;

    @Mock
    private MarketplaceProfileRepository practiceProfileRepository;

    @Mock
    private CustomerTaxRequirementRepository requirementRepository;

    @Mock
    private TaxServiceRepository taxServiceRepository;

    @Mock
    private com.taxoryn.module.notification.service.NotificationService notificationService;

    @Mock
    private CustomerTaxRequirementMapper requirementMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuditService auditService;

    private MarketplaceMapper mapper;
    private CustomerProfileCompletenessCalculator completenessCalculator;
    private MarketplaceCustomerServiceImpl customerService;

    private final UUID customerUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(MarketplaceMapper.class);
        completenessCalculator = new CustomerProfileCompletenessCalculator();
        customerService = new MarketplaceCustomerServiceImpl(
                userRepository,
                roleRepository,
                customerProfileRepository,
                leadRepository,
                consultationRepository,
                proposalRepository,
                reviewRepository,
                enquiryMessageRepository,
                employeeRepository,
                practiceProfileRepository,
                requirementRepository,
                taxServiceRepository,
                notificationService,
                requirementMapper,
                mapper,
                completenessCalculator,
                passwordEncoder,
                jwtTokenProvider,
                auditService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityCustomer() {
        SecurityUser securityUser = SecurityUser.builder()
                .userId(customerUserId)
                .organizationId(null)
                .email("customer@example.com")
                .password("hash")
                .roles(Set.of("ROLE_MARKETPLACE_CUSTOMER", "MARKETPLACE_CUSTOMER"))
                .permissions(Set.of("MARKETPLACE_CUSTOMER_ACCESS"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                securityUser, null, securityUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Successfully registers a new marketplace customer with encrypted password and customer role")
    void testRegisterCustomer_Success() {
        RegisterCustomerRequest req = RegisterCustomerRequest.builder()
                .firstName("Suresh")
                .lastName("Kumar")
                .email("suresh.kumar@example.com")
                .phone("9876543210")
                .password("Password123!")
                .customerType(CustomerType.INDIVIDUAL)
                .city("Chennai")
                .state("Tamil Nadu")
                .pincode("600001")
                .preferredLanguage("English")
                .build();

        when(userRepository.findByEmailIgnoreCase("suresh.kumar@example.com")).thenReturn(Optional.empty());
        when(customerProfileRepository.existsByEmailIgnoreCase("suresh.kumar@example.com")).thenReturn(false);

        RoleEntity role = RoleEntity.builder()
                .code("MARKETPLACE_CUSTOMER")
                .name("Marketplace Customer")
                .isSystemRole(true)
                .permissions(Set.of())
                .build();
        when(roleRepository.findByCodeAndIsSystemRoleTrue("MARKETPLACE_CUSTOMER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedHash123");

        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(customerUserId);
            return u;
        });

        when(customerProfileRepository.save(any(MarketplaceCustomerProfileEntity.class))).thenAnswer(inv -> {
            MarketplaceCustomerProfileEntity p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), anyString(), any(), any())).thenReturn("mockAccessToken");
        when(jwtTokenProvider.generateRefreshToken(any(), any(), any(), anyString())).thenReturn("mockRefreshToken");

        CustomerAuthResponseDto response = customerService.registerCustomer(req);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mockAccessToken");
        assertThat(response.getCustomer()).isNotNull();
        assertThat(response.getCustomer().getEmail()).isEqualTo("suresh.kumar@example.com");
        assertThat(response.getCustomer().getFirstName()).isEqualTo("Suresh");
        assertThat(response.getCustomer().getDisplayName()).isEqualTo("Suresh Kumar");
        assertThat(response.getCustomer().getProfileCompleteness().getPercentage()).isEqualTo(100);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getOrganizationId()).isNull(); // Isolated from any practice organization
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encodedHash123");
    }

    @Test
    @DisplayName("Rejects registration when email already exists")
    void testRegisterCustomer_DuplicateEmail_ThrowsException() {
        RegisterCustomerRequest req = RegisterCustomerRequest.builder()
                .firstName("Duplicate")
                .email("existing@example.com")
                .password("Password123!")
                .build();

        when(userRepository.findByEmailIgnoreCase("existing@example.com"))
                .thenReturn(Optional.of(UserEntity.builder().email("existing@example.com").build()));

        assertThatThrownBy(() -> customerService.registerCustomer(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("User already exists with email");
    }

    @Test
    @DisplayName("Retrieves profile for authenticated customer with completeness calculation")
    void testGetCurrentCustomerProfile_Success() {
        mockSecurityCustomer();

        MarketplaceCustomerProfileEntity entity = MarketplaceCustomerProfileEntity.builder()
                .userId(customerUserId)
                .customerType(CustomerType.INDIVIDUAL)
                .firstName("Rohan")
                .lastName("Verma")
                .displayName("Rohan Verma")
                .email("rohan@example.com")
                .phone("9988776655")
                .city("Pune")
                .state("Maharashtra")
                .pincode("411001")
                .preferredLanguage("English")
                .build();
        entity.setId(UUID.randomUUID());

        when(customerProfileRepository.findByUserId(customerUserId)).thenReturn(Optional.of(entity));

        CustomerProfileDto profile = customerService.getCurrentCustomerProfile();

        assertThat(profile).isNotNull();
        assertThat(profile.getDisplayName()).isEqualTo("Rohan Verma");
        assertThat(profile.getProfileCompleteness().getPercentage()).isEqualTo(100);
        assertThat(profile.getProfileCompleteness().getCompletedItems()).contains("Full Name", "City", "State");
    }

    @Test
    @DisplayName("Updates profile and synchronizes User name and phone")
    void testUpdateCurrentCustomerProfile_Success() {
        mockSecurityCustomer();

        MarketplaceCustomerProfileEntity profile = MarketplaceCustomerProfileEntity.builder()
                .userId(customerUserId)
                .customerType(CustomerType.INDIVIDUAL)
                .firstName("Rohan")
                .displayName("Rohan")
                .email("rohan@example.com")
                .build();
        profile.setId(UUID.randomUUID());

        UserEntity user = UserEntity.builder()
                .email("rohan@example.com")
                .firstName("Rohan")
                .build();
        user.setId(customerUserId);

        when(customerProfileRepository.findByUserId(customerUserId)).thenReturn(Optional.of(profile));
        when(userRepository.findById(customerUserId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(customerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateCustomerProfileRequest updateReq = UpdateCustomerProfileRequest.builder()
                .firstName("Rohan")
                .lastName("Verma")
                .displayName("Rohan Verma (Taxpayer)")
                .phone("9876543210")
                .city("Pune")
                .state("Maharashtra")
                .pincode("411001")
                .preferredLanguage("Hindi")
                .customerType(CustomerType.BUSINESS)
                .businessName("Verma Logistics")
                .build();

        CustomerProfileDto updated = customerService.updateCurrentCustomerProfile(updateReq);

        assertThat(updated.getDisplayName()).isEqualTo("Rohan Verma (Taxpayer)");
        assertThat(updated.getCustomerType()).isEqualTo(CustomerType.BUSINESS);
        assertThat(updated.getBusinessName()).isEqualTo("Verma Logistics");
        assertThat(user.getLastName()).isEqualTo("Verma");
        assertThat(user.getPhone()).isEqualTo("9876543210");
        verify(auditService).logEvent(eq("CUSTOMER_PROFILE_UPDATED"), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Aggregates customer dashboard metrics and recent submissions")
    void testGetCustomerDashboard_Success() {
        mockSecurityCustomer();

        MarketplaceCustomerProfileEntity profile = MarketplaceCustomerProfileEntity.builder()
                .userId(customerUserId)
                .firstName("Aarti")
                .displayName("Aarti Rao")
                .email("aarti@example.com")
                .build();

        when(customerProfileRepository.findByUserId(customerUserId)).thenReturn(Optional.of(profile));
        when(leadRepository.countByCustomerId(customerUserId)).thenReturn(3L);
        when(consultationRepository.countByCustomerId(customerUserId)).thenReturn(1L);
        when(proposalRepository.countByCustomerId(customerUserId)).thenReturn(2L);
        when(reviewRepository.countByCustomerId(customerUserId)).thenReturn(1L);

        when(leadRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerUserId)).thenReturn(List.of(
                MarketplaceLeadEntity.builder().clientName("Aarti Rao").serviceCategory("GST Advisory").build()
        ));
        when(consultationRepository.findAllByCustomerIdOrderByBookingDateDesc(customerUserId)).thenReturn(List.of());
        when(proposalRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerUserId)).thenReturn(List.of());
        when(reviewRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerUserId)).thenReturn(List.of());

        CustomerDashboardDto dashboard = customerService.getCustomerDashboard();

        assertThat(dashboard).isNotNull();
        assertThat(dashboard.getTotalRequests()).isEqualTo(3L);
        assertThat(dashboard.getTotalConsultations()).isEqualTo(1L);
        assertThat(dashboard.getTotalProposals()).isEqualTo(2L);
        assertThat(dashboard.getTotalReviews()).isEqualTo(1L);
        assertThat(dashboard.getRecentLeads()).hasSize(1);
    }
}
