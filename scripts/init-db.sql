-- ==============================================================================
-- Taxoryn Platform — Complete Consolidated Database Schema (PostgreSQL 16)
-- Multi-Tenant SaaS Practice Management Platform
-- ==============================================================================

-- 1. Enable Required Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Organizations / Tenants
CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    pan VARCHAR(10),
    gstin VARCHAR(15),
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    pincode VARCHAR(20),
    tax_registration_number VARCHAR(100),
    subscription_plan VARCHAR(50) NOT NULL DEFAULT 'STARTER',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_organizations_status ON organizations(status);
CREATE INDEX IF NOT EXISTS idx_organizations_email ON organizations(email);

-- 3. Organization Settings (1-to-1)
CREATE TABLE IF NOT EXISTS organization_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL UNIQUE,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    date_format VARCHAR(50) NOT NULL DEFAULT 'dd/MM/yyyy',
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    fiscal_year_start_month INT NOT NULL DEFAULT 4,
    email_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    invoice_prefix VARCHAR(20) NOT NULL DEFAULT 'INV',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_org_settings_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- 4. Permissions Catalog
CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    module VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_permissions_module ON permissions(module);

-- 5. Roles (System-Wide & Tenant-Custom)
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_roles_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT uq_roles_org_code UNIQUE (organization_id, code)
);

CREATE INDEX IF NOT EXISTS idx_roles_org_id ON roles(organization_id);
CREATE INDEX IF NOT EXISTS idx_roles_is_system ON roles(is_system_role);

-- 6. Role Permissions Mapping
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- 7. Users
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    phone VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_users_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_users_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE SET NULL,
    CONSTRAINT uq_users_org_email UNIQUE (organization_id, email)
);

CREATE INDEX IF NOT EXISTS idx_users_organization_id ON users(organization_id);
CREATE INDEX IF NOT EXISTS idx_users_client_id ON users(client_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 8. User Roles Mapping
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 9. Employees
CREATE TABLE IF NOT EXISTS employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id UUID,
    employee_code VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL DEFAULT '',
    last_name VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(20),
    designation VARCHAR(100),
    department VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    joining_date DATE,
    manager_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_employees_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_employees_manager FOREIGN KEY (manager_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT uq_employees_org_code UNIQUE (organization_id, employee_code),
    CONSTRAINT uq_employees_org_email UNIQUE (organization_id, email)
);

CREATE INDEX IF NOT EXISTS idx_employees_organization_id ON employees(organization_id);
CREATE INDEX IF NOT EXISTS idx_employees_user_id ON employees(user_id);
CREATE INDEX IF NOT EXISTS idx_employees_email ON employees(email);
CREATE INDEX IF NOT EXISTS idx_employees_department ON employees(department);
CREATE INDEX IF NOT EXISTS idx_employees_status ON employees(status);
CREATE INDEX IF NOT EXISTS idx_employees_manager_id ON employees(manager_id);

-- 10. Clients
CREATE TABLE IF NOT EXISTS clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_type VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL',
    display_name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    trade_name VARCHAR(255),
    pan VARCHAR(10),
    gstin VARCHAR(15),
    tan VARCHAR(10),
    cin VARCHAR(21),
    date_of_incorporation DATE,
    email VARCHAR(255),
    phone VARCHAR(20),
    alt_phone VARCHAR(20),
    contact_person_name VARCHAR(100),
    contact_person_designation VARCHAR(100),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    pincode VARCHAR(20),
    assigned_employee_id UUID,
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_clients_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_clients_assigned_employee FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT uq_clients_org_pan UNIQUE (organization_id, pan)
);

