-- ==============================================================================
-- Taxoryn Platform — Development Seed Data
-- Demo Tenant: "Apex Tax Advisors LLP"
-- All seed accounts password: "Password123!"
-- BCrypt Hash: $2a$12$G8j3e8y3F7hQ8k0m2P5s.eF3m5h7j9l1n3p5r7t9v1x3z5b7d9f1
-- ==============================================================================

-- 1. Demo Organization
INSERT INTO organizations (id, name, legal_name, email, phone, pan, gstin, address, city, state, country, pincode, tax_registration_number, subscription_plan, status)
VALUES (
    'aa000000-0000-0000-0000-000000000001',
    'Apex Tax Advisors LLP',
    'Apex Tax Advisors Chartered Accountants LLP',
    'contact@apextax.com',
    '+919820012345',
    'AABFA1234K',
    '27AABFA1234K1Z5',
    '101 Nariman Point, Express Towers',
    'Mumbai',
    'Maharashtra',
    'India',
    '400021',
    'FRN-123456N',
    'ENTERPRISE',
    'ACTIVE'
) ON CONFLICT (id) DO NOTHING;

-- 2. Organization Settings
INSERT INTO organization_settings (id, organization_id, timezone, date_format, currency, fiscal_year_start_month, email_notifications_enabled, sms_notifications_enabled, invoice_prefix)
VALUES (
    'aa000000-0000-0000-0000-000000000002',
    'aa000000-0000-0000-0000-000000000001',
    'Asia/Kolkata',
    'dd/MM/yyyy',
    'INR',
    4,
    TRUE,
    TRUE,
    'APEX'
) ON CONFLICT (organization_id) DO NOTHING;

