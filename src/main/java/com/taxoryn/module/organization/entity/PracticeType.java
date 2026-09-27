package com.taxoryn.module.organization.entity;

/**
 * Standard classification of practice profile types for Taxoryn SaaS platform.
 *
 * Supported values:
 * - UNKNOWN: Unclassified or legacy practice
 * - SOLO: Single practitioner or very small practice
 * - FIRM: Multiple practitioners/staff operating together
 * - ENTERPRISE: Large tax/accounting organization with larger operational requirements and multiple locations
 *
 * NOTE: PracticeType is strictly used for onboarding, practice profiling, plan recommendation,
 * and default configuration. It is NEVER used directly as an authorization mechanism.
 */
public enum PracticeType {
    UNKNOWN,
    SOLO,
    FIRM,
    ENTERPRISE;

    public static PracticeType fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return PracticeType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Handle aliases
            String normalized = value.trim().toUpperCase();
            if (normalized.contains("SOLO")) {
                return SOLO;
            } else if (normalized.contains("FIRM") || normalized.contains("GROWING") || normalized.contains("PRACTICE")) {
                return FIRM;
            } else if (normalized.contains("ENTERPRISE") || normalized.contains("BUSINESS")) {
                return ENTERPRISE;
            }
            return UNKNOWN;
        }
    }

    public static PracticeType fromOrganizationType(OrganizationType orgType) {
        if (orgType == null) {
            return UNKNOWN;
        }
        return switch (orgType) {
            case SOLO, SOLO_PRACTITIONER -> SOLO;
            case FIRM, SMALL_TAX_FIRM, GROWING_PRACTICE -> FIRM;
            case ENTERPRISE, BUSINESS -> ENTERPRISE;
            case UNKNOWN -> UNKNOWN;
        };
    }
}
