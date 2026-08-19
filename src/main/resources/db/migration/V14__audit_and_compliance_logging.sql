-- ==============================================================================
-- Taxoryn Platform - Phase 15 Migration (V14)
-- Enterprise Audit Logging & Compliance Schema Enhancements
-- ==============================================================================

-- 1. Schema Enhancements on audit_logs table
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS request_id VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS entity_type VARCHAR(100);

-- Backfill entity_type with entity_name if existing records exist
UPDATE audit_logs SET entity_type = entity_name WHERE entity_type IS NULL AND entity_name IS NOT NULL;

-- 2. Performance Indexes for Tenant-Isolated Auditing & Search
CREATE INDEX IF NOT EXISTS idx_audit_logs_org_created ON audit_logs(organization_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_org_entity ON audit_logs(organization_id, entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_org_action ON audit_logs(organization_id, action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_org_user ON audit_logs(organization_id, user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_request_id ON audit_logs(request_id);
