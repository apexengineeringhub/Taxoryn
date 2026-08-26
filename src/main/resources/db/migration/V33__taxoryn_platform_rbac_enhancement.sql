-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V33)
-- Multi-Context RBAC Enhancement & Platform Roles Architecture
-- ==============================================================================

-- 1. Insert Granular Platform Permissions
INSERT INTO permissions (id, code, name, module, description) VALUES
    ('10000000-0000-0000-0000-000000000101', 'PLATFORM_VIEW', 'View Platform Overview', 'PLATFORM', 'View global metrics, analytics and platform health'),
    ('10000000-0000-0000-0000-000000000102', 'PRACTICE_VIEW', 'View Practice Tenants', 'PRACTICE', 'View practice profiles, subscription status and verification'),
    ('10000000-0000-0000-0000-000000000103', 'PRACTICE_CREATE', 'Create Practice Tenant', 'PRACTICE', 'Onboard new practice tenants'),
    ('10000000-0000-0000-0000-000000000104', 'PRACTICE_UPDATE', 'Update Practice Tenant', 'PRACTICE', 'Update practice settings and profile details'),
    ('10000000-0000-0000-0000-000000000105', 'PRACTICE_VERIFY', 'Verify Practice Credentials', 'PRACTICE', 'Verify practice CA credentials and ICAI/GST certifications'),
    ('10000000-0000-0000-0000-000000000106', 'PRACTICE_SUSPEND', 'Suspend/Deactivate Practice', 'PRACTICE', 'Change practice lifecycle status to suspended/inactive'),
    ('10000000-0000-0000-0000-000000000107', 'USER_DISABLE', 'Disable User Account', 'USER', 'Suspend or disable user accounts platform-wide'),
    ('10000000-0000-0000-0000-000000000108', 'MARKETPLACE_VIEW', 'View Marketplace Operations', 'MARKETPLACE', 'View inbound marketplace leads, service categories and demand'),
    ('10000000-0000-0000-0000-000000000109', 'MARKETPLACE_MANAGE', 'Manage Marketplace Catalog', 'MARKETPLACE', 'Manage marketplace listings, services, and lead allocation'),
    ('10000000-0000-0000-0000-000000000110', 'SUBSCRIPTION_VIEW', 'View SaaS Subscriptions', 'SUBSCRIPTION', 'View platform subscription plans, active tiers, and MRR metrics'),
    ('10000000-0000-0000-0000-000000000111', 'SUBSCRIPTION_MANAGE', 'Manage SaaS Subscriptions', 'SUBSCRIPTION', 'Manage subscription plans, pricing tiers, and manual overrides'),
    ('10000000-0000-0000-0000-000000000112', 'PAYMENT_VIEW', 'View Platform Payments', 'BILLING', 'View platform SaaS billing transactions, invoices and receipts'),
    ('10000000-0000-0000-0000-000000000113', 'PAYMENT_MANAGE', 'Manage Platform Payments', 'BILLING', 'Manage payment refunds, invoicing adjustments, and payment gateways'),
    ('10000000-0000-0000-0000-000000000114', 'CONTENT_VIEW', 'View Taxoryn Content', 'CONTENT', 'View platform knowledge base, announcements, and tax templates'),
    ('10000000-0000-0000-0000-000000000115', 'CONTENT_MANAGE', 'Manage Taxoryn Content', 'CONTENT', 'Draft, edit and publish platform content and compliance updates'),
    ('10000000-0000-0000-0000-000000000116', 'CONTENT_PUBLISH', 'Publish Taxoryn Content', 'CONTENT', 'Publish platform-wide notices and articles'),
    ('10000000-0000-0000-0000-000000000117', 'SECURITY_VIEW', 'View Security Operations', 'SECURITY', 'View active security posture, token invalidations, and threat logs'),
    ('10000000-0000-0000-0000-000000000118', 'SECURITY_MANAGE', 'Manage Security Operations', 'SECURITY', 'Configure security policies, 2FA enforcement, and IP whitelists'),
    ('10000000-0000-0000-0000-000000000119', 'AUDIT_VIEW', 'View Platform Audit Trail', 'AUDIT', 'View administrative and security audit trail across the platform'),
    ('10000000-0000-0000-0000-000000000120', 'PLATFORM_SETTINGS_VIEW', 'View Platform Settings', 'SETTINGS', 'View platform configuration and feature flags'),
    ('10000000-0000-0000-0000-000000000121', 'PLATFORM_SETTINGS_MANAGE', 'Manage Platform Settings', 'SETTINGS', 'Modify platform configuration, mailers and integrations')
