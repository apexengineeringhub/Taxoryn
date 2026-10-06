-- ==============================================================================
-- Taxoryn Platform - Migration V133
-- Phase 29.2: Compliance Rule Catalog
-- ==============================================================================

-- 1. Upgrade compliance_rules table for Compliance Intelligence architecture
CREATE TABLE IF NOT EXISTS compliance_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID,
    rule_code VARCHAR(100) NOT NULL,
    name VARCHAR(255),
    rule_name VARCHAR(255),
    compliance_type VARCHAR(50),
    domain VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    frequency VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    period_type VARCHAR(50) NOT NULL DEFAULT 'MONTH',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    due_day INT,
    due_month_offset INT DEFAULT 1,
    fixed_due_month INT,
    description_template TEXT,
    applicable_client_types VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system_rule BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    statutory_act VARCHAR(255),
    statutory_section VARCHAR(255),
    statutory_form_code VARCHAR(100),
    penalty_details TEXT,
    due_date_rule_type VARCHAR(50) NOT NULL DEFAULT 'DAY_OF_FOLLOWING_MONTH',
    due_day_offset INT,
    fixed_month INT,
    fixed_day INT,
    statutory_grace_days INT NOT NULL DEFAULT 0,
    due_date_description VARCHAR(255),
    required_module VARCHAR(50),
    applicable_entity_types TEXT,
    applicable_gst_registration_types TEXT,
    applicable_filing_frequencies TEXT,
    requires_tax_audit BOOLEAN,
    requires_transfer_pricing BOOLEAN,
    requires_tds_deductor BOOLEAN,
    requires_mca_filing BOOLEAN,
    default_work_template_code VARCHAR(100),
    effective_from DATE,
    effective_to DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Add any missing columns to existing compliance_rules table
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS rule_name VARCHAR(255);
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS domain VARCHAR(50) DEFAULT 'OTHER';
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS period_type VARCHAR(50) DEFAULT 'MONTH';
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE';
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS statutory_act VARCHAR(255);
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS statutory_section VARCHAR(255);
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS statutory_form_code VARCHAR(100);
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS penalty_details TEXT;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS due_date_rule_type VARCHAR(50) DEFAULT 'DAY_OF_FOLLOWING_MONTH';
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS due_day_offset INT;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS fixed_month INT;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS fixed_day INT;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS statutory_grace_days INT DEFAULT 0;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS due_date_description VARCHAR(255);
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS required_module VARCHAR(50);
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS applicable_entity_types TEXT;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS applicable_gst_registration_types TEXT;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS applicable_filing_frequencies TEXT;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS requires_tax_audit BOOLEAN;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS requires_transfer_pricing BOOLEAN;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS requires_tds_deductor BOOLEAN;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS requires_mca_filing BOOLEAN;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS default_work_template_code VARCHAR(100);
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS effective_from DATE;
ALTER TABLE compliance_rules ADD COLUMN IF NOT EXISTS effective_to DATE;

-- Drop legacy not-null constraints to support the updated schema
ALTER TABLE compliance_rules ALTER COLUMN due_day DROP NOT NULL;
ALTER TABLE compliance_rules ALTER COLUMN due_month_offset DROP NOT NULL;
ALTER TABLE compliance_rules ALTER COLUMN name DROP NOT NULL;
ALTER TABLE compliance_rules ALTER COLUMN compliance_type DROP NOT NULL;

-- Synchronize name and rule_name if existing records exist
UPDATE compliance_rules SET rule_name = name WHERE rule_name IS NULL AND name IS NOT NULL;
UPDATE compliance_rules SET name = rule_name WHERE name IS NULL AND rule_name IS NOT NULL;

