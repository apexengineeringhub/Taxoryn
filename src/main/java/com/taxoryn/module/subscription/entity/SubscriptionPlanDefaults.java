package com.taxoryn.module.subscription.entity;

import com.taxoryn.module.subscription.dto.SubscriptionPlanDto;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;

import java.math.BigDecimal;
import java.util.List;

public final class SubscriptionPlanDefaults {

    private SubscriptionPlanDefaults() {
    }

    public static int getDefaultMaxUsers(SubscriptionPlan plan) {
        return switch (plan) {
            case STARTER -> 5;
            case PROFESSIONAL -> 15;
            case BUSINESS -> 50;
            case ENTERPRISE -> 250;
        };
    }

    public static int getDefaultMaxClients(SubscriptionPlan plan) {
        return switch (plan) {
            case STARTER -> 25;
            case PROFESSIONAL -> 100;
            case BUSINESS -> 500;
            case ENTERPRISE -> 2500;
        };
    }

    public static long getDefaultMaxStorageBytes(SubscriptionPlan plan) {
        return switch (plan) {
            case STARTER -> 5L * 1024 * 1024 * 1024;      // 5 GB
            case PROFESSIONAL -> 25L * 1024 * 1024 * 1024;  // 25 GB
            case BUSINESS -> 100L * 1024 * 1024 * 1024;    // 100 GB
            case ENTERPRISE -> 500L * 1024 * 1024 * 1024;  // 500 GB
        };
    }

    public static BigDecimal getDefaultMonthlyPrice(SubscriptionPlan plan) {
        return switch (plan) {
            case STARTER -> new BigDecimal("999.00");
            case PROFESSIONAL -> new BigDecimal("2499.00");
            case BUSINESS -> new BigDecimal("4999.00");
            case ENTERPRISE -> new BigDecimal("9999.00");
        };
    }

    public static BigDecimal getDefaultYearlyPrice(SubscriptionPlan plan) {
        return switch (plan) {
            case STARTER -> new BigDecimal("9990.00");
            case PROFESSIONAL -> new BigDecimal("24990.00");
            case BUSINESS -> new BigDecimal("49990.00");
            case ENTERPRISE -> new BigDecimal("99990.00");
        };
    }

    public static List<SubscriptionPlanDto> getAvailablePlanCatalog() {
        return List.of(
                SubscriptionPlanDto.builder()
                        .plan(SubscriptionPlan.STARTER)
                        .name("Starter Practice")
                        .description("Essential compliance and client management for solo practitioners and boutique tax consultants")
                        .monthlyPrice(getDefaultMonthlyPrice(SubscriptionPlan.STARTER))
                        .yearlyPrice(getDefaultYearlyPrice(SubscriptionPlan.STARTER))
                        .maxUsers(getDefaultMaxUsers(SubscriptionPlan.STARTER))
                        .maxClients(getDefaultMaxClients(SubscriptionPlan.STARTER))
                        .maxStorageBytes(getDefaultMaxStorageBytes(SubscriptionPlan.STARTER))
                        .formattedStorage("5 GB")
                        .features(List.of(
                                "Up to 5 team members",
                                "Up to 25 active clients",
                                "5 GB document vault storage",
                                "GST & ITR return tracking",
                                "Compliance calendar & reminders",
                                "Client portal self-service"
                        ))
                        .isPopular(false)
                        .build(),

                SubscriptionPlanDto.builder()
                        .plan(SubscriptionPlan.PROFESSIONAL)
                        .name("Professional Practice")
                        .description("Comprehensive tax practice management with multi-service invoicing and staff workload balancing")
                        .monthlyPrice(getDefaultMonthlyPrice(SubscriptionPlan.PROFESSIONAL))
                        .yearlyPrice(getDefaultYearlyPrice(SubscriptionPlan.PROFESSIONAL))
                        .maxUsers(getDefaultMaxUsers(SubscriptionPlan.PROFESSIONAL))
                        .maxClients(getDefaultMaxClients(SubscriptionPlan.PROFESSIONAL))
                        .maxStorageBytes(getDefaultMaxStorageBytes(SubscriptionPlan.PROFESSIONAL))
                        .formattedStorage("25 GB")
                        .features(List.of(
                                "Up to 15 team members",
                                "Up to 100 active clients",
                                "25 GB document vault storage",
                                "Full GST (GSTR-1, 3B, 9) & ITR lifecycle",
                                "Client billing & payment receipts",
                                "Granular RBAC role delegation",
                                "Priority email & chat support"
                        ))
                        .isPopular(true)
                        .build(),

                SubscriptionPlanDto.builder()
                        .plan(SubscriptionPlan.BUSINESS)
                        .name("Business Firm")
                        .description("High-volume operations for growing multi-partner CA firms and corporate compliance departments")
                        .monthlyPrice(getDefaultMonthlyPrice(SubscriptionPlan.BUSINESS))
                        .yearlyPrice(getDefaultYearlyPrice(SubscriptionPlan.BUSINESS))
                        .maxUsers(getDefaultMaxUsers(SubscriptionPlan.BUSINESS))
                        .maxClients(getDefaultMaxClients(SubscriptionPlan.BUSINESS))
                        .maxStorageBytes(getDefaultMaxStorageBytes(SubscriptionPlan.BUSINESS))
                        .formattedStorage("100 GB")
                        .features(List.of(
                                "Up to 50 team members",
                                "Up to 500 active clients",
                                "100 GB document vault storage",
                                "Executive analytics & partner dashboards",
                                "Automated batch compliance generator",
                                "Custom role creation & audit logging",
                                "Dedicated account manager"
                        ))
                        .isPopular(false)
                        .build(),

                SubscriptionPlanDto.builder()
                        .plan(SubscriptionPlan.ENTERPRISE)
                        .name("Enterprise / Network")
                        .description("Maximum scale, multi-branch practice networks, and custom compliance automation integrations")
                        .monthlyPrice(getDefaultMonthlyPrice(SubscriptionPlan.ENTERPRISE))
                        .yearlyPrice(getDefaultYearlyPrice(SubscriptionPlan.ENTERPRISE))
                        .maxUsers(getDefaultMaxUsers(SubscriptionPlan.ENTERPRISE))
                        .maxClients(getDefaultMaxClients(SubscriptionPlan.ENTERPRISE))
                        .maxStorageBytes(getDefaultMaxStorageBytes(SubscriptionPlan.ENTERPRISE))
                        .formattedStorage("500 GB")
                        .features(List.of(
                                "Up to 250 team members",
                                "Up to 2500 active clients",
                                "500 GB document vault storage",
                                "White-label client portal & branding",
                                "Custom S3 storage bucket configuration",
                                "Enterprise SLA & 24/7 dedicated support",
                                "API webhooks & custom ERP integrations"
                        ))
                        .isPopular(false)
                        .build()
        );
    }
}
