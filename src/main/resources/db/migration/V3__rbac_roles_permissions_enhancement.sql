-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V3)
-- Organization-Level RBAC & Granular Permission Catalog
-- ==============================================================================

-- 1. Insert/Update Granular System Permissions
INSERT INTO permissions (id, code, name, module, description) VALUES
    ('10000000-0000-0000-0000-000000000021', 'ORGANIZATION_VIEW', 'View Organization Details', 'ORGANIZATION', 'Allows viewing organization profile and settings'),
    ('10000000-0000-0000-0000-000000000022', 'ORGANIZATION_UPDATE', 'Update Organization Details', 'ORGANIZATION', 'Allows modifying organization profile, settings, and status'),
    ('10000000-0000-0000-0000-000000000023', 'USER_CREATE', 'Create User', 'USER', 'Allows registering team members'),
    ('10000000-0000-0000-0000-000000000024', 'USER_VIEW', 'View Users', 'USER', 'Allows viewing team members and roles'),
    ('10000000-0000-0000-0000-000000000025', 'USER_UPDATE', 'Update User', 'USER', 'Allows updating user profiles, statuses and role assignments'),
    ('10000000-0000-0000-0000-000000000026', 'USER_DELETE', 'Delete/Deactivate User', 'USER', 'Allows deactivating team members'),
    ('10000000-0000-0000-0000-000000000027', 'EMPLOYEE_CREATE', 'Create Employee Record', 'EMPLOYEE', 'Allows onboarding employee master records'),
    ('10000000-0000-0000-0000-000000000028', 'EMPLOYEE_VIEW', 'View Employee Record', 'EMPLOYEE', 'Allows viewing employee master data'),
    ('10000000-0000-0000-0000-000000000029', 'EMPLOYEE_UPDATE', 'Update Employee Record', 'EMPLOYEE', 'Allows modifying employee designation, department, status'),
    ('10000000-0000-0000-0000-000000000030', 'CLIENT_CREATE', 'Create Client', 'CLIENT', 'Allows onboarding new clients'),
    ('10000000-0000-0000-0000-000000000031', 'CLIENT_VIEW', 'View Clients', 'CLIENT', 'Allows viewing client master records and details'),
    ('10000000-0000-0000-0000-000000000032', 'CLIENT_UPDATE', 'Update Client', 'CLIENT', 'Allows modifying client profiles and registrations'),
    ('10000000-0000-0000-0000-000000000033', 'CLIENT_DELETE', 'Delete/Archive Client', 'CLIENT', 'Allows archiving client records'),
    ('10000000-0000-0000-0000-000000000034', 'TASK_CREATE', 'Create Task', 'TASK', 'Allows creating new workflow tasks'),
    ('10000000-0000-0000-0000-000000000035', 'TASK_VIEW', 'View Tasks', 'TASK', 'Allows viewing assigned and organization tasks'),
    ('10000000-0000-0000-0000-000000000036', 'TASK_UPDATE', 'Update Task', 'TASK', 'Allows modifying task status, priority, and progress'),
    ('10000000-0000-0000-0000-000000000037', 'TASK_ASSIGN', 'Assign Task', 'TASK', 'Allows delegating tasks to other team members'),
    ('10000000-0000-0000-0000-000000000038', 'GST_VIEW', 'View GST Returns', 'GST', 'Allows viewing GST filing status, ledgers, and computations'),
    ('10000000-0000-0000-0000-000000000039', 'GST_CREATE', 'Prepare GST Returns', 'GST', 'Allows preparing GSTR-1, GSTR-3B filings'),
    ('10000000-0000-0000-0000-000000000040', 'GST_UPDATE', 'Modify GST Filings', 'GST', 'Allows updating draft returns and filing status'),
    ('10000000-0000-0000-0000-000000000041', 'ITR_VIEW', 'View ITR Computations', 'ITR', 'Allows viewing income tax return computations'),
    ('10000000-0000-0000-0000-000000000042', 'ITR_CREATE', 'Prepare ITR Forms', 'ITR', 'Allows drafting ITR-1 to ITR-7 filings'),
    ('10000000-0000-0000-0000-000000000043', 'ITR_UPDATE', 'Modify ITR Filings', 'ITR', 'Allows modifying ITR schedules and computation figures'),
    ('10000000-0000-0000-0000-000000000044', 'DOCUMENT_VIEW', 'View Documents', 'DOCUMENT', 'Allows browsing and downloading client documents'),
    ('10000000-0000-0000-0000-000000000045', 'DOCUMENT_UPLOAD', 'Upload Documents', 'DOCUMENT', 'Allows uploading client bills, certificates, and reports'),
    ('10000000-0000-0000-0000-000000000046', 'DOCUMENT_DELETE', 'Delete Documents', 'DOCUMENT', 'Allows deleting uploaded files from client vault'),
    ('10000000-0000-0000-0000-000000000047', 'BILLING_VIEW', 'View Invoices & Billing', 'BILLING', 'Allows viewing invoices, receivables and receipts'),
    ('10000000-0000-0000-0000-000000000048', 'BILLING_CREATE', 'Generate Invoices', 'BILLING', 'Allows generating client invoices and recording payments'),
    ('10000000-0000-0000-0000-000000000049', 'ROLE_READ', 'View Roles', 'ROLE', 'Allows viewing role definitions and permissions'),
    ('10000000-0000-0000-0000-000000000050', 'ROLE_WRITE', 'Manage Roles', 'ROLE', 'Allows creating and managing custom organization roles')
