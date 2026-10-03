-- ==============================================================================
-- Taxoryn Platform - Stage 2.5 Migration (V116)
-- P0.5: Reminder & Automation Foundation Hardening
-- Adds retry tracking, error recording, and parent lineage for recurring reminders
-- ==============================================================================

ALTER TABLE reminders
    ADD COLUMN IF NOT EXISTS notification_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_error TEXT,
    ADD COLUMN IF NOT EXISTS parent_reminder_id UUID;

-- Bounded index for scheduler retry and due query
CREATE INDEX IF NOT EXISTS idx_reminders_pending_retry
    ON reminders(organization_id, status, scheduled_at, notification_attempts)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_reminders_parent
    ON reminders(parent_reminder_id);
