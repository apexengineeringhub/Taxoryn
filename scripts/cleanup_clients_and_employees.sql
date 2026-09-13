-- ==============================================================================
-- Taxoryn Platform — Targeted Client, Employee & Practitioner Data Purge
-- ==============================================================================
-- PURPOSE:
-- Safely purges all Client, Employee, and Practitioner operational data
-- from the database, while PRESERVING:
--   ✓ Organizations & Organization Settings
--   ✓ Subscriptions
--   ✓ Practice Admin / Practice Owner user accounts
--   ✓ Platform SuperAdmin accounts
--   ✓ All System RBAC Roles & Permissions
--   ✓ Statutory Compliance Rules & Tax Service Master Catalog
--
-- TARGETS PURGED:
--   - All Client Master records (`clients`, `client_notes`, `client_document_requests`, `client_notifications`)
--   - All Employee & Practitioner Master records (`employees`, `organization_employee_counters`)
--   - All Client & Employee Compliance Filings (GST, ITR, TDS, Tax Notices)
--   - All Invoices, Invoiced Items, and Payments
--   - All Tasks & Compliance Obligations
--   - All Documents & Vault Files
--   - All Practitioner & Client User Logins (`users` with roles PRACTITIONER, STAFF, PRACTICE_EMPLOYEE, ACCOUNTANT, TAX_PROFESSIONAL, CLIENT_USER)
-- ==============================================================================

BEGIN;

-- 1. Purge Client Document Requests & Document Vault
DELETE FROM client_document_requests;
DELETE FROM documents;

-- 2. Purge Tax Notice Center Data (Clients & Assignees)
DELETE FROM tax_notice_timeline;
DELETE FROM tax_notice_attachments;
DELETE FROM tax_notices;

-- 3. Purge GST Compliance Filings & Client Profiles
DELETE FROM gst_monthly_summaries;
DELETE FROM gst_return_filings;
DELETE FROM gst_profiles;

-- 4. Purge ITR Compliance Returns & Client Profiles
DELETE FROM itr_returns;
DELETE FROM itr_profiles;

-- 5. Purge TDS Compliance Returns, Challans, Certificates & Profiles
DELETE FROM tds_certificates;
DELETE FROM tds_deductee_entries;
DELETE FROM tds_challans;
DELETE FROM tds_returns;
DELETE FROM tds_profiles;

-- 6. Purge Billing & Invoicing Data
DELETE FROM invoice_payments;
DELETE FROM invoice_items;
DELETE FROM invoices;

-- 7. Purge Tasks, Task Workflows & Compliance Obligations
DELETE FROM compliance_obligations;
DELETE FROM tasks;

-- 8. Purge Client Communication Logs & Notes
DELETE FROM client_notes;

-- 9. Purge Marketplace Leads, Onboardings, Proposals & Reviews
DELETE FROM marketplace_enquiry_messages;
DELETE FROM marketplace_proposals;
DELETE FROM marketplace_consultations;
DELETE FROM marketplace_reviews;
DELETE FROM marketplace_onboarding_documents;
DELETE FROM marketplace_onboardings;
DELETE FROM marketplace_leads;
DELETE FROM customer_tax_requirements;
DELETE FROM marketplace_customer_profiles;

-- 10. Purge Client Master Records
DELETE FROM clients;

-- 11. Purge Employee Records & Reset Employee Number Counters
DELETE FROM employees;
DELETE FROM organization_employee_counters;

-- 12. Purge Practitioner, Staff & Client User Accounts
-- Identifies users linked to clients or non-admin roles (PRACTITIONER, STAFF, PRACTICE_EMPLOYEE, ACCOUNTANT, TAX_PROFESSIONAL, CLIENT_USER, etc.)
-- Keeps users with PRACTICE_OWNER, PRACTICE_ADMIN, ORG_ADMIN, or TAXORYN_SUPERADMIN
DELETE FROM user_refresh_tokens
WHERE user_id IN (
    SELECT u.id FROM users u
    WHERE u.client_id IS NOT NULL
       OR u.id NOT IN (
           SELECT ur.user_id FROM user_roles ur
           JOIN roles r ON ur.role_id = r.id
           WHERE r.code IN ('PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'TAXORYN_SUPERADMIN', 'SUPER_ADMIN')
       )
);

DELETE FROM password_reset_tokens
WHERE user_id IN (
    SELECT u.id FROM users u
    WHERE u.client_id IS NOT NULL
       OR u.id NOT IN (
           SELECT ur.user_id FROM user_roles ur
           JOIN roles r ON ur.role_id = r.id
           WHERE r.code IN ('PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'TAXORYN_SUPERADMIN', 'SUPER_ADMIN')
       )
);

DELETE FROM user_roles
WHERE user_id IN (
    SELECT u.id FROM users u
    WHERE u.client_id IS NOT NULL
       OR u.id NOT IN (
           SELECT ur.user_id FROM user_roles ur
           JOIN roles r ON ur.role_id = r.id
           WHERE r.code IN ('PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'TAXORYN_SUPERADMIN', 'SUPER_ADMIN')
       )
);

DELETE FROM users
WHERE client_id IS NOT NULL
   OR id NOT IN (
       SELECT ur.user_id FROM user_roles ur
       JOIN roles r ON ur.role_id = r.id
       WHERE r.code IN ('PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'TAXORYN_SUPERADMIN', 'SUPER_ADMIN')
   );

-- 13. Purge Client Notifications
DELETE FROM client_notifications;

-- 14. Purge Client & Employee Audit Trail
DELETE FROM audit_logs
WHERE entity_name IN (
    'ClientEntity', 'EmployeeEntity', 'TaskEntity', 'GstProfileEntity',
    'GstReturnFilingEntity', 'ItrProfileEntity', 'ItrReturnEntity',
    'TdsProfileEntity', 'TdsReturnEntity', 'InvoiceEntity', 'TaxNoticeEntity',
    'DocumentEntity'
);

COMMIT;

-- Verification Summary
SELECT 'clients' AS entity, COUNT(*) AS remaining_count FROM clients
UNION ALL
SELECT 'employees' AS entity, COUNT(*) AS remaining_count FROM employees
UNION ALL
SELECT 'tasks' AS entity, COUNT(*) AS remaining_count FROM tasks
UNION ALL
SELECT 'gst_profiles' AS entity, COUNT(*) AS remaining_count FROM gst_profiles
UNION ALL
SELECT 'itr_profiles' AS entity, COUNT(*) AS remaining_count FROM itr_profiles
UNION ALL
SELECT 'tds_profiles' AS entity, COUNT(*) AS remaining_count FROM tds_profiles
UNION ALL
SELECT 'invoices' AS entity, COUNT(*) AS remaining_count FROM invoices
UNION ALL
SELECT 'documents' AS entity, COUNT(*) AS remaining_count FROM documents
UNION ALL
SELECT 'tax_notices' AS entity, COUNT(*) AS remaining_count FROM tax_notices
UNION ALL
SELECT 'practitioner_staff_client_users' AS entity, COUNT(*) AS remaining_count FROM users WHERE client_id IS NOT NULL
UNION ALL
SELECT 'preserved_admin_users' AS entity, COUNT(*) AS remaining_count FROM users;
