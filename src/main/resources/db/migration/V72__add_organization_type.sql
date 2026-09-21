-- V72__add_organization_type.sql
-- Introduce OrganizationType foundation at the Organization level

ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS organization_type VARCHAR(50);

-- Migrate all existing organizations to 'UNKNOWN'
UPDATE organizations
    SET organization_type = 'UNKNOWN'
    WHERE organization_type IS NULL;

-- Enforce NOT NULL constraint and default value
ALTER TABLE organizations
    ALTER COLUMN organization_type SET DEFAULT 'UNKNOWN',
    ALTER COLUMN organization_type SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_organizations_type ON organizations(organization_type);
