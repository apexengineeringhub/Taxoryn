-- ==============================================================================
-- Taxoryn Platform - Stage 2.5 Migration (V146)
-- Phase 32.6: Follow-up & Reminder Engine Foundation & Optimization
-- Composite indexes for follow-up queries, client-scoped reminders, and reference lookups
-- ==============================================================================

-- 1. Index on (organization_id, client_id) for rapid client portal and practice client follow-up lookups
CREATE INDEX IF NOT EXISTS idx_reminders_org_client
    ON reminders(organization_id, client_id);

-- 2. Index on (organization_id, reference_type, reference_id) for rapid source entity deduplication and lookup
CREATE INDEX IF NOT EXISTS idx_reminders_reference
    ON reminders(organization_id, reference_type, reference_id);

-- 3. Composite index on (organization_id, reminder_type, status) for filtered follow-up category queries
CREATE INDEX IF NOT EXISTS idx_reminders_type_status
    ON reminders(organization_id, reminder_type, status);
