-- ==============================================================================
-- Taxoryn Platform - Migration V81
-- Align workflow audit columns with AuditableEntity
--
-- V77 originally created created_by / updated_by as UUID.
-- AuditableEntity defines both fields as String/VARCHAR(255).
-- ==============================================================================

-- 1. Client Service Periods
ALTER TABLE client_service_periods
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::TEXT,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::TEXT;

-- 2. Service Workflow Templates
ALTER TABLE service_workflow_templates
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::TEXT,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::TEXT;

-- 3. Client Service Workflows
ALTER TABLE client_service_workflows
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::TEXT,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::TEXT;

-- 4. Client Service Workflow Steps
ALTER TABLE client_service_workflow_steps
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::TEXT,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::TEXT;