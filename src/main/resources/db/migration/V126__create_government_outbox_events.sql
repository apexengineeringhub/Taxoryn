-- ============================================================================
-- Taxoryn Phase 27.2: Government Outbox & Background Processing Foundation
-- Database Migration: V126__create_government_outbox_events.sql
-- Description: Transactional outbox event tracking and performance indexes for
--              durable asynchronous government integration background processing.
-- ============================================================================

CREATE TABLE IF NOT EXISTS gov_outbox_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100),
    aggregate_id UUID,
    operation_id UUID,
    provider_type VARCHAR(50),
    correlation_id VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payload TEXT,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    available_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at TIMESTAMP WITH TIME ZONE,
    processed_at TIMESTAMP WITH TIME ZONE,
    last_error_code VARCHAR(50),
    last_error_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Performance, Concurrency Claiming, and Tenant Isolation Indexes
CREATE INDEX IF NOT EXISTS idx_gov_outbox_tenant_status
    ON gov_outbox_events (organization_id, status, available_at);

CREATE INDEX IF NOT EXISTS idx_gov_outbox_status_avail
    ON gov_outbox_events (status, available_at);

CREATE INDEX IF NOT EXISTS idx_gov_outbox_tenant_corr
    ON gov_outbox_events (organization_id, correlation_id);

CREATE INDEX IF NOT EXISTS idx_gov_outbox_tenant_op
    ON gov_outbox_events (organization_id, operation_id);

CREATE INDEX IF NOT EXISTS idx_gov_outbox_stale_lock
    ON gov_outbox_events (status, locked_at);
