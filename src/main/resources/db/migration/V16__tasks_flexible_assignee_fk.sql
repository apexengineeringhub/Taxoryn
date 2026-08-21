-- ==============================================================================
-- Taxoryn Platform - Tasks Flexible Assignee Assignment Migration (V16)
-- Multi-Tenant Practice Management Modular Monolith
-- ==============================================================================

-- 1. Drop the legacy strict foreign key constraint on tasks.assigned_to referencing users(id)
-- This allows tasks to be assigned flexibly to employees (employees.id) or users (users.id)
-- without throwing database foreign key constraint violations when assigning staff.
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS fk_tasks_assignee;

-- 2. Drop legacy foreign key constraint on notifications.user_id referencing users(id)
-- This ensures notifications triggered for employees without direct user logins
-- can still be recorded and dispatched via Email/SMS without database FK violations.
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS fk_notifications_user;
