-- ==============================================================================
-- Taxoryn Platform - Gmail Conversation Management Foundation (V102)
-- Architecture Principle: Gmail is the system of record. Taxoryn indexes ONLY
-- metadata (threads, assignments, status, client links, SLA metrics).
-- ==============================================================================

-- 1. Connected Gmail Accounts Table (Practice Shared or Practitioner Personal Mailbox)
CREATE TABLE IF NOT EXISTS gmail_accounts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    user_id UUID,
    email_address VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) NOT NULL DEFAULT 'PRACTICE_SHARED',
    status VARCHAR(50) NOT NULL DEFAULT 'CONNECTED',
    encrypted_access_token TEXT,
    encrypted_refresh_token TEXT,
    token_expires_at TIMESTAMP WITH TIME ZONE,
    last_history_id VARCHAR(100),
    last_synced_at TIMESTAMP WITH TIME ZONE,
    sync_error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_gmail_accounts_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_gmail_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_gmail_accounts_org_email UNIQUE (organization_id, email_address)
);

CREATE INDEX IF NOT EXISTS idx_gmail_acc_org_status ON gmail_accounts(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_gmail_acc_org_user ON gmail_accounts(organization_id, user_id);

-- 2. Gmail Conversations Metadata Table (Indexed headers, status, assignments, SLA)
CREATE TABLE IF NOT EXISTS gmail_conversations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    gmail_account_id UUID NOT NULL,
    thread_id VARCHAR(100) NOT NULL,
    client_id UUID,
    location_id UUID,
    assigned_user_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    subject VARCHAR(500),
    snippet VARCHAR(1000),
    sender_email VARCHAR(255),
    sender_name VARCHAR(255),
    recipient_emails TEXT,
    message_count INT NOT NULL DEFAULT 1,
    last_message_at TIMESTAMP WITH TIME ZONE,
    first_response_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    is_unread BOOLEAN NOT NULL DEFAULT TRUE,
    is_starred BOOLEAN NOT NULL DEFAULT FALSE,
    gmail_labels VARCHAR(500),
    web_link VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_gmail_conv_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_gmail_conv_account FOREIGN KEY (gmail_account_id) REFERENCES gmail_accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_gmail_conv_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE SET NULL,
    CONSTRAINT fk_gmail_conv_location FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL,
    CONSTRAINT fk_gmail_conv_assignee FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_gmail_conversations_org_thread UNIQUE (organization_id, thread_id)
);

CREATE INDEX IF NOT EXISTS idx_gmail_conv_org_status_last_msg ON gmail_conversations(organization_id, status, last_message_at DESC);
CREATE INDEX IF NOT EXISTS idx_gmail_conv_org_assignee_status ON gmail_conversations(organization_id, assigned_user_id, status);
CREATE INDEX IF NOT EXISTS idx_gmail_conv_org_client_status ON gmail_conversations(organization_id, client_id, status);
CREATE INDEX IF NOT EXISTS idx_gmail_conv_org_loc_status ON gmail_conversations(organization_id, location_id, status);
CREATE INDEX IF NOT EXISTS idx_gmail_conv_org_thread ON gmail_conversations(organization_id, thread_id);

-- 3. Gmail Sync History Table (Audit ledger of incremental and full sync runs)
CREATE TABLE IF NOT EXISTS gmail_sync_history (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    gmail_account_id UUID NOT NULL,
    sync_type VARCHAR(50) NOT NULL DEFAULT 'INCREMENTAL',
    threads_synced INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_gmail_sync_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_gmail_sync_account FOREIGN KEY (gmail_account_id) REFERENCES gmail_accounts(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_gmail_sync_org_acc_created ON gmail_sync_history(organization_id, gmail_account_id, created_at DESC);
