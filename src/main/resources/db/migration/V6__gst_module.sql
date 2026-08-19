-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V6)
-- GST Management Module: Profiles, Return Filings & Monthly Summaries
-- ==============================================================================

-- 1. GST Profiles & Registrations Table
CREATE TABLE IF NOT EXISTS gst_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    gstin VARCHAR(15) NOT NULL,
    legal_name VARCHAR(255),
    trade_name VARCHAR(255),
    gst_type VARCHAR(50) NOT NULL DEFAULT 'REGULAR',
    filing_frequency VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    registration_date DATE,
    state_code VARCHAR(10),
    principal_place_of_business TEXT,
    assigned_employee_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_gst_profiles_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_profiles_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_profiles_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT uq_gst_profiles_org_gstin UNIQUE (organization_id, gstin)
);

CREATE INDEX IF NOT EXISTS idx_gst_profiles_org ON gst_profiles(organization_id);
CREATE INDEX IF NOT EXISTS idx_gst_profiles_client ON gst_profiles(client_id);
CREATE INDEX IF NOT EXISTS idx_gst_profiles_gstin ON gst_profiles(gstin);
CREATE INDEX IF NOT EXISTS idx_gst_profiles_assigned_emp ON gst_profiles(assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_gst_profiles_status ON gst_profiles(status);

-- 2. GST Return Filings Table
CREATE TABLE IF NOT EXISTS gst_return_filings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    gst_profile_id UUID NOT NULL,
    client_id UUID NOT NULL,
    return_type VARCHAR(50) NOT NULL,
    return_period VARCHAR(50) NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    due_date DATE,
    filing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    filing_date DATE,
    acknowledgement_number VARCHAR(100),
    total_taxable_value NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_tax_liability NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_itc_claimed NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    tax_paid_cash NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    tax_paid_itc NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    assigned_employee_id UUID,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_gst_filings_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_filings_profile FOREIGN KEY (gst_profile_id) REFERENCES gst_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_filings_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_filings_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT uq_gst_filings_org_profile_type_period UNIQUE (organization_id, gst_profile_id, return_type, return_period)
);

CREATE INDEX IF NOT EXISTS idx_gst_filings_org ON gst_return_filings(organization_id);
CREATE INDEX IF NOT EXISTS idx_gst_filings_profile ON gst_return_filings(gst_profile_id);
CREATE INDEX IF NOT EXISTS idx_gst_filings_client ON gst_return_filings(client_id);
CREATE INDEX IF NOT EXISTS idx_gst_filings_period ON gst_return_filings(return_period);
CREATE INDEX IF NOT EXISTS idx_gst_filings_status ON gst_return_filings(filing_status);
CREATE INDEX IF NOT EXISTS idx_gst_filings_due_date ON gst_return_filings(due_date);
CREATE INDEX IF NOT EXISTS idx_gst_filings_assigned_emp ON gst_return_filings(assigned_employee_id);

-- 3. GST Monthly Summaries (Sales, Purchase, ITC, Liability) Table
CREATE TABLE IF NOT EXISTS gst_monthly_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    gst_profile_id UUID NOT NULL,
    client_id UUID NOT NULL,
    period VARCHAR(50) NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    total_sales_taxable NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    igst_sales NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    cgst_sales NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    sgst_sales NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    cess_sales NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_purchase_taxable NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    igst_purchase NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    cgst_purchase NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    sgst_purchase NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    cess_purchase NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    itc_eligible NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    itc_ineligible NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    itc_reversed NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    itc_net_claimed NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    net_tax_liability NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    challan_status VARCHAR(50) NOT NULL DEFAULT 'NOT_GENERATED',
    challan_cprn VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_gst_summaries_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_summaries_profile FOREIGN KEY (gst_profile_id) REFERENCES gst_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_gst_summaries_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT uq_gst_summaries_org_profile_period UNIQUE (organization_id, gst_profile_id, period)
);

CREATE INDEX IF NOT EXISTS idx_gst_summaries_org ON gst_monthly_summaries(organization_id);
CREATE INDEX IF NOT EXISTS idx_gst_summaries_profile ON gst_monthly_summaries(gst_profile_id);
CREATE INDEX IF NOT EXISTS idx_gst_summaries_period ON gst_monthly_summaries(period);
