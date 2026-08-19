package com.taxoryn.module.subscription.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.subscription.dto.ChangePlanRequest;
import com.taxoryn.module.subscription.dto.SubscriptionDto;
import com.taxoryn.module.subscription.dto.SubscriptionPlanDto;
import com.taxoryn.module.subscription.dto.SubscriptionUsageDto;
import com.taxoryn.module.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/subscriptions", "/api/subscriptions"})
@RequiredArgsConstructor
@Tag(name = "SaaS Subscriptions & Billing", description = "SaaS Plan Management, Quota Limits (MAX_USERS, MAX_CLIENTS, MAX_STORAGE), and Subscription Upgrades")
@SecurityRequirement(name = "BearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get active subscription", description = "Retrieves the active SaaS subscription tier, renewal date, and configured limits for the current organization.")
    public ResponseEntity<ApiResponse<SubscriptionDto>> getCurrentSubscription() {
        SubscriptionDto subscription = subscriptionService.getCurrentSubscription();
        return ResponseEntity.ok(ApiResponse.success("Subscription retrieved successfully", subscription));
    }

    @GetMapping("/usage")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get subscription usage & limits", description = "Retrieves current usage statistics against subscription limits for Users (MAX_USERS), Clients (MAX_CLIENTS), and Storage (MAX_STORAGE).")
    public ResponseEntity<ApiResponse<SubscriptionUsageDto>> getSubscriptionUsage() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        SubscriptionUsageDto usage = subscriptionService.getSubscriptionUsage(organizationId);
        return ResponseEntity.ok(ApiResponse.success("Subscription usage retrieved successfully", usage));
    }

    @GetMapping("/plans")
    @Operation(summary = "List subscription plans catalog", description = "Retrieves public list of all available SaaS subscription tiers (STARTER, PROFESSIONAL, BUSINESS, ENTERPRISE) with pricing and limits.")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanDto>>> getAvailablePlans() {
        List<SubscriptionPlanDto> plans = subscriptionService.getAvailablePlans();
        return ResponseEntity.ok(ApiResponse.success("Subscription plans catalog retrieved successfully", plans));
    }

    @PostMapping("/change-plan")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Change subscription plan", description = "Upgrades or downgrades the organization's subscription plan tier and updates resource quotas.")
    public ResponseEntity<ApiResponse<SubscriptionDto>> changePlan(@Valid @RequestBody ChangePlanRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        SubscriptionDto updated = subscriptionService.changePlan(organizationId, request);
        return ResponseEntity.ok(ApiResponse.success("Subscription plan updated successfully", updated));
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Cancel subscription", description = "Cancels active subscription and disables auto-renewal at end of billing cycle.")
    public ResponseEntity<ApiResponse<SubscriptionDto>> cancelSubscription() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        SubscriptionDto cancelled = subscriptionService.cancelSubscription(organizationId);
        return ResponseEntity.ok(ApiResponse.success("Subscription cancelled successfully", cancelled));
    }

    @PostMapping("/renew")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Renew subscription", description = "Renews subscription and extends renewal date by one billing cycle.")
    public ResponseEntity<ApiResponse<SubscriptionDto>> renewSubscription() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        SubscriptionDto renewed = subscriptionService.renewSubscription(organizationId);
        return ResponseEntity.ok(ApiResponse.success("Subscription renewed successfully", renewed));
    }
}
