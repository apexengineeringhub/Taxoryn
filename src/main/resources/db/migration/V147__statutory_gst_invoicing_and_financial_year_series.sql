-- ==============================================================================
-- Taxoryn Platform - Phase 33.2 Migration (V147)
-- Statutory GST Invoicing & Financial Year Sequential Series
-- Compliant with Rule 46 of CGST Rules 2017 & Indian April-March Financial Year
-- ==============================================================================

-- 1. Create Practice Invoice Numbering Series Table (Thread-safe sequence per FY)
CREATE TABLE IF NOT EXISTS practice_invoice_series (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    series_prefix VARCHAR(50) NOT NULL DEFAULT 'INV',
    delimiter VARCHAR(5) NOT NULL DEFAULT '-',
    current_sequence BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_practice_invoice_series_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT uq_practice_invoice_series UNIQUE (organization_id, financial_year, series_prefix)
);

CREATE INDEX IF NOT EXISTS idx_practice_invoice_series_org ON practice_invoice_series(organization_id);
CREATE INDEX IF NOT EXISTS idx_practice_invoice_series_lookup ON practice_invoice_series(organization_id, financial_year, series_prefix);

-- 2. Enhance Invoices Table with GST Components, Place of Supply, and Financial Year
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS cgst_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS sgst_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS igst_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS place_of_supply_state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS place_of_supply_state_code VARCHAR(10),
    ADD COLUMN IF NOT EXISTS supplier_state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS supplier_state_code VARCHAR(10),
    ADD COLUMN IF NOT EXISTS is_rcm BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS financial_year VARCHAR(20),
    ADD COLUMN IF NOT EXISTS series_prefix VARCHAR(50) DEFAULT 'INV';

CREATE INDEX IF NOT EXISTS idx_invoices_org_fy ON invoices(organization_id, financial_year);
CREATE INDEX IF NOT EXISTS idx_invoices_org_pos ON invoices(organization_id, place_of_supply_state_code);

-- 3. Enhance Invoice Line Items with SAC/HSN Code and Component Breakdowns
ALTER TABLE invoice_items
    ADD COLUMN IF NOT EXISTS sac_hsn_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS cgst_rate NUMERIC(5, 2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS cgst_amount NUMERIC(15, 2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS sgst_rate NUMERIC(5, 2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS sgst_amount NUMERIC(15, 2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS igst_rate NUMERIC(5, 2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS igst_amount NUMERIC(15, 2) DEFAULT 0.00;

CREATE INDEX IF NOT EXISTS idx_invoice_items_sac ON invoice_items(sac_hsn_code);
