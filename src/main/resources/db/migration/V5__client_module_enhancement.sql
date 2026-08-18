-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V5)
-- Client Management 360 Enhancements & Communication History
-- ==============================================================================

-- 1. Enhance Clients Table
ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS trade_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS tan VARCHAR(10),
    ADD COLUMN IF NOT EXISTS cin VARCHAR(21),
    ADD COLUMN IF NOT EXISTS date_of_incorporation DATE,
    ADD COLUMN IF NOT EXISTS alt_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS contact_person_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS contact_person_designation VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address_line1 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_line2 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS country VARCHAR(100) DEFAULT 'India',
    ADD COLUMN IF NOT EXISTS pincode VARCHAR(20),
    ADD COLUMN IF NOT EXISTS assigned_employee_id UUID,
    ADD COLUMN IF NOT EXISTS notes TEXT;

-- 2. Foreign Key for Assigned Employee
ALTER TABLE clients
    ADD CONSTRAINT fk_clients_assigned_employee FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL;

-- 3. Performance Indexes for Client Search & Filtering
CREATE INDEX IF NOT EXISTS idx_clients_tan ON clients(tan);
CREATE INDEX IF NOT EXISTS idx_clients_assigned_emp ON clients(assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_clients_type ON clients(client_type);
CREATE INDEX IF NOT EXISTS idx_clients_city ON clients(city);
CREATE INDEX IF NOT EXISTS idx_clients_state ON clients(state);

-- 4. Client Communication Notes / History Table
CREATE TABLE IF NOT EXISTS client_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    author_id UUID,
    author_name VARCHAR(100),
    note_type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_client_notes_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_notes_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_client_notes_org_client ON client_notes(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_client_notes_created_at ON client_notes(created_at);
