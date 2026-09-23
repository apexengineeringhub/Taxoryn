-- V77__client_service_workflow_operations.sql
-- Phase 13: Client Engagement Operations & Compliance Workflow Foundation

-- 1. Client Service Periods Table
CREATE TABLE IF NOT EXISTS client_service_periods (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    client_service_id UUID NOT NULL REFERENCES client_services(id) ON DELETE CASCADE,
    period_type VARCHAR(50) NOT NULL,
    period_label VARCHAR(100) NOT NULL,
    financial_year VARCHAR(20),
    assessment_year VARCHAR(20),
    start_date DATE,
    end_date DATE,
    due_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_csp_tenant_service_label UNIQUE (organization_id, client_service_id, period_label)
);

CREATE INDEX IF NOT EXISTS idx_csp_tenant_client ON client_service_periods (organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_csp_tenant_service ON client_service_periods (organization_id, client_service_id);
CREATE INDEX IF NOT EXISTS idx_csp_tenant_status ON client_service_periods (organization_id, status);
CREATE INDEX IF NOT EXISTS idx_csp_tenant_due_date ON client_service_periods (organization_id, due_date);

-- 2. Service Workflow Templates Table
CREATE TABLE IF NOT EXISTS service_workflow_templates (
    id UUID PRIMARY KEY,
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    service_type VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_system_default BOOLEAN NOT NULL DEFAULT false,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_swt_tenant_type ON service_workflow_templates (organization_id, service_type);
CREATE INDEX IF NOT EXISTS idx_swt_system_default ON service_workflow_templates (service_type, is_system_default);

-- 3. Service Workflow Step Templates Table
CREATE TABLE IF NOT EXISTS service_workflow_step_templates (
    id UUID PRIMARY KEY,
    workflow_template_id UUID NOT NULL REFERENCES service_workflow_templates(id) ON DELETE CASCADE,
    sequence INT NOT NULL,
    work_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    default_days_before_due_date INT,
    mandatory BOOLEAN NOT NULL DEFAULT true,
    requires_client_input BOOLEAN NOT NULL DEFAULT false,
    requires_review BOOLEAN NOT NULL DEFAULT false,
    active BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uk_swst_template_sequence UNIQUE (workflow_template_id, sequence)
);

CREATE INDEX IF NOT EXISTS idx_swst_template ON service_workflow_step_templates (workflow_template_id);

-- 4. Client Service Workflows Table (Operational Workflow Instance)
CREATE TABLE IF NOT EXISTS client_service_workflows (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    client_service_id UUID NOT NULL REFERENCES client_services(id) ON DELETE CASCADE,
    period_id UUID NOT NULL REFERENCES client_service_periods(id) ON DELETE CASCADE,
    template_id UUID REFERENCES service_workflow_templates(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    assigned_employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    current_step_sequence INT NOT NULL DEFAULT 1,
    total_steps INT NOT NULL DEFAULT 0,
    completed_steps INT NOT NULL DEFAULT 0,
    due_date DATE,
    internal_target_date DATE,
    waiting_for_client BOOLEAN NOT NULL DEFAULT false,
    pending_client_action_summary VARCHAR(500),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_csw_tenant_service_period UNIQUE (organization_id, client_service_id, period_id)
);

CREATE INDEX IF NOT EXISTS idx_csw_tenant_client ON client_service_workflows (organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_csw_tenant_service ON client_service_workflows (organization_id, client_service_id);
CREATE INDEX IF NOT EXISTS idx_csw_tenant_period ON client_service_workflows (organization_id, period_id);
CREATE INDEX IF NOT EXISTS idx_csw_tenant_status ON client_service_workflows (organization_id, status);
CREATE INDEX IF NOT EXISTS idx_csw_tenant_assignee ON client_service_workflows (organization_id, assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_csw_tenant_due_date ON client_service_workflows (organization_id, due_date);
CREATE INDEX IF NOT EXISTS idx_csw_tenant_waiting ON client_service_workflows (organization_id, waiting_for_client);

-- 5. Client Service Workflow Steps Table (Instantiated Steps)
CREATE TABLE IF NOT EXISTS client_service_workflow_steps (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    workflow_id UUID NOT NULL REFERENCES client_service_workflows(id) ON DELETE CASCADE,
    sequence INT NOT NULL,
    work_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    assigned_employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    task_id UUID REFERENCES tasks(id) ON DELETE SET NULL,
    due_date DATE,
    mandatory BOOLEAN NOT NULL DEFAULT true,
    requires_client_input BOOLEAN NOT NULL DEFAULT false,
    requires_review BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMP WITH TIME ZONE,
    completed_by UUID,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_csws_workflow_sequence UNIQUE (workflow_id, sequence)
);

CREATE INDEX IF NOT EXISTS idx_csws_tenant_workflow ON client_service_workflow_steps (organization_id, workflow_id);
CREATE INDEX IF NOT EXISTS idx_csws_tenant_assignee ON client_service_workflow_steps (organization_id, assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_csws_tenant_status ON client_service_workflow_steps (organization_id, status);

-- 6. Link Tasks to Service Workflow Steps if column does not exist
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS service_workflow_id UUID REFERENCES client_service_workflows(id) ON DELETE SET NULL;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS service_workflow_step_id UUID REFERENCES client_service_workflow_steps(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_tasks_service_workflow ON tasks (service_workflow_id);
CREATE INDEX IF NOT EXISTS idx_tasks_service_workflow_step ON tasks (service_workflow_step_id);

-- 7. Seed Default System Templates
-- GST Compliance Template
INSERT INTO service_workflow_templates (id, organization_id, service_type, name, description, is_system_default, active, created_at, updated_at, version)
VALUES ('00000000-0000-0000-0001-000000000001', NULL, 'GST_COMPLIANCE', 'Standard GST Monthly Compliance Workflow', 'Standard 8-step internal practice workflow for monthly GSTR-1 and GSTR-3B filings', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO service_workflow_step_templates (id, workflow_template_id, sequence, work_type, name, description, default_days_before_due_date, mandatory, requires_client_input, requires_review, active)
VALUES 
('00000000-0000-0000-0001-000000000101', '00000000-0000-0000-0001-000000000001', 1, 'DATA_COLLECTION', 'Sales & Purchase Invoices Collection', 'Collect sales invoices, purchase bills, and e-way bill records from client', 10, true, true, false, true),
('00000000-0000-0000-0001-000000000102', '00000000-0000-0000-0001-000000000001', 2, 'PREPARATION', 'GSTR-2B ITC Reconciliation & Data Entry', 'Reconcile auto-drafted ITC from 2B with purchase register and prepare return sheets', 7, true, false, false, true),
('00000000-0000-0000-0001-000000000103', '00000000-0000-0000-0001-000000000001', 3, 'REVIEW', 'Senior Tax Review & Liability Check', 'Review output tax computation, ITC eligibility, RCM liability, and interest computations', 5, true, false, true, true),
('00000000-0000-0000-0001-000000000104', '00000000-0000-0000-0001-000000000001', 4, 'CLIENT_CONFIRMATION', 'Client Tax Computation & Challan Approval', 'Dispatch tax liability summary to client and obtain final payment / filing confirmation', 3, true, true, false, true),
('00000000-0000-0000-0001-000000000105', '00000000-0000-0000-0001-000000000001', 5, 'FILING_PREPARATION', 'Challan Payment Verification & JSON Generation', 'Verify PMT-06 tax payment challan and generate GST portal upload payloads', 2, true, false, false, true),
('00000000-0000-0000-0001-000000000106', '00000000-0000-0000-0001-000000000001', 6, 'FILING', 'GST Portal E-Filing via DSC/EVC', 'File return on the GSTN portal with Digital Signature Certificate or OTP/EVC verification', 0, true, false, false, true),
('00000000-0000-0000-0001-000000000107', '00000000-0000-0000-0001-000000000001', 7, 'ACKNOWLEDGEMENT', 'ARN Archival & Client Dispatch', 'Download ARN receipt, archive in client document vault, and send acknowledgement email to client', 0, true, false, false, true),
('00000000-0000-0000-0001-000000000108', '00000000-0000-0000-0001-000000000001', 8, 'COMPLETION', 'Monthly Engagement Period Closure', 'Close monthly compliance period and update client compliance ledger', 0, true, false, false, true)
ON CONFLICT (id) DO NOTHING;

-- ITR Compliance Template
INSERT INTO service_workflow_templates (id, organization_id, service_type, name, description, is_system_default, active, created_at, updated_at, version)
VALUES ('00000000-0000-0000-0001-000000000002', NULL, 'ITR_COMPLIANCE', 'Standard Income Tax Return Filing Workflow', 'Standard 7-step internal practice workflow for annual direct tax computation and return filing', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO service_workflow_step_templates (id, workflow_template_id, sequence, work_type, name, description, default_days_before_due_date, mandatory, requires_client_input, requires_review, active)
VALUES 
('00000000-0000-0000-0001-000000000201', '00000000-0000-0000-0001-000000000002', 1, 'DOCUMENT_COLLECTION', 'Financials, AIS/TIS & 26AS Gathering', 'Collect P&L, balance sheet, Form 16/16A, bank statements, and fetch latest AIS/TIS and Form 26AS', 15, true, true, false, true),
('00000000-0000-0000-0001-000000000202', '00000000-0000-0000-0001-000000000002', 2, 'COMPUTATION', 'Computation of Total Income & Tax', 'Draft heads of income, chapter VI-A deductions, set-off of losses, and rebate/surcharge computation', 10, true, false, false, true),
('00000000-0000-0000-0001-000000000203', '00000000-0000-0000-0001-000000000002', 3, 'REVIEW', 'Senior Partner Review & Tax Optimization', 'Verify tax deductions, foreign asset disclosures, capital gains schedule, and tax audit applicability', 7, true, false, true, true),
('00000000-0000-0000-0001-000000000204', '00000000-0000-0000-0001-000000000002', 4, 'CLIENT_CONFIRMATION', 'Draft Computation Approval & Self-Assessment Tax', 'Share draft computation sheet with taxpayer client and collect self-assessment tax challan', 4, true, true, false, true),
('00000000-0000-0000-0001-000000000205', '00000000-0000-0000-0001-000000000002', 5, 'FILING', 'Income Tax Portal E-Filing', 'Upload XML/JSON to ITD e-Filing portal and trigger e-verification (Aadhaar OTP / DSC)', 0, true, false, false, true),
('00000000-0000-0000-0001-000000000206', '00000000-0000-0000-0001-000000000002', 6, 'ACKNOWLEDGEMENT', 'ITR-V Receipt Archival & Client Dispatch', 'Download ITR-V acknowledgement, store in document vault, and deliver copy to taxpayer', 0, true, false, false, true),
('00000000-0000-0000-0001-000000000207', '00000000-0000-0000-0001-000000000002', 7, 'COMPLETION', 'Assessment Year Engagement Closure', 'Complete engagement milestone and schedule CPC intimation tracking', 0, true, false, false, true)
ON CONFLICT (id) DO NOTHING;

-- TDS Compliance Template
INSERT INTO service_workflow_templates (id, organization_id, service_type, name, description, is_system_default, active, created_at, updated_at, version)
VALUES ('00000000-0000-0000-0001-000000000003', NULL, 'TDS_COMPLIANCE', 'Standard Quarterly TDS Compliance Workflow', 'Standard 6-step internal practice workflow for quarterly Forms 24Q, 26Q, 27Q preparation and filing', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO service_workflow_step_templates (id, workflow_template_id, sequence, work_type, name, description, default_days_before_due_date, mandatory, requires_client_input, requires_review, active)
VALUES 
('00000000-0000-0000-0001-000000000301', '00000000-0000-0000-0001-000000000003', 1, 'DATA_COLLECTION', 'Deductee Records & Challan Gathering', 'Collect salary/vendor deduction records, Section 194 breakdown, and BSR-coded ITNS 281 payment challans', 12, true, true, false, true),
('00000000-0000-0000-0001-000000000302', '00000000-0000-0000-0001-000000000003', 2, 'PREPARATION', 'TDS Return Compilation & FVU Validation', 'Match CSI files, validate deductee PANs, compile quarterly return, and run NSDL RPU/FVU validation', 7, true, false, false, true),
('00000000-0000-0000-0001-000000000303', '00000000-0000-0000-0001-000000000003', 3, 'REVIEW', 'Short Deduction & Interest Check', 'Review short deduction reports, late filing fees u/s 234E, and interest liabilities u/s 201(1A)', 4, true, false, true, true),
('00000000-0000-0000-0001-000000000304', '00000000-0000-0000-0001-000000000003', 4, 'FILING', 'TIN-FC Upload / Portal E-Filing', 'Upload FVU file to ITD portal or submit to TIN-FC counter', 0, true, false, false, true),
('00000000-0000-0000-0001-000000000305', '00000000-0000-0000-0001-000000000003', 5, 'ACKNOWLEDGEMENT', 'Provisional Receipt & Form 16/16A Generation', 'Download TRACES Form 16/16A certificates upon return processing and dispatch to client', 0, true, false, false, true),
('00000000-0000-0000-0001-000000000306', '00000000-0000-0000-0001-000000000003', 6, 'COMPLETION', 'Quarterly TDS Period Closure', 'Close quarterly cycle and archive return records', 0, true, false, false, true)
ON CONFLICT (id) DO NOTHING;

-- Tax Notice Management Template
INSERT INTO service_workflow_templates (id, organization_id, service_type, name, description, is_system_default, active, created_at, updated_at, version)
VALUES ('00000000-0000-0000-0001-000000000004', NULL, 'TAX_NOTICE_MANAGEMENT', 'Standard Tax Notice Assessment Representation Workflow', 'Standard 9-step internal practice workflow for handling statutory tax notices and hearings', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO service_workflow_step_templates (id, workflow_template_id, sequence, work_type, name, description, default_days_before_due_date, mandatory, requires_client_input, requires_review, active)
VALUES 
('00000000-0000-0000-0001-000000000401', '00000000-0000-0000-0001-000000000004', 1, 'PREPARATION', 'Notice Ingestion & Statutory Analysis', 'Extract statutory section, DIN, issuing authority, and examine allegations in notice', 15, true, false, false, true),
('00000000-0000-0000-0001-000000000402', '00000000-0000-0000-0001-000000000004', 2, 'REVIEW', 'Notice Strategy & Legal Precedents Review', 'Formulate defense strategy, identify applicable case laws, and assess limitation periods', 12, true, false, true, true),
('00000000-0000-0000-0001-000000000403', '00000000-0000-0000-0001-000000000004', 3, 'DOCUMENT_COLLECTION', 'Evidentiary Documentation Gathering', 'Collect supporting ledgers, agreements, confirmations, and bank statements from client', 10, true, true, false, true),
('00000000-0000-0000-0001-000000000404', '00000000-0000-0000-0001-000000000004', 4, 'PREPARATION', 'Written Response & Annexures Drafting', 'Draft parawise reply, prepare reconciliation annexures, and index exhibit documents', 7, true, false, false, true),
('00000000-0000-0000-0001-000000000405', '00000000-0000-0000-0001-000000000004', 5, 'REVIEW', 'Senior Partner Legal & Technical Signoff', 'Detailed review of written submission by Senior Partner / Advocate', 4, true, false, true, true),
('00000000-0000-0000-0001-000000000406', '00000000-0000-0000-0001-000000000004', 6, 'CLIENT_CONFIRMATION', 'Client Approval of Final Response', 'Obtain written approval / signature on reply draft from client', 2, true, true, false, true),
('00000000-0000-0000-0001-000000000407', '00000000-0000-0000-0001-000000000004', 7, 'FILING', 'Statutory Portal Response Submission', 'Submit response draft and exhibits on the e-Proceedings portal (ITD / GSTN)', 0, true, false, false, true),
('00000000-0000-0000-0001-000000000408', '00000000-0000-0000-0001-000000000004', 8, 'HEARING_FOLLOW_UP', 'Video Hearing / Adjournment Tracking', 'Represent at virtual hearing, submit supplementary submissions or seek adjournment if needed', 0, false, false, false, true),
('00000000-0000-0000-0001-000000000409', '00000000-0000-0000-0001-000000000004', 9, 'COMPLETION', 'Assessment Order Receipt & Case Closure', 'Record final assessment/appeal order, compute demand or refund, and close notice case', 0, true, false, false, true)
ON CONFLICT (id) DO NOTHING;
