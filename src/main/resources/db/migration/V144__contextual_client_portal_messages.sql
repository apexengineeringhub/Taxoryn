-- V144__contextual_client_portal_messages.sql
-- Enables contextual client consultation communication linked to document requests, checklist items, workflows, and notices

ALTER TABLE client_portal_messages
    ADD COLUMN IF NOT EXISTS context_type VARCHAR(50) DEFAULT 'GENERAL',
    ADD COLUMN IF NOT EXISTS context_id UUID,
    ADD COLUMN IF NOT EXISTS context_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS parent_message_id UUID;

CREATE INDEX IF NOT EXISTS idx_cpm_org_client_context 
    ON client_portal_messages(organization_id, client_id, context_type, context_id);

CREATE INDEX IF NOT EXISTS idx_cpm_parent_message 
    ON client_portal_messages(parent_message_id);
