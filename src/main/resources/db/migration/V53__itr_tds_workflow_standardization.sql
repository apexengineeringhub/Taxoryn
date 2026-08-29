-- ==============================================================================
-- Taxoryn Platform - Phase 2 Migration (V53)
-- Complete ITR & TDS Workflow Standardization Linkages: Compliance, Tasks, & Document Requests
-- ==============================================================================

-- 1. Add bidirectional relationship columns to itr_returns
ALTER TABLE itr_returns
    ADD COLUMN IF NOT EXISTS compliance_id UUID REFERENCES compliance_obligations(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS task_id UUID REFERENCES tasks(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS document_request_id UUID REFERENCES document_requests(id) ON DELETE SET NULL;

-- 2. Add bidirectional relationship columns to tds_returns
ALTER TABLE tds_returns
    ADD COLUMN IF NOT EXISTS compliance_id UUID REFERENCES compliance_obligations(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS task_id UUID REFERENCES tasks(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS document_request_id UUID REFERENCES document_requests(id) ON DELETE SET NULL;

-- 3. Add reverse linkage columns to compliance_obligations
ALTER TABLE compliance_obligations
    ADD COLUMN IF NOT EXISTS itr_return_id UUID REFERENCES itr_returns(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS tds_return_id UUID REFERENCES tds_returns(id) ON DELETE SET NULL;

-- 4. Add reverse linkage columns to tasks
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS itr_return_id UUID REFERENCES itr_returns(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS tds_return_id UUID REFERENCES tds_returns(id) ON DELETE SET NULL;

-- 5. Add reverse linkage columns to document_requests
ALTER TABLE document_requests
    ADD COLUMN IF NOT EXISTS itr_return_id UUID REFERENCES itr_returns(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS tds_return_id UUID REFERENCES tds_returns(id) ON DELETE SET NULL;

-- 6. Performance and foreign key indexes for ITR & TDS
CREATE INDEX IF NOT EXISTS idx_itr_returns_compliance ON itr_returns(compliance_id);
CREATE INDEX IF NOT EXISTS idx_itr_returns_task ON itr_returns(task_id);
CREATE INDEX IF NOT EXISTS idx_itr_returns_doc_request ON itr_returns(document_request_id);
CREATE INDEX IF NOT EXISTS idx_compliance_itr_return ON compliance_obligations(itr_return_id);
CREATE INDEX IF NOT EXISTS idx_tasks_itr_return ON tasks(itr_return_id);
CREATE INDEX IF NOT EXISTS idx_doc_requests_itr_return ON document_requests(itr_return_id);
CREATE INDEX IF NOT EXISTS idx_itr_returns_org_status_due ON itr_returns(organization_id, status, due_date);

CREATE INDEX IF NOT EXISTS idx_tds_returns_compliance ON tds_returns(compliance_id);
CREATE INDEX IF NOT EXISTS idx_tds_returns_task ON tds_returns(task_id);
CREATE INDEX IF NOT EXISTS idx_tds_returns_doc_request ON tds_returns(document_request_id);
CREATE INDEX IF NOT EXISTS idx_compliance_tds_return ON compliance_obligations(tds_return_id);
CREATE INDEX IF NOT EXISTS idx_tasks_tds_return ON tasks(tds_return_id);
CREATE INDEX IF NOT EXISTS idx_doc_requests_tds_return ON document_requests(tds_return_id);
CREATE INDEX IF NOT EXISTS idx_tds_returns_org_status_due ON tds_returns(organization_id, filing_status, due_date);
