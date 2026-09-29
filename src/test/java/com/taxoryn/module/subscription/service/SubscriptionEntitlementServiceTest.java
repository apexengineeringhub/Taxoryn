package com.taxoryn.module.subscription.service;

import com.taxoryn.core.exception.SubscriptionLimitExceededException;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.subscription.dto.EntitlementResult;
import com.taxoryn.module.subscription.dto.SubscriptionEntitlementsResponse;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.BillingInterval;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionStatus;
import com.taxoryn.module.subscription.entity.SubscriptionResourceType;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionEntitlementServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private SubscriptionPlanEntitlementService subscriptionPlanEntitlementService;

    @InjectMocks
    private SubscriptionEntitlementServiceImpl entitlementService;

    private UUID organizationId;
    private SubscriptionEntity starterSubscription;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();

        starterSubscription = SubscriptionEntity.builder()
                .organizationId(organizationId)
                .plan(SubscriptionPlan.STARTER)
                .status(SubscriptionStatus.ACTIVE)
                .billingInterval(BillingInterval.MONTHLY)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusDays(30))
                .maxUsers(5)
                .maxClients(25)
                .maxStorageBytes(5L * 1024 * 1024 * 1024)
                .price(new BigDecimal("999.00"))
                .autoRenew(true)
                .build();
    }

    @Test
    @DisplayName("TEAM_MEMBER: Evaluation at normal usage (<80%) returns allowed=true, warning=false")
    void testTeamMember_NormalUsage() {
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        when(userRepository.countByOrganizationIdAndClientIdIsNull(organizationId)).thenReturn(2L); // 2/5 = 40%

        EntitlementResult result = entitlementService.getEntitlement(organizationId, SubscriptionResourceType.TEAM_MEMBER);

        assertNotNull(result);
        assertEquals(2L, result.getCurrentUsage());
        assertEquals(5L, result.getLimit());
        assertEquals(3L, result.getRemaining());
        assertEquals(40.0, result.getPercentageUsed());
        assertTrue(result.isAllowed());
        assertFalse(result.isWarning());
    }

    @Test
    @DisplayName("TEAM_MEMBER: Evaluation at warning usage (80%) returns allowed=true, warning=true")
    void testTeamMember_WarningUsage() {
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        when(userRepository.countByOrganizationIdAndClientIdIsNull(organizationId)).thenReturn(4L); // 4/5 = 80%

        EntitlementResult result = entitlementService.getEntitlement(organizationId, SubscriptionResourceType.TEAM_MEMBER);

        assertNotNull(result);
        assertEquals(4L, result.getCurrentUsage());
        assertEquals(5L, result.getLimit());
        assertEquals(1L, result.getRemaining());
        assertEquals(80.0, result.getPercentageUsed());
        assertTrue(result.isAllowed());
        assertTrue(result.isWarning());
    }

    @Test
    @DisplayName("TEAM_MEMBER: Evaluation at 100% quota returns allowed=false, warning=false, throws on checkCanCreate")
    void testTeamMember_LimitReached() {
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        when(userRepository.countByOrganizationIdAndClientIdIsNull(organizationId)).thenReturn(5L); // 5/5 = 100%

        EntitlementResult result = entitlementService.getEntitlement(organizationId, SubscriptionResourceType.TEAM_MEMBER);

        assertNotNull(result);
        assertEquals(5L, result.getCurrentUsage());
        assertEquals(5L, result.getLimit());
        assertEquals(0L, result.getRemaining());
        assertEquals(100.0, result.getPercentageUsed());
        assertFalse(result.isAllowed());

        assertThrows(SubscriptionLimitExceededException.class,
                () -> entitlementService.checkCanCreate(organizationId, SubscriptionResourceType.TEAM_MEMBER));
    }

    @Test
    @DisplayName("CLIENT: checkCanCreate passes when below quota, throws when quota reached")
    void testClient_QuotaCheck() {
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        when(clientRepository.countByOrganizationId(organizationId)).thenReturn(24L);

        entitlementService.checkCanCreate(organizationId, SubscriptionResourceType.CLIENT);

        when(clientRepository.countByOrganizationId(organizationId)).thenReturn(25L);
        assertThrows(SubscriptionLimitExceededException.class,
                () -> entitlementService.checkCanCreate(organizationId, SubscriptionResourceType.CLIENT));
    }

    @Test
    @DisplayName("STORAGE: checkCanStore passes when space available, throws when limit exceeded")
    void testStorage_CheckCanStore() {
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        long currentStorage = 4L * 1024 * 1024 * 1024; // 4 GB of 5 GB
        when(documentRepository.getTotalStorageBytesByOrganizationId(organizationId)).thenReturn(currentStorage);

        // Uploading 500 MB (total 4.5 GB <= 5 GB) should pass
        entitlementService.checkCanStore(organizationId, 500L * 1024 * 1024);

        // Uploading 1.5 GB (total 5.5 GB > 5 GB) should throw
        assertThrows(SubscriptionLimitExceededException.class,
                () -> entitlementService.checkCanStore(organizationId, (long) (1.5 * 1024 * 1024 * 1024)));
    }

    @Test
    @DisplayName("getAllEntitlements aggregates all resources and correctly sets flags")
    void testGetAllEntitlements() {
        OrganizationEntity org = OrganizationEntity.builder().name("Apex CA Practice").build();
        org.setId(organizationId);

        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(org));
        when(userRepository.countByOrganizationIdAndClientIdIsNull(organizationId)).thenReturn(4L); // 80% (warning)
        when(clientRepository.countByOrganizationId(organizationId)).thenReturn(10L); // 40%
        when(documentRepository.getTotalStorageBytesByOrganizationId(organizationId)).thenReturn(1024L);
        when(locationRepository.countByOrganizationIdAndIsActiveTrue(organizationId)).thenReturn(1L);
        when(subscriptionPlanEntitlementService.getMaxLocations(SubscriptionPlan.STARTER)).thenReturn(1);
        when(subscriptionPlanEntitlementService.isMultiLocationEnabled(SubscriptionPlan.STARTER)).thenReturn(false);

        SubscriptionEntitlementsResponse response = entitlementService.getAllEntitlements(organizationId);

        assertNotNull(response);
        assertEquals(organizationId, response.getOrganizationId());
        assertEquals("Apex CA Practice", response.getOrganizationName());
        assertEquals(SubscriptionPlan.STARTER, response.getPlan());
        assertEquals(4, response.getEntitlements().size());
        assertTrue(response.isAnyWarning());
        assertTrue(response.isAnyLimitReached()); // location 1/1 = 100%
    }

    @Test
    @DisplayName("validateDowngrade passes when usage is within target plan quotas")
    void testValidateDowngrade_Success() {
        when(userRepository.countByOrganizationIdAndClientIdIsNull(organizationId)).thenReturn(3L); // <= 5
        when(clientRepository.countByOrganizationId(organizationId)).thenReturn(20L); // <= 25
        when(documentRepository.getTotalStorageBytesByOrganizationId(organizationId)).thenReturn(1024L); // <= 5GB
        when(locationRepository.countByOrganizationIdAndIsActiveTrue(organizationId)).thenReturn(1L); // <= 1
        when(subscriptionPlanEntitlementService.getMaxLocations(SubscriptionPlan.STARTER)).thenReturn(1);
        when(subscriptionPlanEntitlementService.isMultiLocationEnabled(SubscriptionPlan.STARTER)).thenReturn(false);

        entitlementService.validateDowngrade(organizationId, SubscriptionPlan.STARTER);
    }

    @Test
    @DisplayName("validateDowngrade throws when active users exceed target plan quota")
    void testValidateDowngrade_UserExceeded() {
        when(userRepository.countByOrganizationIdAndClientIdIsNull(organizationId)).thenReturn(10L); // > 5

        assertThrows(SubscriptionLimitExceededException.class,
                () -> entitlementService.validateDowngrade(organizationId, SubscriptionPlan.STARTER));
    }

    @Test
    @DisplayName("validateDowngrade throws when active clients exceed target plan quota")
    void testValidateDowngrade_ClientExceeded() {
        when(userRepository.countByOrganizationIdAndClientIdIsNull(organizationId)).thenReturn(3L);
        when(clientRepository.countByOrganizationId(organizationId)).thenReturn(50L); // > 25

        assertThrows(SubscriptionLimitExceededException.class,
                () -> entitlementService.validateDowngrade(organizationId, SubscriptionPlan.STARTER));
    }

    @Test
    @DisplayName("validateDowngrade throws when locations exceed target plan quota")
    void testValidateDowngrade_LocationExceeded() {
        when(userRepository.countByOrganizationIdAndClientIdIsNull(organizationId)).thenReturn(3L);
        when(clientRepository.countByOrganizationId(organizationId)).thenReturn(20L);
        when(documentRepository.getTotalStorageBytesByOrganizationId(organizationId)).thenReturn(1024L);
        when(locationRepository.countByOrganizationIdAndIsActiveTrue(organizationId)).thenReturn(2L); // 2 locations
        when(subscriptionPlanEntitlementService.getMaxLocations(SubscriptionPlan.STARTER)).thenReturn(1);
        when(subscriptionPlanEntitlementService.isMultiLocationEnabled(SubscriptionPlan.STARTER)).thenReturn(false);

        assertThrows(SubscriptionLimitExceededException.class,
                () -> entitlementService.validateDowngrade(organizationId, SubscriptionPlan.STARTER));
    }
}
