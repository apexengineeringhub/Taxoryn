-- ==============================================================================
-- Taxoryn Platform - Phase 31.4 Migration (V143)
-- Compliance Quality Control Authorization History & Immutable Provenance Records
-- ==============================================================================

-- 1. Create Compliance Quality Control Authorization Records Table
CREATE TABLE IF NOT EXISTS compliance_qc_authorization_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    workflow_id UUID NOT NULL REFERENCES compliance_workflows(id) ON DELETE CASCADE,
    action VARCHAR(30) NOT NULL,
    authorization_method VARCHAR(50),
    authorization_reference VARCHAR(255),
    authorization_notes TEXT,
    authorized_by VARCHAR(255),
    revocation_reason TEXT,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_email VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 2. Indexes for Authorization Records
CREATE INDEX IF NOT EXISTS idx_compliance_qc_auth_workflow ON compliance_qc_authorization_records(organization_id, workflow_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_compliance_qc_auth_action ON compliance_qc_authorization_records(organization_id, workflow_id, action);
