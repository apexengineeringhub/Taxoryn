-- V117__dsc_register_foundation.sql
-- Digital Signature Certificate (DSC) Register and Lifecycle Management

CREATE TABLE IF NOT EXISTS dsc_register (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    client_id UUID REFERENCES clients(id) ON DELETE SET NULL,
    holder_name VARCHAR(255) NOT NULL,
    certificate_identifier VARCHAR(255),
    certificate_type VARCHAR(50) NOT NULL DEFAULT 'CLASS_3',
    issuer VARCHAR(255),
    issued_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    applicable_services VARCHAR(500),
    notes TEXT,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dsc_org_status ON dsc_register (organization_id, status);
CREATE INDEX IF NOT EXISTS idx_dsc_org_expiry ON dsc_register (organization_id, expiry_date);
CREATE INDEX IF NOT EXISTS idx_dsc_org_client ON dsc_register (organization_id, client_id);
