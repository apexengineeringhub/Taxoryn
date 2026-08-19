-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V7)
-- ITR Management Module: ITR Profiles & Return Filings
-- ==============================================================================

-- 1. ITR Profiles Table
CREATE TABLE IF NOT EXISTS itr_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    pan VARCHAR(10) NOT NULL,
    taxpayer_type VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL',
    default_itr_type VARCHAR(50) NOT NULL DEFAULT 'ITR_1',
    residential_status VARCHAR(50) NOT NULL DEFAULT 'RESIDENT',
    assigned_employee_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_itr_profiles_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_itr_profiles_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_itr_profiles_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT uq_itr_profiles_org_pan UNIQUE (organization_id, pan),
    CONSTRAINT uq_itr_profiles_org_client UNIQUE (organization_id, client_id)
);

CREATE INDEX IF NOT EXISTS idx_itr_profiles_org ON itr_profiles(organization_id);
CREATE INDEX IF NOT EXISTS idx_itr_profiles_client ON itr_profiles(client_id);
CREATE INDEX IF NOT EXISTS idx_itr_profiles_pan ON itr_profiles(pan);
CREATE INDEX IF NOT EXISTS idx_itr_profiles_assigned_emp ON itr_profiles(assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_itr_profiles_status ON itr_profiles(status);

-- 2. ITR Returns Table
CREATE TABLE IF NOT EXISTS itr_returns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    itr_profile_id UUID,
    assessment_year VARCHAR(20) NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    itr_type VARCHAR(50) NOT NULL,
    taxpayer_type VARCHAR(50) NOT NULL,
    due_date DATE,
    filing_date DATE,
    acknowledgement_number VARCHAR(100),
    verification_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'DOCUMENTS_PENDING',
    assigned_employee_id UUID,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_itr_returns_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_itr_returns_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_itr_returns_profile FOREIGN KEY (itr_profile_id) REFERENCES itr_profiles(id) ON DELETE SET NULL,
    CONSTRAINT fk_itr_returns_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT uq_itr_returns_org_client_ay UNIQUE (organization_id, client_id, assessment_year)
);

CREATE INDEX IF NOT EXISTS idx_itr_returns_org ON itr_returns(organization_id);
CREATE INDEX IF NOT EXISTS idx_itr_returns_client ON itr_returns(client_id);
CREATE INDEX IF NOT EXISTS idx_itr_returns_profile ON itr_returns(itr_profile_id);
CREATE INDEX IF NOT EXISTS idx_itr_returns_ay ON itr_returns(assessment_year);
CREATE INDEX IF NOT EXISTS idx_itr_returns_status ON itr_returns(status);
CREATE INDEX IF NOT EXISTS idx_itr_returns_due_date ON itr_returns(due_date);
CREATE INDEX IF NOT EXISTS idx_itr_returns_assigned_emp ON itr_returns(assigned_employee_id);
