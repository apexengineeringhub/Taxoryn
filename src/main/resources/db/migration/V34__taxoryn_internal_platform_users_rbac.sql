-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V34)
-- Granular RBAC Permissions for 8 Internal Taxoryn Platform Roles
-- ==============================================================================

-- 1. Insert Granular Domain Permissions for Platform Operations
INSERT INTO permissions (id, code, name, module, description) VALUES
    -- Onboarding & Support
    ('10000000-0000-0000-0000-000000000201', 'ONBOARDING_VIEW', 'View Practice Onboarding', 'ONBOARDING', 'View practice tenant verification and onboarding queue'),
    ('10000000-0000-0000-0000-000000000202', 'ONBOARDING_MANAGE', 'Manage Practice Onboarding', 'ONBOARDING', 'Approve, verify and manage practice tenant onboarding'),
    ('10000000-0000-0000-0000-000000000203', 'SUPPORT_VIEW', 'View Support Operations', 'SUPPORT', 'View customer, practice and employee support tickets'),
    ('10000000-0000-0000-0000-000000000204', 'SUPPORT_CREATE', 'Create Support Ticket', 'SUPPORT', 'Log internal support tickets and customer enquiries'),
    ('10000000-0000-0000-0000-000000000205', 'SUPPORT_ASSIGN', 'Assign Support Ticket', 'SUPPORT', 'Assign support issues to team members'),
    ('10000000-0000-0000-0000-000000000206', 'SUPPORT_RESOLVE', 'Resolve Support Ticket', 'SUPPORT', 'Close, resolve and respond to support tickets'),
    ('10000000-0000-0000-0000-000000000207', 'FEEDBACK_RESPOND', 'Respond to User Feedback', 'FEEDBACK', 'Provide official platform responses to user feedback'),
    ('10000000-0000-0000-0000-000000000208', 'CUSTOMER_VIEW_BASIC', 'View Basic Customer Profile', 'CUSTOMER', 'View non-tax sensitive customer profile metadata'),
    ('10000000-0000-0000-0000-000000000209', 'PRACTICE_VIEW_BASIC', 'View Basic Practice Profile', 'PRACTICE', 'View non-confidential practice contact information'),

    -- Marketplace Deep Ops
    ('10000000-0000-0000-0000-000000000210', 'MARKETPLACE_REQUIREMENT_VIEW', 'View Tax Requirements', 'MARKETPLACE', 'View submitted taxpayer requirements across India'),
    ('10000000-0000-0000-0000-000000000211', 'MARKETPLACE_ENQUIRY_VIEW', 'View Marketplace Enquiries', 'MARKETPLACE', 'View inbound enquiries and lead distributions'),
    ('10000000-0000-0000-0000-000000000212', 'MARKETPLACE_MATCH_VIEW', 'View Marketplace Matches', 'MARKETPLACE', 'View algorithmic matches between taxpayers and CAs'),
    ('10000000-0000-0000-0000-000000000213', 'MARKETPLACE_MATCH_MANAGE', 'Manage Marketplace Matches', 'MARKETPLACE', 'Manually override or assign lead routing to practices'),
    ('10000000-0000-0000-0000-000000000214', 'CONSULTATION_VIEW', 'View Consultations', 'MARKETPLACE', 'View consultation scheduling and booking activity'),
    ('10000000-0000-0000-0000-000000000215', 'MARKETPLACE_DISPUTE_MANAGE', 'Manage Marketplace Disputes', 'MARKETPLACE', 'Mediate and resolve taxpayer-practitioner disputes'),
    ('10000000-0000-0000-0000-000000000216', 'PRACTICE_MARKETPLACE_PROFILE_VIEW', 'View Practice Marketplace Profile', 'MARKETPLACE', 'View public listings and verification status'),

    -- Finance & Revenue
    ('10000000-0000-0000-0000-000000000217', 'MRR_VIEW', 'View MRR Analytics', 'FINANCE', 'View monthly and annual recurring subscription revenue'),
    ('10000000-0000-0000-0000-000000000218', 'REFUND_MANAGE', 'Manage Commercial Refunds', 'FINANCE', 'Authorize and process SaaS subscription refunds'),
    ('10000000-0000-0000-0000-000000000219', 'FINANCE_REPORT_VIEW', 'View Financial Reports', 'FINANCE', 'View commercial platform financial health and ledgers'),

    -- Content & Knowledge
    ('10000000-0000-0000-0000-000000000220', 'ARTICLE_CREATE', 'Create Knowledge Base Article', 'CONTENT', 'Draft educational and compliance tax articles'),
    ('10000000-0000-0000-0000-000000000221', 'ARTICLE_UPDATE', 'Update Knowledge Base Article', 'CONTENT', 'Edit and maintain knowledge base content'),
    ('10000000-0000-0000-0000-000000000222', 'ARTICLE_PUBLISH', 'Publish Knowledge Base Article', 'CONTENT', 'Publish articles to the public knowledge base'),
    ('10000000-0000-0000-0000-000000000223', 'ARTICLE_ARCHIVE', 'Archive Knowledge Base Article', 'CONTENT', 'Archive outdated knowledge base articles'),
    ('10000000-0000-0000-0000-000000000224', 'VIDEO_CREATE', 'Create Video Content', 'CONTENT', 'Upload and draft video guide assets'),
    ('10000000-0000-0000-0000-000000000225', 'VIDEO_UPDATE', 'Update Video Content', 'CONTENT', 'Edit video content titles, tags and metadata'),
    ('10000000-0000-0000-0000-000000000226', 'VIDEO_PUBLISH', 'Publish Video Content', 'CONTENT', 'Publish video tutorials and guides'),
    ('10000000-0000-0000-0000-000000000227', 'VIDEO_ARCHIVE', 'Archive Video Content', 'CONTENT', 'Archive obsolete video guide assets'),
    ('10000000-0000-0000-0000-000000000228', 'CONTENT_ANALYTICS_VIEW', 'View Content Analytics', 'CONTENT', 'View content engagement, article views, and ratings'),

    -- Security & Governance
    ('10000000-0000-0000-0000-000000000229', 'AUDIT_SEARCH', 'Search Audit Trail', 'AUDIT', 'Perform keyword and multi-dimensional search over audit records'),
    ('10000000-0000-0000-0000-000000000230', 'AUDIT_EXPORT', 'Export Audit Records', 'AUDIT', 'Export immutable audit trails for compliance inspections'),
    ('10000000-0000-0000-0000-000000000231', 'SECURITY_ALERT_VIEW', 'View Security Alerts', 'SECURITY', 'View platform security posture and threat triggers'),
    ('10000000-0000-0000-0000-000000000232', 'SECURITY_ALERT_MANAGE', 'Manage Security Alerts', 'SECURITY', 'Triage, escalate and mitigate security alert triggers'),
    ('10000000-0000-0000-0000-000000000233', 'ACCESS_REVIEW', 'Perform Access Reviews', 'SECURITY', 'Review user roles, administrative assignments and token validity'),
    ('10000000-0000-0000-0000-000000000234', 'ROLE_ASSIGNMENT_REVIEW', 'Review Role Assignments', 'SECURITY', 'Inspect platform and practice role assignment trails'),
    ('10000000-0000-0000-0000-000000000235', 'SESSION_REVIEW', 'Review Active Sessions', 'SECURITY', 'Audit active sessions and invalidate anomalous tokens'),

    -- Engineering & Platform Health
    ('10000000-0000-0000-0000-000000000236', 'PLATFORM_HEALTH_VIEW', 'View Platform Health', 'ENGINEERING', 'View database pools, memory, CPU and background workers'),
    ('10000000-0000-0000-0000-000000000237', 'SYSTEM_STATUS_VIEW', 'View Subsystem Status', 'ENGINEERING', 'Monitor API gateway, auth services, and database latencies'),
    ('10000000-0000-0000-0000-000000000238', 'INTEGRATION_VIEW', 'View External Integrations', 'ENGINEERING', 'View GSTN, ITD, TRACES and payment gateway connectors'),
    ('10000000-0000-0000-0000-000000000239', 'INTEGRATION_MANAGE', 'Manage External Integrations', 'ENGINEERING', 'Configure sandbox/production API keys and webhooks'),
    ('10000000-0000-0000-0000-000000000240', 'TECHNICAL_INCIDENT_VIEW', 'View Technical Incidents', 'ENGINEERING', 'View platform exceptions, dead-letter queues and incidents'),
    ('10000000-0000-0000-0000-000000000241', 'TECHNICAL_INCIDENT_MANAGE', 'Manage Technical Incidents', 'ENGINEERING', 'Resolve technical engineering issues and trigger replays')
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 2. TAXORYN_SUPERADMIN (Full Platform Governance)
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000101', id FROM permissions
WHERE code LIKE 'PLATFORM_%'
   OR code LIKE 'PRACTICE_%'
   OR code LIKE 'USER_%'
   OR code LIKE 'ONBOARDING_%'
   OR code LIKE 'SUPPORT_%'
   OR code LIKE 'FEEDBACK_%'
   OR code LIKE 'MARKETPLACE_%'
   OR code LIKE 'SUBSCRIPTION_%'
   OR code LIKE 'PAYMENT_%'
   OR code LIKE 'MRR_%'
   OR code LIKE 'REFUND_%'
   OR code LIKE 'FINANCE_%'
   OR code LIKE 'CONTENT_%'
   OR code LIKE 'ARTICLE_%'
   OR code LIKE 'VIDEO_%'
   OR code LIKE 'AUDIT_%'
   OR code LIKE 'SECURITY_%'
   OR code LIKE 'ACCESS_%'
   OR code LIKE 'ROLE_%'
   OR code LIKE 'SESSION_%'
   OR code LIKE 'SYSTEM_%'
   OR code LIKE 'INTEGRATION_%'
   OR code LIKE 'TECHNICAL_%'
