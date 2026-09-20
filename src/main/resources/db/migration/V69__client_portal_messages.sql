-- V69__client_portal_messages.sql
-- Enables secure, auditable, multi-tenant consultation chat between Clients and Practitioners

CREATE TABLE IF NOT EXISTS client_portal_messages (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    sender_type VARCHAR(30) NOT NULL, -- 'CLIENT' or 'PRACTICE'
    sender_user_id UUID REFERENCES users(id),
    sender_name VARCHAR(150) NOT NULL,
    sender_email VARCHAR(255),
    message_body TEXT NOT NULL,
    attachments_json TEXT,
    is_read_by_client BOOLEAN NOT NULL DEFAULT FALSE,
    is_read_by_practice BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_cpm_org_client_created ON client_portal_messages(organization_id, client_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_cpm_client_unread ON client_portal_messages(client_id, is_read_by_client);
CREATE INDEX IF NOT EXISTS idx_cpm_practice_unread ON client_portal_messages(organization_id, client_id, is_read_by_practice);
