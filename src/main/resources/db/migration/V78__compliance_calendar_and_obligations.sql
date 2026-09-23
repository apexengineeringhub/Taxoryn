-- ==============================================================================
-- Taxoryn Platform - Phase 14 Migration (V78)
-- Compliance Calendar & Recurring Compliance Cycle Foundation
-- ==============================================================================

-- 1. Upgrade Compliance Obligations Table
ALTER TABLE compliance_obligations
    ADD COLUMN IF NOT EXISTS client_service_id UUID,
    ADD COLUMN IF NOT EXISTS service_period_id UUID,
    ADD COLUMN IF NOT EXISTS workflow_id UUID,
    ADD COLUMN IF NOT EXISTS obligation_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS statutory_due_date DATE,
    ADD COLUMN IF NOT EXISTS internal_target_date DATE,
    ADD COLUMN IF NOT EXISTS period_label VARCHAR(100),
    ADD COLUMN IF NOT EXISTS financial_year VARCHAR(20),
    ADD COLUMN IF NOT EXISTS assessment_year VARCHAR(20),
    ADD COLUMN IF NOT EXISTS description TEXT;

-- Backfill new columns from legacy data where applicable
UPDATE compliance_obligations
SET statutory_due_date = due_date
WHERE statutory_due_date IS NULL AND due_date IS NOT NULL;

UPDATE compliance_obligations
SET period_label = period
WHERE period_label IS NULL AND period IS NOT NULL;

UPDATE compliance_obligations
SET obligation_type = CASE
    WHEN compliance_type = 'GST' THEN 'GST_RETURN'
    WHEN compliance_type = 'ITR' THEN 'ITR_FILING'
    WHEN compliance_type = 'TDS' THEN 'TDS_RETURN'
    WHEN compliance_type = 'ROC' THEN 'ROC_COMPLIANCE'
    WHEN compliance_type = 'ADVANCE_TAX' THEN 'ADVANCE_TAX'
    ELSE 'OTHER'
END
WHERE obligation_type IS NULL;

-- Make statutory_due_date NOT NULL for future safety after backfill
ALTER TABLE compliance_obligations ALTER COLUMN statutory_due_date SET NOT NULL;