ON CONFLICT (code) DO NOTHING;

-- 2. Insert Standard Default System Roles
INSERT INTO roles (id, organization_id, code, name, description, is_system_role) VALUES
    ('20000000-0000-0000-0000-000000000001', NULL, 'SUPER_ADMIN', 'Platform Super Administrator', 'Full platform administrative access across all tenants', TRUE),
    ('20000000-0000-0000-0000-000000000002', NULL, 'ORG_ADMIN', 'Organization Administrator', 'Full administrative authority within an organization', TRUE),
    ('20000000-0000-0000-0000-000000000004', NULL, 'MANAGER', 'Practice Manager', 'Manages tasks, assignments, clients, workflows and staff review', TRUE),
    ('20000000-0000-0000-0000-000000000010', NULL, 'TAX_PROFESSIONAL', 'Senior Tax Professional', 'Handles GST, ITR, client filings, computation and document reviews', TRUE),
    ('20000000-0000-0000-0000-000000000011', NULL, 'ACCOUNTANT', 'Staff Accountant', 'Prepares GST filings, invoices, receipts, and client data entry', TRUE),
    ('20000000-0000-0000-0000-000000000012', NULL, 'EMPLOYEE', 'Firm Employee / Staff', 'Executes assigned tasks, reviews documents, and views client work', TRUE),
    ('20000000-0000-0000-0000-000000000013', NULL, 'VIEWER', 'Read-Only Viewer', 'Read-only viewing permissions across practice modules', TRUE)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 3. Assign Permissions to Default Roles

-- 3.1 SUPER_ADMIN: All Permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000001', id FROM permissions
ON CONFLICT DO NOTHING;

-- 3.2 ORG_ADMIN: All Permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000002', id FROM permissions
ON CONFLICT DO NOTHING;

-- 3.3 MANAGER:
-- Organization View, User View, Employee View, Client CRUD, Task CRUD + Assign, GST All, ITR All, Document View/Upload, Billing View, Role Read
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000004', id FROM permissions
WHERE code IN (
    'ORGANIZATION_VIEW', 'USER_VIEW', 'EMPLOYEE_VIEW',
    'CLIENT_CREATE', 'CLIENT_VIEW', 'CLIENT_UPDATE', 'CLIENT_DELETE',
    'TASK_CREATE', 'TASK_VIEW', 'TASK_UPDATE', 'TASK_ASSIGN',
    'GST_VIEW', 'GST_CREATE', 'GST_UPDATE',
    'ITR_VIEW', 'ITR_CREATE', 'ITR_UPDATE',
    'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD',
    'BILLING_VIEW', 'ROLE_READ'
)
ON CONFLICT DO NOTHING;

-- 3.4 TAX_PROFESSIONAL:
-- Client View/Update, Task View/Update, GST All, ITR All, Document View/Upload, Billing View
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000010', id FROM permissions
WHERE code IN (
    'ORGANIZATION_VIEW', 'CLIENT_VIEW', 'CLIENT_UPDATE',
    'TASK_VIEW', 'TASK_UPDATE',
    'GST_VIEW', 'GST_CREATE', 'GST_UPDATE',
    'ITR_VIEW', 'ITR_CREATE', 'ITR_UPDATE',
    'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD',
    'BILLING_VIEW'
)
ON CONFLICT DO NOTHING;

-- 3.5 ACCOUNTANT:
-- Client View, Task View/Update, GST All, ITR View, Document View/Upload, Billing View/Create
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000011', id FROM permissions
WHERE code IN (
    'ORGANIZATION_VIEW', 'CLIENT_VIEW',
    'TASK_VIEW', 'TASK_UPDATE',
    'GST_VIEW', 'GST_CREATE', 'GST_UPDATE',
    'ITR_VIEW',
    'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD',
    'BILLING_VIEW', 'BILLING_CREATE'
)
ON CONFLICT DO NOTHING;

-- 3.6 EMPLOYEE:
-- Client View, Task View/Update, GST View, ITR View, Document View/Upload
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000012', id FROM permissions
WHERE code IN (
    'ORGANIZATION_VIEW', 'CLIENT_VIEW',
    'TASK_VIEW', 'TASK_UPDATE',
    'GST_VIEW', 'ITR_VIEW',
    'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD'
)
ON CONFLICT DO NOTHING;

-- 3.7 VIEWER:
-- View-only across all modules
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000013', id FROM permissions
WHERE code IN (
    'ORGANIZATION_VIEW', 'USER_VIEW', 'EMPLOYEE_VIEW',
    'CLIENT_VIEW', 'TASK_VIEW',
    'GST_VIEW', 'ITR_VIEW',
    'DOCUMENT_VIEW', 'BILLING_VIEW', 'ROLE_READ'
)
ON CONFLICT DO NOTHING;
