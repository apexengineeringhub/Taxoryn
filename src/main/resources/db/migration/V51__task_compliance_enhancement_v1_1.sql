-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V51)
-- Task & Compliance Enhancement V1.1: Unified Worklist & Cross-Entity Linkages
-- ==============================================================================

-- 1. Enhance tasks table with compliance, document request links and blocked metadata
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS compliance_id UUID;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS document_request_id UUID;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS blocked_reason TEXT;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;

-- Add foreign key constraints safely
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_compliance'
    ) THEN
        ALTER TABLE tasks
            ADD CONSTRAINT fk_tasks_compliance
            FOREIGN KEY (compliance_id) REFERENCES compliance_obligations(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_doc_request'
    ) THEN
        ALTER TABLE tasks
            ADD CONSTRAINT fk_tasks_doc_request
            FOREIGN KEY (document_request_id) REFERENCES document_requests(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Performance indexes on tasks
CREATE INDEX IF NOT EXISTS idx_tasks_compliance ON tasks(compliance_id);
CREATE INDEX IF NOT EXISTS idx_tasks_doc_request ON tasks(document_request_id);
CREATE INDEX IF NOT EXISTS idx_tasks_due_status ON tasks(organization_id, due_date, status);
CREATE INDEX IF NOT EXISTS idx_tasks_assignee_status ON tasks(organization_id, assigned_to, status);

-- 2. Enhance document_requests table with task and compliance linkages
ALTER TABLE document_requests ADD COLUMN IF NOT EXISTS task_id UUID;
ALTER TABLE document_requests ADD COLUMN IF NOT EXISTS compliance_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_doc_requests_task'
    ) THEN
        ALTER TABLE document_requests
            ADD CONSTRAINT fk_doc_requests_task
            FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_doc_requests_compliance'
    ) THEN
        ALTER TABLE document_requests
            ADD CONSTRAINT fk_doc_requests_compliance
            FOREIGN KEY (compliance_id) REFERENCES compliance_obligations(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_doc_requests_task ON document_requests(task_id);
CREATE INDEX IF NOT EXISTS idx_doc_requests_compliance ON document_requests(compliance_id);
