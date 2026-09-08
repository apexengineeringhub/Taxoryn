-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V60)
-- Organization-Scoped Employee Numbers & Concurrency-Safe Counter
-- ==============================================================================

-- 1. Create Organization Employee Counter Table
CREATE TABLE IF NOT EXISTS organization_employee_counters (
    organization_id UUID PRIMARY KEY,
    last_number BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_oec_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- 2. Deterministically Re-index Existing Employees Per Organization
WITH numbered_employees AS (
    SELECT id, organization_id,
           ROW_NUMBER() OVER (
               PARTITION BY organization_id
               ORDER BY created_at ASC, id ASC
           ) as row_num
    FROM employees
)
UPDATE employees e
SET employee_code = 'EMP-' || LPAD(ne.row_num::TEXT, 4, '0')
FROM numbered_employees ne
WHERE e.id = ne.id;

-- 3. Seed/Update Organization Employee Counters with current max employee count
INSERT INTO organization_employee_counters (organization_id, last_number, updated_at)
SELECT organization_id, COUNT(*), CURRENT_TIMESTAMP
FROM employees
GROUP BY organization_id
ON CONFLICT (organization_id)
DO UPDATE SET last_number = EXCLUDED.last_number, updated_at = CURRENT_TIMESTAMP;

-- 4. Verify/Ensure Unique Index on (organization_id, employee_code)
CREATE UNIQUE INDEX IF NOT EXISTS uq_employees_org_code_idx ON employees(organization_id, employee_code);