CREATE INDEX IF NOT EXISTS idx_clients_organization_id ON clients(organization_id);
CREATE INDEX IF NOT EXISTS idx_clients_pan ON clients(pan);
CREATE INDEX IF NOT EXISTS idx_clients_gstin ON clients(gstin);
CREATE INDEX IF NOT EXISTS idx_clients_tan ON clients(tan);
CREATE INDEX IF NOT EXISTS idx_clients_assigned_emp ON clients(assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_clients_type ON clients(client_type);
CREATE INDEX IF NOT EXISTS idx_clients_city ON clients(city);
CREATE INDEX IF NOT EXISTS idx_clients_state ON clients(state);

-- 11. Client Notes / Communication History
CREATE TABLE IF NOT EXISTS client_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    author_id UUID,
    author_name VARCHAR(100),
    note_type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_client_notes_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_notes_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_client_notes_org_client ON client_notes(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_client_notes_created_at ON client_notes(created_at);

-- 12. GST Profiles & Registrations
CREATE TABLE IF NOT EXISTS gst_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    gstin VARCHAR(15) NOT NULL,
    legal_name VARCHAR(255),
    trade_name VARCHAR(255),
    gst_type VARCHAR(50) NOT NULL DEFAULT 'REGULAR',
    filing_frequency VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    registration_date DATE,
    state_code VARCHAR(10),
    principal_place_of_business TEXT,
    assigned_employee_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_gst_profiles_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_profiles_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_profiles_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT uq_gst_profiles_org_gstin UNIQUE (organization_id, gstin)
);

CREATE INDEX IF NOT EXISTS idx_gst_profiles_org ON gst_profiles(organization_id);
CREATE INDEX IF NOT EXISTS idx_gst_profiles_client ON gst_profiles(client_id);
CREATE INDEX IF NOT EXISTS idx_gst_profiles_gstin ON gst_profiles(gstin);
CREATE INDEX IF NOT EXISTS idx_gst_profiles_assigned_emp ON gst_profiles(assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_gst_profiles_status ON gst_profiles(status);

-- 13. GST Return Filings
CREATE TABLE IF NOT EXISTS gst_return_filings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    gst_profile_id UUID NOT NULL,
    client_id UUID NOT NULL,
    return_type VARCHAR(50) NOT NULL,
    return_period VARCHAR(50) NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    due_date DATE,
    filing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    filing_date DATE,
    acknowledgement_number VARCHAR(100),
    total_taxable_value NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_tax_liability NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_itc_claimed NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    tax_paid_cash NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    tax_paid_itc NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    assigned_employee_id UUID,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_gst_filings_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_filings_profile FOREIGN KEY (gst_profile_id) REFERENCES gst_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_filings_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_filings_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT uq_gst_filings_org_profile_type_period UNIQUE (organization_id, gst_profile_id, return_type, return_period)
);

CREATE INDEX IF NOT EXISTS idx_gst_filings_org ON gst_return_filings(organization_id);
CREATE INDEX IF NOT EXISTS idx_gst_filings_profile ON gst_return_filings(gst_profile_id);
CREATE INDEX IF NOT EXISTS idx_gst_filings_client ON gst_return_filings(client_id);
CREATE INDEX IF NOT EXISTS idx_gst_filings_period ON gst_return_filings(return_period);
CREATE INDEX IF NOT EXISTS idx_gst_filings_status ON gst_return_filings(filing_status);
CREATE INDEX IF NOT EXISTS idx_gst_filings_due_date ON gst_return_filings(due_date);
CREATE INDEX IF NOT EXISTS idx_gst_filings_assigned_emp ON gst_return_filings(assigned_employee_id);

-- 14. GST Monthly Summaries
CREATE TABLE IF NOT EXISTS gst_monthly_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    gst_profile_id UUID NOT NULL,
    client_id UUID NOT NULL,
    period VARCHAR(50) NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    total_sales_taxable NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    igst_sales NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    cgst_sales NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    sgst_sales NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    cess_sales NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_purchase_taxable NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    igst_purchase NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    cgst_purchase NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    sgst_purchase NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    cess_purchase NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    itc_eligible NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    itc_ineligible NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    itc_reversed NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    itc_net_claimed NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    net_tax_liability NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    challan_status VARCHAR(50) NOT NULL DEFAULT 'NOT_GENERATED',
    challan_cprn VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_gst_summaries_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_summaries_profile FOREIGN KEY (gst_profile_id) REFERENCES gst_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_summaries_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT uq_gst_summaries_org_profile_period UNIQUE (organization_id, gst_profile_id, period)
);

CREATE INDEX IF NOT EXISTS idx_gst_summaries_org ON gst_monthly_summaries(organization_id);
CREATE INDEX IF NOT EXISTS idx_gst_summaries_profile ON gst_monthly_summaries(gst_profile_id);
CREATE INDEX IF NOT EXISTS idx_gst_summaries_period ON gst_monthly_summaries(period);

-- 15. ITR Profiles
CREATE TABLE IF NOT EXISTS itr_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    pan VARCHAR(10) NOT NULL,
    taxpayer_type VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL',
    default_itr_type VARCHAR(50) NOT NULL DEFAULT 'ITR_1',
    residential_status VARCHAR(50) NOT NULL DEFAULT 'RESIDENT',
    assigned_employee_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_itr_profiles_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_itr_profiles_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_itr_profiles_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT uq_itr_profiles_org_pan UNIQUE (organization_id, pan),
    CONSTRAINT uq_itr_profiles_org_client UNIQUE (organization_id, client_id)
);

