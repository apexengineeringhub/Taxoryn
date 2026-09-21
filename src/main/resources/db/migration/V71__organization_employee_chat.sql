-- V71__organization_employee_chat.sql
-- Organization Internal Employee Chat & Channel Communication System with Security Scoping

CREATE TABLE IF NOT EXISTS employee_chat_channels (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    channel_type VARCHAR(30) NOT NULL DEFAULT 'GENERAL', -- 'GENERAL', 'DEPARTMENT'
    department VARCHAR(100),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_ecc_org_type ON employee_chat_channels(organization_id, channel_type);
CREATE INDEX IF NOT EXISTS idx_ecc_org_dept ON employee_chat_channels(organization_id, department);

CREATE TABLE IF NOT EXISTS employee_chat_messages (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    channel_id UUID REFERENCES employee_chat_channels(id) ON DELETE CASCADE,
    sender_employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    recipient_employee_id UUID REFERENCES employees(id) ON DELETE CASCADE,
    message_body TEXT NOT NULL,
    attachments_json TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_ecm_org_direct ON employee_chat_messages(organization_id, sender_employee_id, recipient_employee_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_ecm_org_channel ON employee_chat_messages(organization_id, channel_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_ecm_recipient_unread ON employee_chat_messages(organization_id, recipient_employee_id, is_read);
