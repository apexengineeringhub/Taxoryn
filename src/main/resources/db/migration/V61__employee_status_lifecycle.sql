-- ==============================================================================
-- Taxoryn Platform - Migration V61
-- Employee Status Lifecycle Support (INVITED, ACTIVE, SUSPENDED, INACTIVE)
-- ==============================================================================

-- 1. Drop any legacy CHECK constraints on employees(status)
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT conname 
        FROM pg_constraint c 
        JOIN pg_class cl ON cl.oid = c.conrelid 
        WHERE c.contype = 'c' 
          AND cl.relname = 'employees'
          AND conname LIKE '%status%'
    ) LOOP
        EXECUTE format('ALTER TABLE employees DROP CONSTRAINT IF EXISTS %I;', r.conname);
    END LOOP;
END $$;

ALTER TABLE employees DROP CONSTRAINT IF EXISTS employees_status_check;

-- 2. Add composite index on (organization_id, status) for filtered directory queries
CREATE INDEX IF NOT EXISTS idx_employees_org_status ON employees(organization_id, status);
