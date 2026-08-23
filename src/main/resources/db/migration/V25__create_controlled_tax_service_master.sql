-- ==============================================================================
-- Taxoryn Platform — Flyway Migration V25: Controlled Tax Service Master
-- Centralized reference catalog for Indian tax and compliance services
-- ==============================================================================

-- 1. Tax Service Categories Master
CREATE TABLE IF NOT EXISTS marketplace_tax_service_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_mp_tax_cat_code ON marketplace_tax_service_categories(code);
CREATE INDEX IF NOT EXISTS idx_mp_tax_cat_active ON marketplace_tax_service_categories(is_active, sort_order);

-- 2. Controlled Tax Services Master
CREATE TABLE IF NOT EXISTS marketplace_tax_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_mp_tax_svc_cat FOREIGN KEY (category_id) REFERENCES marketplace_tax_service_categories(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_mp_tax_svc_code ON marketplace_tax_services(code);
CREATE INDEX IF NOT EXISTS idx_mp_tax_svc_cat ON marketplace_tax_services(category_id);
CREATE INDEX IF NOT EXISTS idx_mp_tax_svc_active ON marketplace_tax_services(is_active, sort_order);

-- 3. Tax Service Search Aliases
CREATE TABLE IF NOT EXISTS marketplace_tax_service_aliases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tax_service_id UUID NOT NULL,
    alias VARCHAR(255) NOT NULL,
    normalized_alias VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mp_tax_alias_svc FOREIGN KEY (tax_service_id) REFERENCES marketplace_tax_services(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mp_tax_alias_svc ON marketplace_tax_service_aliases(tax_service_id);
CREATE INDEX IF NOT EXISTS idx_mp_tax_alias_norm ON marketplace_tax_service_aliases(normalized_alias);

-- 4. Practice Service Associations (Selected services offered by a practice)
CREATE TABLE IF NOT EXISTS marketplace_practice_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    marketplace_profile_id UUID NOT NULL,
    tax_service_id UUID NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_mp_prac_svc_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_mp_prac_svc_profile FOREIGN KEY (marketplace_profile_id) REFERENCES marketplace_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_mp_prac_svc_master FOREIGN KEY (tax_service_id) REFERENCES marketplace_tax_services(id) ON DELETE RESTRICT,
    CONSTRAINT uk_mp_prac_profile_svc UNIQUE (marketplace_profile_id, tax_service_id)
);

CREATE INDEX IF NOT EXISTS idx_mp_prac_svc_profile ON marketplace_practice_services(marketplace_profile_id, is_active);
CREATE INDEX IF NOT EXISTS idx_mp_prac_svc_master ON marketplace_practice_services(tax_service_id, is_active);
CREATE INDEX IF NOT EXISTS idx_mp_prac_svc_org ON marketplace_practice_services(organization_id);

-- ==============================================================================
-- 5. SEED INITIAL CONTROLLED SERVICE CATALOGUE
-- ==============================================================================

-- Categories
INSERT INTO marketplace_tax_service_categories (id, code, name, description, icon, sort_order, is_active) VALUES
('c0000001-0000-0000-0000-000000000001', 'INCOME_TAX', 'Income Tax', 'Direct tax return filing, scrutiny notices, tax assessments and refunds', 'FileText', 1, TRUE),
('c0000001-0000-0000-0000-000000000002', 'GST', 'Goods & Services Tax', 'GST registrations, monthly/annual returns, ITC audit and departmental representation', 'Layers', 2, TRUE),
('c0000001-0000-0000-0000-000000000003', 'TAX_PLANNING', 'Tax Planning & Advisory', 'Strategic tax structuring for salaried, HNIs, startups and corporate entities', 'TrendingUp', 3, TRUE),
('c0000001-0000-0000-0000-000000000004', 'BUSINESS_REGISTRATION', 'Business Registration & Formation', 'Company incorporation, LLP formation, MSME Udyam, and government licenses', 'Building', 4, TRUE),
('c0000001-0000-0000-0000-000000000005', 'ACCOUNTING_COMPLIANCE', 'Accounting & Compliance', 'Bookkeeping, tax audit u/s 44AB, statutory audit, and RoC secretarial compliance', 'ShieldCheck', 5, TRUE)
ON CONFLICT (code) DO NOTHING;

-- Income Tax Services
INSERT INTO marketplace_tax_services (id, category_id, code, name, description, sort_order, is_active) VALUES
('s0000001-0000-0000-0000-000000000001', 'c0000001-0000-0000-0000-000000000001', 'INCOME_TAX_RETURN', 'Income Tax Return Filing', 'Prepare and file your annual income tax return (ITR-1 to ITR-7) with maximum deductions under old and new regimes.', 1, TRUE),
('s0000001-0000-0000-0000-000000000002', 'c0000001-0000-0000-0000-000000000001', 'INCOME_TAX_RETURN_CORRECTION', 'Income Tax Return Correction', 'Rectify defective returns, file revised returns u/s 139(5), or submit updated returns (ITR-U).', 2, TRUE),
('s0000001-0000-0000-0000-000000000003', 'c0000001-0000-0000-0000-000000000001', 'INCOME_TAX_NOTICE', 'Income Tax Notice Assistance', 'Professional reply drafting and department representation for scrutiny notices u/s 143(2), 148, or demand intimations.', 3, TRUE),
('s0000001-0000-0000-0000-000000000004', 'c0000001-0000-0000-0000-000000000001', 'INCOME_TAX_REFUND', 'Income Tax Refund Assistance', 'Track delayed refunds, resolve bank account validation issues, and file grievance petitions with CPC.', 4, TRUE),
('s0000001-0000-0000-0000-000000000005', 'c0000001-0000-0000-0000-000000000001', 'INCOME_TAX_PLANNING', 'Income Tax Planning', 'Strategic tax optimization and investment planning under Section 80C, 80D, NPS, and capital gains exemptions.', 5, TRUE)
ON CONFLICT (code) DO NOTHING;

-- GST Services
INSERT INTO marketplace_tax_services (id, category_id, code, name, description, sort_order, is_active) VALUES
('s0000001-0000-0000-0000-000000000006', 'c0000001-0000-0000-0000-000000000002', 'GST_REGISTRATION', 'GST Registration', 'New Goods & Services Tax (GSTIN) registration for sole proprietors, LLPs, partnerships, and companies.', 6, TRUE),
('s0000001-0000-0000-0000-000000000007', 'c0000001-0000-0000-0000-000000000002', 'GST_RETURN_FILING', 'GST Return Filing', 'Accurate and on-time monthly and quarterly filings for GSTR-1, GSTR-3B, GSTR-4, and annual GSTR-9.', 7, TRUE),
('s0000001-0000-0000-0000-000000000008', 'c0000001-0000-0000-0000-000000000002', 'GST_RETURN_RECONCILIATION', 'GST Return Reconciliation', 'Automated reconciliation of Purchase Register with GSTR-2B to safeguard 100% eligible Input Tax Credit (ITC).', 8, TRUE),
('s0000001-0000-0000-0000-000000000009', 'c0000001-0000-0000-0000-000000000002', 'GST_NOTICE', 'GST Notice Assistance', 'Expert assistance for GST Show Cause Notices (SCN), DRC-01 mismatches, and departmental audit queries.', 9, TRUE),
('s0000001-0000-0000-0000-000000000010', 'c0000001-0000-0000-0000-000000000002', 'GST_CANCELLATION', 'GST Cancellation', 'Voluntary surrender or formal cancellation of GST registration for discontinued business operations.', 10, TRUE),
('s0000001-0000-0000-0000-000000000011', 'c0000001-0000-0000-0000-000000000002', 'GST_AMENDMENT', 'GST Registration Amendment', 'Modification of core and non-core fields including address change, additional place of business, and partner details.', 11, TRUE)
ON CONFLICT (code) DO NOTHING;

-- Tax Planning Services
INSERT INTO marketplace_tax_services (id, category_id, code, name, description, sort_order, is_active) VALUES
('s0000001-0000-0000-0000-000000000012', 'c0000001-0000-0000-0000-000000000003', 'PERSONAL_TAX_PLANNING', 'Personal Tax Planning', 'Customized direct tax advisory for salaried professionals, doctors, consultants, and NRIs.', 12, TRUE),
('s0000001-0000-0000-0000-000000000013', 'c0000001-0000-0000-0000-000000000003', 'BUSINESS_TAX_PLANNING', 'Business Tax Planning', 'Corporate tax planning, advance tax forecasting, MAT optimization, and startup tax exemptions (80-IAC).', 13, TRUE)
ON CONFLICT (code) DO NOTHING;

-- Business Registration Services
INSERT INTO marketplace_tax_services (id, category_id, code, name, description, sort_order, is_active) VALUES
('s0000001-0000-0000-0000-000000000014', 'c0000001-0000-0000-0000-000000000004', 'BUSINESS_REGISTRATION', 'Business Registration', 'End-to-end incorporation of Private Limited, Limited Liability Partnership (LLP), OPC, or Section 8 entity.', 14, TRUE),
('s0000001-0000-0000-0000-000000000015', 'c0000001-0000-0000-0000-000000000004', 'MSME_REGISTRATION', 'MSME / Udyam Registration', 'Government Udyam registration certificate for MSMEs to unlock priority bank lending and tax subsidies.', 15, TRUE)
ON CONFLICT (code) DO NOTHING;

-- Accounting & Compliance Services
INSERT INTO marketplace_tax_services (id, category_id, code, name, description, sort_order, is_active) VALUES
('s0000001-0000-0000-0000-000000000016', 'c0000001-0000-0000-0000-000000000005', 'BOOKKEEPING', 'Bookkeeping', 'Day-to-day ledger maintenance, bank account reconciliation, accounts receivable/payable tracking.', 16, TRUE),
('s0000001-0000-0000-0000-000000000017', 'c0000001-0000-0000-0000-000000000005', 'ACCOUNTING_SUPPORT', 'Accounting Support', 'Preparation of statutory financial statements, Balance Sheet, Profit & Loss, and audit finalization.', 17, TRUE),
('s0000001-0000-0000-0000-000000000018', 'c0000001-0000-0000-0000-000000000005', 'ANNUAL_COMPLIANCE', 'Annual Compliance & Audit', 'Statutory Tax Audit u/s 44AB, RoC annual filing (AOC-4, MGT-7), and Director KYC compliance.', 18, TRUE)
ON CONFLICT (code) DO NOTHING;

-- Search Aliases
INSERT INTO marketplace_tax_service_aliases (id, tax_service_id, alias, normalized_alias, is_active) VALUES
-- INCOME_TAX_RETURN aliases
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000001', 'ITR', 'itr', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000001', 'ITR Filing', 'itr filing', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000001', 'IT Return', 'it return', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000001', 'Income Tax Return', 'income tax return', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000001', 'Income Tax Filing', 'income tax filing', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000001', 'IT Return Filing', 'it return filing', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000001', 'Tax Return', 'tax return', TRUE),

-- INCOME_TAX_NOTICE aliases
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000003', 'IT Notice', 'it notice', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000003', 'Income Tax Notice', 'income tax notice', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000003', '143 Notice', '143 notice', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000003', '148 Notice', '148 notice', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000003', 'Scrutiny Notice', 'scrutiny notice', TRUE),

-- GST_REGISTRATION aliases
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000006', 'GST Registration', 'gst registration', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000006', 'New GST', 'new gst', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000006', 'GST Apply', 'gst apply', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000006', 'GSTIN Apply', 'gstin apply', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000006', 'GST Number', 'gst number', TRUE),

-- GST_RETURN_FILING aliases
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000007', 'GST Return', 'gst return', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000007', 'GST Filing', 'gst filing', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000007', 'GSTR 1', 'gstr 1', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000007', 'GSTR 3B', 'gstr 3b', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000007', 'GSTR 9', 'gstr 9', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000007', 'GSTR1', 'gstr1', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000007', 'GSTR3B', 'gstr3b', TRUE),

-- GST_NOTICE aliases
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000009', 'GST Notice', 'gst notice', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000009', 'GST SCN', 'gst scn', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000009', 'GST Audit Notice', 'gst audit notice', TRUE),

-- BUSINESS_REGISTRATION aliases
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000014', 'Company Registration', 'company registration', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000014', 'Pvt Ltd Registration', 'pvt ltd registration', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000014', 'LLP Registration', 'llp registration', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000014', 'Incorporation', 'incorporation', TRUE),

