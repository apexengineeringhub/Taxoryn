-- ==============================================================================
-- Taxoryn Platform - Phase 2 Migration (V89)
-- Client Portfolio & Location Assignment Architecture
-- ==============================================================================

-- 1. Add client_code to clients table if missing
ALTER TABLE clients ADD COLUMN IF NOT EXISTS client_code VARCHAR(50);
CREATE INDEX IF NOT EXISTS idx_clients_org_code ON clients(organization_id, client_code);

-- 2. Create client_location_assignments Table
CREATE TABLE IF NOT EXISTS client_location_assignments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    location_id UUID NOT NULL,
    primary_location BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_cla_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_cla_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_cla_location FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cla_org ON client_location_assignments(organization_id);
CREATE INDEX IF NOT EXISTS idx_cla_client ON client_location_assignments(client_id);
CREATE INDEX IF NOT EXISTS idx_cla_location ON client_location_assignments(location_id);
CREATE INDEX IF NOT EXISTS idx_cla_client_active ON client_location_assignments(client_id, active);

-- 3. Create client_user_assignments Table
CREATE TABLE IF NOT EXISTS client_user_assignments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    user_id UUID NOT NULL,
    assignment_role VARCHAR(50) NOT NULL DEFAULT 'PRIMARY',
    primary_responsible BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_cua_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_cua_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_cua_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cua_org ON client_user_assignments(organization_id);
CREATE INDEX IF NOT EXISTS idx_cua_client ON client_user_assignments(client_id);
CREATE INDEX IF NOT EXISTS idx_cua_user ON client_user_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_cua_client_active ON client_user_assignments(client_id, active);
