-- V87__practice_profile_features_locations_architecture.sql
-- Implements Practice Profile, Feature Catalog & Configuration, Plan Entitlements, and Practice Locations

-- 1. Practice Profiles
CREATE TABLE IF NOT EXISTS practice_profiles (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    practice_type VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    years_in_practice INT DEFAULT 0,
    approximate_client_count INT DEFAULT 0,
    services_offered TEXT,
    practitioner_count INT DEFAULT 1,
    employee_count INT DEFAULT 1,
    location_count INT DEFAULT 1,
    primary_tax_services TEXT,
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    recommended_plan VARCHAR(50),
    confirmed_plan VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_practice_profile_org UNIQUE (organization_id)
);

CREATE INDEX IF NOT EXISTS idx_practice_profile_org ON practice_profiles(organization_id);
CREATE INDEX IF NOT EXISTS idx_practice_profile_type ON practice_profiles(practice_type);

-- 2. Subscription Plans Master
CREATE TABLE IF NOT EXISTS subscription_plans (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    monthly_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    yearly_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    max_users INT NOT NULL DEFAULT 5,
    max_clients INT NOT NULL DEFAULT 25,
    max_locations INT NOT NULL DEFAULT 1,
    multi_location_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    max_storage_bytes BIGINT NOT NULL DEFAULT 5368709120,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sub_plans_code ON subscription_plans(code);
CREATE INDEX IF NOT EXISTS idx_sub_plans_status ON subscription_plans(status);

-- Seed Subscription Plans Master
INSERT INTO subscription_plans (id, code, name, description, monthly_price, yearly_price, max_users, max_clients, max_locations, multi_location_enabled, max_storage_bytes, status, created_at, updated_at)
VALUES
    ('b0000000-0000-0000-0000-000000000001', 'STARTER', 'Starter Practice', 'Essential compliance and client management for solo practitioners', 999.00, 9990.00, 5, 25, 1, FALSE, 5368709120, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000002', 'PROFESSIONAL', 'Professional Practice', 'Comprehensive tax practice management with multi-service invoicing', 2499.00, 24990.00, 15, 100, 3, TRUE, 26843545600, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000003', 'BUSINESS', 'Business Firm', 'High-volume operations for growing multi-partner CA firms', 4999.00, 49990.00, 50, 500, 10, TRUE, 107374182400, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000004', 'ENTERPRISE', 'Enterprise / Network', 'Maximum scale, multi-branch practice networks, and integrations', 9999.00, 99990.00, 250, 2500, 50, TRUE, 536870912000, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    max_locations = EXCLUDED.max_locations,
    multi_location_enabled = EXCLUDED.multi_location_enabled;

-- 3. Subscription Plan Module Entitlements
CREATE TABLE IF NOT EXISTS subscription_plan_modules (
    id UUID PRIMARY KEY,
    plan_code VARCHAR(50) NOT NULL REFERENCES subscription_plans(code) ON DELETE CASCADE,
    module_code VARCHAR(50) NOT NULL REFERENCES product_modules(code) ON DELETE CASCADE,
    is_included BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_plan_module UNIQUE (plan_code, module_code)
);

CREATE INDEX IF NOT EXISTS idx_spm_plan ON subscription_plan_modules(plan_code);
CREATE INDEX IF NOT EXISTS idx_spm_module ON subscription_plan_modules(module_code);

-- Seed Plan Module Entitlements (All core modules included in all tiers, advanced modules scaled)
INSERT INTO subscription_plan_modules (id, plan_code, module_code, is_included, created_at, updated_at)
SELECT gen_random_uuid(), p.code, m.code, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM subscription_plans p
CROSS JOIN product_modules m
ON CONFLICT (plan_code, module_code) DO NOTHING;

-- 4. Product Features Catalog
CREATE TABLE IF NOT EXISTS product_features (
    id UUID PRIMARY KEY,
    module_code VARCHAR(50) NOT NULL REFERENCES product_modules(code) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    enabled_by_default BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_product_feature UNIQUE (module_code, code)
);

CREATE INDEX IF NOT EXISTS idx_pf_module ON product_features(module_code);
CREATE INDEX IF NOT EXISTS idx_pf_code ON product_features(code);

-- Seed Initial Product Features
INSERT INTO product_features (id, module_code, code, name, description, enabled_by_default, display_order, created_at, updated_at)
VALUES
    -- TAX_NOTICES features
    ('c0000000-0000-0000-0000-000000000001', 'TAX_NOTICES', 'NOTICE_CAPTURE', 'Notice Capture & Upload', 'Ingest and classify incoming tax notices from ITD and GSTN.', TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000002', 'TAX_NOTICES', 'DEADLINE_TRACKING', 'Statutory Deadline Tracking', 'Automated computation of limitation dates and alert triggers.', TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000003', 'TAX_NOTICES', 'RESPONSE_DRAFT', 'Response & Grounds of Appeal Drafting', 'Draft legal replies, statement of facts, and submissions.', TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000004', 'TAX_NOTICES', 'RESPONSE_SUBMISSION', 'Portal Response Submission Tracking', 'Record submission date, acknowledgement receipt, and response copy.', TRUE, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000005', 'TAX_NOTICES', 'HEARING_MANAGEMENT', 'Hearing & Appearance Management', 'Track virtual / personal hearing dates and record proceedings.', TRUE, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000006', 'TAX_NOTICES', 'OUTCOME_TRACKING', 'Assessment Outcome & Demand Tracking', 'Record final orders, rectified demands, and refund outcomes.', TRUE, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- GST features
    ('c0000000-0000-0000-0000-000000000010', 'GST', 'GSTR1_FILING', 'GSTR-1 Monthly / Quarterly Filing', 'Sales register reconciliation, B2B/B2C summary, and filing.', TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000011', 'GST', 'GSTR3B_FILING', 'GSTR-3B Monthly Return Filing', 'Tax liability offsetting, ITC claim, cash ledger management.', TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000012', 'GST', 'GSTR9_FILING', 'GSTR-9 & 9C Annual Return & Audit', 'Annual reconciliation, table-by-table review, and certification.', TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000013', 'GST', 'ITC_RECONCILIATION', 'GSTR-2B vs Purchase Register Match', 'Multi-level supplier invoice matching and ITC mismatch flags.', TRUE, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- ITR features
    ('c0000000-0000-0000-0000-000000000020', 'ITR', 'ITR_COMPUTATION', 'Income Tax Computation', 'Head-wise income computation, Chapter VI-A deductions, rebate.', TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000021', 'ITR', 'AIS_TIS_RECONCILIATION', 'AIS/TIS 26AS Tax Credit Match', 'Pre-filing verification of TDS/TCS and high-value transactions.', TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- TDS features
    ('c0000000-0000-0000-0000-000000000030', 'TDS', 'QUARTERLY_RETURNS', 'TDS 24Q / 26Q / 27Q Filing', 'Quarterly return preparation, CSI challan validation, FVU check.', TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000031', 'TDS', 'FORM_16_GENERATION', 'Form 16 / 16A Certificate Vault', 'Bulk certificate extraction, digital signing, and client dispatch.', TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (module_code, code) DO NOTHING;

-- 5. Organization Features Configuration (Tenant Scoped)
CREATE TABLE IF NOT EXISTS organization_features (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    module_code VARCHAR(50) NOT NULL,
    feature_code VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_org_feature UNIQUE (organization_id, module_code, feature_code)
);

CREATE INDEX IF NOT EXISTS idx_of_org_id ON organization_features(organization_id);
CREATE INDEX IF NOT EXISTS idx_of_org_module ON organization_features(organization_id, module_code);
CREATE INDEX IF NOT EXISTS idx_of_org_feature ON organization_features(organization_id, module_code, feature_code);

-- 6. Subscription Plan Feature Entitlements
CREATE TABLE IF NOT EXISTS subscription_plan_features (
    id UUID PRIMARY KEY,
    plan_code VARCHAR(50) NOT NULL REFERENCES subscription_plans(code) ON DELETE CASCADE,
    module_code VARCHAR(50) NOT NULL,
    feature_code VARCHAR(50) NOT NULL,
    is_included BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_plan_feature UNIQUE (plan_code, module_code, feature_code)
);

CREATE INDEX IF NOT EXISTS idx_spf_plan ON subscription_plan_features(plan_code);
CREATE INDEX IF NOT EXISTS idx_spf_feature ON subscription_plan_features(module_code, feature_code);

-- Seed Plan Feature Entitlements (All core features included across plans by default)
INSERT INTO subscription_plan_features (id, plan_code, module_code, feature_code, is_included, created_at, updated_at)
SELECT gen_random_uuid(), p.code, f.module_code, f.code, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM subscription_plans p
CROSS JOIN product_features f
ON CONFLICT (plan_code, module_code, feature_code) DO NOTHING;

-- 7. Practice Locations (Multi-Location Practice Management)
CREATE TABLE IF NOT EXISTS locations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(20),
    phone VARCHAR(20),
    email VARCHAR(100),
    is_head_office BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_locations_org_id ON locations(organization_id);
CREATE INDEX IF NOT EXISTS idx_locations_is_active ON locations(organization_id, is_active);

-- 8. Employee - Location Assignments
CREATE TABLE IF NOT EXISTS employee_locations (
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    location_id UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    PRIMARY KEY (employee_id, location_id)
);

CREATE INDEX IF NOT EXISTS idx_el_employee ON employee_locations(employee_id);
CREATE INDEX IF NOT EXISTS idx_el_location ON employee_locations(location_id);

-- 9. Add location_id to clients table for optional location scoping
ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS location_id UUID REFERENCES locations(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_clients_location_id ON clients(location_id);
