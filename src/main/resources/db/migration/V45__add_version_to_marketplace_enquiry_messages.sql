-- ==============================================================================
-- Taxoryn Platform - Flyway Migration V45
-- Add version column to marketplace_enquiry_messages for optimistic locking
-- ==============================================================================

ALTER TABLE marketplace_enquiry_messages
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
