-- ==============================================================================
-- Taxoryn Platform — Flyway Migration V17: TDS & TCS Practice Management Module
-- Multi-Tenant Indian Tax Deducted at Source (TDS) & Tax Collected at Source (TCS)
-- ==============================================================================

-- 1. TDS Deductor & Collector Profiles (TAN Master)
CREATE TABLE IF NOT EXISTS tds_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    tan VARCHAR(10) NOT NULL,
    deductor_type VARCHAR(50) NOT NULL DEFAULT 'COMPANY',
    branch_division_name VARCHAR(255),
    pa_code VARCHAR(50),
    ddo_code VARCHAR(50),
    ministry_name VARCHAR(255),
    responsible_person_name VARCHAR(255),
    responsible_person_pan VARCHAR(10),
    responsible_person_designation VARCHAR(100),
    responsible_person_father_name VARCHAR(255),
    responsible_person_email VARCHAR(255),
    responsible_person_mobile VARCHAR(20),
    responsible_person_address TEXT,
    assigned_employee_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    traces_username VARCHAR(100),
    traces_status VARCHAR(50) NOT NULL DEFAULT 'NOT_REGISTERED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tds_profiles_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_profiles_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_profiles_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT uq_tds_profiles_org_tan UNIQUE (organization_id, tan)
);

CREATE INDEX IF NOT EXISTS idx_tds_profiles_org ON tds_profiles(organization_id);
CREATE INDEX IF NOT EXISTS idx_tds_profiles_client ON tds_profiles(client_id);
CREATE INDEX IF NOT EXISTS idx_tds_profiles_tan ON tds_profiles(tan);
CREATE INDEX IF NOT EXISTS idx_tds_profiles_assigned_emp ON tds_profiles(assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_tds_profiles_status ON tds_profiles(status);

-- 2. TDS / TCS Quarterly Returns Lifecycle (Forms 24Q, 26Q, 27Q, 27EQ, 26QB, 26QC, etc.)
CREATE TABLE IF NOT EXISTS tds_returns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    tds_profile_id UUID NOT NULL,
    form_type VARCHAR(50) NOT NULL,
    quarter VARCHAR(10) NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    assessment_year VARCHAR(20) NOT NULL,
    due_date DATE,
    filing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    filing_date DATE,
    token_number VARCHAR(20),
    receipt_number VARCHAR(100),
    total_amount_paid NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_tax_deducted NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_tax_deposited NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_interest NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_late_fee NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_penalty NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    assigned_employee_id UUID,
    fvu_validation_status VARCHAR(50) NOT NULL DEFAULT 'NOT_VALIDATED',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tds_returns_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_returns_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_returns_profile FOREIGN KEY (tds_profile_id) REFERENCES tds_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_returns_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT uq_tds_returns_org_profile_form_quarter_fy UNIQUE (organization_id, tds_profile_id, form_type, quarter, financial_year)
);

CREATE INDEX IF NOT EXISTS idx_tds_returns_org ON tds_returns(organization_id);
CREATE INDEX IF NOT EXISTS idx_tds_returns_client ON tds_returns(client_id);
CREATE INDEX IF NOT EXISTS idx_tds_returns_profile ON tds_returns(tds_profile_id);
CREATE INDEX IF NOT EXISTS idx_tds_returns_quarter_fy ON tds_returns(quarter, financial_year);
CREATE INDEX IF NOT EXISTS idx_tds_returns_status ON tds_returns(filing_status);
CREATE INDEX IF NOT EXISTS idx_tds_returns_due_date ON tds_returns(due_date);
CREATE INDEX IF NOT EXISTS idx_tds_returns_assigned_emp ON tds_returns(assigned_employee_id);

