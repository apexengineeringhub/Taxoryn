package com.taxoryn.module.subscription.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
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
import com.taxoryn.module.subscription.entity.SubscriptionPlanDefaults;
import com.taxoryn.module.subscription.entity.SubscriptionResourceType;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionEntitlementServiceImpl implements SubscriptionEntitlementService {

    private final SubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;
    private final LocationRepository locationRepository;
    private final SubscriptionPlanEntitlementService subscriptionPlanEntitlementService;

    @Override
    @Transactional(readOnly = true)
    public EntitlementResult getEntitlement(UUID organizationId, SubscriptionResourceType resourceType) {
        if (organizationId == null || resourceType == null) {
            throw new IllegalArgumentException("Organization ID and Resource Type must not be null");
        }

        SubscriptionEntity sub = getOrCreateSubscriptionEntity(organizationId);
        return calculateEntitlement(organizationId, sub, resourceType);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionEntitlementsResponse getAllEntitlements(UUID organizationId) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID must not be null");
        }

        SubscriptionEntity sub = getOrCreateSubscriptionEntity(organizationId);
        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        List<EntitlementResult> results = new ArrayList<>();
        boolean anyLimitReached = false;
        boolean anyWarning = false;

        for (SubscriptionResourceType type : SubscriptionResourceType.values()) {
            EntitlementResult res = calculateEntitlement(organizationId, sub, type);
            results.add(res);
            if (!res.isAllowed()) {
                anyLimitReached = true;
            }
            if (res.isWarning()) {
                anyWarning = true;
            }
        }

        return SubscriptionEntitlementsResponse.builder()
                .organizationId(organizationId)
                .organizationName(org.getName())
                .plan(sub.getPlan())
                .status(sub.getStatus())
                .billingInterval(sub.getBillingInterval())
                .startDate(sub.getStartDate())
                .renewalDate(sub.getRenewalDate())
                .anyLimitReached(anyLimitReached)
                .anyWarning(anyWarning)
                .entitlements(results)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void checkCanCreate(UUID organizationId, SubscriptionResourceType resourceType) {
        if (organizationId == null || resourceType == null) {
            return;
        }

        SubscriptionEntity sub = getOrCreateSubscriptionEntity(organizationId);
        validateSubscriptionActive(sub);

        EntitlementResult result = calculateEntitlement(organizationId, sub, resourceType);
        if (!result.isAllowed()) {
            log.warn("Subscription limit reached for tenant={} on resource={}: current={}, limit={}",
                    organizationId, resourceType, result.getCurrentUsage(), result.getLimit());
            throw new SubscriptionLimitExceededException(
                    resourceType,
                    result.getCurrentUsage(),
                    result.getLimit(),
                    sub.getPlan() != null ? sub.getPlan().name() : "CURRENT"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void checkCanStore(UUID organizationId, long additionalBytes) {
        if (organizationId == null || additionalBytes <= 0) {
            return;
        }

        SubscriptionEntity sub = getOrCreateSubscriptionEntity(organizationId);
        validateSubscriptionActive(sub);

        long currentStorage = documentRepository.getTotalStorageBytesByOrganizationId(organizationId);
        long limit = sub.getMaxStorageBytes() > 0 ? sub.getMaxStorageBytes() : SubscriptionPlanDefaults.getDefaultMaxStorageBytes(sub.getPlan());

        if ((currentStorage + additionalBytes) > limit) {
            log.warn("Storage subscription limit exceeded for tenant={}: current={} bytes, adding={} bytes, limit={} bytes",
                    organizationId, currentStorage, additionalBytes, limit);
            throw new SubscriptionLimitExceededException(
                    SubscriptionResourceType.STORAGE,
                    currentStorage + additionalBytes,
                    limit,
                    sub.getPlan() != null ? sub.getPlan().name() : "CURRENT"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateDowngrade(UUID organizationId, SubscriptionPlan targetPlan) {
        if (organizationId == null || targetPlan == null) {
            throw new IllegalArgumentException("Organization ID and Target Plan must not be null");
        }

        int targetMaxUsers = SubscriptionPlanDefaults.getDefaultMaxUsers(targetPlan);
        int targetMaxClients = SubscriptionPlanDefaults.getDefaultMaxClients(targetPlan);
        long targetMaxStorage = SubscriptionPlanDefaults.getDefaultMaxStorageBytes(targetPlan);
        int targetMaxLocations = subscriptionPlanEntitlementService.getMaxLocations(targetPlan);
        boolean targetMultiLocation = subscriptionPlanEntitlementService.isMultiLocationEnabled(targetPlan);

        // 1. Check Active Users
        long currentUsers = userRepository.countByOrganizationIdAndClientIdIsNull(organizationId);
        if (currentUsers > targetMaxUsers) {
            throw new SubscriptionLimitExceededException(
                    String.format("Cannot change to %s plan: Organization currently has %d active team members, which exceeds the new plan limit of %d. Please remove or deactivate team members before downgrading.",
                            targetPlan.name(), currentUsers, targetMaxUsers));
        }

        // 2. Check Active Clients
        long currentClients = clientRepository.countByOrganizationId(organizationId);
        if (currentClients > targetMaxClients) {
            throw new SubscriptionLimitExceededException(
                    String.format("Cannot change to %s plan: Organization currently has %d active clients, which exceeds the new plan limit of %d. Please archive clients before downgrading.",
                            targetPlan.name(), currentClients, targetMaxClients));
        }

        // 3. Check Storage
        long currentStorage = documentRepository.getTotalStorageBytesByOrganizationId(organizationId);
        if (currentStorage > targetMaxStorage) {
            throw new SubscriptionLimitExceededException(
                    String.format("Cannot change to %s plan: Organization current storage (%s) exceeds the new plan limit of %s. Please free storage before downgrading.",
                            targetPlan.name(), formatBytes(currentStorage), formatBytes(targetMaxStorage)));
        }

        // 4. Check Locations
        long currentLocations = locationRepository.countByOrganizationIdAndIsActiveTrue(organizationId);
        if (currentLocations > targetMaxLocations || (currentLocations > 1 && !targetMultiLocation)) {
            throw new SubscriptionLimitExceededException(
                    String.format("Cannot change to %s plan: Organization currently has %d active locations, but the %s plan allows %d location(s). Please deactivate extra branch locations before downgrading.",
                            targetPlan.name(), currentLocations, targetPlan.name(), targetMaxLocations));
        }
    }

    // =========================================================================
    // Helpers & Calculation
    // =========================================================================

    private EntitlementResult calculateEntitlement(UUID organizationId, SubscriptionEntity sub, SubscriptionResourceType resourceType) {
        long currentUsage;
        long limit;
        String formattedUsage;
        String message;

        switch (resourceType) {
            case TEAM_MEMBER -> {
                currentUsage = userRepository.countByOrganizationIdAndClientIdIsNull(organizationId);
                limit = sub.getMaxUsers() > 0 ? sub.getMaxUsers() : SubscriptionPlanDefaults.getDefaultMaxUsers(sub.getPlan());
                formattedUsage = String.format("%d of %d members", currentUsage, limit);
            }
            case CLIENT -> {
                currentUsage = clientRepository.countByOrganizationId(organizationId);
                limit = sub.getMaxClients() > 0 ? sub.getMaxClients() : SubscriptionPlanDefaults.getDefaultMaxClients(sub.getPlan());
                formattedUsage = String.format("%d of %d clients", currentUsage, limit);
            }
            case STORAGE -> {
                currentUsage = documentRepository.getTotalStorageBytesByOrganizationId(organizationId);
                limit = sub.getMaxStorageBytes() > 0 ? sub.getMaxStorageBytes() : SubscriptionPlanDefaults.getDefaultMaxStorageBytes(sub.getPlan());
                formattedUsage = String.format("%s / %s", formatBytes(currentUsage), formatBytes(limit));
            }
            case LOCATION -> {
                currentUsage = locationRepository.countByOrganizationIdAndIsActiveTrue(organizationId);
                limit = subscriptionPlanEntitlementService.getMaxLocations(sub.getPlan());
                boolean multiLocation = subscriptionPlanEntitlementService.isMultiLocationEnabled(sub.getPlan());
                if (!multiLocation && limit > 1) {
                    limit = 1;
                }
                formattedUsage = String.format("%d of %d locations", currentUsage, limit);
            }
            default -> throw new IllegalArgumentException("Unsupported resource type: " + resourceType);
        }

        boolean unlimited = limit <= 0;
        double percentageUsed = unlimited ? 0.0 : ((double) currentUsage / limit) * 100.0;
        percentageUsed = round(percentageUsed);

        long remaining = unlimited ? Long.MAX_VALUE : Math.max(0, limit - currentUsage);
        boolean allowed = unlimited || currentUsage < limit;
        boolean warning = !unlimited && percentageUsed >= 80.0;

        if (!allowed) {
            message = String.format("100%% limit reached for %s. Upgrade subscription to add more.", resourceType.getDisplayName());
        } else if (warning) {
            message = String.format("%.0f%% of %s quota used. Consider upgrading soon.", percentageUsed, resourceType.getDisplayName());
        } else {
            message = String.format("%d %s remaining", remaining, resourceType.getDisplayName().toLowerCase());
        }

        return EntitlementResult.builder()
                .resourceType(resourceType)
                .currentUsage(currentUsage)
                .limit(limit)
                .remaining(remaining)
                .percentageUsed(percentageUsed)
                .allowed(allowed)
                .warning(warning)
                .unlimited(unlimited)
                .formattedUsage(formattedUsage)
                .message(message)
                .build();
    }

    private SubscriptionEntity getOrCreateSubscriptionEntity(UUID organizationId) {
        return subscriptionRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> {
                    try {
                        return createInitialSubscription(organizationId, SubscriptionPlan.STARTER);
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        return subscriptionRepository.findByOrganizationId(organizationId)
                                .orElseThrow(() -> e);
                    }
                });
    }

    private SubscriptionEntity createInitialSubscription(UUID organizationId, SubscriptionPlan plan) {
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

    private void validateSubscriptionActive(SubscriptionEntity sub) {
        if (sub != null && sub.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new SubscriptionLimitExceededException("Subscription has expired for plan " +
                    (sub.getPlan() != null ? sub.getPlan().name() : "CURRENT") + ". Please renew your subscription.");
        }
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