ON CONFLICT (code) DO NOTHING;

-- 2. Insert Standard System Platform & Practice Roles
INSERT INTO roles (id, organization_id, code, name, description, is_system_role) VALUES
    ('20000000-0000-0000-0000-000000000101', NULL, 'TAXORYN_SUPERADMIN', 'Taxoryn Platform SuperAdmin', 'Full platform administrative and operations authority', TRUE),
    ('20000000-0000-0000-0000-000000000102', NULL, 'TAXORYN_OPERATIONS_ADMIN', 'Taxoryn Operations Admin', 'Day-to-day platform operations, practice verification and account support', TRUE),
    ('20000000-0000-0000-0000-000000000103', NULL, 'TAXORYN_SUPPORT_ADMIN', 'Taxoryn Support Admin', 'Practice and user support, feedback triage and issue resolution', TRUE),
    ('20000000-0000-0000-0000-000000000104', NULL, 'TAXORYN_FINANCE_ADMIN', 'Taxoryn Finance Admin', 'Platform SaaS subscriptions, MRR/ARR and commercial revenue management', TRUE),
    ('20000000-0000-0000-0000-000000000105', NULL, 'TAXORYN_MARKETPLACE_ADMIN', 'Taxoryn Marketplace Admin', 'Marketplace service catalog, demand routing and partner optimization', TRUE),
    ('20000000-0000-0000-0000-000000000106', NULL, 'TAXORYN_CONTENT_ADMIN', 'Taxoryn Content Admin', 'Platform knowledge base, compliance calendars and publication', TRUE),
    ('20000000-0000-0000-0000-000000000107', NULL, 'TAXORYN_SECURITY_ADMIN', 'Taxoryn Security Admin', 'Platform security governance, access auditing and session control', TRUE),
    ('20000000-0000-0000-0000-000000000108', NULL, 'TAXORYN_ENGINEERING_ADMIN', 'Taxoryn Engineering Admin', 'Subsystem health monitoring and engineering issue resolution', TRUE),
    ('20000000-0000-0000-0000-000000000109', NULL, 'PRACTICE_OWNER', 'Practice Owner / Managing Partner', 'Practice founding principal with full executive ownership and billing control', TRUE),
    ('20000000-0000-0000-0000-000000000110', NULL, 'PRACTICE_ADMIN', 'Practice Administrator', 'Full administrative authority within a practice tenant', TRUE),
    ('20000000-0000-0000-0000-000000000111', NULL, 'PRACTICE_EMPLOYEE', 'Practice Staff Member', 'Executes assigned client compliance and workflow tasks', TRUE),
    ('20000000-0000-0000-0000-000000000112', NULL, 'MARKETPLACE_CUSTOMER', 'Marketplace Customer', 'Taxpayer exploring marketplace services, submitting requirements and direct inquiries', TRUE),
    ('20000000-0000-0000-0000-000000000113', NULL, 'PRACTICE_CLIENT', 'Practice Client', 'Client taxpayer associated with a specific practice tenant for compliance filing', TRUE)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 3. Assign Granular Permissions to TAXORYN_SUPERADMIN and Legacy SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000101', id FROM permissions