-- 3. TDS Challan ITNS 281 Records & Allocation
CREATE TABLE IF NOT EXISTS tds_challans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    tds_profile_id UUID NOT NULL,
    tds_return_id UUID,
    bsr_code VARCHAR(10) NOT NULL,
    challan_date DATE NOT NULL,
    challan_serial_no VARCHAR(10) NOT NULL,
    cin VARCHAR(50),
    major_head VARCHAR(50) NOT NULL DEFAULT 'HEAD_0021_NON_COMPANY',
    minor_head VARCHAR(50) NOT NULL DEFAULT 'HEAD_200_PAYABLE_BY_TAXPAYER',
    section_code VARCHAR(20) NOT NULL,
    tds_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    surcharge_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    cess_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    interest_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    fee_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    penalty_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    utilized_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    balance_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    challan_status VARCHAR(50) NOT NULL DEFAULT 'UNUTILIZED',
    quarter VARCHAR(10) NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    payment_mode VARCHAR(50) NOT NULL DEFAULT 'NET_BANKING',
    bank_name VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tds_challans_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_challans_profile FOREIGN KEY (tds_profile_id) REFERENCES tds_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_challans_return FOREIGN KEY (tds_return_id) REFERENCES tds_returns(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_tds_challans_org ON tds_challans(organization_id);
CREATE INDEX IF NOT EXISTS idx_tds_challans_profile ON tds_challans(tds_profile_id);
CREATE INDEX IF NOT EXISTS idx_tds_challans_cin ON tds_challans(cin);
CREATE INDEX IF NOT EXISTS idx_tds_challans_quarter_fy ON tds_challans(quarter, financial_year);
CREATE INDEX IF NOT EXISTS idx_tds_challans_status ON tds_challans(challan_status);

-- 4. TDS Deductee / Payee Entries
CREATE TABLE IF NOT EXISTS tds_deductee_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    tds_profile_id UUID NOT NULL,
    tds_return_id UUID,
    challan_id UUID,
    deductee_pan VARCHAR(10) NOT NULL,
    deductee_name VARCHAR(255) NOT NULL,
    deductee_type VARCHAR(50) NOT NULL DEFAULT 'NON_COMPANY',
    section_code VARCHAR(20) NOT NULL,
    payment_credit_date DATE NOT NULL,
    invoice_ref_number VARCHAR(100),
    amount_paid_credited NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    tds_rate NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    tds_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    surcharge_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    cess_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_tax_deducted NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    deduction_date DATE NOT NULL,
    certificate_number_197 VARCHAR(50),
    reason_code VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    quarter VARCHAR(10) NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tds_deductees_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_deductees_profile FOREIGN KEY (tds_profile_id) REFERENCES tds_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_deductees_return FOREIGN KEY (tds_return_id) REFERENCES tds_returns(id) ON DELETE SET NULL,
    CONSTRAINT fk_tds_deductees_challan FOREIGN KEY (challan_id) REFERENCES tds_challans(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_tds_deductees_org ON tds_deductee_entries(organization_id);
CREATE INDEX IF NOT EXISTS idx_tds_deductees_profile ON tds_deductee_entries(tds_profile_id);
CREATE INDEX IF NOT EXISTS idx_tds_deductees_pan ON tds_deductee_entries(deductee_pan);
CREATE INDEX IF NOT EXISTS idx_tds_deductees_return ON tds_deductee_entries(tds_return_id);
CREATE INDEX IF NOT EXISTS idx_tds_deductees_quarter_fy ON tds_deductee_entries(quarter, financial_year);

-- 5. Form 16 / 16A / 27D Certificates Tracking
CREATE TABLE IF NOT EXISTS tds_certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    tds_profile_id UUID NOT NULL,
    tds_return_id UUID,
    certificate_type VARCHAR(50) NOT NULL DEFAULT 'FORM_16A',
    financial_year VARCHAR(20) NOT NULL,
    quarter VARCHAR(10),
    deductee_pan VARCHAR(10) NOT NULL,
    deductee_name VARCHAR(255) NOT NULL,
    traces_request_number VARCHAR(50),
    certificate_number VARCHAR(100),
    generation_date DATE,
    dispatch_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    dispatched_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tds_certs_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_certs_profile FOREIGN KEY (tds_profile_id) REFERENCES tds_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_tds_certs_return FOREIGN KEY (tds_return_id) REFERENCES tds_returns(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_tds_certs_org ON tds_certificates(organization_id);
CREATE INDEX IF NOT EXISTS idx_tds_certs_profile ON tds_certificates(tds_profile_id);
CREATE INDEX IF NOT EXISTS idx_tds_certs_pan ON tds_certificates(deductee_pan);
CREATE INDEX IF NOT EXISTS idx_tds_certs_status ON tds_certificates(dispatch_status);