-- MSME_REGISTRATION aliases
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000015', 'MSME', 'msme', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000015', 'Udyam', 'udyam', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000015', 'MSME Certificate', 'msme certificate', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000015', 'Udyam Registration', 'udyam registration', TRUE),

-- BOOKKEEPING aliases
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000016', 'Book Keeping', 'book keeping', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000016', 'Accounts Writing', 'accounts writing', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000016', 'Tally', 'tally', TRUE),

-- ANNUAL_COMPLIANCE aliases
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000018', 'Tax Audit', 'tax audit', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000018', '44AB Audit', '44ab audit', TRUE),
(gen_random_uuid(), 's0000001-0000-0000-0000-000000000018', 'Audit', 'audit', TRUE);

-- ==============================================================================
-- 6. DATA MIGRATION: Connect existing practice profiles to default standard services
-- ==============================================================================

-- Assign default standard services (ITR, GST, Audit) to all existing practice profiles
INSERT INTO marketplace_practice_services (id, organization_id, marketplace_profile_id, tax_service_id, is_active)
SELECT 
    gen_random_uuid(),
    mp.organization_id,
    mp.id,
    ts.id,
    TRUE
FROM marketplace_profiles mp
CROSS JOIN marketplace_tax_services ts
WHERE ts.code IN ('INCOME_TAX_RETURN', 'GST_RETURN_FILING', 'GST_REGISTRATION', 'ANNUAL_COMPLIANCE')
ON CONFLICT (marketplace_profile_id, tax_service_id) DO NOTHING;
