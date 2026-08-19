-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V8)
-- Compliance Calendar Module: Configurable Compliance Rules & Obligations
-- ==============================================================================

-- 1. Compliance Rules Table
CREATE TABLE IF NOT EXISTS compliance_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID,
    rule_code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    compliance_type VARCHAR(50) NOT NULL,
    frequency VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    due_day INT NOT NULL,
    due_month_offset INT NOT NULL DEFAULT 1,
    fixed_due_month INT,
    description_template TEXT,
    applicable_client_types VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system_rule BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_compliance_rules_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_compliance_rules_org ON compliance_rules(organization_id);
CREATE INDEX IF NOT EXISTS idx_compliance_rules_type ON compliance_rules(compliance_type);
CREATE INDEX IF NOT EXISTS idx_compliance_rules_code ON compliance_rules(rule_code);
CREATE INDEX IF NOT EXISTS idx_compliance_rules_active ON compliance_rules(is_active);

-- Seed System Default Compliance Rules
INSERT INTO compliance_rules (id, rule_code, name, compliance_type, frequency, due_day, due_month_offset, fixed_due_month, description_template, is_system_rule, is_active)
VALUES
    (gen_random_uuid(), 'GST_GSTR1_MONTHLY', 'GSTR-1 Monthly Return', 'GST', 'MONTHLY', 11, 1, NULL, 'Monthly summary of outward supplies of goods and services for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'GST_GSTR3B_MONTHLY', 'GSTR-3B Monthly Return & Tax Payment', 'GST', 'MONTHLY', 20, 1, NULL, 'Monthly self-declaration return and net tax liability payment for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'GST_CMP08_QUARTERLY', 'CMP-08 Composition Scheme Statement', 'GST', 'QUARTERLY', 18, 1, NULL, 'Quarterly statement for payment of self-assessed tax by composition dealers for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'TDS_CHALLAN_281', 'TDS / TCS Monthly Deposit (Challan 281)', 'TDS', 'MONTHLY', 7, 1, NULL, 'Monthly deposit of Tax Deducted / Collected at Source for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'TDS_RETURN_QUARTERLY', 'TDS Quarterly Return (Form 24Q / 26Q / 27Q)', 'TDS', 'QUARTERLY', 31, 1, NULL, 'Quarterly return of tax deducted at source for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'ITR_NON_AUDIT', 'Income Tax Return (Non-Audit Individual / HUF / Firm)', 'ITR', 'ANNUALLY', 31, 0, 7, 'Annual Income Tax Return for non-audit assesses for Assessment Year {period}', TRUE, TRUE),
    (gen_random_uuid(), 'ITR_AUDIT', 'Income Tax Return (Tax Audit / Corporate Assesses)', 'ITR', 'ANNUALLY', 31, 0, 10, 'Annual Income Tax Return for corporate & audit assesses for Assessment Year {period}', TRUE, TRUE),
    (gen_random_uuid(), 'ADVANCE_TAX_Q1', 'Advance Tax Installment 1 (15%)', 'OTHER', 'QUARTERLY', 15, 0, 6, 'First installment of 15% estimated advance income tax for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'ADVANCE_TAX_Q2', 'Advance Tax Installment 2 (45%)', 'OTHER', 'QUARTERLY', 15, 0, 9, 'Second installment of 45% estimated advance income tax for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'ADVANCE_TAX_Q3', 'Advance Tax Installment 3 (75%)', 'OTHER', 'QUARTERLY', 15, 0, 12, 'Third installment of 75% estimated advance income tax for {period}', TRUE, TRUE),
    (gen_random_uuid(), 'ADVANCE_TAX_Q4', 'Advance Tax Installment 4 (100%)', 'OTHER', 'QUARTERLY', 15, 0, 3, 'Fourth and final installment of 100% estimated advance income tax for {period}', TRUE, TRUE)
ON CONFLICT DO NOTHING;

-- 2. Compliance Obligations Table
CREATE TABLE IF NOT EXISTS compliance_obligations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    rule_id UUID,
    title VARCHAR(255) NOT NULL,
    compliance_type VARCHAR(50) NOT NULL,
    period VARCHAR(50) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    assigned_employee_id UUID,
    task_id UUID,
    completed_at TIMESTAMPTZ,
    completed_by VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_compliance_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_compliance_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_compliance_rule FOREIGN KEY (rule_id) REFERENCES compliance_rules(id) ON DELETE SET NULL,
    CONSTRAINT fk_compliance_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT fk_compliance_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    CONSTRAINT uq_compliance_org_client_period_rule UNIQUE (organization_id, client_id, period, rule_id)
);

CREATE INDEX IF NOT EXISTS idx_compliance_org_due_date ON compliance_obligations(organization_id, due_date);
CREATE INDEX IF NOT EXISTS idx_compliance_client ON compliance_obligations(client_id);
CREATE INDEX IF NOT EXISTS idx_compliance_status ON compliance_obligations(status);
CREATE INDEX IF NOT EXISTS idx_compliance_type ON compliance_obligations(compliance_type);
CREATE INDEX IF NOT EXISTS idx_compliance_emp ON compliance_obligations(assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_compliance_task ON compliance_obligations(task_id);
