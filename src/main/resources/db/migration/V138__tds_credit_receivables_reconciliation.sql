-- ==============================================================================
-- Taxoryn Platform - Phase 3.3 Migration (V138)
-- Automatic TDS Credit Reconciliation for Receivables
-- Multi-Tenant TDS Withholding Credits, Inward Ledger & Automatic Matching
-- ==============================================================================

-- 1. TDS Credit Records Table (Attributable Inward Withholding Credits)
CREATE TABLE IF NOT EXISTS tds_credits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    deductor_pan VARCHAR(10) NOT NULL,
    deductor_tan VARCHAR(10),
    deductor_name VARCHAR(255) NOT NULL,
    section_code VARCHAR(20) NOT NULL DEFAULT '194J',
    assessment_year VARCHAR(20) NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    quarter VARCHAR(10),
    gross_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    tds_deducted NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    source_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL_ENTRY',
    source_document_ref VARCHAR(100),
    certificate_number VARCHAR(100),
    certificate_date DATE,
    credit_status VARCHAR(50) NOT NULL DEFAULT 'UNRECONCILED',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tds_credits_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_credits_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tds_credits_org ON tds_credits(organization_id);
CREATE INDEX IF NOT EXISTS idx_tds_credits_client ON tds_credits(client_id);
CREATE INDEX IF NOT EXISTS idx_tds_credits_deductor_pan ON tds_credits(deductor_pan);
CREATE INDEX IF NOT EXISTS idx_tds_credits_fy_ay ON tds_credits(financial_year, assessment_year);
CREATE INDEX IF NOT EXISTS idx_tds_credits_status ON tds_credits(credit_status);

-- 2. TDS Credit Reconciliation Match Records (Audit Trail & Matching Entries)
CREATE TABLE IF NOT EXISTS tds_credit_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    invoice_id UUID NOT NULL,
    tds_credit_id UUID NOT NULL,
    match_status VARCHAR(50) NOT NULL DEFAULT 'EXACT_MATCH',
    match_confidence NUMERIC(5, 2) NOT NULL DEFAULT 100.00,
    matched_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    invoice_subtotal NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    credit_gross_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    expected_tds_rate NUMERIC(5, 2) NOT NULL DEFAULT 10.00,
    expected_tds_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    actual_tds_deducted NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    rate_discrepancy NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    remarks TEXT,
    reconciled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reconciled_by VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tds_matches_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_matches_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_matches_credit FOREIGN KEY (tds_credit_id) REFERENCES tds_credits(id) ON DELETE CASCADE,
    CONSTRAINT uq_tds_matches_inv_credit UNIQUE (organization_id, invoice_id, tds_credit_id)
);

CREATE INDEX IF NOT EXISTS idx_tds_matches_org ON tds_credit_matches(organization_id);
CREATE INDEX IF NOT EXISTS idx_tds_matches_invoice ON tds_credit_matches(invoice_id);
CREATE INDEX IF NOT EXISTS idx_tds_matches_credit ON tds_credit_matches(tds_credit_id);
CREATE INDEX IF NOT EXISTS idx_tds_matches_status ON tds_credit_matches(match_status);
