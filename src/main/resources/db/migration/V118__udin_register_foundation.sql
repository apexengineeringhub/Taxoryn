-- V118__udin_register_foundation.sql
-- Unique Document Identification Number (UDIN) Register and Verification Tracking

CREATE TABLE IF NOT EXISTS udin_register (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    udin VARCHAR(18) NOT NULL,
    client_id UUID REFERENCES clients(id) ON DELETE SET NULL,
    engagement_id UUID REFERENCES engagements(id) ON DELETE SET NULL,
    service_id UUID REFERENCES services(id) ON DELETE SET NULL,
    document_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    invoice_id UUID REFERENCES invoices(id) ON DELETE SET NULL,
    document_type VARCHAR(100) NOT NULL,
    document_title VARCHAR(255) NOT NULL,
    document_description TEXT,
    signatory_name VARCHAR(255) NOT NULL,
    signatory_membership_no VARCHAR(50),
    generation_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    verification_status VARCHAR(50) NOT NULL DEFAULT 'NOT_VERIFIED',
    verification_source VARCHAR(100) DEFAULT 'MANUAL',
    verified_by VARCHAR(255),
    verified_at TIMESTAMP WITH TIME ZONE,
    verification_remarks TEXT,
    financial_figures_json JSONB,
    notes TEXT,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_udin_length CHECK (length(udin) = 18)
);

CREATE INDEX IF NOT EXISTS idx_udin_org_udin ON udin_register (organization_id, udin);
CREATE INDEX IF NOT EXISTS idx_udin_org_status ON udin_register (organization_id, status);
CREATE INDEX IF NOT EXISTS idx_udin_org_verification ON udin_register (organization_id, verification_status);
CREATE INDEX IF NOT EXISTS idx_udin_org_client ON udin_register (organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_udin_org_gen_date ON udin_register (organization_id, generation_date);
CREATE INDEX IF NOT EXISTS idx_udin_org_doc_type ON udin_register (organization_id, document_type);

ALTER TABLE dsc_register
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE udin_register
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;