-- 3. Demo Users (Password for all: Password123!)
-- Password Hash using BCrypt 12 strength for "Password123!"
INSERT INTO users (id, organization_id, email, password_hash, first_name, last_name, phone, status)
VALUES
    ('bb000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 'admin@apextax.com', '$2a$12$KkQ0c8Y9fL7mH5nJ6oP.YeKj1h7f2c8a9m4p6q8t0v2x4z6b8d0f2', 'Rajesh', 'Verma', '+919820011111', 'ACTIVE'),
    ('bb000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-000000000001', 'manager@apextax.com', '$2a$12$KkQ0c8Y9fL7mH5nJ6oP.YeKj1h7f2c8a9m4p6q8t0v2x4z6b8d0f2', 'Vikram', 'Sharma', '+919820022222', 'ACTIVE'),
    ('bb000000-0000-0000-0000-000000000003', 'aa000000-0000-0000-0000-000000000001', 'taxpro@apextax.com', '$2a$12$KkQ0c8Y9fL7mH5nJ6oP.YeKj1h7f2c8a9m4p6q8t0v2x4z6b8d0f2', 'Amit', 'Desai', '+919820033333', 'ACTIVE'),
    ('bb000000-0000-0000-0000-000000000004', 'aa000000-0000-0000-0000-000000000001', 'accountant@apextax.com', '$2a$12$KkQ0c8Y9fL7mH5nJ6oP.YeKj1h7f2c8a9m4p6q8t0v2x4z6b8d0f2', 'Priya', 'Mehta', '+919820044444', 'ACTIVE'),
    ('bb000000-0000-0000-0000-000000000005', 'aa000000-0000-0000-0000-000000000001', 'staff@apextax.com', '$2a$12$KkQ0c8Y9fL7mH5nJ6oP.YeKj1h7f2c8a9m4p6q8t0v2x4z6b8d0f2', 'Suresh', 'Patel', '+919820055555', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 4. User Roles Assignment
INSERT INTO user_roles (user_id, role_id)
VALUES
    ('bb000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002'), -- Admin -> ORG_ADMIN
    ('bb000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000004'), -- Manager -> MANAGER
    ('bb000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000010'), -- Taxpro -> TAX_PROFESSIONAL
    ('bb000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000011'), -- Accountant -> ACCOUNTANT
    ('bb000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000012')  -- Staff -> EMPLOYEE
ON CONFLICT DO NOTHING;

-- 5. Demo Employees
INSERT INTO employees (id, organization_id, user_id, employee_code, first_name, last_name, email, phone, designation, department, status, joining_date, manager_id)
VALUES
    ('cc000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 'bb000000-0000-0000-0000-000000000001', 'EMP-001', 'Rajesh', 'Verma', 'admin@apextax.com', '+919820011111', 'Managing Partner', 'Executive', 'ACTIVE', '2020-01-01', NULL),
    ('cc000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-000000000001', 'bb000000-0000-0000-0000-000000000002', 'EMP-002', 'Vikram', 'Sharma', 'manager@apextax.com', '+919820022222', 'Practice Manager', 'Operations', 'ACTIVE', '2021-03-15', 'cc000000-0000-0000-0000-000000000001'),
    ('cc000000-0000-0000-0000-000000000003', 'aa000000-0000-0000-0000-000000000001', 'bb000000-0000-0000-0000-000000000003', 'EMP-003', 'Amit', 'Desai', 'taxpro@apextax.com', '+919820033333', 'Senior Tax Consultant', 'Taxation', 'ACTIVE', '2022-06-01', 'cc000000-0000-0000-0000-000000000002'),
    ('cc000000-0000-0000-0000-000000000004', 'aa000000-0000-0000-0000-000000000001', 'bb000000-0000-0000-0000-000000000004', 'EMP-004', 'Priya', 'Mehta', 'accountant@apextax.com', '+919820044444', 'Lead Accountant', 'Accounting', 'ACTIVE', '2023-01-10', 'cc000000-0000-0000-0000-000000000002'),
    ('cc000000-0000-0000-0000-000000000005', 'aa000000-0000-0000-0000-000000000001', 'bb000000-0000-0000-0000-000000000005', 'EMP-005', 'Suresh', 'Patel', 'staff@apextax.com', '+919820055555', 'Junior Tax Associate', 'Taxation', 'ACTIVE', '2023-08-01', 'cc000000-0000-0000-0000-000000000003')
ON CONFLICT (id) DO NOTHING;

-- 6. Demo Clients
INSERT INTO clients (id, organization_id, client_type, display_name, pan, gstin, email, phone, status)
VALUES
    ('dd000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 'COMPANY', 'Zenith Infotech Pvt Ltd', 'AAACZ1234D', '27AAACZ1234D1Z8', 'finance@zenithinfo.com', '+919811122233', 'ACTIVE'),
    ('dd000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-000000000001', 'LLP', 'Bluecrest Logistics LLP', 'AAALB5678E', '27AAALB5678E1Z4', 'accounts@bluecrestlog.com', '+919811144455', 'ACTIVE'),
    ('dd000000-0000-0000-0000-000000000003', 'aa000000-0000-0000-0000-000000000001', 'INDIVIDUAL', 'Anand Ramesh Joshi', 'ABCPJ9876M', NULL, 'anand.joshi@gmail.com', '+919811166677', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 7. Demo Tasks
INSERT INTO tasks (id, organization_id, client_id, assigned_to, title, description, task_category, status, priority, due_date)
VALUES
    ('ee000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 'dd000000-0000-0000-0000-000000000001', 'cc000000-0000-0000-0000-000000000003', 'GSTR-3B Monthly Return Filing', 'Reconciliation of GSTR-2B with Purchase Register and Filing', 'GST', 'IN_PROGRESS', 'HIGH', CURRENT_DATE + INTERVAL '5 days'),
    ('ee000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-000000000001', 'dd000000-0000-0000-0000-000000000001', 'cc000000-0000-0000-0000-000000000003', 'ITR-6 Corporate Computation & Audit', 'Annual audit under Section 44AB and computation preparation', 'ITR', 'TODO', 'URGENT', CURRENT_DATE + INTERVAL '15 days'),
    ('ee000000-0000-0000-0000-000000000003', 'aa000000-0000-0000-0000-000000000001', 'dd000000-0000-0000-0000-000000000002', 'cc000000-0000-0000-0000-000000000004', 'Q3 TDS 26Q Statement Filing', 'Verification of challans and Form 26Q filing on TRACES', 'COMPLIANCE', 'COMPLETED', 'MEDIUM', CURRENT_DATE - INTERVAL '2 days'),
    ('ee000000-0000-0000-0000-000000000004', 'aa000000-0000-0000-0000-000000000001', 'dd000000-0000-0000-0000-000000000003', 'cc000000-0000-0000-0000-000000000005', 'ITR-2 Individual Capital Gain Computation', 'Capital gain schedule preparation for stock trading and mutual funds', 'ITR', 'UNDER_REVIEW', 'MEDIUM', CURRENT_DATE + INTERVAL '8 days'),
    ('ee000000-0000-0000-0000-000000000005', 'aa000000-0000-0000-0000-000000000001', 'dd000000-0000-0000-0000-000000000001', 'cc000000-0000-0000-0000-000000000003', 'Overdue GST Notice 161 Reply', 'Drafting rectification response for ASMT-10 notice', 'GST', 'TODO', 'URGENT', CURRENT_DATE - INTERVAL '3 days')
ON CONFLICT (id) DO NOTHING;