-- Foreign Keys for Compliance Obligations
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_client_service'
    ) THEN
        ALTER TABLE compliance_obligations
            ADD CONSTRAINT fk_compliance_client_service
            FOREIGN KEY (client_service_id) REFERENCES client_services(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_service_period'
    ) THEN
        ALTER TABLE compliance_obligations
            ADD CONSTRAINT fk_compliance_service_period
            FOREIGN KEY (service_period_id) REFERENCES client_service_periods(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_workflow'
    ) THEN
        ALTER TABLE compliance_obligations
            ADD CONSTRAINT fk_compliance_workflow
            FOREIGN KEY (workflow_id) REFERENCES client_service_workflows(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Indexes for Calendar Queries and Zero-Leakage Performance
CREATE INDEX IF NOT EXISTS idx_compliance_org_statutory_due ON compliance_obligations(organization_id, statutory_due_date);
CREATE INDEX IF NOT EXISTS idx_compliance_org_internal_target ON compliance_obligations(organization_id, internal_target_date);
CREATE INDEX IF NOT EXISTS idx_compliance_client_service ON compliance_obligations(client_service_id);
CREATE INDEX IF NOT EXISTS idx_compliance_service_period ON compliance_obligations(service_period_id);
CREATE INDEX IF NOT EXISTS idx_compliance_workflow ON compliance_obligations(workflow_id);
CREATE INDEX IF NOT EXISTS idx_compliance_obligation_type ON compliance_obligations(obligation_type);

-- Unique constraint index preventing duplicate obligations for the same service period and obligation type
CREATE UNIQUE INDEX IF NOT EXISTS uq_compliance_org_service_period_type 
    ON compliance_obligations(organization_id, client_service_id, service_period_id, obligation_type)
    WHERE client_service_id IS NOT NULL AND service_period_id IS NOT NULL;


-- 2. Create Recurring Compliance Cycle Templates Table
CREATE TABLE IF NOT EXISTS compliance_cycle_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID, -- NULL indicates system-wide default template
    service_type VARCHAR(50) NOT NULL,
    obligation_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    recurrence_type VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    default_due_day INT,
    default_due_month_offset INT NOT NULL DEFAULT 1,
    fixed_due_month INT,
    internal_target_offset_days INT NOT NULL DEFAULT 3,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cycle_tmpl_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cycle_tmpl_org ON compliance_cycle_templates(organization_id);
CREATE INDEX IF NOT EXISTS idx_cycle_tmpl_service_type ON compliance_cycle_templates(service_type);
CREATE INDEX IF NOT EXISTS idx_cycle_tmpl_obligation_type ON compliance_cycle_templates(obligation_type);
CREATE INDEX IF NOT EXISTS idx_cycle_tmpl_recurrence ON compliance_cycle_templates(recurrence_type);

-- Seed System Default Compliance Cycle Templates
INSERT INTO compliance_cycle_templates (id, organization_id, service_type, obligation_type, name, description, recurrence_type, default_due_day, default_due_month_offset, fixed_due_month, internal_target_offset_days, is_active, is_system)
VALUES
    (gen_random_uuid(), NULL, 'GST_COMPLIANCE', 'GST_RETURN', 'GSTR-3B Monthly Compliance Cycle', 'Monthly GSTR-3B summary return and net tax payment due by the 20th of the following month', 'MONTHLY', 20, 1, NULL, 3, TRUE, TRUE),
    (gen_random_uuid(), NULL, 'GST_COMPLIANCE', 'GST_RETURN', 'GSTR-1 Monthly Outward Supplies Cycle', 'Monthly GSTR-1 outward supplies return due by the 11th of the following month', 'MONTHLY', 11, 1, NULL, 2, TRUE, TRUE),
    (gen_random_uuid(), NULL, 'GST_COMPLIANCE', 'GST_RETURN', 'CMP-08 Quarterly Composition Cycle', 'Quarterly self-assessed tax payment statement for composition taxpayers due by the 18th of the month following quarter', 'QUARTERLY', 18, 1, NULL, 3, TRUE, TRUE),
    (gen_random_uuid(), NULL, 'TDS_COMPLIANCE', 'TDS_RETURN', 'TDS Quarterly Return Cycle (Form 24Q/26Q/27Q)', 'Quarterly TDS return filing due by the last day of the month following quarter end', 'QUARTERLY', 31, 1, NULL, 4, TRUE, TRUE),
    (gen_random_uuid(), NULL, 'TDS_COMPLIANCE', 'TDS_RETURN', 'TDS Monthly Challan 281 Deposit Cycle', 'Monthly deposit of tax deducted at source due by 7th of the following month', 'MONTHLY', 7, 1, NULL, 2, TRUE, TRUE),
    (gen_random_uuid(), NULL, 'INCOME_TAX_FILING', 'ITR_FILING', 'Non-Audit Annual ITR Filing Cycle', 'Annual income tax return for individuals, HUFs, and non-audit entities due by 31st July', 'ANNUALLY', 31, 0, 7, 7, TRUE, TRUE),
    (gen_random_uuid(), NULL, 'INCOME_TAX_FILING', 'ITR_FILING', 'Corporate & Tax Audit ITR Filing Cycle', 'Annual income tax return for corporate assesses and tax audit cases due by 31st October', 'ANNUALLY', 31, 0, 10, 10, TRUE, TRUE),
    (gen_random_uuid(), NULL, 'INCOME_TAX_FILING', 'TAX_AUDIT', 'Tax Audit Report Form 3CA/3CB-3CD Cycle', 'Tax audit report submission deadline under section 44AB due by 30th September', 'ANNUALLY', 30, 0, 9, 7, TRUE, TRUE),
    (gen_random_uuid(), NULL, 'INCOME_TAX_FILING', 'ADVANCE_TAX', 'Advance Tax Installment 1 (15%)', 'First installment of 15% estimated advance tax due by 15th June', 'QUARTERLY', 15, 0, 6, 3, TRUE, TRUE),
    (gen_random_uuid(), NULL, 'INCOME_TAX_FILING', 'ADVANCE_TAX', 'Advance Tax Installment 2 (45%)', 'Second installment of 45% cumulative advance tax due by 15th September', 'QUARTERLY', 15, 0, 9, 3, TRUE, TRUE),
    (gen_random_uuid(), NULL, 'INCOME_TAX_FILING', 'ADVANCE_TAX', 'Advance Tax Installment 3 (75%)', 'Third installment of 75% cumulative advance tax due by 15th December', 'QUARTERLY', 15, 0, 12, 3, TRUE, TRUE),
    (gen_random_uuid(), NULL, 'INCOME_TAX_FILING', 'ADVANCE_TAX', 'Advance Tax Installment 4 (100%)', 'Fourth and final installment of 100% advance tax due by 15th March', 'QUARTERLY', 15, 0, 3, 3, TRUE, TRUE),
    (gen_random_uuid(), NULL, 'TAX_NOTICE_MANAGEMENT', 'TAX_NOTICE_RESPONSE', 'Tax Notice Statutory Response Cycle', 'Notice-specific statutory response deadline provided in the notice document', 'EVENT_BASED', NULL, 0, NULL, 3, TRUE, TRUE),
    (gen_random_uuid(), NULL, 'TAX_NOTICE_MANAGEMENT', 'TAX_NOTICE_HEARING', 'Tax Notice Personal Hearing Cycle', 'Scheduled hearing or video conference deadline', 'EVENT_BASED', NULL, 0, NULL, 2, TRUE, TRUE)
ON CONFLICT DO NOTHING;


-- 3. Create Compliance Reminder Rules Table
CREATE TABLE IF NOT EXISTS compliance_reminder_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID, -- NULL indicates system-wide default rules
    obligation_type VARCHAR(50) NOT NULL,
    reminder_type VARCHAR(50) NOT NULL,
    days_offset INT NOT NULL, -- negative for days before (e.g., -7, -3, -1), 0 for on-date, positive for post-overdue (e.g., +1)
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_reminder_rule_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_reminder_rule_org ON compliance_reminder_rules(organization_id);
CREATE INDEX IF NOT EXISTS idx_reminder_rule_type ON compliance_reminder_rules(obligation_type);

-- Seed Default Reminder Rules
INSERT INTO compliance_reminder_rules (id, organization_id, obligation_type, reminder_type, days_offset, is_active)
VALUES
    (gen_random_uuid(), NULL, 'GST_RETURN', 'STATUTORY_DEADLINE', -7, TRUE),
    (gen_random_uuid(), NULL, 'GST_RETURN', 'INTERNAL_TARGET', -3, TRUE),
    (gen_random_uuid(), NULL, 'GST_RETURN', 'STATUTORY_DEADLINE', -1, TRUE),
    (gen_random_uuid(), NULL, 'GST_RETURN', 'OVERDUE', 1, TRUE),
    (gen_random_uuid(), NULL, 'ITR_FILING', 'STATUTORY_DEADLINE', -15, TRUE),
    (gen_random_uuid(), NULL, 'ITR_FILING', 'INTERNAL_TARGET', -7, TRUE),
    (gen_random_uuid(), NULL, 'ITR_FILING', 'STATUTORY_DEADLINE', -1, TRUE),
    (gen_random_uuid(), NULL, 'TDS_RETURN', 'STATUTORY_DEADLINE', -7, TRUE),
    (gen_random_uuid(), NULL, 'TDS_RETURN', 'INTERNAL_TARGET', -3, TRUE),
    (gen_random_uuid(), NULL, 'TAX_NOTICE_RESPONSE', 'STATUTORY_DEADLINE', -3, TRUE),
    (gen_random_uuid(), NULL, 'TAX_NOTICE_RESPONSE', 'STATUTORY_DEADLINE', -1, TRUE)
ON CONFLICT DO NOTHING;
