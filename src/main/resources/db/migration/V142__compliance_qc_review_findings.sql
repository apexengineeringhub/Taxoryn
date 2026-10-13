-- ==============================================================================
-- Taxoryn Platform - Phase 31.3 Migration (V142)
-- Compliance Quality Control Review Findings & Correction Workflow
-- ==============================================================================

-- 1. Create Compliance Quality Control Review Findings Table
CREATE TABLE IF NOT EXISTS compliance_qc_review_findings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    workflow_id UUID NOT NULL REFERENCES compliance_workflows(id) ON DELETE CASCADE,
    review_cycle INT NOT NULL DEFAULT 1,
    category VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'BLOCKING',
    description TEXT NOT NULL,
    corrective_guidance TEXT,
    source_reference VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    preparer_response TEXT,
    responded_at TIMESTAMPTZ,
    responded_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    resolution_decision VARCHAR(30),
    resolved_at TIMESTAMPTZ,
    resolved_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewer_notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 2. Indexes for Review Findings
CREATE INDEX IF NOT EXISTS idx_qc_findings_workflow ON compliance_qc_review_findings(organization_id, workflow_id);
CREATE INDEX IF NOT EXISTS idx_qc_findings_status ON compliance_qc_review_findings(organization_id, workflow_id, status);
CREATE INDEX IF NOT EXISTS idx_qc_findings_cycle ON compliance_qc_review_findings(organization_id, workflow_id, review_cycle);

-- 3. Add active review cycle and validation freshness tracking to compliance_workflows
ALTER TABLE compliance_workflows
    ADD COLUMN IF NOT EXISTS review_cycle INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS last_validation_fingerprint VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_validated_at TIMESTAMPTZ;
