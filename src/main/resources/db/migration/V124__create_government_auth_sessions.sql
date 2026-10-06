-- ============================================================================
-- Taxoryn Phase 26.1: Government Authentication Foundation
-- Database Migration: V124__create_government_auth_sessions.sql
-- Description: Tenant-scoped government authentication session tracking.
-- ============================================================================

CREATE TABLE IF NOT EXISTS gov_auth_sessions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    connection_id UUID NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    auth_method VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AUTHENTICATION_STARTED',
    expires_at TIMESTAMP WITH TIME ZONE,
    authenticated_at TIMESTAMP WITH TIME ZONE,
    last_activity_at TIMESTAMP WITH TIME ZONE,
    failure_code VARCHAR(100),
    safe_failure_message VARCHAR(1000),
    correlation_id VARCHAR(255),
    requires_user_action BOOLEAN NOT NULL DEFAULT FALSE,
    action_prompt VARCHAR(1000),
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for gov_auth_sessions
CREATE INDEX IF NOT EXISTS idx_gov_auth_sess_tenant_conn
    ON gov_auth_sessions (organization_id, connection_id);

CREATE INDEX IF NOT EXISTS idx_gov_auth_sess_tenant_status
    ON gov_auth_sessions (organization_id, status);

CREATE INDEX IF NOT EXISTS idx_gov_auth_sess_expires_at
    ON gov_auth_sessions (expires_at);

CREATE INDEX IF NOT EXISTS idx_gov_auth_sess_created_at
    ON gov_auth_sessions (created_at);
