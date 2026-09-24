-- ==============================================================================
-- Taxoryn Platform - Phase 15 Migration (V79)
-- Compliance Execution Workflow & Practitioner Workbench
-- ==============================================================================

-- 1. Create Compliance Execution Workflows Table
CREATE TABLE IF NOT EXISTS compliance_workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    client_service_id UUID REFERENCES client_services(id) ON DELETE SET NULL,
    compliance_obligation_id UUID NOT NULL REFERENCES compliance_obligations(id) ON DELETE CASCADE,
    workflow_status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    assigned_employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    reviewer_employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    target_date DATE,
    statutory_due_date DATE,
    
    -- Client Action / Waiting State Tracking
    waiting_for_client BOOLEAN NOT NULL DEFAULT FALSE,
    waiting_reason TEXT,
    waiting_requested_at TIMESTAMPTZ,
    waiting_requested_by VARCHAR(255),
    expected_response_date DATE,

    -- Execution & Review Lifecycles
    started_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ,
    reviewed_at TIMESTAMPTZ,
    reviewed_by VARCHAR(255),
    review_notes TEXT,
    changes_requested_reason TEXT,
    approved_at TIMESTAMPTZ,
    approved_by VARCHAR(255),

    -- Government Filing Execution Metadata (Pre-integration tracking)
    filed_date DATE,
    filed_at TIMESTAMPTZ,
    filed_by VARCHAR(255),
    acknowledgement_number VARCHAR(100),

    -- Completion & Cancellation Details
    completed_at TIMESTAMPTZ,
    completed_by VARCHAR(255),
    cancelled_at TIMESTAMPTZ,
    cancelled_by VARCHAR(255),
    cancellation_reason TEXT,
    notes TEXT,

    -- Optimistic Locking & Audit
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    CONSTRAINT uq_compliance_workflow_obligation UNIQUE (organization_id, compliance_obligation_id)
);

-- Indexes for fast workbench filtering and multi-tenant performance
CREATE INDEX IF NOT EXISTS idx_cw_org_status ON compliance_workflows(organization_id, workflow_status);
CREATE INDEX IF NOT EXISTS idx_cw_org_client ON compliance_workflows(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_cw_org_assignee ON compliance_workflows(organization_id, assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_cw_org_reviewer ON compliance_workflows(organization_id, reviewer_employee_id);
CREATE INDEX IF NOT EXISTS idx_cw_org_target_date ON compliance_workflows(organization_id, target_date);
CREATE INDEX IF NOT EXISTS idx_cw_org_statutory_due ON compliance_workflows(organization_id, statutory_due_date);
CREATE INDEX IF NOT EXISTS idx_cw_obligation ON compliance_workflows(compliance_obligation_id);


-- 2. Create Compliance Workflow Checklist Items Table
CREATE TABLE IF NOT EXISTS compliance_workflow_checklist_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    workflow_id UUID NOT NULL REFERENCES compliance_workflows(id) ON DELETE CASCADE,
    item_key VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    sequence_order INT NOT NULL DEFAULT 1,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    completed_at TIMESTAMPTZ,
    completed_by_user_id UUID,
    completed_by_name VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_cw_checklist_item UNIQUE (organization_id, workflow_id, item_key)
);

CREATE INDEX IF NOT EXISTS idx_cw_checklist_workflow ON compliance_workflow_checklist_items(workflow_id);
CREATE INDEX IF NOT EXISTS idx_cw_checklist_org ON compliance_workflow_checklist_items(organization_id);