CREATE INDEX IF NOT EXISTS idx_itr_profiles_org ON itr_profiles(organization_id);
CREATE INDEX IF NOT EXISTS idx_itr_profiles_client ON itr_profiles(client_id);
CREATE INDEX IF NOT EXISTS idx_itr_profiles_pan ON itr_profiles(pan);
CREATE INDEX IF NOT EXISTS idx_itr_profiles_assigned_emp ON itr_profiles(assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_itr_profiles_status ON itr_profiles(status);

-- 16. ITR Returns
CREATE TABLE IF NOT EXISTS itr_returns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    itr_profile_id UUID,
    assessment_year VARCHAR(20) NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    itr_type VARCHAR(50) NOT NULL,
    taxpayer_type VARCHAR(50) NOT NULL,
    due_date DATE,
    filing_date DATE,
    acknowledgement_number VARCHAR(100),
    verification_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'DOCUMENTS_PENDING',
    assigned_employee_id UUID,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_itr_returns_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_itr_returns_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_itr_returns_profile FOREIGN KEY (itr_profile_id) REFERENCES itr_profiles(id) ON DELETE SET NULL,
    CONSTRAINT fk_itr_returns_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT uq_itr_returns_org_client_ay UNIQUE (organization_id, client_id, assessment_year)
);

CREATE INDEX IF NOT EXISTS idx_itr_returns_org ON itr_returns(organization_id);
CREATE INDEX IF NOT EXISTS idx_itr_returns_client ON itr_returns(client_id);
CREATE INDEX IF NOT EXISTS idx_itr_returns_profile ON itr_returns(itr_profile_id);
CREATE INDEX IF NOT EXISTS idx_itr_returns_ay ON itr_returns(assessment_year);
CREATE INDEX IF NOT EXISTS idx_itr_returns_status ON itr_returns(status);
CREATE INDEX IF NOT EXISTS idx_itr_returns_due_date ON itr_returns(due_date);
CREATE INDEX IF NOT EXISTS idx_itr_returns_assigned_emp ON itr_returns(assigned_employee_id);

-- 17. Compliance Rules Table
CREATE TABLE IF NOT EXISTS compliance_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID,
    rule_code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    compliance_type VARCHAR(50) NOT NULL,
    frequency VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    due_day INT NOT NULL,
    due_month_offset INT NOT NULL DEFAULT 1,
    fixed_due_month INT,
    description_template TEXT,
    applicable_client_types VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system_rule BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_compliance_rules_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_compliance_rules_org ON compliance_rules(organization_id);
CREATE INDEX IF NOT EXISTS idx_compliance_rules_type ON compliance_rules(compliance_type);
CREATE INDEX IF NOT EXISTS idx_compliance_rules_code ON compliance_rules(rule_code);
CREATE INDEX IF NOT EXISTS idx_compliance_rules_active ON compliance_rules(is_active);

