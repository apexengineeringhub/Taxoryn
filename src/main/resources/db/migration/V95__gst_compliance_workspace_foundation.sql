-- =========================================================================
-- Flyway Migration V95: GST Compliance Workspace Foundation
--
-- Phase 16 introduces:
-- 1. gst_registrations table for multi-GSTIN tracking per client
-- 2. Reverse linkage columns on compliance_obligations and compliance_workflows
-- 3. Tenant-scoped uniqueness and performance indexes
-- =========================================================================

-- 1. Create gst_registrations table
CREATE TABLE IF NOT EXISTS gst_registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    location_id UUID REFERENCES locations(id) ON DELETE SET NULL,
    gstin VARCHAR(15) NOT NULL,
    legal_name VARCHAR(255),
    trade_name VARCHAR(255),
    registration_type VARCHAR(50) NOT NULL DEFAULT 'REGULAR',
    registration_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    registration_date DATE,
    state_code VARCHAR(10),
    jurisdiction VARCHAR(255),
    filing_frequency VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    effective_from DATE,
    effective_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT uq_gst_registrations_org_gstin UNIQUE (organization_id, gstin)
);

CREATE INDEX IF NOT EXISTS idx_gst_registrations_org_client ON gst_registrations(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_gst_registrations_org_loc ON gst_registrations(organization_id, location_id);
CREATE INDEX IF NOT EXISTS idx_gst_registrations_org_status ON gst_registrations(organization_id, registration_status);
CREATE INDEX IF NOT EXISTS idx_gst_registrations_gstin ON gst_registrations(organization_id, gstin);

-- 2. Add gst_registration_id to compliance_obligations
ALTER TABLE compliance_obligations ADD COLUMN IF NOT EXISTS gst_registration_id UUID;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_obligations_gst_reg'
    ) THEN
        ALTER TABLE compliance_obligations
            ADD CONSTRAINT fk_compliance_obligations_gst_reg
            FOREIGN KEY (gst_registration_id) REFERENCES gst_registrations(id) ON DELETE SET NULL;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_compliance_obligations_gst_reg ON compliance_obligations(organization_id, gst_registration_id);

-- 3. Add gst_registration_id to compliance_workflows
ALTER TABLE compliance_workflows ADD COLUMN IF NOT EXISTS gst_registration_id UUID;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_workflows_gst_reg'
    ) THEN
        ALTER TABLE compliance_workflows
            ADD CONSTRAINT fk_compliance_workflows_gst_reg
            FOREIGN KEY (gst_registration_id) REFERENCES gst_registrations(id) ON DELETE SET NULL;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_compliance_workflows_gst_reg ON compliance_workflows(organization_id, gst_registration_id);
