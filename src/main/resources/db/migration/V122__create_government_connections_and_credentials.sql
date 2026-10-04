-- ============================================================================
-- Taxoryn Phase 22.2: Government Connection & Secure Credential Foundation
-- Database Migration: V122__create_government_connections_and_credentials.sql
-- Description: Tenant-scoped government connection management and encrypted
--              credential reference storage.
-- ============================================================================

-- 1. Government Connections Table
CREATE TABLE IF NOT EXISTS gov_connections (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    environment VARCHAR(50) NOT NULL DEFAULT 'PRODUCTION',
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    credential_reference_id UUID,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for gov_connections
CREATE INDEX IF NOT EXISTS idx_gov_conn_tenant_provider
    ON gov_connections (organization_id, provider_type);

CREATE INDEX IF NOT EXISTS idx_gov_conn_tenant_status
    ON gov_connections (organization_id, status);

CREATE INDEX IF NOT EXISTS idx_gov_conn_tenant_cred
    ON gov_connections (organization_id, credential_reference_id);

CREATE INDEX IF NOT EXISTS idx_gov_conn_created_at
    ON gov_connections (created_at);

-- 2. Government Credential References Table
CREATE TABLE IF NOT EXISTS gov_credential_references (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    connection_id UUID NOT NULL,
    credential_type VARCHAR(50) NOT NULL,
    credential_status VARCHAR(50) NOT NULL DEFAULT 'VALID',
    masked_identifier VARCHAR(255),
    encrypted_secret TEXT NOT NULL,
    secret_storage_provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL_AES_GCM',
    last_validated_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for gov_credential_references
CREATE INDEX IF NOT EXISTS idx_gov_cred_tenant_conn
    ON gov_credential_references (organization_id, connection_id);

CREATE INDEX IF NOT EXISTS idx_gov_cred_tenant_status
    ON gov_credential_references (organization_id, credential_status);

CREATE INDEX IF NOT EXISTS idx_gov_cred_created_at
    ON gov_credential_references (created_at);