-- Seed System Default Compliance Rules
INSERT INTO compliance_rules (id, rule_code, name, compliance_type, frequency, due_day, due_month_offset, fixed_due_month, description_template, is_system_rule, is_active)
VALUES
    (gen_random_uuid(), 'GST_GSTR1_MONTHLY', 'GSTR-1 Monthly Return', 'GST', 'MONTHLY', 11, 1, NULL, 'Monthly summary of outward supplies of goods and services for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'GST_GSTR3B_MONTHLY', 'GSTR-3B Monthly Return & Tax Payment', 'GST', 'MONTHLY', 20, 1, NULL, 'Monthly self-declaration return and net tax liability payment for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'GST_CMP08_QUARTERLY', 'CMP-08 Composition Scheme Statement', 'GST', 'QUARTERLY', 18, 1, NULL, 'Quarterly statement for payment of self-assessed tax by composition dealers for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'TDS_CHALLAN_281', 'TDS / TCS Monthly Deposit (Challan 281)', 'TDS', 'MONTHLY', 7, 1, NULL, 'Monthly deposit of Tax Deducted / Collected at Source for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'TDS_RETURN_QUARTERLY', 'TDS Quarterly Return (Form 24Q / 26Q / 27Q)', 'TDS', 'QUARTERLY', 31, 1, NULL, 'Quarterly return of tax deducted at source for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'ITR_NON_AUDIT', 'Income Tax Return (Non-Audit Individual / HUF / Firm)', 'ITR', 'ANNUALLY', 31, 0, 7, 'Annual Income Tax Return for non-audit assesses for Assessment Year {period}', TRUE, TRUE),
    (gen_random_uuid(), 'ITR_AUDIT', 'Income Tax Return (Tax Audit / Corporate Assesses)', 'ITR', 'ANNUALLY', 31, 0, 10, 'Annual Income Tax Return for corporate & audit assesses for Assessment Year {period}', TRUE, TRUE),
    (gen_random_uuid(), 'ADVANCE_TAX_Q1', 'Advance Tax Installment 1 (15%)', 'OTHER', 'QUARTERLY', 15, 0, 6, 'First installment of 15% estimated advance income tax for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'ADVANCE_TAX_Q2', 'Advance Tax Installment 2 (45%)', 'OTHER', 'QUARTERLY', 15, 0, 9, 'Second installment of 45% estimated advance income tax for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'ADVANCE_TAX_Q3', 'Advance Tax Installment 3 (75%)', 'OTHER', 'QUARTERLY', 15, 0, 12, 'Third installment of 75% estimated advance income tax for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'ADVANCE_TAX_Q4', 'Advance Tax Installment 4 (100%)', 'OTHER', 'QUARTERLY', 15, 0, 3, 'Fourth and final installment of 100% estimated advance income tax for {period}', TRUE, TRUE)
ON CONFLICT DO NOTHING;

-- 18. Compliance Obligations Table
CREATE TABLE IF NOT EXISTS compliance_obligations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    rule_id UUID,
    title VARCHAR(255) NOT NULL,
    compliance_type VARCHAR(50) NOT NULL,
    period VARCHAR(50) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    assigned_employee_id UUID,
    task_id UUID,
    completed_at TIMESTAMPTZ,
    completed_by VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_compliance_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_compliance_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_compliance_rule FOREIGN KEY (rule_id) REFERENCES compliance_rules(id) ON DELETE SET NULL,
    CONSTRAINT fk_compliance_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT fk_compliance_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    CONSTRAINT uq_compliance_org_client_period_rule UNIQUE (organization_id, client_id, period, rule_id)
);

CREATE INDEX IF NOT EXISTS idx_compliance_org_due_date ON compliance_obligations(organization_id, due_date);
CREATE INDEX IF NOT EXISTS idx_compliance_client ON compliance_obligations(client_id);
CREATE INDEX IF NOT EXISTS idx_compliance_status ON compliance_obligations(status);
CREATE INDEX IF NOT EXISTS idx_compliance_type ON compliance_obligations(compliance_type);
CREATE INDEX IF NOT EXISTS idx_compliance_emp ON compliance_obligations(assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_compliance_task ON compliance_obligations(task_id);

-- 19. Documents Table
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID,
    gst_filing_id UUID,
    itr_return_id UUID,
    task_id UUID,
    document_type VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    storage_provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    financial_year VARCHAR(20),
    assessment_year VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    checksum VARCHAR(64),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_documents_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE SET NULL,
    CONSTRAINT fk_documents_gst FOREIGN KEY (gst_filing_id) REFERENCES gst_return_filings(id) ON DELETE SET NULL,
    CONSTRAINT fk_documents_itr FOREIGN KEY (itr_return_id) REFERENCES itr_returns(id) ON DELETE SET NULL,
    CONSTRAINT fk_documents_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_documents_org ON documents(organization_id);
CREATE INDEX IF NOT EXISTS idx_documents_client ON documents(client_id);
CREATE INDEX IF NOT EXISTS idx_documents_gst ON documents(gst_filing_id);
CREATE INDEX IF NOT EXISTS idx_documents_itr ON documents(itr_return_id);
CREATE INDEX IF NOT EXISTS idx_documents_task ON documents(task_id);
CREATE INDEX IF NOT EXISTS idx_documents_type ON documents(document_type);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_storage_key ON documents(storage_key);

-- 20. Tasks
CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID,
    assigned_to UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    task_category VARCHAR(100) NOT NULL DEFAULT 'OTHER',
    status VARCHAR(50) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tasks_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_tasks_organization_id ON tasks(organization_id);
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to ON tasks(assigned_to);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date);

-- 12. Audit Logs (Append-Only)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by UUID,
    old_state JSONB,
    new_state JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_org_id ON audit_logs(organization_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);

-- ==============================================================================
-- Seed Standard System Permissions & Default Roles
-- ==============================================================================

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

-- Default System Roles
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

-- SUPER_ADMIN & ORG_ADMIN: All Permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000001', id FROM permissions
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000002', id FROM permissions
ON CONFLICT DO NOTHING;

-- MANAGER
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

-- TAX_PROFESSIONAL
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

-- ACCOUNTANT
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

-- EMPLOYEE
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000012', id FROM permissions
WHERE code IN (
    'ORGANIZATION_VIEW', 'CLIENT_VIEW',
    'TASK_VIEW', 'TASK_UPDATE',
    'GST_VIEW', 'ITR_VIEW',
    'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD'
)
ON CONFLICT DO NOTHING;

-- VIEWER
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000013', id FROM permissions
WHERE code IN (
    'ORGANIZATION_VIEW', 'USER_VIEW', 'EMPLOYEE_VIEW',
    'CLIENT_VIEW', 'TASK_VIEW',
    'GST_VIEW', 'ITR_VIEW',
    'DOCUMENT_VIEW', 'BILLING_VIEW', 'ROLE_READ'
)
ON CONFLICT DO NOTHING;

-- 21. Client Portal Permissions
INSERT INTO permissions (id, code, name, module, description)
VALUES
    ('10000000-0000-0000-0000-000000000051', 'CLIENT_PORTAL_ACCESS', 'Client Portal Access', 'PORTAL', 'Access to Taxoryn Client Portal dashboard and services'),
    ('10000000-0000-0000-0000-000000000052', 'CLIENT_PORTAL_DOCUMENT_UPLOAD', 'Client Document Upload', 'PORTAL', 'Upload client compliance documents to the vault'),
    ('10000000-0000-0000-0000-000000000053', 'CLIENT_PORTAL_DOCUMENT_VIEW', 'Client Document View', 'PORTAL', 'View and download client vault documents'),
    ('10000000-0000-0000-0000-000000000054', 'CLIENT_PORTAL_PROFILE_VIEW', 'Client Profile View', 'PORTAL', 'View client business profile and registration details'),
    ('10000000-0000-0000-0000-000000000055', 'CLIENT_PORTAL_PROFILE_UPDATE', 'Client Profile Update', 'PORTAL', 'Update client contact information and address'),
    ('10000000-0000-0000-0000-000000000056', 'CLIENT_PORTAL_STATUS_VIEW', 'Client Compliance Status View', 'PORTAL', 'View GST and ITR return filing progress and status')
ON CONFLICT (code) DO NOTHING;

-- 22. Client Portal Roles
INSERT INTO roles (id, organization_id, code, name, description, is_system_role)
VALUES
    ('20000000-0000-0000-0000-000000000014', NULL, 'CLIENT_ADMIN', 'Client Administrator', 'Primary client contact with full access to client portal, documents, and profile management', TRUE),
    ('20000000-0000-0000-0000-000000000015', NULL, 'CLIENT_USER', 'Client User', 'Client staff with access to view compliance status and upload documents', TRUE)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- Map CLIENT_ADMIN permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CLIENT_ADMIN' AND p.code IN (
    'CLIENT_PORTAL_ACCESS',
    'CLIENT_PORTAL_DOCUMENT_UPLOAD',
    'CLIENT_PORTAL_DOCUMENT_VIEW',
    'CLIENT_PORTAL_PROFILE_VIEW',
    'CLIENT_PORTAL_PROFILE_UPDATE',
    'CLIENT_PORTAL_STATUS_VIEW'
)
ON CONFLICT DO NOTHING;

