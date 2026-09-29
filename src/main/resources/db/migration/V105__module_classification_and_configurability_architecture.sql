-- V105__module_classification_and_configurability_architecture.sql
-- Establishes the 4-Tier Module Classification & Configurability Architecture:
-- 1. CORE (Mandatory, Non-configurable, Core Platform)
-- 2. FOUNDATION (Mandatory, Non-configurable, Practice Operations, Quota/Usage Controlled)
-- 3. BUSINESS (Optional per Org, Configurable, Subscription Controlled)
-- 4. OPTIONAL (Optional per Org, Configurable, Revenue/Add-on Streams)

-- 1. Add metadata columns to product_modules
ALTER TABLE product_modules ADD COLUMN IF NOT EXISTS mandatory BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE product_modules ADD COLUMN IF NOT EXISTS configurable BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE product_modules ADD COLUMN IF NOT EXISTS subscription_controlled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE product_modules ADD COLUMN IF NOT EXISTS usage_controlled BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Update existing rows with accurate 4-tier classification & metadata
-- CORE Modules
UPDATE product_modules
SET category = 'CORE',
    mandatory = TRUE,
    configurable = FALSE,
    subscription_controlled = FALSE,
    usage_controlled = FALSE
WHERE code IN ('NOTIFICATIONS', 'AUDIT');

-- FOUNDATION Modules (Fundamental practice operations, non-configurable, usage controlled)
UPDATE product_modules
SET category = 'FOUNDATION',
    mandatory = TRUE,
    configurable = FALSE,
    subscription_controlled = FALSE,
    usage_controlled = TRUE
WHERE code IN ('CLIENTS', 'TASKS', 'DOCUMENTS');

UPDATE product_modules
SET category = 'FOUNDATION',
    mandatory = TRUE,
    configurable = FALSE,
    subscription_controlled = FALSE,
    usage_controlled = FALSE
WHERE code IN ('DOCUMENT_REQUESTS', 'BILLING', 'REPORTS', 'DASHBOARD', 'PRACTICE_DASHBOARD');

-- BUSINESS Modules (Specialized tax compliance, configurable per organization, subscription controlled)
UPDATE product_modules
SET category = 'BUSINESS',
    mandatory = FALSE,
    configurable = TRUE,
    subscription_controlled = TRUE,
    usage_controlled = FALSE
WHERE code IN ('GST', 'GST_COMPLIANCE', 'ITR', 'ITR_COMPLIANCE', 'TDS', 'TDS_COMPLIANCE', 'TAX_NOTICES', 'TAX_NOTICE_MANAGEMENT');

-- OPTIONAL Modules (Extensions, Client Portal, Marketplace, Gmail, Self ITR)
UPDATE product_modules
SET category = 'OPTIONAL',
    mandatory = FALSE,
    configurable = TRUE,
    subscription_controlled = TRUE,
    usage_controlled = FALSE
WHERE code IN ('CLIENT_PORTAL', 'MARKETPLACE');

-- 3. Ensure Gmail and Self ITR catalog entries exist
INSERT INTO product_modules (id, code, name, description, category, status, enabled_by_default, display_order, mandatory, configurable, subscription_controlled, usage_controlled, version, created_at, updated_at)
VALUES
    ('a0000000-0000-0000-0000-000000000015', 'GMAIL', 'Gmail & Email Integration', 'Seamless Gmail OAuth integration for synchronizing client correspondence.', 'OPTIONAL', 'ACTIVE', TRUE, 15, FALSE, TRUE, TRUE, FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000016', 'SELF_ITR', 'Self ITR Filing', 'Direct taxpayer self-filing interface and calculation engine.', 'OPTIONAL', 'ACTIVE', FALSE, 16, FALSE, TRUE, TRUE, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO UPDATE
SET category = EXCLUDED.category,
    mandatory = EXCLUDED.mandatory,
    configurable = EXCLUDED.configurable,
    subscription_controlled = EXCLUDED.subscription_controlled,
    usage_controlled = EXCLUDED.usage_controlled,
    updated_at = CURRENT_TIMESTAMP;

-- 4. Clean up any stale disabled records for CORE/FOUNDATION in organization_modules
-- Foundation and Core modules can never be disabled
DELETE FROM organization_modules
WHERE module_code IN ('CLIENTS', 'TASKS', 'DOCUMENTS', 'DOCUMENT_REQUESTS', 'BILLING', 'REPORTS', 'NOTIFICATIONS', 'AUDIT', 'DASHBOARD', 'PRACTICE_DASHBOARD')
  AND enabled = FALSE;
