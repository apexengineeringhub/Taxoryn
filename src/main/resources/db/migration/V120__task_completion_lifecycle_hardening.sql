-- ==============================================================================
-- TAXORYN — STAGE 2.5 — TASK COMPLETION TIMESTAMP & LIFECYCLE HARDENING
-- Migration: V120__task_completion_lifecycle_hardening.sql
-- ==============================================================================

-- 1. Ensure task completion columns exist idempotently
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS completed_by UUID;

-- 2. Indexes for completed tasks reporting, operational metrics & filtering
CREATE INDEX IF NOT EXISTS idx_tasks_org_completed_at ON tasks (organization_id, completed_at);
CREATE INDEX IF NOT EXISTS idx_tasks_org_status_completed_at ON tasks (organization_id, status, completed_at);
CREATE INDEX IF NOT EXISTS idx_tasks_org_completed_by ON tasks (organization_id, completed_by);
