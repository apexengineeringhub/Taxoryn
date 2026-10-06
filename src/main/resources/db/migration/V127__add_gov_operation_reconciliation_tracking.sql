-- ============================================================================
-- Taxoryn Phase 27.3: Government Operation Reconciliation & Batch Processing
-- Database Migration: V127__add_gov_operation_reconciliation_tracking.sql
-- Description: Adds reconciliation tracking timestamps and performance indexes
--              for bounded batch reconciliation of government operations.
-- ============================================================================

ALTER TABLE gov_integration_operations
    ADD COLUMN IF NOT EXISTS last_reconciled_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS next_reconciliation_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS reconciliation_attempt_count INT NOT NULL DEFAULT 0;

-- Performance Indexes for Bounded Batch Querying and Tenant Filtering
CREATE INDEX IF NOT EXISTS idx_gov_ops_reconciliation
    ON gov_integration_operations (status, next_reconciliation_at);

CREATE INDEX IF NOT EXISTS idx_gov_ops_tenant_reconcile
    ON gov_integration_operations (organization_id, status, next_reconciliation_at);
