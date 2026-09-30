-- ==============================================================================
-- Taxoryn Platform - Stage 2.5 Migration (V109)
-- Document Management Foundation: Engagement & Work Instance Linkages
-- ==============================================================================

-- 1. Enhance Documents Table with Engagement and Work Instance References
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS engagement_id UUID,
    ADD COLUMN IF NOT EXISTS work_instance_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_documents_engagement'
    ) THEN
        ALTER TABLE documents
            ADD CONSTRAINT fk_documents_engagement
            FOREIGN KEY (engagement_id) REFERENCES engagements(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_documents_work_instance'
    ) THEN
        ALTER TABLE documents
            ADD CONSTRAINT fk_documents_work_instance
            FOREIGN KEY (work_instance_id) REFERENCES work_instances(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 2. Create Multi-Tenant Performance Indexes
CREATE INDEX IF NOT EXISTS idx_documents_engagement_id ON documents(engagement_id);
CREATE INDEX IF NOT EXISTS idx_documents_work_instance_id ON documents(work_instance_id);
CREATE INDEX IF NOT EXISTS idx_documents_org_engagement ON documents(organization_id, engagement_id);
CREATE INDEX IF NOT EXISTS idx_documents_org_work_instance ON documents(organization_id, work_instance_id);

-- ============================================================
-- Fix: work_template_tasks missing created_by
-- ============================================================

ALTER TABLE work_template_tasks
    ADD COLUMN IF NOT EXISTS created_by BIGINT;

ALTER TABLE work_template_tasks
     ALTER COLUMN created_by TYPE VARCHAR(255);

 -- Fix missing audit column on work_template_tasks
ALTER TABLE work_template_tasks
   ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);