package com.taxoryn.module.organization.entity;

/**
 * Domain classification of the Organization customer segment.
 *
 * Supported values:
 * - UNKNOWN: Default fallback for unclassified or legacy organizations
 * - SOLO_PRACTITIONER: Individual practitioner / solo tax consultant
 * - SMALL_TAX_FIRM: Small tax firm (e.g. 2-10 practitioners/staff)
 * - GROWING_PRACTICE: Mid-to-large accounting and tax practice
 * - BUSINESS: Commercial business / enterprise managing in-house tax & compliance
 *
 * NOTE: OrganizationType is strictly a domain classification and is NOT a security mechanism.
 * It is completely decoupled from RBAC roles, permissions, tenant access, and client visibility.
 */
public enum OrganizationType {
    UNKNOWN,
    SOLO_PRACTITIONER,
    SMALL_TAX_FIRM,
    GROWING_PRACTICE,
    BUSINESS
}
