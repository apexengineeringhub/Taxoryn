-- ==============================================================================
-- TAXORYN — STAGE 2.5 — P0.3
-- TASK / CALENDAR / WORK MANAGEMENT FOUNDATION
-- Migration: V108__task_work_management_foundation.sql
-- ==============================================================================

-- 1. Enhance existing tasks table with direct engagement linkage and execution attributes
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS engagement_id UUID,
    ADD COLUMN IF NOT EXISTS start_date DATE,
    ADD COLUMN IF NOT EXISTS completed_by UUID,
    ADD COLUMN IF NOT EXISTS estimated_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS actual_minutes INTEGER;

-- 2. Add Foreign Key for Engagement
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_engagement'
    ) THEN
        ALTER TABLE tasks
            ADD CONSTRAINT fk_tasks_engagement
            FOREIGN KEY (engagement_id) REFERENCES engagements(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 3. Performance Indexes for Work Management, Overdue, Calendar Range, and Assignment Queries
CREATE INDEX IF NOT EXISTS idx_tasks_org_engagement ON tasks (organization_id, engagement_id);
CREATE INDEX IF NOT EXISTS idx_tasks_org_assigned_due ON tasks (organization_id, assigned_to, due_date);
CREATE INDEX IF NOT EXISTS idx_tasks_org_status_due ON tasks (organization_id, status, due_date);
CREATE INDEX IF NOT EXISTS idx_tasks_org_start_date ON tasks (organization_id, start_date);
CREATE INDEX IF NOT EXISTS idx_tasks_org_due_date ON tasks (organization_id, due_date);
