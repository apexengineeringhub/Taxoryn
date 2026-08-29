-- ==============================================================================
-- Taxoryn Platform - Phase 1 Migration (V52)
-- Complete GST End-to-End Workflow Linkages: Compliance, Tasks, & Document Requests
-- ==============================================================================

-- 1. Add bidirectional relationship columns to gst_return_filings
ALTER TABLE gst_return_filings
    ADD COLUMN IF NOT EXISTS compliance_id UUID REFERENCES compliance_obligations(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS task_id UUID REFERENCES tasks(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS document_request_id UUID REFERENCES document_requests(id) ON DELETE SET NULL;

-- 2. Add reverse linkage to compliance_obligations
ALTER TABLE compliance_obligations
    ADD COLUMN IF NOT EXISTS gst_filing_id UUID REFERENCES gst_return_filings(id) ON DELETE SET NULL;

-- 3. Add reverse linkage to tasks
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS gst_filing_id UUID REFERENCES gst_return_filings(id) ON DELETE SET NULL;

-- 4. Add reverse linkage to document_requests
ALTER TABLE document_requests
    ADD COLUMN IF NOT EXISTS gst_filing_id UUID REFERENCES gst_return_filings(id) ON DELETE SET NULL;

-- 5. Performance and foreign key indexes
CREATE INDEX IF NOT EXISTS idx_gst_filings_compliance ON gst_return_filings(compliance_id);
CREATE INDEX IF NOT EXISTS idx_gst_filings_task ON gst_return_filings(task_id);
CREATE INDEX IF NOT EXISTS idx_gst_filings_doc_request ON gst_return_filings(document_request_id);
CREATE INDEX IF NOT EXISTS idx_compliance_gst_filing ON compliance_obligations(gst_filing_id);
CREATE INDEX IF NOT EXISTS idx_tasks_gst_filing ON tasks(gst_filing_id);
CREATE INDEX IF NOT EXISTS idx_doc_requests_gst_filing ON document_requests(gst_filing_id);
CREATE INDEX IF NOT EXISTS idx_gst_filings_org_status_due ON gst_return_filings(organization_id, filing_status, due_date);
