-- ============================================================
-- TAXORYN DATABASE MIGRATION
-- Migration: V129__add_client_lifecycle_status_metadata.sql
-- Description: Adds lifecycle status metadata columns to clients table
-- Phase: 28.3 (Client Lifecycle & Status)
-- ============================================================

ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS status_changed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS status_changed_by UUID,
    ADD COLUMN IF NOT EXISTS status_change_reason VARCHAR(500);

-- Ensure index exists on organization_id and status for fast lifecycle filtering
CREATE INDEX IF NOT EXISTS idx_clients_org_status ON clients(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_clients_status_changed_at ON clients(organization_id, status_changed_at);
