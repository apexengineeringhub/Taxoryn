-- ==============================================================================
-- Taxoryn Platform — Production Data Purge & Reset Script
-- ==============================================================================
-- PURPOSE:
-- Wipes ALL practice tenant data, client records, filings, tasks, invoices,
-- documents, notifications, marketplace records, and tenant users from a
-- production PostgreSQL database to provide a 100% clean, fresh start.
--
-- PRESERVES:
--   ✓ System RBAC Permissions (`permissions`)
--   ✓ System RBAC Roles (`roles` where is_system_role = true)
--   ✓ System Role-Permission mappings (`role_permissions`)
--   ✓ Controlled Tax Service Master (`marketplace_tax_service_categories`, `marketplace_tax_services`, `marketplace_tax_service_aliases`)
--   ✓ Compliance Rule Templates (`compliance_rules` where is_system_rule = true)
--   ✓ Learn Content Taxonomy (`content_tags`)
--   ✓ Flyway Migration History (`flyway_schema_history`)
--
-- USAGE:
-- Execute this script using psql or Render PostgreSQL Console:
--   psql -U <username> -d <database_name> -f prod_data_cleanup.sql
-- ==============================================================================

BEGIN;

-- 1. Disable triggers temporarily if needed for high-speed clean
-- SET session_replication_role = 'replica'; -- (Optional: superuser only)

-- 2. Purge Document & Storage Linkages
DELETE FROM client_document_requests;
DELETE FROM documents;

-- 3. Purge Tax Notice Module Data
DELETE FROM tax_notice_timeline;
DELETE FROM tax_notice_attachments;
DELETE FROM tax_notices;

-- 4. Purge GST Compliance Module Data
DELETE FROM gst_monthly_summaries;
DELETE FROM gst_return_filings;
DELETE FROM gst_profiles;

-- 5. Purge ITR Compliance Module Data
DELETE FROM itr_returns;
DELETE FROM itr_profiles;

-- 6. Purge TDS Compliance Module Data
DELETE FROM tds_certificates;
DELETE FROM tds_deductee_entries;
DELETE FROM tds_challans;
DELETE FROM tds_returns;
DELETE FROM tds_profiles;

-- 7. Purge Billing & Invoicing Data
DELETE FROM invoice_payments;
DELETE FROM invoice_items;
DELETE FROM invoices;

-- 8. Purge Tasks & Compliance Obligations
DELETE FROM compliance_obligations;
DELETE FROM tasks;

-- 9. Purge Client Communications & Notes
DELETE FROM client_notes;

-- 10. Purge Marketplace & Customer Engagement Data
DELETE FROM marketplace_enquiry_messages;
DELETE FROM marketplace_proposals;
DELETE FROM marketplace_consultations;
DELETE FROM marketplace_reviews;
DELETE FROM marketplace_onboarding_documents;
DELETE FROM marketplace_onboardings;
DELETE FROM marketplace_leads;
DELETE FROM marketplace_practice_services;
DELETE FROM marketplace_practice_locations;
DELETE FROM marketplace_verifications;
DELETE FROM marketplace_profile_slug_redirects;
DELETE FROM marketplace_customer_profiles;
DELETE FROM customer_tax_requirements;
DELETE FROM marketplace_profiles;

-- 11. Purge Client Master Records
DELETE FROM clients;

-- 12. Purge Support & Admin Feedback Data
DELETE FROM feedback_status_history;
DELETE FROM feedback_notes;
DELETE FROM feedback_assignments;
DELETE FROM engineering_issues;
DELETE FROM application_feedback;

-- 13. Purge Notification & Communication Logs
DELETE FROM client_notifications;
DELETE FROM notifications;
DELETE FROM whatsapp_webhooks;
DELETE FROM whatsapp_messages;

-- 14. Purge Employee Records & Organization Employee Counters
DELETE FROM employees;
DELETE FROM organization_employee_counters;

-- 15. Purge Auth Tokens & Sessions
DELETE FROM user_refresh_tokens;
DELETE FROM password_reset_tokens;
DELETE FROM organization_activation_tokens;

-- 16. Purge Tenant Roles (Preserving System Roles)
DELETE FROM user_roles WHERE user_id NOT IN (
    SELECT u.id FROM users u
    JOIN user_roles ur ON u.id = ur.user_id
    JOIN roles r ON ur.role_id = r.id
    WHERE r.code = 'TAXORYN_SUPERADMIN'
);
DELETE FROM role_permissions WHERE role_id IN (
    SELECT id FROM roles WHERE is_system_role = false
);
DELETE FROM roles WHERE is_system_role = false;

-- 17. Purge Tenant Users (Preserving Root System SuperAdmin if already provisioned)
DELETE FROM users WHERE id NOT IN (
    SELECT ur.user_id FROM user_roles ur
    JOIN roles r ON ur.role_id = r.id
    WHERE r.code = 'TAXORYN_SUPERADMIN'
);

-- 18. Purge Subscriptions, Organization Settings, and Organizations
DELETE FROM subscriptions;
DELETE FROM organization_settings;
DELETE FROM organizations;

-- 19. Purge Audit & Activity Logs
DELETE FROM audit_logs;

-- Re-enable triggers if disabled
-- SET session_replication_role = 'origin';

COMMIT;

-- Verify Clean State
SELECT 'organizations' AS table_name, COUNT(*) AS remaining_count FROM organizations
UNION ALL
SELECT 'users' AS table_name, COUNT(*) AS remaining_count FROM users
UNION ALL
SELECT 'clients' AS table_name, COUNT(*) AS remaining_count FROM clients
UNION ALL
SELECT 'employees' AS table_name, COUNT(*) AS remaining_count FROM employees
UNION ALL
SELECT 'tasks' AS table_name, COUNT(*) AS remaining_count FROM tasks
UNION ALL
SELECT 'gst_profiles' AS table_name, COUNT(*) AS remaining_count FROM gst_profiles
UNION ALL
SELECT 'itr_profiles' AS table_name, COUNT(*) AS remaining_count FROM itr_profiles
UNION ALL
SELECT 'tds_profiles' AS table_name, COUNT(*) AS remaining_count FROM tds_profiles
UNION ALL
SELECT 'invoices' AS table_name, COUNT(*) AS remaining_count FROM invoices
UNION ALL
SELECT 'documents' AS table_name, COUNT(*) AS remaining_count FROM documents
UNION ALL
SELECT 'tax_notices' AS table_name, COUNT(*) AS remaining_count FROM tax_notices;