ON CONFLICT DO NOTHING;

-- 3. TAXORYN_OPERATIONS_ADMIN Role Permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000102', id FROM permissions
WHERE code IN (
    'PLATFORM_VIEW',
    'PRACTICE_VIEW', 'PRACTICE_CREATE', 'PRACTICE_UPDATE', 'PRACTICE_VERIFY', 'PRACTICE_SUSPEND',
    'USER_VIEW', 'USER_CREATE', 'USER_UPDATE', 'USER_DISABLE',
    'ONBOARDING_VIEW', 'ONBOARDING_MANAGE',
    'FEEDBACK_VIEW', 'FEEDBACK_REVIEW', 'FEEDBACK_ASSIGN', 'FEEDBACK_RESOLVE', 'FEEDBACK_RESPOND',
    'AUDIT_VIEW'
)
ON CONFLICT DO NOTHING;

-- 4. TAXORYN_SUPPORT_ADMIN Role Permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000103', id FROM permissions
WHERE code IN (
    'PLATFORM_VIEW',
    'SUPPORT_VIEW', 'SUPPORT_CREATE', 'SUPPORT_ASSIGN', 'SUPPORT_RESOLVE',
    'FEEDBACK_VIEW', 'FEEDBACK_REVIEW', 'FEEDBACK_RESPOND',
    'CUSTOMER_VIEW_BASIC', 'PRACTICE_VIEW_BASIC', 'PRACTICE_VIEW',
    'AUDIT_VIEW'
)
ON CONFLICT DO NOTHING;

