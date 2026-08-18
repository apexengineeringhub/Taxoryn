-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V4)
-- Employee Management Enhancements (Contact, Name, Manager & Hierarchy)
-- ==============================================================================

-- 1. Add Employee Details Columns
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS manager_id UUID;

-- 2. Add Foreign Key for Manager Hierarchy
ALTER TABLE employees
    ADD CONSTRAINT fk_employees_manager FOREIGN KEY (manager_id) REFERENCES employees(id) ON DELETE SET NULL;

-- 3. Add Unique Constraint on Tenant Email
ALTER TABLE employees
    ADD CONSTRAINT uq_employees_org_email UNIQUE (organization_id, email);

-- 4. Create Performance Indexes
CREATE INDEX IF NOT EXISTS idx_employees_email ON employees(email);
CREATE INDEX IF NOT EXISTS idx_employees_department ON employees(department);
CREATE INDEX IF NOT EXISTS idx_employees_status ON employees(status);
CREATE INDEX IF NOT EXISTS idx_employees_manager_id ON employees(manager_id);
