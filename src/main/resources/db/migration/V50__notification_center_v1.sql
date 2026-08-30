-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V50)
-- Notification Center V1: Severity, Categories, Entity Routing & Performance Indexes
-- ==============================================================================

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS severity VARCHAR(32) NOT NULL DEFAULT 'INFO',
    ADD COLUMN IF NOT EXISTS category VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS entity_type VARCHAR(64),
    ADD COLUMN IF NOT EXISTS entity_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;

-- Performance indexes for Notification Center queries
CREATE INDEX IF NOT EXISTS idx_notifications_user_read_cat ON notifications(user_id, is_read, category);
CREATE INDEX IF NOT EXISTS idx_notifications_client_read_cat ON notifications(client_id, is_read, category);
CREATE INDEX IF NOT EXISTS idx_notifications_entity ON notifications(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_notifications_severity ON notifications(severity);