-- 5. TAXORYN_FINANCE_ADMIN Role Permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000104', id FROM permissions
WHERE code IN (
    'PLATFORM_VIEW',
    'SUBSCRIPTION_VIEW', 'SUBSCRIPTION_MANAGE',
    'MRR_VIEW',
    'PAYMENT_VIEW',
    'REFUND_MANAGE',
    'BILLING_VIEW',
    'FINANCE_REPORT_VIEW',
    'AUDIT_VIEW'
)
ON CONFLICT DO NOTHING;

-- 6. TAXORYN_MARKETPLACE_ADMIN Role Permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000105', id FROM permissions
WHERE code IN (
    'PLATFORM_VIEW',
    'MARKETPLACE_VIEW', 'MARKETPLACE_MANAGE',
    'MARKETPLACE_REQUIREMENT_VIEW',
    'MARKETPLACE_ENQUIRY_VIEW',
    'MARKETPLACE_MATCH_VIEW', 'MARKETPLACE_MATCH_MANAGE',
    'CONSULTATION_VIEW',
    'MARKETPLACE_DISPUTE_MANAGE',
    'PRACTICE_MARKETPLACE_PROFILE_VIEW',
    'AUDIT_VIEW'
)
ON CONFLICT DO NOTHING;

-- 7. TAXORYN_CONTENT_ADMIN Role Permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000106', id FROM permissions
WHERE code IN (
    'PLATFORM_VIEW',
    'CONTENT_VIEW', 'CONTENT_MANAGE', 'CONTENT_PUBLISH',
    'ARTICLE_CREATE', 'ARTICLE_UPDATE', 'ARTICLE_PUBLISH', 'ARTICLE_ARCHIVE',
    'VIDEO_CREATE', 'VIDEO_UPDATE', 'VIDEO_PUBLISH', 'VIDEO_ARCHIVE',
    'CONTENT_ANALYTICS_VIEW'
)
ON CONFLICT DO NOTHING;

-- 8. TAXORYN_SECURITY_ADMIN Role Permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000107', id FROM permissions
WHERE code IN (
    'PLATFORM_VIEW',
    'AUDIT_VIEW', 'AUDIT_SEARCH', 'AUDIT_EXPORT',
    'SECURITY_VIEW', 'SECURITY_MANAGE',
    'SECURITY_ALERT_VIEW', 'SECURITY_ALERT_MANAGE',
    'ACCESS_REVIEW',
    'ROLE_ASSIGNMENT_REVIEW',
    'SESSION_REVIEW'
)
ON CONFLICT DO NOTHING;

-- 9. TAXORYN_ENGINEERING_ADMIN Role Permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000108', id FROM permissions
WHERE code IN (
    'PLATFORM_VIEW',
    'PLATFORM_HEALTH_VIEW',
    'SYSTEM_STATUS_VIEW',
    'INTEGRATION_VIEW', 'INTEGRATION_MANAGE',
    'TECHNICAL_INCIDENT_VIEW', 'TECHNICAL_INCIDENT_MANAGE',
    'FEEDBACK_VIEW'
)
ON CONFLICT DO NOTHING;
