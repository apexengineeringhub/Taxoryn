-- ============================================================================
-- Taxoryn Phase 22.3: Provider Handshake, Health Polling & Dynamic Adapter Routing
-- Database Migration: V123__add_government_connection_health_tracking.sql
-- Description: Adds provider-neutral health tracking columns and performance
--              indexes to gov_connections table.
-- ============================================================================

ALTER TABLE gov_connections
    ADD COLUMN IF NOT EXISTS health_status VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN IF NOT EXISTS last_health_check_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS health_message VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS health_latency_ms BIGINT;

CREATE INDEX IF NOT EXISTS idx_gov_conn_tenant_health
    ON gov_connections (organization_id, health_status);
