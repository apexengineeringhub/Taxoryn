-- ==============================================================================
-- TAXORYN — STAGE 2.5 — ENGAGEMENT ↔ WORK INSTANCE ↔ TASK RELATIONSHIP HARDENING
-- Migration: V119__engagement_work_instance_task_relationship_hardening.sql
-- ==============================================================================

-- 1. Create performance index on tasks (organization_id, work_instance_id) if not present
CREATE INDEX IF NOT EXISTS idx_tasks_org_work_instance 
    ON tasks (organization_id, work_instance_id);

-- 2. Create composite index on tasks (organization_id, engagement_id, status) for rapid Engagement 360 task queries
CREATE INDEX IF NOT EXISTS idx_tasks_org_engagement_status 
    ON tasks (organization_id, engagement_id, status);

-- 3. Data Consistency & Deterministic Backfill:
-- Ensure all tasks instantiated from a WorkInstance authoritatively possess the engagement_id of the parent WorkInstance
UPDATE tasks t
SET engagement_id = wi.engagement_id
FROM work_instances wi
WHERE t.work_instance_id = wi.id
  AND t.organization_id = wi.organization_id
  AND (t.engagement_id IS NULL OR t.engagement_id != wi.engagement_id);
