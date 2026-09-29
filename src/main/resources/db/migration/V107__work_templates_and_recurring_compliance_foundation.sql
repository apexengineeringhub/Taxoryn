-- V107__work_templates_and_recurring_compliance_foundation.sql
-- Stage 2.5 - P0.2: Recurring Compliance & Work Templates Foundation

-- 1. Create Work Templates Table
CREATE TABLE IF NOT EXISTS work_templates (
    id UUID PRIMARY KEY,
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
    template_code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    template_type VARCHAR(50) NOT NULL DEFAULT 'STATUTORY_COMPLIANCE',
    recurrence_type VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    recurrence_interval INT NOT NULL DEFAULT 1,
    day_of_month INT,
    month_of_year INT,
    recurrence_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_system_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Unique template code per organization (or system global default where org is null)
CREATE UNIQUE INDEX IF NOT EXISTS uk_work_templates_org_code 
    ON work_templates (COALESCE(organization_id, '00000000-0000-0000-0000-000000000000'::uuid), template_code);

CREATE INDEX IF NOT EXISTS idx_work_templates_org ON work_templates(organization_id);
CREATE INDEX IF NOT EXISTS idx_work_templates_service ON work_templates(service_id);
CREATE INDEX IF NOT EXISTS idx_work_templates_category ON work_templates(category);
CREATE INDEX IF NOT EXISTS idx_work_templates_status ON work_templates(status);
CREATE INDEX IF NOT EXISTS idx_work_templates_recurrence ON work_templates(recurrence_type);

-- 2. Create Work Template Tasks Table
CREATE TABLE IF NOT EXISTS work_template_tasks (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES work_templates(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sequence_order INT NOT NULL,
    default_assignee_role VARCHAR(100),
    default_priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    relative_due_days INT NOT NULL DEFAULT 0,
    mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_work_template_task_seq UNIQUE (template_id, sequence_order)
);

CREATE INDEX IF NOT EXISTS idx_work_template_tasks_tpl ON work_template_tasks(template_id);
CREATE INDEX IF NOT EXISTS idx_work_template_tasks_seq ON work_template_tasks(template_id, sequence_order);

-- 3. Create Engagement Work Templates Table (Association & Schedule Override)
CREATE TABLE IF NOT EXISTS engagement_work_templates (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    engagement_id UUID NOT NULL REFERENCES engagements(id) ON DELETE CASCADE,
    template_id UUID NOT NULL REFERENCES work_templates(id) ON DELETE RESTRICT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    recurrence_type VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    recurrence_interval INT NOT NULL DEFAULT 1,
    day_of_month INT,
    month_of_year INT,
    start_date DATE,
    end_date DATE,
    last_generated_period_start DATE,
    last_generated_period_end DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_eng_work_template UNIQUE (organization_id, engagement_id, template_id)
);

CREATE INDEX IF NOT EXISTS idx_eng_work_tpl_org ON engagement_work_templates(organization_id);
CREATE INDEX IF NOT EXISTS idx_eng_work_tpl_eng ON engagement_work_templates(engagement_id);
CREATE INDEX IF NOT EXISTS idx_eng_work_tpl_tpl ON engagement_work_templates(template_id);

-- 4. Create Work Instances Table
CREATE TABLE IF NOT EXISTS work_instances (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    engagement_id UUID NOT NULL REFERENCES engagements(id) ON DELETE CASCADE,
    template_id UUID REFERENCES work_templates(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    due_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    assigned_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewer_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_work_instance_period UNIQUE (organization_id, engagement_id, template_id, period_start, period_end)
);

CREATE INDEX IF NOT EXISTS idx_work_instances_org ON work_instances(organization_id);
CREATE INDEX IF NOT EXISTS idx_work_instances_eng ON work_instances(engagement_id);
CREATE INDEX IF NOT EXISTS idx_work_instances_tpl ON work_instances(template_id);
CREATE INDEX IF NOT EXISTS idx_work_instances_status ON work_instances(status);
CREATE INDEX IF NOT EXISTS idx_work_instances_period ON work_instances(period_start, period_end);
CREATE INDEX IF NOT EXISTS idx_work_instances_due ON work_instances(due_date);
CREATE INDEX IF NOT EXISTS idx_work_instances_assigned ON work_instances(assigned_user_id);

-- 5. Extend Existing Tasks Table with Work Instance linkages
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS work_instance_id UUID REFERENCES work_instances(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS work_template_task_id UUID REFERENCES work_template_tasks(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_tasks_work_instance ON tasks(work_instance_id);
CREATE INDEX IF NOT EXISTS idx_tasks_work_template_task ON tasks(work_template_task_id);

-- 6. Seed Standard Default Work Templates & Template Tasks
-- GST Monthly Compliance Template
INSERT INTO work_templates (
    id, organization_id, service_id, template_code, name, description, category,
    status, template_type, recurrence_type, recurrence_interval, recurrence_enabled, is_system_default,
    created_at, updated_at, version
) VALUES (
    'c0000000-0000-0000-0000-000000000001', NULL, 'b0000000-0000-0000-0000-000000000001',
    'GST_MONTHLY_COMPLIANCE', 'GST Monthly Compliance Template',
    'Standard monthly compliance workflow for GSTR-1, GSTR-3B filings and GSTR-2B reconciliation.',
    'GST', 'ACTIVE', 'STATUTORY_COMPLIANCE', 'MONTHLY', 1, TRUE, TRUE,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (COALESCE(organization_id, '00000000-0000-0000-0000-000000000000'::uuid), template_code) DO NOTHING;

INSERT INTO work_template_tasks (
    id, template_id, name, description, sequence_order, default_assignee_role, default_priority, relative_due_days, mandatory, active, version
) VALUES
('c0000000-0000-0000-0000-000000000101', 'c0000000-0000-0000-0000-000000000001', 'Request & Collect Documents', 'Request sales invoices, purchase bills, and e-way bill records from client', 1, 'STAFF', 'MEDIUM', 5, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000102', 'c0000000-0000-0000-0000-000000000001', 'Collect Purchase & Sales Registers', 'Compile and verify outward sales register and inward purchase register', 2, 'STAFF', 'MEDIUM', 8, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000103', 'c0000000-0000-0000-0000-000000000001', 'GSTR-2B ITC Reconciliation & Data Entry', 'Reconcile auto-drafted ITC from 2B with books and identify mismatches', 3, 'STAFF', 'HIGH', 11, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000104', 'c0000000-0000-0000-0000-000000000001', 'Prepare GSTR-1 & GSTR-3B Return', 'Draft computation sheet, compute net tax payable and ITC utilization', 4, 'STAFF', 'HIGH', 14, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000105', 'c0000000-0000-0000-0000-000000000001', 'Senior Partner Review & Liability Check', 'Review output tax, RCM liability, and interest computations', 5, 'PARTNER', 'HIGH', 17, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000106', 'c0000000-0000-0000-0000-000000000001', 'Client Tax Summary & Payment Approval', 'Share computation summary with client and obtain final confirmation', 6, 'MANAGER', 'MEDIUM', 19, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000107', 'c0000000-0000-0000-0000-000000000001', 'Filing on GST Portal', 'File return on the GST portal using DSC/EVC and generate challans', 7, 'STAFF', 'URGENT', 20, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000108', 'c0000000-0000-0000-0000-000000000001', 'ARN Download & Client Acknowledgement', 'Download ARN receipt, archive in documents vault, and send to client', 8, 'STAFF', 'MEDIUM', 20, TRUE, TRUE, 0)
ON CONFLICT (id) DO NOTHING;

-- TDS Quarterly Compliance Template
INSERT INTO work_templates (
    id, organization_id, service_id, template_code, name, description, category,
    status, template_type, recurrence_type, recurrence_interval, recurrence_enabled, is_system_default,
    created_at, updated_at, version
) VALUES (
    'c0000000-0000-0000-0000-000000000002', NULL, 'b0000000-0000-0000-0000-000000000002',
    'TDS_QUARTERLY_COMPLIANCE', 'TDS Quarterly Compliance Template',
    'Standard quarterly compliance workflow for Forms 24Q, 26Q, 27Q preparation, FVU validation, and filing.',
    'TDS', 'ACTIVE', 'STATUTORY_COMPLIANCE', 'QUARTERLY', 1, TRUE, TRUE,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (COALESCE(organization_id, '00000000-0000-0000-0000-000000000000'::uuid), template_code) DO NOTHING;

INSERT INTO work_template_tasks (
    id, template_id, name, description, sequence_order, default_assignee_role, default_priority, relative_due_days, mandatory, active, version
) VALUES
('c0000000-0000-0000-0000-000000000201', 'c0000000-0000-0000-0000-000000000002', 'Collect Deductee Records & Challans', 'Collect salary/vendor deduction records, Section 194 details, and ITNS 281 challans', 1, 'STAFF', 'MEDIUM', 10, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000202', 'c0000000-0000-0000-0000-000000000002', 'TDS Return Compilation & FVU Validation', 'Validate deductee PANs, compile quarterly return, and generate FVU file', 2, 'STAFF', 'HIGH', 20, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000203', 'c0000000-0000-0000-0000-000000000002', 'Short Deduction & Interest Check', 'Review short deductions, interest u/s 201(1A), and late fee calculations', 3, 'MANAGER', 'HIGH', 25, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000204', 'c0000000-0000-0000-0000-000000000002', 'Senior Review & Signoff', 'Partner review of quarterly statement before submission', 4, 'PARTNER', 'HIGH', 28, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000205', 'c0000000-0000-0000-0000-000000000002', 'Portal E-Filing & Provisional Receipt', 'Upload FVU file to ITD portal / TIN-FC and archive token receipt', 5, 'STAFF', 'URGENT', 30, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000206', 'c0000000-0000-0000-0000-000000000002', 'TRACES Form 16/16A Generation & Dispatch', 'Download Form 16/16A from TRACES upon processing and dispatch to client', 6, 'STAFF', 'MEDIUM', 45, TRUE, TRUE, 0)
ON CONFLICT (id) DO NOTHING;

-- ITR Annual Filing Template
INSERT INTO work_templates (
    id, organization_id, service_id, template_code, name, description, category,
    status, template_type, recurrence_type, recurrence_interval, recurrence_enabled, is_system_default,
    created_at, updated_at, version
) VALUES (
    'c0000000-0000-0000-0000-000000000003', NULL, 'b0000000-0000-0000-0000-000000000003',
    'ITR_ANNUAL_FILING', 'ITR Annual Filing Template',
    'Standard annual income tax return filing workflow including computation, partner review, and e-verification.',
    'ITR', 'ACTIVE', 'STATUTORY_COMPLIANCE', 'YEARLY', 1, TRUE, TRUE,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (COALESCE(organization_id, '00000000-0000-0000-0000-000000000000'::uuid), template_code) DO NOTHING;

INSERT INTO work_template_tasks (
    id, template_id, name, description, sequence_order, default_assignee_role, default_priority, relative_due_days, mandatory, active, version
) VALUES
('c0000000-0000-0000-0000-000000000301', 'c0000000-0000-0000-0000-000000000003', 'Collect Financials, AIS/TIS & 26AS', 'Gather P&L, balance sheet, Form 16/16A, and fetch AIS/TIS/26AS from portal', 1, 'STAFF', 'MEDIUM', 15, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000302', 'c0000000-0000-0000-0000-000000000003', 'Draft Computation of Total Income', 'Compute heads of income, Chapter VI-A deductions, and set-off of losses', 2, 'STAFF', 'HIGH', 25, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000303', 'c0000000-0000-0000-0000-000000000003', 'Senior Partner Review & Tax Optimization', 'Verify deductions, capital gains, foreign assets, and final tax payable', 3, 'PARTNER', 'HIGH', 35, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000304', 'c0000000-0000-0000-0000-000000000003', 'Client Confirmation & Self-Assessment Tax', 'Obtain client signoff on computation and verify self-assessment tax payment', 4, 'MANAGER', 'MEDIUM', 40, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000305', 'c0000000-0000-0000-0000-000000000003', 'Income Tax Portal E-Filing & E-Verification', 'Upload JSON return to ITD portal and complete Aadhaar OTP / DSC e-verification', 5, 'STAFF', 'URGENT', 45, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000306', 'c0000000-0000-0000-0000-000000000003', 'ITR-V Acknowledgement Archival', 'Download ITR-V acknowledgement, store in vault, and deliver copy to client', 6, 'STAFF', 'MEDIUM', 45, TRUE, TRUE, 0)
ON CONFLICT (id) DO NOTHING;

-- Tax Audit Annual Work Template
INSERT INTO work_templates (
    id, organization_id, service_id, template_code, name, description, category,
    status, template_type, recurrence_type, recurrence_interval, recurrence_enabled, is_system_default,
    created_at, updated_at, version
) VALUES (
    'c0000000-0000-0000-0000-000000000004', NULL, 'b0000000-0000-0000-0000-000000000004',
    'TAX_AUDIT_ANNUAL', 'Tax Audit Annual Work Template',
    'Standard tax audit finalization workflow for Form 3CA/3CB/3CD u/s 44AB and statutory assurance.',
    'AUDIT', 'ACTIVE', 'STATUTORY_COMPLIANCE', 'YEARLY', 1, TRUE, TRUE,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (COALESCE(organization_id, '00000000-0000-0000-0000-000000000000'::uuid), template_code) DO NOTHING;

INSERT INTO work_template_tasks (
    id, template_id, name, description, sequence_order, default_assignee_role, default_priority, relative_due_days, mandatory, active, version
) VALUES
('c0000000-0000-0000-0000-000000000401', 'c0000000-0000-0000-0000-000000000004', 'Turnover & Audit Applicability Analysis', 'Examine 44AB applicability, cash transaction thresholds, and presumptive limits', 1, 'STAFF', 'MEDIUM', 15, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000402', 'c0000000-0000-0000-0000-000000000004', 'Trial Balance & Ledger Scrutiny', 'Scrutinize ledger accounts, related party transactions, and 40A(2)/40A(3) payments', 2, 'STAFF', 'HIGH', 30, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000403', 'c0000000-0000-0000-0000-000000000004', 'Form 3CD Clause-by-Clause Preparation', 'Prepare Form 3CD clauses (depreciation, 43B statutory dues, TDS compliance, etc.)', 3, 'STAFF', 'HIGH', 45, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000404', 'c0000000-0000-0000-0000-000000000004', 'Senior Partner Assurance Review & Signoff', 'Detailed engagement partner audit review, qualifications, and UDIN generation', 4, 'PARTNER', 'HIGH', 55, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000405', 'c0000000-0000-0000-0000-000000000004', 'Upload Tax Audit Report to ITD Portal', 'Upload Form 3CA/3CB-3CD with DSC on the income tax portal and await client approval', 5, 'PARTNER', 'URGENT', 60, TRUE, TRUE, 0),
('c0000000-0000-0000-0000-000000000406', 'c0000000-0000-0000-0000-000000000004', 'Client Approval & Filing Confirmation', 'Track client portal approval, download final signed report, and archive in vault', 6, 'STAFF', 'MEDIUM', 60, TRUE, TRUE, 0)
ON CONFLICT (id) DO NOTHING;
