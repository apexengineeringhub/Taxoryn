-- V76__compliance_work_items.sql
-- Phase 12: Compliance Workflow Foundation & Compliance Work Item Management

CREATE TABLE IF NOT EXISTS compliance_work_items (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    client_service_id UUID NOT NULL REFERENCES client_services(id) ON DELETE CASCADE,
    work_type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    financial_year VARCHAR(50),
    assessment_year VARCHAR(50),
    compliance_period VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    statutory_due_date DATE,
    internal_target_date DATE,
    assigned_employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    reviewer_employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_comp_work_org_client ON compliance_work_items(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_comp_work_org_service ON compliance_work_items(organization_id, client_service_id);
CREATE INDEX IF NOT EXISTS idx_comp_work_org_status ON compliance_work_items(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_comp_work_org_assignee ON compliance_work_items(organization_id, assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_comp_work_org_reviewer ON compliance_work_items(organization_id, reviewer_employee_id);
CREATE INDEX IF NOT EXISTS idx_comp_work_statutory_due ON compliance_work_items(organization_id, statutory_due_date);
CREATE INDEX IF NOT EXISTS idx_comp_work_internal_target ON compliance_work_items(organization_id, internal_target_date);

-- Add optional foreign key from tasks to compliance_work_items for operational task breakdown
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS compliance_work_item_id UUID REFERENCES compliance_work_items(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_comp_work_item ON tasks(compliance_work_item_id);
