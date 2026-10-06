-- ==============================================================================
-- Taxoryn Platform - Migration V135
-- Phase 29.5: Period & Due-Date Engine
-- ==============================================================================

-- 1. Add Due Date calculation status and metadata columns to compliance_obligations
ALTER TABLE compliance_obligations ADD COLUMN IF NOT EXISTS due_date_calculation_status VARCHAR(50) DEFAULT 'NOT_CONFIGURED';
ALTER TABLE compliance_obligations ADD COLUMN IF NOT EXISTS due_date_explanation TEXT;
ALTER TABLE compliance_obligations ADD COLUMN IF NOT EXISTS due_date_calculated_at TIMESTAMPTZ;
ALTER TABLE compliance_obligations ADD COLUMN IF NOT EXISTS due_date_rule_type VARCHAR(50);

-- 2. Indexes for efficient due-date querying and compliance calendar lookups
CREATE INDEX IF NOT EXISTS idx_compliance_obligations_due_date ON compliance_obligations(organization_id, statutory_due_date);
CREATE INDEX IF NOT EXISTS idx_compliance_obligations_due_status ON compliance_obligations(organization_id, due_date_calculation_status);
CREATE INDEX IF NOT EXISTS idx_compliance_obligations_client_due ON compliance_obligations(organization_id, client_id, statutory_due_date);
