package com.taxoryn.module.moduleconfig.model;

/**
 * Functional category groupings for product modules in Taxoryn:
 * - CORE: Essential platform capabilities that cannot be disabled (Auth, Roles, Users, Audit, Notifications).
 * - FOUNDATION: Fundamental practice operations that cannot be disabled (Clients, Tasks, Documents, Invoices, Dashboard).
 * - BUSINESS: Specialized tax compliance modules configurable per tenant and governed by plan (GST, ITR, TDS, Notices).
 * - OPTIONAL: Modular extensions, client portal, integrations, and revenue streams (Marketplace, Gmail, Self ITR).
 */
public enum ProductModuleCategory {
    CORE,
    FOUNDATION,
    BUSINESS,
    OPTIONAL
}
