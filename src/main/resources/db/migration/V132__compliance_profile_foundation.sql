-- Migration: V132__compliance_profile_foundation.sql
-- Description: Creates compliance_profiles table for Phase 29.1 Compliance Profile Foundation

CREATE TABLE IF NOT EXISTS compliance_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    
    -- GST Configuration Facts
    gst_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    gst_registration_type VARCHAR(50),
    gst_filing_frequency VARCHAR(50),
    gst_composition_scheme BOOLEAN NOT NULL DEFAULT FALSE,
    gst_einvoice_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    gst_ewaybill_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- TDS Configuration Facts
    tds_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    tds_filing_frequency VARCHAR(50),
    tds_deductor_category VARCHAR(50),
    tds_lower_deduction_certificate BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- ITR Configuration Facts
    itr_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    itr_category VARCHAR(50),
    itr_tax_audit_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    itr_transfer_pricing_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Other Statutory Compliance Flags
    advance_tax_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    mca_filing_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    professional_tax_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    pf_esi_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Notes & Auditing Metadata
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT fk_compliance_profiles_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT uk_compliance_profiles_org_client UNIQUE (organization_id, client_id)
);

CREATE INDEX IF NOT EXISTS idx_compliance_profiles_org_client
    ON compliance_profiles(organization_id, client_id);

CREATE INDEX IF NOT EXISTS idx_compliance_profiles_org_status
    ON compliance_profiles(organization_id, status);