-- Map CLIENT_USER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CLIENT_USER' AND p.code IN (
    'CLIENT_PORTAL_ACCESS',
    'CLIENT_PORTAL_DOCUMENT_UPLOAD',
    'CLIENT_PORTAL_DOCUMENT_VIEW',
    'CLIENT_PORTAL_PROFILE_VIEW',
    'CLIENT_PORTAL_STATUS_VIEW'
)
ON CONFLICT DO NOTHING;

-- 23. Client Notifications Table
CREATE TABLE IF NOT EXISTS client_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    action_url VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_client_notifications_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_notifications_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_client_notifications_org ON client_notifications(organization_id);
CREATE INDEX IF NOT EXISTS idx_client_notifications_client ON client_notifications(client_id);
CREATE INDEX IF NOT EXISTS idx_client_notifications_unread ON client_notifications(client_id, is_read);

-- 24. Client Document Requests / Checklist Table
CREATE TABLE IF NOT EXISTS client_document_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATE,
    financial_year VARCHAR(20),
    assessment_year VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    uploaded_document_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_doc_requests_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_requests_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_requests_doc FOREIGN KEY (uploaded_document_id) REFERENCES documents(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_doc_requests_org ON client_document_requests(organization_id);
CREATE INDEX IF NOT EXISTS idx_doc_requests_client ON client_document_requests(client_id);
CREATE INDEX IF NOT EXISTS idx_doc_requests_status ON client_document_requests(status);

