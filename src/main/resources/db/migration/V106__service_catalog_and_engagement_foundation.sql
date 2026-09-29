-- V106__service_catalog_and_engagement_foundation.sql
-- Phase 1 Stage 2.5: Service Catalog & Engagement Foundation

-- 1. Create Services Master Catalog Table
CREATE TABLE IF NOT EXISTS services (
    id UUID PRIMARY KEY,
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    service_code VARCHAR(100) NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    configurable BOOLEAN NOT NULL DEFAULT TRUE,
    module_code VARCHAR(50) REFERENCES product_modules(code) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Unique index allowing distinct service_code per organization (handling NULL as system global service)
CREATE UNIQUE INDEX IF NOT EXISTS uk_services_org_code ON services (COALESCE(organization_id, '00000000-0000-0000-0000-000000000000'::uuid), service_code);

CREATE INDEX IF NOT EXISTS idx_services_org ON services(organization_id);
CREATE INDEX IF NOT EXISTS idx_services_code ON services(service_code);
CREATE INDEX IF NOT EXISTS idx_services_category ON services(category);
CREATE INDEX IF NOT EXISTS idx_services_status ON services(status);

-- 2. Seed Platform System Services
INSERT INTO services (id, organization_id, service_code, service_name, description, category, status, configurable, module_code, created_at, updated_at, version) VALUES
('b0000000-0000-0000-0000-000000000001', NULL, 'GST_COMPLIANCE', 'GST Compliance & Returns', 'Monthly and quarterly GSTR-1, GSTR-3B filings, 2B reconciliations, and annual compliance.', 'GST', 'ACTIVE', TRUE, 'GST', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('b0000000-0000-0000-0000-000000000002', NULL, 'TDS_COMPLIANCE', 'TDS / TCS Compliance', 'Quarterly 24Q, 26Q, 27Q filings, ITNS 281 challans, and TRACES Form 16/16A generation.', 'TDS', 'ACTIVE', TRUE, 'TDS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('b0000000-0000-0000-0000-000000000003', NULL, 'ITR_FILING', 'Income Tax Returns & Computation', 'ITR-1 to ITR-7 computation, advance tax forecasting, capital gains, and e-filing.', 'ITR', 'ACTIVE', TRUE, 'ITR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('b0000000-0000-0000-0000-000000000004', NULL, 'TAX_AUDIT', 'Tax Audit u/s 44AB & Statutory Assurance', 'Form 3CA/3CB/3CD tax audit finalization, turnover analysis, and statutory assurance.', 'AUDIT', 'ACTIVE', TRUE, 'ITR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('b0000000-0000-0000-0000-000000000005', NULL, 'NOTICE_MANAGEMENT', 'Tax Notice & Assessment Representation', 'Scrutiny notices, 148 reassessment, DRC-01 mismatches, reply drafting, and hearings.', 'NOTICE', 'ACTIVE', TRUE, 'TAX_NOTICES', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('b0000000-0000-0000-0000-000000000006', NULL, 'TAX_ADVISORY', 'Tax Planning & Business Advisory', 'Strategic corporate & individual tax advisory, corporate restructuring, and transaction advisory.', 'ADVISORY', 'ACTIVE', TRUE, 'CLIENTS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('b0000000-0000-0000-0000-000000000007', NULL, 'OTHER', 'General Accounting & Compliance', 'Custom professional mandates, secretarial compliance, certifications, and general accounting.', 'OTHER', 'ACTIVE', TRUE, 'CLIENTS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (COALESCE(organization_id, '00000000-0000-0000-0000-000000000000'::uuid), service_code) DO NOTHING;

-- 3. Enhance Engagements Table
ALTER TABLE engagements
    ADD COLUMN IF NOT EXISTS service_id UUID REFERENCES services(id) ON DELETE RESTRICT,
    ADD COLUMN IF NOT EXISTS reviewer_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM';

-- Enforce unique engagement_code per organization
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_engagements_org_code') THEN
        ALTER TABLE engagements ADD CONSTRAINT uk_engagements_org_code UNIQUE (organization_id, engagement_code);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_engagements_service_id ON engagements(service_id);
CREATE INDEX IF NOT EXISTS idx_engagements_reviewer ON engagements(reviewer_user_id);
CREATE INDEX IF NOT EXISTS idx_engagements_priority ON engagements(priority);
