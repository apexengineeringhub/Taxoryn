-- ==============================================================================
-- Taxoryn Platform - Phase 19 Migration (V98)
-- Tax Notice Management: Workspace Foundation, Location Scoping, Workflow & Work Item Linkage
-- ==============================================================================

-- 1. Extend tax_notices with location_id and workflow_id
ALTER TABLE tax_notices
    ADD COLUMN IF NOT EXISTS location_id UUID REFERENCES locations(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS workflow_id UUID REFERENCES compliance_workflows(id) ON DELETE SET NULL;

-- 2. Extend work_items with notice_id
ALTER TABLE work_items
    ADD COLUMN IF NOT EXISTS notice_id UUID REFERENCES tax_notices(id) ON DELETE SET NULL;

-- 3. Composite Indexes for Performance and Multi-Tenant Isolation
CREATE INDEX IF NOT EXISTS idx_tax_notices_org_loc ON tax_notices(organization_id, location_id);
CREATE INDEX IF NOT EXISTS idx_tax_notices_org_workflow ON tax_notices(organization_id, workflow_id);
CREATE INDEX IF NOT EXISTS idx_work_items_org_notice ON work_items(organization_id, notice_id);
