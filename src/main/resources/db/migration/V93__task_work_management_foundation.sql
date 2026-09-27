-- ==============================================================================
-- Taxoryn Platform - Phase 14 Migration (V93)
-- Task & Work Management Foundation
-- ==============================================================================

-- 1. Create Generic Work Items Table
CREATE TABLE IF NOT EXISTS work_items (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    location_id UUID REFERENCES locations(id) ON DELETE SET NULL,
    client_id UUID REFERENCES clients(id) ON DELETE CASCADE,
    client_service_id UUID REFERENCES client_services(id) ON DELETE SET NULL,
    workflow_id UUID REFERENCES compliance_workflows(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    assigned_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    due_date DATE,
    completed_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for Work Items
CREATE INDEX IF NOT EXISTS idx_work_items_org ON work_items(organization_id);
CREATE INDEX IF NOT EXISTS idx_work_items_org_client ON work_items(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_work_items_org_location ON work_items(organization_id, location_id);
CREATE INDEX IF NOT EXISTS idx_work_items_org_service ON work_items(organization_id, client_service_id);
CREATE INDEX IF NOT EXISTS idx_work_items_org_workflow ON work_items(organization_id, workflow_id);
CREATE INDEX IF NOT EXISTS idx_work_items_org_status ON work_items(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_work_items_org_assignee ON work_items(organization_id, assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_work_items_org_due_date ON work_items(organization_id, due_date);

-- 2. Enhance Tasks Table with Work Item and Location Hierarchy
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS work_item_id UUID,
    ADD COLUMN IF NOT EXISTS location_id UUID,
    ADD COLUMN IF NOT EXISTS notes TEXT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_work_item'
    ) THEN
        ALTER TABLE tasks
            ADD CONSTRAINT fk_tasks_work_item
            FOREIGN KEY (work_item_id) REFERENCES work_items(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_location'
    ) THEN
        ALTER TABLE tasks
            ADD CONSTRAINT fk_tasks_location
            FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_tasks_work_item_id ON tasks(work_item_id);
CREATE INDEX IF NOT EXISTS idx_tasks_location_id ON tasks(location_id);
CREATE INDEX IF NOT EXISTS idx_tasks_org_location ON tasks(organization_id, location_id);
