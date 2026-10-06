-- =====================================================================
-- V136: Compliance Work Generation
-- Phase 29.7 — Obligation → Work Generation
-- =====================================================================
-- 1. Add compliance_obligation_id to work_instances for traceability
-- 2. Add unique constraint to prevent duplicate work per obligation
-- 3. Add work_instance_id back-reference on compliance_obligations
-- 4. Update defaultWorkTemplateCode in compliance_rules to match actual codes
-- =====================================================================

-- 1. Add compliance_obligation_id to work_instances
ALTER TABLE work_instances
    ADD COLUMN IF NOT EXISTS compliance_obligation_id UUID;

-- 2. Unique constraint: one work instance per compliance obligation per org
CREATE UNIQUE INDEX IF NOT EXISTS uq_work_instance_compliance_obligation
    ON work_instances (organization_id, compliance_obligation_id)
    WHERE compliance_obligation_id IS NOT NULL;

-- Index for lookup
CREATE INDEX IF NOT EXISTS idx_work_instances_compliance_obligation_id
    ON work_instances (compliance_obligation_id)
    WHERE compliance_obligation_id IS NOT NULL;

-- 3. Add work_instance_id back-reference to compliance_obligations (reverse traceability)
ALTER TABLE compliance_obligations
    ADD COLUMN IF NOT EXISTS work_instance_id UUID;

CREATE INDEX IF NOT EXISTS idx_compliance_obligations_work_instance_id
    ON compliance_obligations (work_instance_id)
    WHERE work_instance_id IS NOT NULL;

-- =====================================================================
-- 4. Update default_work_template_code in compliance_rules
--    Map compliance rule codes to actual work template codes
--    Existing templates: GST_MONTHLY_COMPLIANCE, TDS_QUARTERLY_COMPLIANCE,
--                        ITR_ANNUAL_FILING, TAX_AUDIT_ANNUAL
-- =====================================================================

-- GST Monthly rules → GST_MONTHLY_COMPLIANCE
UPDATE compliance_rules
SET    default_work_template_code = 'GST_MONTHLY_COMPLIANCE'
WHERE  rule_code IN (
    'GST_GSTR3B_MONTHLY',
    'GST_GSTR3B_QUARTERLY',
    'GST_GSTR1_MONTHLY',
    'GST_GSTR1_QUARTERLY',
    'GST_GSTR1A_MONTHLY',
    'GST_GSTR1A_QUARTERLY',
    'GST_GSTR2B_MONTHLY',
    'GST_GSTR2B_QUARTERLY',
    'GST_PMT06_QUARTERLY'
)
  AND (default_work_template_code IS NULL
       OR default_work_template_code NOT IN (
           'GST_MONTHLY_COMPLIANCE','TDS_QUARTERLY_COMPLIANCE',
           'ITR_ANNUAL_FILING','TAX_AUDIT_ANNUAL'
       ));

-- GST Annual rules → GST_MONTHLY_COMPLIANCE (same template family)
UPDATE compliance_rules
SET    default_work_template_code = 'GST_MONTHLY_COMPLIANCE'
WHERE  rule_code IN (
    'GST_GSTR9_ANNUAL',
    'GST_GSTR9C_ANNUAL'
)
  AND (default_work_template_code IS NULL
       OR default_work_template_code NOT IN (
           'GST_MONTHLY_COMPLIANCE','TDS_QUARTERLY_COMPLIANCE',
           'ITR_ANNUAL_FILING','TAX_AUDIT_ANNUAL'
       ));

-- TDS rules → TDS_QUARTERLY_COMPLIANCE
UPDATE compliance_rules
SET    default_work_template_code = 'TDS_QUARTERLY_COMPLIANCE'
WHERE  rule_code IN (
    'TDS_24Q_QUARTERLY',
    'TDS_26Q_QUARTERLY',
    'TDS_27Q_QUARTERLY',
    'TDS_27EQ_QUARTERLY',
    'TDS_26QB_MONTHLY',
    'TDS_26QC_MONTHLY',
    'TDS_FORM16_ANNUAL',
    'TDS_FORM16A_QUARTERLY'
)
  AND (default_work_template_code IS NULL
       OR default_work_template_code NOT IN (
           'GST_MONTHLY_COMPLIANCE','TDS_QUARTERLY_COMPLIANCE',
           'ITR_ANNUAL_FILING','TAX_AUDIT_ANNUAL'
       ));

-- ITR rules → ITR_ANNUAL_FILING
UPDATE compliance_rules
SET    default_work_template_code = 'ITR_ANNUAL_FILING'
WHERE  rule_code IN (
    'ITR_INDIVIDUAL_ANNUAL',
    'ITR_COMPANY_ANNUAL',
    'ITR_LLP_ANNUAL',
    'ITR_TRUST_ANNUAL',
    'ITR_PARTNERSHIP_ANNUAL'
)
  AND (default_work_template_code IS NULL
       OR default_work_template_code NOT IN (
           'GST_MONTHLY_COMPLIANCE','TDS_QUARTERLY_COMPLIANCE',
           'ITR_ANNUAL_FILING','TAX_AUDIT_ANNUAL'
       ));

-- Tax Audit / ROC rules → TAX_AUDIT_ANNUAL
UPDATE compliance_rules
SET    default_work_template_code = 'TAX_AUDIT_ANNUAL'
WHERE  rule_code IN (
    'TAX_AUDIT_3CA_3CB',
    'TAX_AUDIT_3CD',
    'ROC_AOC4_ANNUAL',
    'ROC_MGT7_ANNUAL',
    'ROC_MGT7A_ANNUAL',
    'ROC_DIR3KYC_ANNUAL',
    'ROC_INC20A_DECLARATION'
)
  AND (default_work_template_code IS NULL
       OR default_work_template_code NOT IN (
           'GST_MONTHLY_COMPLIANCE','TDS_QUARTERLY_COMPLIANCE',
           'ITR_ANNUAL_FILING','TAX_AUDIT_ANNUAL'
       ));

-- =====================================================================
-- Comments
-- =====================================================================
COMMENT ON COLUMN work_instances.compliance_obligation_id
    IS 'UUID reference (no FK) to compliance_obligations.id. Nullable — set only for compliance-generated work instances.';

COMMENT ON COLUMN compliance_obligations.work_instance_id
    IS 'UUID reference (no FK) to work_instances.id. Set when work has been generated for this obligation.';
