package com.taxoryn.module.organization.entity;

/**
 * Domain classification of the Organization customer segment.
 *
 * Supported values:
 * - UNKNOWN: Default fallback for unclassified or legacy organizations
 * - SOLO / SOLO_PRACTITIONER: Individual practitioner / solo tax consultant
 * - FIRM / SMALL_TAX_FIRM / GROWING_PRACTICE: Tax firm (practitioners/staff operating together)
 * - ENTERPRISE / BUSINESS: Commercial business / enterprise managing large-scale operations
 *
 * NOTE: OrganizationType is strictly a domain classification and is NOT a security mechanism.
 * It is completely decoupled from RBAC roles, permissions, tenant access, and client visibility.
 */
public enum OrganizationType {
    UNKNOWN,
    SOLO,
    FIRM,
    ENTERPRISE,
    SOLO_PRACTITIONER,
    SMALL_TAX_FIRM,
    GROWING_PRACTICE,
    BUSINESS
}