WHERE code IN (
    'PLATFORM_VIEW', 'PRACTICE_VIEW', 'PRACTICE_CREATE', 'PRACTICE_UPDATE', 'PRACTICE_VERIFY', 'PRACTICE_SUSPEND',
    'USER_VIEW', 'USER_CREATE', 'USER_UPDATE', 'USER_DELETE', 'USER_DISABLE',
    'MARKETPLACE_VIEW', 'MARKETPLACE_MANAGE',
    'SUBSCRIPTION_VIEW', 'SUBSCRIPTION_MANAGE', 'PAYMENT_VIEW', 'PAYMENT_MANAGE',
    'FEEDBACK_VIEW', 'FEEDBACK_REVIEW', 'FEEDBACK_ASSIGN', 'FEEDBACK_RESOLVE', 'FEEDBACK_ESCALATE', 'FEEDBACK_MANAGE',
    'CONTENT_VIEW', 'CONTENT_MANAGE', 'CONTENT_PUBLISH',
    'SECURITY_VIEW', 'SECURITY_MANAGE', 'AUDIT_VIEW',
    'ROLE_READ', 'ROLE_WRITE',
    'PLATFORM_SETTINGS_VIEW', 'PLATFORM_SETTINGS_MANAGE'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000001', id FROM permissions
WHERE code IN (
    'PLATFORM_VIEW', 'PRACTICE_VIEW', 'PRACTICE_CREATE', 'PRACTICE_UPDATE', 'PRACTICE_VERIFY', 'PRACTICE_SUSPEND',
    'USER_VIEW', 'USER_CREATE', 'USER_UPDATE', 'USER_DELETE', 'USER_DISABLE',
    'MARKETPLACE_VIEW', 'MARKETPLACE_MANAGE',
    'SUBSCRIPTION_VIEW', 'SUBSCRIPTION_MANAGE', 'PAYMENT_VIEW', 'PAYMENT_MANAGE',
    'FEEDBACK_VIEW', 'FEEDBACK_REVIEW', 'FEEDBACK_ASSIGN', 'FEEDBACK_RESOLVE', 'FEEDBACK_ESCALATE', 'FEEDBACK_MANAGE',
    'CONTENT_VIEW', 'CONTENT_MANAGE', 'CONTENT_PUBLISH',
    'SECURITY_VIEW', 'SECURITY_MANAGE', 'AUDIT_VIEW',
    'ROLE_READ', 'ROLE_WRITE',
    'PLATFORM_SETTINGS_VIEW', 'PLATFORM_SETTINGS_MANAGE'
)
ON CONFLICT DO NOTHING;

-- 4. Assign Permissions to TAXORYN_OPERATIONS_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000102', id FROM permissions
WHERE code IN (
    'PLATFORM_VIEW', 'PRACTICE_VIEW', 'PRACTICE_CREATE', 'PRACTICE_UPDATE', 'PRACTICE_VERIFY', 'PRACTICE_SUSPEND',
    'USER_VIEW', 'USER_UPDATE', 'USER_DISABLE',
    'MARKETPLACE_VIEW',
    'FEEDBACK_VIEW', 'FEEDBACK_REVIEW', 'FEEDBACK_ASSIGN', 'FEEDBACK_RESOLVE', 'FEEDBACK_ESCALATE',
    'AUDIT_VIEW'
)
ON CONFLICT DO NOTHING;

-- 5. Assign Permissions to TAXORYN_SUPPORT_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000103', id FROM permissions
WHERE code IN (
    'PLATFORM_VIEW', 'PRACTICE_VIEW', 'USER_VIEW',
    'MARKETPLACE_VIEW',
    'SUBSCRIPTION_VIEW',
    'FEEDBACK_VIEW', 'FEEDBACK_REVIEW', 'FEEDBACK_ASSIGN', 'FEEDBACK_RESOLVE',
    'AUDIT_VIEW'
)
ON CONFLICT DO NOTHING;

-- 6. Assign Permissions to TAXORYN_FINANCE_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000104', id FROM permissions
WHERE code IN (
    'PLATFORM_VIEW', 'PRACTICE_VIEW',
    'SUBSCRIPTION_VIEW', 'SUBSCRIPTION_MANAGE', 'PAYMENT_VIEW', 'PAYMENT_MANAGE',
    'AUDIT_VIEW'
)
ON CONFLICT DO NOTHING;

-- 7. Grant TAXORYN_SUPERADMIN to superadmin@taxoryn.com
INSERT INTO user_roles (user_id, role_id)
VALUES ('00000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000101')
ON CONFLICT DO NOTHING;
