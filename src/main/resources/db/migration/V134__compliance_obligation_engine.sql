-- ==============================================================================
-- Taxoryn Platform - Migration V134
-- Phase 29.4: Compliance Obligation Engine
-- ==============================================================================

-- 1. Add Phase 29.4 columns to compliance_obligations table
ALTER TABLE compliance_obligations
    ADD COLUMN IF NOT EXISTS rule_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS rule_version INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS rule_name_snapshot VARCHAR(255),
    ADD COLUMN IF NOT EXISTS domain VARCHAR(50) DEFAULT 'OTHER',
    ADD COLUMN IF NOT EXISTS period_type VARCHAR(50) DEFAULT 'MONTH',
    ADD COLUMN IF NOT EXISTS period_key VARCHAR(50),
    ADD COLUMN IF NOT EXISTS applicability_state VARCHAR(50) DEFAULT 'APPLICABLE',
    ADD COLUMN IF NOT EXISTS applicability_reason TEXT,
    ADD COLUMN IF NOT EXISTS generated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS cancellation_reason TEXT;

-- 2. Relax legacy NOT NULL constraints so statutory_due_date / due_date can be null prior to Phase 29.5
ALTER TABLE compliance_obligations ALTER COLUMN statutory_due_date DROP NOT NULL;
ALTER TABLE compliance_obligations ALTER COLUMN due_date DROP NOT NULL;
ALTER TABLE compliance_obligations ALTER COLUMN title DROP NOT NULL;

-- 3. Unique identity index for Phase 29.4 obligations ensuring idempotency and concurrency safety
CREATE UNIQUE INDEX IF NOT EXISTS uq_compliance_obligations_identity 
    ON compliance_obligations (organization_id, client_id, rule_code, rule_version, period_type, period_key)
    WHERE rule_code IS NOT NULL AND period_key IS NOT NULL;

-- 4. Optimized performance indexes for tenant-scoped obligation queries
CREATE INDEX IF NOT EXISTS idx_compliance_obligations_client ON compliance_obligations(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_compliance_obligations_status ON compliance_obligations(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_compliance_obligations_period_key ON compliance_obligations(organization_id, period_key);
CREATE INDEX IF NOT EXISTS idx_compliance_obligations_rule_code ON compliance_obligations(organization_id, rule_code);
CREATE INDEX IF NOT EXISTS idx_compliance_obligations_client_period ON compliance_obligations(organization_id, client_id, period_type, period_key);
