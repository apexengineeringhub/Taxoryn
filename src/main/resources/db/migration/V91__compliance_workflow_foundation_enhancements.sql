-- ==============================================================================
-- Taxoryn Platform - Phase 12 Migration (V91)
-- Compliance Workflow Foundation Enhancements
-- ==============================================================================

-- 1. Enhance Compliance Obligations Table with Location and User Scope
ALTER TABLE compliance_obligations
    ADD COLUMN IF NOT EXISTS assigned_user_id UUID,
    ADD COLUMN IF NOT EXISTS location_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_obligations_user'
    ) THEN
        ALTER TABLE compliance_obligations
            ADD CONSTRAINT fk_compliance_obligations_user
            FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_obligations_location'
    ) THEN
        ALTER TABLE compliance_obligations
            ADD CONSTRAINT fk_compliance_obligations_location
            FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_compliance_obligations_user ON compliance_obligations(assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_compliance_obligations_location ON compliance_obligations(location_id);
CREATE INDEX IF NOT EXISTS idx_compliance_obligations_org_loc ON compliance_obligations(organization_id, location_id);


-- 2. Enhance Compliance Execution Workflows Table with Location, User Scope, and Workflow Type
ALTER TABLE compliance_workflows
    ADD COLUMN IF NOT EXISTS assigned_user_id UUID,
    ADD COLUMN IF NOT EXISTS location_id UUID,
    ADD COLUMN IF NOT EXISTS workflow_type VARCHAR(50);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_workflows_user'
    ) THEN
        ALTER TABLE compliance_workflows
            ADD CONSTRAINT fk_compliance_workflows_user
            FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_workflows_location'
    ) THEN
        ALTER TABLE compliance_workflows
            ADD CONSTRAINT fk_compliance_workflows_location
            FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_compliance_workflows_user ON compliance_workflows(assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_compliance_workflows_location ON compliance_workflows(location_id);
CREATE INDEX IF NOT EXISTS idx_compliance_workflows_org_loc ON compliance_workflows(organization_id, location_id);
CREATE INDEX IF NOT EXISTS idx_compliance_workflows_type ON compliance_workflows(workflow_type);
