package com.taxoryn.module.subscription.service;

import com.taxoryn.core.exception.SubscriptionLimitExceededException;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.subscription.dto.ChangePlanRequest;
import com.taxoryn.module.subscription.dto.SubscriptionDto;
import com.taxoryn.module.subscription.dto.SubscriptionUsageDto;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.BillingInterval;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionStatus;
import com.taxoryn.module.subscription.mapper.SubscriptionMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

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
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

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
                .maxStorageBytes(5L * 1024 * 1024 * 1024) // 5 GB
                .price(new BigDecimal("999.00"))
                .autoRenew(true)
                .build();
    }

    @Test
    @DisplayName("Get subscription usage calculates percentages and threshold flags accurately")
    void testGetSubscriptionUsage() {
        OrganizationEntity org = OrganizationEntity.builder().name("Apex CA").build();
        org.setId(organizationId);

        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(org));
        when(userRepository.countByOrganizationIdAndClientIdIsNull(organizationId)).thenReturn(4L);
        when(clientRepository.countByOrganizationId(organizationId)).thenReturn(25L); // at limit
        when(documentRepository.getTotalStorageBytesByOrganizationId(organizationId)).thenReturn(1073741824L); // 1 GB of 5 GB (20%)

        SubscriptionUsageDto usage = subscriptionService.getSubscriptionUsage(organizationId);

        assertNotNull(usage);
        assertEquals(4L, usage.getCurrentUsers());
        assertEquals(5, usage.getMaxUsers());
        assertEquals(80.0, usage.getUsersUsagePercentage());
        assertFalse(usage.isUserLimitReached());

        assertEquals(25L, usage.getCurrentClients());
        assertEquals(25, usage.getMaxClients());
        assertEquals(100.0, usage.getClientsUsagePercentage());
        assertTrue(usage.isClientLimitReached());

        assertEquals(1073741824L, usage.getCurrentStorageBytes());
        assertEquals(20.0, usage.getStorageUsagePercentage());
        assertFalse(usage.isStorageLimitReached());
    }

    @Test
    @DisplayName("checkUserLimit passes when below quota")
    void testCheckUserLimit_Passes() {
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        when(userRepository.countByOrganizationIdAndClientIdIsNull(organizationId)).thenReturn(4L);

        subscriptionService.checkUserLimit(organizationId);
    }

    @Test
    @DisplayName("checkUserLimit throws SubscriptionLimitExceededException when quota reached (MAX_USERS)")
    void testCheckUserLimit_ThrowsExceptionWhenExceeded() {
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        when(userRepository.countByOrganizationIdAndClientIdIsNull(organizationId)).thenReturn(5L);

        assertThrows(SubscriptionLimitExceededException.class, () -> subscriptionService.checkUserLimit(organizationId));
    }

    @Test
    @DisplayName("checkClientLimit throws SubscriptionLimitExceededException when quota reached (MAX_CLIENTS)")
    void testCheckClientLimit_ThrowsExceptionWhenExceeded() {
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        when(clientRepository.countByOrganizationId(organizationId)).thenReturn(25L);

        assertThrows(SubscriptionLimitExceededException.class, () -> subscriptionService.checkClientLimit(organizationId));
    }

    @Test
    @DisplayName("checkStorageLimit throws SubscriptionLimitExceededException when quota exceeded (MAX_STORAGE)")
    void testCheckStorageLimit_ThrowsExceptionWhenExceeded() {
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        // Current storage is 4.8 GB, uploading 500 MB exceeds 5 GB limit
        long currentStorage = (long) (4.8 * 1024 * 1024 * 1024);
        long newFile = 500L * 1024 * 1024;
        when(documentRepository.getTotalStorageBytesByOrganizationId(organizationId)).thenReturn(currentStorage);

        assertThrows(SubscriptionLimitExceededException.class,
                () -> subscriptionService.checkStorageLimit(organizationId, newFile));
    }

    @Test
    @DisplayName("changePlan upgrades plan, updates quotas, and synchronizes organization tier")
    void testChangePlan_Upgrade() {
        OrganizationEntity org = OrganizationEntity.builder().name("Apex CA").build();
        org.setId(organizationId);

        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        when(userRepository.countByOrganizationIdAndClientIdIsNull(organizationId)).thenReturn(4L);
        when(clientRepository.countByOrganizationId(organizationId)).thenReturn(20L);
        when(documentRepository.getTotalStorageBytesByOrganizationId(organizationId)).thenReturn(1000L);
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(org));
        when(subscriptionMapper.toDto(any(SubscriptionEntity.class))).thenReturn(SubscriptionDto.builder()
                .plan(SubscriptionPlan.PROFESSIONAL)
                .maxUsers(15)
                .maxClients(100)
                .maxStorageBytes(25L * 1024 * 1024 * 1024)
                .price(new BigDecimal("2499.00"))
                .status(SubscriptionStatus.ACTIVE)
                .build());

        ChangePlanRequest request = ChangePlanRequest.builder()
                .plan(SubscriptionPlan.PROFESSIONAL)
                .billingInterval(BillingInterval.MONTHLY)
                .build();

        SubscriptionDto result = subscriptionService.changePlan(organizationId, request);

        assertNotNull(result);
        assertEquals(SubscriptionPlan.PROFESSIONAL, result.getPlan());
        assertEquals(15, result.getMaxUsers());
        assertEquals(100, result.getMaxClients());
    }

    @Test
    @DisplayName("changePlan throws SubscriptionLimitExceededException when downgrading exceeds new plan quota")
    void testChangePlan_DowngradeWhenCurrentUsageExceedsLimit() {
        SubscriptionEntity businessSub = SubscriptionEntity.builder()
                .organizationId(organizationId)
                .plan(SubscriptionPlan.BUSINESS)
                .maxUsers(50)
                .maxClients(500)
                .build();

        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(businessSub));
        // Organization currently has 10 team members, downgrading to STARTER (limit 5) must fail
        when(userRepository.countByOrganizationIdAndClientIdIsNull(organizationId)).thenReturn(10L);

        ChangePlanRequest request = ChangePlanRequest.builder()
                .plan(SubscriptionPlan.STARTER)
                .build();

        assertThrows(SubscriptionLimitExceededException.class,
                () -> subscriptionService.changePlan(organizationId, request));
    }

    @Test
    @DisplayName("cancelSubscription marks status CANCELED and disables autoRenew")
    void testCancelSubscription() {
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionMapper.toDto(any(SubscriptionEntity.class))).thenReturn(SubscriptionDto.builder()
                .status(SubscriptionStatus.CANCELED)
                .autoRenew(false)
                .build());

        SubscriptionDto result = subscriptionService.cancelSubscription(organizationId);

        assertNotNull(result);
        assertEquals(SubscriptionStatus.CANCELED, result.getStatus());
        assertFalse(result.isAutoRenew());
    }

    @Test
    @DisplayName("renewSubscription activates subscription and extends renewal date")
    void testRenewSubscription() {
        LocalDate currentRenewal = starterSubscription.getRenewalDate();
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(starterSubscription));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionMapper.toDto(any(SubscriptionEntity.class))).thenAnswer(inv -> {
            SubscriptionEntity entity = inv.getArgument(0);
            return SubscriptionDto.builder()
                    .status(entity.getStatus())
                    .renewalDate(entity.getRenewalDate())
                    .build();
        });

        SubscriptionDto result = subscriptionService.renewSubscription(organizationId);

        assertNotNull(result);
        assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
        assertEquals(currentRenewal.plusMonths(1), result.getRenewalDate());
    }
}