-- 2. Indexes
CREATE UNIQUE INDEX IF NOT EXISTS uq_compliance_rules_system_code ON compliance_rules(rule_code) WHERE organization_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_compliance_rules_domain_status ON compliance_rules(domain, status);
CREATE INDEX IF NOT EXISTS idx_compliance_rules_org_status ON compliance_rules(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_compliance_rules_code ON compliance_rules(rule_code);

-- 3. Upsert Master Standard Indian Compliance Rules (System Master Catalog)

-- GST Rules
INSERT INTO compliance_rules (
    organization_id, rule_code, name, rule_name, compliance_type, domain, frequency, period_type, status, is_system_rule, is_active,
    description, statutory_act, statutory_section, statutory_form_code, penalty_details,
    due_date_rule_type, due_day, due_day_offset, due_month_offset, statutory_grace_days, due_date_description,
    required_module, applicable_gst_registration_types, applicable_filing_frequencies, default_work_template_code
) VALUES
(NULL, 'GST_GSTR1_MONTHLY', 'GSTR-1 Monthly Return', 'GSTR-1 Monthly Return', 'GST', 'GST', 'MONTHLY', 'MONTH', 'ACTIVE', TRUE, TRUE,
 'Monthly statement of outward supplies of goods or services for regular taxpayers.',
 'Central Goods and Services Tax Act, 2017', 'Section 37', 'GSTR-1', 'Late fee of ₹50/day (₹20 for nil return) under Section 47',
 'DAY_OF_FOLLOWING_MONTH', 11, 11, 1, 0, '11th of following month',
 'GST', 'REGULAR', 'MONTHLY', 'GST_GSTR1_FILING'),

(NULL, 'GST_GSTR1_QUARTERLY_QRMP', 'GSTR-1 Quarterly (QRMP) Return', 'GSTR-1 Quarterly (QRMP) Return', 'GST', 'GST', 'QUARTERLY', 'QUARTER', 'ACTIVE', TRUE, TRUE,
 'Quarterly statement of outward supplies for taxpayers under QRMP scheme.',
 'Central Goods and Services Tax Act, 2017', 'Section 37', 'GSTR-1', 'Late fee under Section 47',
 'DAY_OF_FOLLOWING_QUARTER_END_MONTH', 13, 13, 1, 0, '13th of month following quarter',
 'GST', 'REGULAR', 'QUARTERLY', 'GST_GSTR1_QRMP_FILING'),

(NULL, 'GST_IFF_MONTHLY', 'Invoice Furnishing Facility (IFF)', 'Invoice Furnishing Facility (IFF)', 'GST', 'GST', 'MONTHLY', 'MONTH', 'ACTIVE', TRUE, TRUE,
 'Optional facility for QRMP taxpayers to upload B2B invoices for M1 and M2 of a quarter.',
 'Central Goods and Services Tax Act, 2017', 'Rule 59(2)', 'IFF', 'No late fee (optional facility)',
 'DAY_OF_FOLLOWING_MONTH', 13, 13, 1, 0, '13th of following month (M1 & M2 only)',
 'GST', 'REGULAR', 'QUARTERLY', 'GST_IFF_UPLOAD'),

(NULL, 'GST_GSTR3B_MONTHLY', 'GSTR-3B Monthly Return & Tax Settlement', 'GSTR-3B Monthly Return & Tax Settlement', 'GST', 'GST', 'MONTHLY', 'MONTH', 'ACTIVE', TRUE, TRUE,
 'Monthly summary return of outward & inward supplies and input tax credit with tax payment.',
 'Central Goods and Services Tax Act, 2017', 'Section 39', 'GSTR-3B', 'Late fee ₹50/day (₹20 nil) + 18% p.a. interest under Section 50',
 'DAY_OF_FOLLOWING_MONTH', 20, 20, 1, 0, '20th of following month',
 'GST', 'REGULAR', 'MONTHLY', 'GST_GSTR3B_FILING'),

(NULL, 'GST_GSTR3B_QUARTERLY_QRMP', 'GSTR-3B Quarterly (QRMP) Return', 'GSTR-3B Quarterly (QRMP) Return', 'GST', 'GST', 'QUARTERLY', 'QUARTER', 'ACTIVE', TRUE, TRUE,
 'Quarterly self-assessed summary return for taxpayers under QRMP scheme.',
 'Central Goods and Services Tax Act, 2017', 'Section 39', 'GSTR-3B', 'Late fee under Section 47 + interest under Section 50',
 'DAY_OF_FOLLOWING_QUARTER_END_MONTH', 22, 22, 1, 0, '22nd or 24th of month following quarter',
 'GST', 'REGULAR', 'QUARTERLY', 'GST_GSTR3B_QRMP_FILING'),

(NULL, 'GST_CMP08_QUARTERLY', 'CMP-08 Quarterly Challan-cum-Statement', 'CMP-08 Quarterly Challan-cum-Statement', 'GST', 'GST', 'QUARTERLY', 'QUARTER', 'ACTIVE', TRUE, TRUE,
 'Quarterly statement for payment of self-assessed tax by composition taxpayers.',
 'Central Goods and Services Tax Act, 2017', 'Section 10 & Rule 62', 'CMP-08', 'Interest @ 18% p.a. under Section 50',
 'DAY_OF_FOLLOWING_QUARTER_END_MONTH', 18, 18, 1, 0, '18th of month following quarter',
 'GST', 'COMPOSITION', 'QUARTERLY', 'GST_CMP08_PAYMENT'),

(NULL, 'GST_GSTR4_ANNUAL', 'GSTR-4 Annual Return for Composition', 'GSTR-4 Annual Return for Composition', 'GST', 'GST', 'ANNUAL', 'FINANCIAL_YEAR', 'ACTIVE', TRUE, TRUE,
 'Annual return for taxpayers registered under the GST Composition Scheme.',
 'Central Goods and Services Tax Act, 2017', 'Section 39(2)', 'GSTR-4', 'Late fee of ₹50/day up to maximum statutory cap',
 'FIXED_DATE_IN_YEAR', 30, 30, 0, 0, '30th April following the financial year',
 'GST', 'COMPOSITION', 'ANNUAL', 'GST_GSTR4_ANNUAL_FILING'),

(NULL, 'GST_GSTR9_ANNUAL', 'GSTR-9 Annual GST Return', 'GSTR-9 Annual GST Return', 'GST', 'GST', 'ANNUAL', 'FINANCIAL_YEAR', 'ACTIVE', TRUE, TRUE,
 'Annual consolidation return for regular GST taxpayers with turnover exceeding threshold.',
 'Central Goods and Services Tax Act, 2017', 'Section 44', 'GSTR-9', 'Late fee ₹200/day subject to 0.5% of turnover cap',
 'FIXED_DATE_IN_YEAR', 31, 31, 0, 0, '31st December following the financial year',
 'GST', 'REGULAR', 'ANNUAL', 'GST_GSTR9_ANNUAL_FILING'),

(NULL, 'GST_GSTR9C_ANNUAL', 'GSTR-9C Reconciliation Statement', 'GSTR-9C Reconciliation Statement', 'GST', 'GST', 'ANNUAL', 'FINANCIAL_YEAR', 'ACTIVE', TRUE, TRUE,
 'Self-certified reconciliation statement between audited annual financials and GSTR-9.',
 'Central Goods and Services Tax Act, 2017', 'Section 44(2)', 'GSTR-9C', 'General penalty under Section 125 up to ₹50,000',
 'FIXED_DATE_IN_YEAR', 31, 31, 0, 0, '31st December following the financial year',
 'GST', 'REGULAR', 'ANNUAL', 'GST_GSTR9C_RECONCILIATION'),

(NULL, 'GST_GSTR7_MONTHLY', 'GSTR-7 Monthly TDS Return under GST', 'GSTR-7 Monthly TDS Return under GST', 'GST', 'GST', 'MONTHLY', 'MONTH', 'ACTIVE', TRUE, TRUE,
 'Monthly return for entities required to deduct tax at source (TDS) under GST.',
 'Central Goods and Services Tax Act, 2017', 'Section 51', 'GSTR-7', 'Late fee ₹50/day under Section 47',
 'DAY_OF_FOLLOWING_MONTH', 10, 10, 1, 0, '10th of following month',
 'GST', 'TDS_TCS', 'MONTHLY', 'GST_GSTR7_FILING'),

(NULL, 'GST_GSTR8_MONTHLY', 'GSTR-8 Monthly TCS Return under GST', 'GSTR-8 Monthly TCS Return under GST', 'GST', 'GST', 'MONTHLY', 'MONTH', 'ACTIVE', TRUE, TRUE,
 'Monthly statement for e-commerce operators collecting tax at source (TCS) under GST.',
 'Central Goods and Services Tax Act, 2017', 'Section 52', 'GSTR-8', 'Late fee ₹50/day under Section 47',
 'DAY_OF_FOLLOWING_MONTH', 10, 10, 1, 0, '10th of following month',
 'GST', 'TDS_TCS', 'MONTHLY', 'GST_GSTR8_FILING')
ON CONFLICT (rule_code) WHERE organization_id IS NULL
DO UPDATE SET
    name = EXCLUDED.name,
    rule_name = EXCLUDED.rule_name,
    domain = EXCLUDED.domain,
    frequency = EXCLUDED.frequency,
    period_type = EXCLUDED.period_type,
    status = EXCLUDED.status,
    description = EXCLUDED.description,
    statutory_act = EXCLUDED.statutory_act,
    statutory_section = EXCLUDED.statutory_section,
    statutory_form_code = EXCLUDED.statutory_form_code,
    penalty_details = EXCLUDED.penalty_details,
    due_date_rule_type = EXCLUDED.due_date_rule_type,
    due_day = EXCLUDED.due_day,
    due_day_offset = EXCLUDED.due_day_offset,
    due_month_offset = EXCLUDED.due_month_offset,
    statutory_grace_days = EXCLUDED.statutory_grace_days,
    due_date_description = EXCLUDED.due_date_description,
    required_module = EXCLUDED.required_module,
    applicable_gst_registration_types = EXCLUDED.applicable_gst_registration_types,
    applicable_filing_frequencies = EXCLUDED.applicable_filing_frequencies,
    default_work_template_code = EXCLUDED.default_work_template_code;

-- TDS / TCS Rules
INSERT INTO compliance_rules (
    organization_id, rule_code, name, rule_name, compliance_type, domain, frequency, period_type, status, is_system_rule, is_active,
    description, statutory_act, statutory_section, statutory_form_code, penalty_details,
    due_date_rule_type, due_day, due_day_offset, due_month_offset, statutory_grace_days, due_date_description,
    required_module, requires_tds_deductor, default_work_template_code
) VALUES
(NULL, 'TDS_CHALLAN_281_MONTHLY', 'Monthly TDS / TCS Deposit Challan ITNS 281', 'Monthly TDS / TCS Deposit Challan ITNS 281', 'TDS', 'TDS', 'MONTHLY', 'MONTH', 'ACTIVE', TRUE, TRUE,
 'Monthly deposit of tax deducted or collected at source to the government treasury via ITNS 281.',
 'Income Tax Act, 1961', 'Section 200(1) & Rule 30', 'ITNS 281', 'Interest @ 1.5% per month or part of month under Section 201(1A)',
 'DAY_OF_FOLLOWING_MONTH', 7, 7, 1, 0, '7th of following month (30th April for March deduction)',
 'TDS', TRUE, 'TDS_CHALLAN_281_DEPOSIT'),

(NULL, 'TDS_24Q_QUARTERLY', 'Form 24Q Quarterly Salary TDS Return', 'Form 24Q Quarterly Salary TDS Return', 'TDS', 'TDS', 'QUARTERLY', 'QUARTER', 'ACTIVE', TRUE, TRUE,
 'Quarterly statement for tax deducted at source from salaries under section 192.',
 'Income Tax Act, 1961', 'Section 200(3)', 'Form 24Q', 'Fee of ₹200/day under Section 234E + penalty under Section 271H',
 'DAY_OF_FOLLOWING_QUARTER_END_MONTH', 31, 31, 1, 0, '31st of month following quarter (31st May for Q4)',
 'TDS', TRUE, 'TDS_24Q_QUARTERLY_FILING'),

(NULL, 'TDS_26Q_QUARTERLY', 'Form 26Q Quarterly Non-Salary TDS Return', 'Form 26Q Quarterly Non-Salary TDS Return', 'TDS', 'TDS', 'QUARTERLY', 'QUARTER', 'ACTIVE', TRUE, TRUE,
 'Quarterly statement for tax deducted at source on payments other than salaries (e.g. 194C, 194J, 194I, 194Q).',
 'Income Tax Act, 1961', 'Section 200(3)', 'Form 26Q', 'Fee of ₹200/day under Section 234E + penalty under Section 271H',
 'DAY_OF_FOLLOWING_QUARTER_END_MONTH', 31, 31, 1, 0, '31st of month following quarter (31st May for Q4)',
 'TDS', TRUE, 'TDS_26Q_QUARTERLY_FILING'),

(NULL, 'TDS_27Q_QUARTERLY', 'Form 27Q Quarterly Non-Resident TDS Return', 'Form 27Q Quarterly Non-Resident TDS Return', 'TDS', 'TDS', 'QUARTERLY', 'QUARTER', 'ACTIVE', TRUE, TRUE,
 'Quarterly statement for tax deducted at source on payments made to non-residents (Section 195).',
 'Income Tax Act, 1961', 'Section 200(3)', 'Form 27Q', 'Fee of ₹200/day under Section 234E + penalty under Section 271H',
 'DAY_OF_FOLLOWING_QUARTER_END_MONTH', 31, 31, 1, 0, '31st of month following quarter (31st May for Q4)',
 'TDS', TRUE, 'TDS_27Q_QUARTERLY_FILING'),

(NULL, 'TDS_27EQ_QUARTERLY', 'Form 27EQ Quarterly TCS Return', 'Form 27EQ Quarterly TCS Return', 'TDS', 'TDS', 'QUARTERLY', 'QUARTER', 'ACTIVE', TRUE, TRUE,
 'Quarterly statement for tax collected at source (TCS) under section 206C.',
 'Income Tax Act, 1961', 'Section 206C(3)', 'Form 27EQ', 'Fee of ₹200/day under Section 234E + penalty under Section 271H',
 'DAY_OF_FOLLOWING_QUARTER_END_MONTH', 15, 15, 1, 0, '15th of month following quarter (15th May for Q4)',
 'TDS', TRUE, 'TDS_27EQ_QUARTERLY_FILING')
ON CONFLICT (rule_code) WHERE organization_id IS NULL
DO UPDATE SET
    name = EXCLUDED.name,
    rule_name = EXCLUDED.rule_name,
    domain = EXCLUDED.domain,
    frequency = EXCLUDED.frequency,
    period_type = EXCLUDED.period_type,
    status = EXCLUDED.status,
    description = EXCLUDED.description,
    statutory_act = EXCLUDED.statutory_act,
    statutory_section = EXCLUDED.statutory_section,
    statutory_form_code = EXCLUDED.statutory_form_code,
    penalty_details = EXCLUDED.penalty_details,
    due_date_rule_type = EXCLUDED.due_date_rule_type,
    due_day = EXCLUDED.due_day,
    due_day_offset = EXCLUDED.due_day_offset,
    due_month_offset = EXCLUDED.due_month_offset,
    statutory_grace_days = EXCLUDED.statutory_grace_days,
    due_date_description = EXCLUDED.due_date_description,
    required_module = EXCLUDED.required_module,
    requires_tds_deductor = EXCLUDED.requires_tds_deductor,
    default_work_template_code = EXCLUDED.default_work_template_code;

-- Income Tax (ITR) Rules
INSERT INTO compliance_rules (
    organization_id, rule_code, name, rule_name, compliance_type, domain, frequency, period_type, status, is_system_rule, is_active,
    description, statutory_act, statutory_section, statutory_form_code, penalty_details,
    due_date_rule_type, due_day, fixed_month, fixed_day, statutory_grace_days, due_date_description,
    required_module, requires_tax_audit, requires_transfer_pricing, default_work_template_code
) VALUES
(NULL, 'ITR_NON_AUDIT_ANNUAL', 'ITR Filing (Non-Audit Cases)', 'ITR Filing (Non-Audit Cases)', 'ITR', 'INCOME_TAX', 'ANNUAL', 'ASSESSMENT_YEAR', 'ACTIVE', TRUE, TRUE,
 'Annual Income Tax Return filing for individuals, HUF, and non-audit business entities.',
 'Income Tax Act, 1961', 'Section 139(1)', 'ITR-1/2/3/4/5', 'Late filing fee ₹5,000 (₹1,000 if income <= 5L) u/s 234F + interest u/s 234A',
 'FIXED_DATE_IN_YEAR', 31, 7, 31, 0, '31st July of Assessment Year',
 'ITR', FALSE, FALSE, 'ITR_NON_AUDIT_FILING'),

(NULL, 'ITR_AUDIT_ANNUAL', 'ITR Filing (Corporate & Tax Audit Cases)', 'ITR Filing (Corporate & Tax Audit Cases)', 'ITR', 'INCOME_TAX', 'ANNUAL', 'ASSESSMENT_YEAR', 'ACTIVE', TRUE, TRUE,
 'Annual Income Tax Return filing for companies and taxpayers liable to tax audit.',
 'Income Tax Act, 1961', 'Section 139(1)', 'ITR-6/5/7', 'Late filing fee ₹5,000 u/s 234F + interest u/s 234A',
 'FIXED_DATE_IN_YEAR', 31, 10, 31, 0, '31st October of Assessment Year',
 'ITR', TRUE, FALSE, 'ITR_AUDIT_FILING'),

(NULL, 'ITR_TRANSFER_PRICING_ANNUAL', 'ITR Filing (Transfer Pricing / Section 92E)', 'ITR Filing (Transfer Pricing / Section 92E)', 'ITR', 'INCOME_TAX', 'ANNUAL', 'ASSESSMENT_YEAR', 'ACTIVE', TRUE, TRUE,
 'Annual Income Tax Return filing for taxpayers entering into international or specified domestic transactions.',
 'Income Tax Act, 1961', 'Section 139(1)', 'ITR-6', 'Late filing fee ₹5,000 u/s 234F + interest u/s 234A',
 'FIXED_DATE_IN_YEAR', 30, 11, 30, 0, '30th November of Assessment Year',
 'ITR', TRUE, TRUE, 'ITR_TRANSFER_PRICING_FILING'),

(NULL, 'ITR_TAX_AUDIT_44AB', 'Tax Audit Report Form 3CA/3CB-3CD', 'Tax Audit Report Form 3CA/3CB-3CD', 'ITR', 'INCOME_TAX', 'ANNUAL', 'ASSESSMENT_YEAR', 'ACTIVE', TRUE, TRUE,
 'Filing of audited financial statements and Tax Audit Report under section 44AB.',
 'Income Tax Act, 1961', 'Section 44AB', 'Form 3CA/3CB-3CD', 'Penalty u/s 271B equal to 0.5% of turnover up to ₹1,50,000',
 'FIXED_DATE_IN_YEAR', 30, 9, 30, 0, '30th September of Assessment Year',
 'ITR', TRUE, FALSE, 'ITR_TAX_AUDIT_REPORT'),

(NULL, 'ITR_TRANSFER_PRICING_92E', 'Transfer Pricing Audit Report Form 3CEB', 'Transfer Pricing Audit Report Form 3CEB', 'ITR', 'INCOME_TAX', 'ANNUAL', 'ASSESSMENT_YEAR', 'ACTIVE', TRUE, TRUE,
 'Accountant report relating to international transactions and specified domestic transactions u/s 92E.',
 'Income Tax Act, 1961', 'Section 92E', 'Form 3CEB', 'Penalty of ₹1,00,000 under Section 271BA',
 'FIXED_DATE_IN_YEAR', 31, 10, 31, 0, '31st October of Assessment Year',
 'ITR', TRUE, TRUE, 'ITR_FORM_3CEB_REPORT'),

(NULL, 'ITR_ADVANCE_TAX_Q1', 'Advance Tax Installment 1 (15%)', 'Advance Tax Installment 1 (15%)', 'OTHER', 'INCOME_TAX', 'QUARTERLY', 'FINANCIAL_YEAR', 'ACTIVE', TRUE, TRUE,
 'First installment of advance tax (minimum 15% of estimated total tax liability).',
 'Income Tax Act, 1961', 'Section 208 / 211', 'Challan 280', 'Interest @ 1% per month under Section 234C for deferment',
 'FIXED_DATE_IN_YEAR', 15, 6, 15, 0, '15th June of Financial Year',
 'ITR', FALSE, FALSE, 'ITR_ADVANCE_TAX_PAYMENT'),

(NULL, 'ITR_ADVANCE_TAX_Q2', 'Advance Tax Installment 2 (45%)', 'Advance Tax Installment 2 (45%)', 'OTHER', 'INCOME_TAX', 'QUARTERLY', 'FINANCIAL_YEAR', 'ACTIVE', TRUE, TRUE,
 'Second installment of advance tax (minimum 45% cumulative of estimated tax liability).',
 'Income Tax Act, 1961', 'Section 208 / 211', 'Challan 280', 'Interest @ 1% per month under Section 234C',
 'FIXED_DATE_IN_YEAR', 15, 9, 15, 0, '15th September of Financial Year',
 'ITR', FALSE, FALSE, 'ITR_ADVANCE_TAX_PAYMENT'),

(NULL, 'ITR_ADVANCE_TAX_Q3', 'Advance Tax Installment 3 (75%)', 'Advance Tax Installment 3 (75%)', 'OTHER', 'INCOME_TAX', 'QUARTERLY', 'FINANCIAL_YEAR', 'ACTIVE', TRUE, TRUE,
 'Third installment of advance tax (minimum 75% cumulative of estimated tax liability).',
 'Income Tax Act, 1961', 'Section 208 / 211', 'Challan 280', 'Interest @ 1% per month under Section 234C',
 'FIXED_DATE_IN_YEAR', 15, 12, 15, 0, '15th December of Financial Year',
 'ITR', FALSE, FALSE, 'ITR_ADVANCE_TAX_PAYMENT'),

(NULL, 'ITR_ADVANCE_TAX_Q4', 'Advance Tax Installment 4 (100%)', 'Advance Tax Installment 4 (100%)', 'OTHER', 'INCOME_TAX', 'QUARTERLY', 'FINANCIAL_YEAR', 'ACTIVE', TRUE, TRUE,
 'Final installment of advance tax (100% of estimated tax liability).',
 'Income Tax Act, 1961', 'Section 208 / 211', 'Challan 280', 'Interest under Section 234B & 234C',
 'FIXED_DATE_IN_YEAR', 15, 3, 15, 0, '15th March of Financial Year',
 'ITR', FALSE, FALSE, 'ITR_ADVANCE_TAX_PAYMENT')
ON CONFLICT (rule_code) WHERE organization_id IS NULL
DO UPDATE SET
    name = EXCLUDED.name,
    rule_name = EXCLUDED.rule_name,
    domain = EXCLUDED.domain,
    frequency = EXCLUDED.frequency,
    period_type = EXCLUDED.period_type,
    status = EXCLUDED.status,
    description = EXCLUDED.description,
    statutory_act = EXCLUDED.statutory_act,
    statutory_section = EXCLUDED.statutory_section,
    statutory_form_code = EXCLUDED.statutory_form_code,
    penalty_details = EXCLUDED.penalty_details,
    due_date_rule_type = EXCLUDED.due_date_rule_type,
    due_day = EXCLUDED.due_day,
    fixed_month = EXCLUDED.fixed_month,
    fixed_day = EXCLUDED.fixed_day,
    statutory_grace_days = EXCLUDED.statutory_grace_days,
    due_date_description = EXCLUDED.due_date_description,
    required_module = EXCLUDED.required_module,
    requires_tax_audit = EXCLUDED.requires_tax_audit,
    requires_transfer_pricing = EXCLUDED.requires_transfer_pricing,
    default_work_template_code = EXCLUDED.default_work_template_code;

-- Corporate, MCA & Other Statutory Rules
INSERT INTO compliance_rules (
    organization_id, rule_code, name, rule_name, compliance_type, domain, frequency, period_type, status, is_system_rule, is_active,
    description, statutory_act, statutory_section, statutory_form_code, penalty_details,
    due_date_rule_type, due_day, fixed_month, fixed_day, statutory_grace_days, due_date_description,
    required_module, requires_mca_filing, default_work_template_code
) VALUES
(NULL, 'MCA_AOC4_ANNUAL', 'MCA Form AOC-4 Financial Statements Filing', 'MCA Form AOC-4 Financial Statements Filing', 'OTHER', 'MCA_ROC', 'ANNUAL', 'FINANCIAL_YEAR', 'ACTIVE', TRUE, TRUE,
 'Filing of audited financial statements, director report, and auditor report with ROC.',
 'Companies Act, 2013', 'Section 137', 'AOC-4 / AOC-4 XBRL', 'Additional fee of ₹100 per day of delay under Section 403',
 'FIXED_DATE_IN_YEAR', 29, 10, 29, 0, 'Within 30 days of AGM (typically 29th October)',
 'CLIENTS', TRUE, 'MCA_AOC4_FILING'),

(NULL, 'MCA_MGT7_ANNUAL', 'MCA Form MGT-7/7A Annual Return', 'MCA Form MGT-7/7A Annual Return', 'OTHER', 'MCA_ROC', 'ANNUAL', 'FINANCIAL_YEAR', 'ACTIVE', TRUE, TRUE,
 'Filing of annual return of company capital, governance, and shareholding with ROC.',
 'Companies Act, 2013', 'Section 92', 'MGT-7 / MGT-7A', 'Additional fee of ₹100 per day of delay under Section 403',
 'FIXED_DATE_IN_YEAR', 29, 11, 29, 0, 'Within 60 days of AGM (typically 29th November)',
 'CLIENTS', TRUE, 'MCA_MGT7_FILING'),

(NULL, 'MCA_DIR3_KYC_ANNUAL', 'Director Annual KYC DIR-3 KYC', 'Director Annual KYC DIR-3 KYC', 'OTHER', 'MCA_ROC', 'ANNUAL', 'FINANCIAL_YEAR', 'ACTIVE', TRUE, TRUE,
 'Annual KYC verification for all individuals holding a Director Identification Number (DIN).',
 'Companies (Appointment and Qualification of Directors) Rules, 2014', 'Rule 12A', 'DIR-3 KYC / Web', 'DIN deactivation + ₹5,000 late fee for reactivation',
 'FIXED_DATE_IN_YEAR', 30, 9, 30, 0, '30th September of Financial Year',
 'CLIENTS', TRUE, 'MCA_DIR3_KYC_VERIFICATION'),

(NULL, 'STATUTORY_AUDIT_COMPANIES_ACT', 'Statutory Company Audit under Companies Act', 'Statutory Company Audit under Companies Act', 'OTHER', 'STATUTORY_AUDIT', 'ANNUAL', 'FINANCIAL_YEAR', 'ACTIVE', TRUE, TRUE,
 'Preparation and finalization of independent statutory auditor report for incorporation.',
 'Companies Act, 2013', 'Section 139 / 143', 'Audited Financials', 'Statutory non-compliance penalty under Companies Act',
 'FIXED_DATE_IN_YEAR', 15, 9, 15, 0, '15th September (before AGM notice issuance)',
 'CLIENTS', TRUE, 'STATUTORY_AUDIT_FINALIZATION'),

(NULL, 'PAYROLL_PF_ECR_MONTHLY', 'Monthly EPFO Electronic Challan cum Return', 'Monthly EPFO Electronic Challan cum Return', 'OTHER', 'PAYROLL_LABOUR', 'MONTHLY', 'MONTH', 'ACTIVE', TRUE, TRUE,
 'Monthly electronic filing of PF contribution and wage details for eligible employees.',
 'Employees Provident Funds and Miscellaneous Provisions Act, 1952', 'Section 6 / Scheme Paragraph 38', 'ECR Challan', 'Damages u/s 14B + interest u/s 7Q @ 12% p.a.',
 'DAY_OF_FOLLOWING_MONTH', 15, 15, 1, 0, '15th of following month',
 'CLIENTS', FALSE, 'PAYROLL_PF_ECR_FILING'),

(NULL, 'PAYROLL_ESIC_MONTHLY', 'Monthly ESIC Contribution & Return', 'Monthly ESIC Contribution & Return', 'OTHER', 'PAYROLL_LABOUR', 'MONTHLY', 'MONTH', 'ACTIVE', TRUE, TRUE,
 'Monthly payment of Employee State Insurance contribution for covered establishments.',
 'Employees State Insurance Act, 1948', 'Section 39 / Regulation 31', 'ESIC Monthly Challan', 'Interest @ 12% p.a. for delay + damages under Section 85B',
 'DAY_OF_FOLLOWING_MONTH', 15, 15, 1, 0, '15th of following month',
 'CLIENTS', FALSE, 'PAYROLL_ESIC_PAYMENT'),

(NULL, 'PAYROLL_PT_MONTHLY', 'Monthly State Professional Tax (PT) Challan', 'Monthly State Professional Tax (PT) Challan', 'OTHER', 'PAYROLL_LABOUR', 'MONTHLY', 'MONTH', 'ACTIVE', TRUE, TRUE,
 'Monthly deduction and deposit of state professional tax on employee salaries.',
 'State Professional Tax Acts (e.g. Maharashtra PT Act 1975)', 'Section 6', 'PT Challan', 'Interest @ 1.25% per month + penalty up to statutory limit',
 'DAY_OF_FOLLOWING_MONTH', 30, 30, 1, 0, 'Last day of following month',
 'CLIENTS', FALSE, 'PAYROLL_PT_PAYMENT')
ON CONFLICT (rule_code) WHERE organization_id IS NULL
DO UPDATE SET
    name = EXCLUDED.name,
    rule_name = EXCLUDED.rule_name,
    domain = EXCLUDED.domain,
    frequency = EXCLUDED.frequency,
    period_type = EXCLUDED.period_type,
    status = EXCLUDED.status,
    description = EXCLUDED.description,
    statutory_act = EXCLUDED.statutory_act,
    statutory_section = EXCLUDED.statutory_section,
    statutory_form_code = EXCLUDED.statutory_form_code,
    penalty_details = EXCLUDED.penalty_details,
    due_date_rule_type = EXCLUDED.due_date_rule_type,
    due_day = EXCLUDED.due_day,
    fixed_month = EXCLUDED.fixed_month,
    fixed_day = EXCLUDED.fixed_day,
    statutory_grace_days = EXCLUDED.statutory_grace_days,
    due_date_description = EXCLUDED.due_date_description,
    required_module = EXCLUDED.required_module,
    requires_mca_filing = EXCLUDED.requires_mca_filing,
    default_work_template_code = EXCLUDED.default_work_template_code;
