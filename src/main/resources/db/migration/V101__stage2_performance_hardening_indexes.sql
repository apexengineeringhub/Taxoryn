-- ==============================================================================
-- Taxoryn Platform - Stage 2 Performance Hardening Indexes (V101)
-- Multi-Tenant Optimization: Composite Indexes for Dashboard Aggregation,
-- Scoped Search, and High-Frequency Statutory Filters
-- ==============================================================================

-- 1. Clients Scoping and Status Composite Index
CREATE INDEX IF NOT EXISTS idx_clients_org_loc_status ON clients(organization_id, location_id, status);

-- 2. Compliance Obligations Due Date and Scoping Indexes
CREATE INDEX IF NOT EXISTS idx_compliance_org_status_due ON compliance_obligations(organization_id, status, statutory_due_date);
CREATE INDEX IF NOT EXISTS idx_compliance_org_client_status ON compliance_obligations(organization_id, client_id, status);
CREATE INDEX IF NOT EXISTS idx_compliance_org_type ON compliance_obligations(organization_id, obligation_type);

-- 3. Work Items Workload & Deadline Indexes
CREATE INDEX IF NOT EXISTS idx_work_items_org_status_due ON work_items(organization_id, status, due_date);
CREATE INDEX IF NOT EXISTS idx_work_items_org_assignee_status ON work_items(organization_id, assigned_user_id, status);
CREATE INDEX IF NOT EXISTS idx_work_items_org_client_status ON work_items(organization_id, client_id, status);

-- 4. Tasks Scoping & Status Indexes
CREATE INDEX IF NOT EXISTS idx_tasks_org_work_item ON tasks(organization_id, work_item_id);

-- 5. Document Requests Workflow & Client Indexes
CREATE INDEX IF NOT EXISTS idx_doc_requests_org_status_due ON document_requests(organization_id, status, due_date);
CREATE INDEX IF NOT EXISTS idx_doc_requests_org_client_status ON document_requests(organization_id, client_id, status);

-- 6. Tax Notices Demand & Due Date Indexes
CREATE INDEX IF NOT EXISTS idx_tax_notices_org_status_due ON tax_notices(organization_id, status, response_due_date);
CREATE INDEX IF NOT EXISTS idx_tax_notices_org_client_status ON tax_notices(organization_id, client_id, status);

-- 7. Billing Profiles Multi-Tenant Lookup Index
CREATE INDEX IF NOT EXISTS idx_billing_profiles_org_client_active ON billing_profiles(organization_id, client_id, active);
