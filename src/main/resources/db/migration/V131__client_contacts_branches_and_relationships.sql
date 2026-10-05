-- Migration: V131__client_contacts_branches_and_relationships.sql
-- Description: Creates client_contacts, client_branches, and client_relationships tables for Phase 28.5

-- 1. Client Contacts Table
CREATE TABLE IF NOT EXISTS client_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    display_name VARCHAR(200),
    designation VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(20),
    alt_phone VARCHAR(20),
    contact_role VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    primary_contact BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_client_contacts_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_client_contacts_org_client
    ON client_contacts(organization_id, client_id);

CREATE INDEX IF NOT EXISTS idx_client_contacts_org_client_active
    ON client_contacts(organization_id, client_id, active);

CREATE INDEX IF NOT EXISTS idx_client_contacts_org_client_primary
    ON client_contacts(organization_id, client_id, primary_contact);

-- 2. Client Branches / Locations Table
CREATE TABLE IF NOT EXISTS client_branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    branch_name VARCHAR(150) NOT NULL,
    branch_code VARCHAR(50),
    branch_type VARCHAR(50) NOT NULL DEFAULT 'BRANCH',
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    state_code VARCHAR(10),
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    pincode VARCHAR(20),
    gstin VARCHAR(15),
    phone VARCHAR(20),
    email VARCHAR(255),
    primary_branch BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_client_branches_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_client_branches_org_client
    ON client_branches(organization_id, client_id);

CREATE INDEX IF NOT EXISTS idx_client_branches_org_client_active
    ON client_branches(organization_id, client_id, active);

CREATE INDEX IF NOT EXISTS idx_client_branches_org_client_primary
    ON client_branches(organization_id, client_id, primary_branch);

-- 3. Client Relationships / Groups Table
CREATE TABLE IF NOT EXISTS client_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    source_client_id UUID NOT NULL,
    target_client_id UUID NOT NULL,
    relationship_type VARCHAR(50) NOT NULL DEFAULT 'RELATED_ENTITY',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_client_rel_source FOREIGN KEY (source_client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_rel_target FOREIGN KEY (target_client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT uk_client_rel_org_source_target_type UNIQUE (organization_id, source_client_id, target_client_id, relationship_type)
);

CREATE INDEX IF NOT EXISTS idx_client_rel_org_source
    ON client_relationships(organization_id, source_client_id);

CREATE INDEX IF NOT EXISTS idx_client_rel_org_target
    ON client_relationships(organization_id, target_client_id);

CREATE INDEX IF NOT EXISTS idx_client_rel_org_type
    ON client_relationships(organization_id, relationship_type);
