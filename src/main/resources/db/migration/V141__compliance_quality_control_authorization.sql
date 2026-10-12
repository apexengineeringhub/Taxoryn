-- ==============================================================================
-- Taxoryn Platform - Phase 31.1 Migration (V141)
-- Compliance Quality Control & Filing Authorization Foundation
-- ==============================================================================

-- 1. Add quality control filing authorization metadata to compliance_workflows
ALTER TABLE compliance_workflows
    ADD COLUMN IF NOT EXISTS is_authorized BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS authorized_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS authorized_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS authorization_method VARCHAR(50),
    ADD COLUMN IF NOT EXISTS authorization_reference VARCHAR(255),
    ADD COLUMN IF NOT EXISTS authorization_notes TEXT;

-- 2. Index for filtering authorized vs pending-authorization workflows
CREATE INDEX IF NOT EXISTS idx_compliance_workflows_auth
    ON compliance_workflows(organization_id, is_authorized);
