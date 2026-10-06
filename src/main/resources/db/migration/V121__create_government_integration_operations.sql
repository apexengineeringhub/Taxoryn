-- ============================================================================
-- Taxoryn Phase 22.1: Government Integration Core Foundation
-- Database Migration: V121__create_government_integration_operations.sql
-- Description: Core operation tracking table and performance indexes for
--              tenant-scoped government integration lifecycle management.
-- ============================================================================

CREATE TABLE IF NOT EXISTS gov_integration_operations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    operation_type VARCHAR(100) NOT NULL,
    business_entity_type VARCHAR(100),
    business_entity_id UUID,
    correlation_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    error_code VARCHAR(50),
    error_message VARCHAR(1000),
    provider_reference_id VARCHAR(255),
    request_metadata TEXT,
    response_metadata TEXT,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Performance and Tenant Isolation Indexes
CREATE INDEX IF NOT EXISTS idx_gov_ops_tenant_idemp
    ON gov_integration_operations (organization_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_gov_ops_tenant_corr
    ON gov_integration_operations (organization_id, correlation_id);

CREATE INDEX IF NOT EXISTS idx_gov_ops_tenant_status
    ON gov_integration_operations (organization_id, status);

CREATE INDEX IF NOT EXISTS idx_gov_ops_tenant_entity
    ON gov_integration_operations (organization_id, business_entity_type, business_entity_id);

CREATE INDEX IF NOT EXISTS idx_gov_ops_created_at
    ON gov_integration_operations (created_at);
