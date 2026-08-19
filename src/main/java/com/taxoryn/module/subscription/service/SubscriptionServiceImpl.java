package com.taxoryn.module.subscription.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.SubscriptionLimitExceededException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.subscription.dto.ChangePlanRequest;
import com.taxoryn.module.subscription.dto.SubscriptionDto;
import com.taxoryn.module.subscription.dto.SubscriptionPlanDto;
import com.taxoryn.module.subscription.dto.SubscriptionUsageDto;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.BillingInterval;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionStatus;
import com.taxoryn.module.subscription.entity.SubscriptionPlanDefaults;
import com.taxoryn.module.subscription.mapper.SubscriptionMapper;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionDto getCurrentSubscription() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return getOrganizationSubscription(organizationId);
    }

    @Override
    @Transactional
    public SubscriptionDto getOrganizationSubscription(UUID organizationId) {
        SubscriptionEntity subscription = getOrCreateSubscriptionEntity(organizationId);
        return enrichDto(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionUsageDto getSubscriptionUsage(UUID organizationId) {
        SubscriptionEntity sub = getOrCreateSubscriptionEntity(organizationId);
        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        long currentUsers = userRepository.countByOrganizationIdAndClientIdIsNull(organizationId);
        long currentClients = clientRepository.countByOrganizationId(organizationId);
        long currentStorageBytes = documentRepository.getTotalStorageBytesByOrganizationId(organizationId);

        double userPct = sub.getMaxUsers() > 0 ? ((double) currentUsers / sub.getMaxUsers()) * 100.0 : 0.0;
        double clientPct = sub.getMaxClients() > 0 ? ((double) currentClients / sub.getMaxClients()) * 100.0 : 0.0;
        double storagePct = sub.getMaxStorageBytes() > 0 ? ((double) currentStorageBytes / sub.getMaxStorageBytes()) * 100.0 : 0.0;

        return SubscriptionUsageDto.builder()
                .organizationId(organizationId)
                .organizationName(org.getName())
                .plan(sub.getPlan())
                .status(sub.getStatus())
                .renewalDate(sub.getRenewalDate())
                .currentUsers(currentUsers)
                .maxUsers(sub.getMaxUsers())
                .usersUsagePercentage(round(userPct))
                .userLimitReached(currentUsers >= sub.getMaxUsers())
                .currentClients(currentClients)
                .maxClients(sub.getMaxClients())
                .clientsUsagePercentage(round(clientPct))
                .clientLimitReached(currentClients >= sub.getMaxClients())
                .currentStorageBytes(currentStorageBytes)
                .maxStorageBytes(sub.getMaxStorageBytes())
                .storageUsagePercentage(round(storagePct))
                .storageLimitReached(currentStorageBytes >= sub.getMaxStorageBytes())
                .build();
    }

    @Override
    public List<SubscriptionPlanDto> getAvailablePlans() {
        return SubscriptionPlanDefaults.getAvailablePlanCatalog();
    }

    @Override
    @Transactional
    public SubscriptionDto changePlan(UUID organizationId, ChangePlanRequest request) {
        SubscriptionEntity sub = getOrCreateSubscriptionEntity(organizationId);
        SubscriptionPlan newPlan = request.getPlan();
        BillingInterval interval = request.getBillingInterval() != null ? request.getBillingInterval() : sub.getBillingInterval();

        int newMaxUsers = SubscriptionPlanDefaults.getDefaultMaxUsers(newPlan);
        int newMaxClients = SubscriptionPlanDefaults.getDefaultMaxClients(newPlan);
        long newMaxStorage = SubscriptionPlanDefaults.getDefaultMaxStorageBytes(newPlan);

        // Validate Downgrade Thresholds
        long currentUsers = userRepository.countByOrganizationIdAndClientIdIsNull(organizationId);
        if (currentUsers > newMaxUsers) {
            throw new SubscriptionLimitExceededException(
                    String.format("Cannot change to %s plan: Organization currently has %d active users, which exceeds the new plan limit of %d. Please remove team members before downgrading.",
                            newPlan.name(), currentUsers, newMaxUsers));
        }

        long currentClients = clientRepository.countByOrganizationId(organizationId);
        if (currentClients > newMaxClients) {
            throw new SubscriptionLimitExceededException(
                    String.format("Cannot change to %s plan: Organization currently has %d active clients, which exceeds the new plan limit of %d. Please archive clients before downgrading.",
                            newPlan.name(), currentClients, newMaxClients));
        }

        long currentStorage = documentRepository.getTotalStorageBytesByOrganizationId(organizationId);
        if (currentStorage > newMaxStorage) {
            throw new SubscriptionLimitExceededException(
                    String.format("Cannot change to %s plan: Organization current storage (%.2f MB) exceeds the new plan limit of %.2f MB. Please free storage before downgrading.",
                            newPlan.name(), currentStorage / (1024.0 * 1024.0), newMaxStorage / (1024.0 * 1024.0)));
        }

        BigDecimal price = interval == BillingInterval.YEARLY ?
                SubscriptionPlanDefaults.getDefaultYearlyPrice(newPlan) :
                SubscriptionPlanDefaults.getDefaultMonthlyPrice(newPlan);

        sub.setPlan(newPlan);
        sub.setBillingInterval(interval);
        sub.setMaxUsers(newMaxUsers);
        sub.setMaxClients(newMaxClients);
        sub.setMaxStorageBytes(newMaxStorage);
        sub.setPrice(price);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setCancelledAt(null);

        SubscriptionEntity saved = subscriptionRepository.save(sub);

        // Sync Organization table
        organizationRepository.findById(organizationId).ifPresent(org -> {
            org.setSubscriptionPlan(OrganizationEntity.SubscriptionPlan.valueOf(newPlan.name()));
            organizationRepository.save(org);
        });

        log.info("Changed subscription plan: organizationId={}, newPlan={}, interval={}, price={}",
                organizationId, newPlan, interval, price);

        return enrichDto(saved);
    }

    @Override
    @Transactional
    public SubscriptionDto cancelSubscription(UUID organizationId) {
        SubscriptionEntity sub = getOrCreateSubscriptionEntity(organizationId);
        sub.setStatus(SubscriptionStatus.CANCELED);
        sub.setAutoRenew(false);
        sub.setCancelledAt(Instant.now());

        SubscriptionEntity saved = subscriptionRepository.save(sub);
        log.info("Cancelled subscription for organizationId={}", organizationId);
        return enrichDto(saved);
    }

    @Override
    @Transactional
    public SubscriptionDto renewSubscription(UUID organizationId) {
        SubscriptionEntity sub = getOrCreateSubscriptionEntity(organizationId);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setAutoRenew(true);
        sub.setCancelledAt(null);

        if (sub.getBillingInterval() == BillingInterval.YEARLY) {
            sub.setRenewalDate(sub.getRenewalDate().plusYears(1));
        } else {
            sub.setRenewalDate(sub.getRenewalDate().plusMonths(1));
        }

        SubscriptionEntity saved = subscriptionRepository.save(sub);
        log.info("Renewed subscription for organizationId={}, nextRenewalDate={}", organizationId, saved.getRenewalDate());
        return enrichDto(saved);
    }

    @Override
    @Transactional
    public SubscriptionEntity createInitialSubscription(UUID organizationId, SubscriptionPlan plan) {
        SubscriptionPlan initialPlan = plan != null ? plan : SubscriptionPlan.STARTER;
        LocalDate now = LocalDate.now();

        SubscriptionEntity sub = SubscriptionEntity.builder()
                .organizationId(organizationId)
                .plan(initialPlan)
                .status(SubscriptionStatus.ACTIVE)
                .billingInterval(BillingInterval.MONTHLY)
                .startDate(now)
                .renewalDate(now.plusDays(30))
                .maxUsers(SubscriptionPlanDefaults.getDefaultMaxUsers(initialPlan))
                .maxClients(SubscriptionPlanDefaults.getDefaultMaxClients(initialPlan))
                .maxStorageBytes(SubscriptionPlanDefaults.getDefaultMaxStorageBytes(initialPlan))
                .price(SubscriptionPlanDefaults.getDefaultMonthlyPrice(initialPlan))
                .autoRenew(true)
                .build();

        return subscriptionRepository.save(sub);
    }

    // =========================================================================
    // Limit Enforcement Checks
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public void checkUserLimit(UUID organizationId) {
        SubscriptionEntity sub = getOrCreateSubscriptionEntity(organizationId);
        validateSubscriptionActive(sub);

        long currentUsers = userRepository.countByOrganizationIdAndClientIdIsNull(organizationId);
        if (currentUsers >= sub.getMaxUsers()) {
            throw new SubscriptionLimitExceededException("MAX_USERS", currentUsers, sub.getMaxUsers(), sub.getPlan().name());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void checkClientLimit(UUID organizationId) {
        SubscriptionEntity sub = getOrCreateSubscriptionEntity(organizationId);
        validateSubscriptionActive(sub);

        long currentClients = clientRepository.countByOrganizationId(organizationId);
        if (currentClients >= sub.getMaxClients()) {
            throw new SubscriptionLimitExceededException("MAX_CLIENTS", currentClients, sub.getMaxClients(), sub.getPlan().name());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void checkStorageLimit(UUID organizationId, long additionalBytes) {
        SubscriptionEntity sub = getOrCreateSubscriptionEntity(organizationId);
        validateSubscriptionActive(sub);

        long currentStorage = documentRepository.getTotalStorageBytesByOrganizationId(organizationId);
        if ((currentStorage + additionalBytes) > sub.getMaxStorageBytes()) {
            throw new SubscriptionLimitExceededException("MAX_STORAGE", currentStorage + additionalBytes, sub.getMaxStorageBytes(), sub.getPlan().name());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private SubscriptionEntity getOrCreateSubscriptionEntity(UUID organizationId) {
        return subscriptionRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> createInitialSubscription(organizationId, SubscriptionPlan.STARTER));
    }

    private void validateSubscriptionActive(SubscriptionEntity sub) {
        if (sub.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new SubscriptionLimitExceededException("Subscription has expired for plan " + sub.getPlan().name() + ". Please renew your subscription.");
        }
    }

    private SubscriptionDto enrichDto(SubscriptionEntity entity) {
        SubscriptionDto dto = subscriptionMapper.toDto(entity);
        organizationRepository.findById(entity.getOrganizationId())
                .ifPresent(org -> dto.setOrganizationName(org.getName()));
        return dto;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
