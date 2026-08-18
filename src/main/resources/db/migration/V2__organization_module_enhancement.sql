-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V2)
-- Organization Module Enhancement & Settings Foundation
-- ==============================================================================

-- 1. Add Address and Tax Registration Number fields to Organizations
ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS address VARCHAR(500),
    ADD COLUMN IF NOT EXISTS city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS country VARCHAR(100) DEFAULT 'India',
    ADD COLUMN IF NOT EXISTS pincode VARCHAR(20),
    ADD COLUMN IF NOT EXISTS tax_registration_number VARCHAR(50);

-- Index on tax_registration_number
CREATE INDEX IF NOT EXISTS idx_organizations_tax_reg_no ON organizations(tax_registration_number);

-- 2. Organization Settings Table (1-to-1 with Organizations)
CREATE TABLE IF NOT EXISTS organization_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL UNIQUE,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    date_format VARCHAR(30) NOT NULL DEFAULT 'DD/MM/YYYY',
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    financial_year_start_month INT NOT NULL DEFAULT 4,
    enable_email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    enable_sms_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    enable_whatsapp_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    invoice_prefix VARCHAR(20) NOT NULL DEFAULT 'INV',
    auto_reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_org_settings_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_org_settings_org_id ON organization_settings(organization_id);

-- 3. Populate default settings for any existing organizations that lack settings
INSERT INTO organization_settings (organization_id, timezone, date_format, currency, financial_year_start_month)
SELECT id, 'Asia/Kolkata', 'DD/MM/YYYY', 'INR', 4
FROM organizations
WHERE id NOT IN (SELECT organization_id FROM organization_settings)
ON CONFLICT (organization_id) DO NOTHING;
