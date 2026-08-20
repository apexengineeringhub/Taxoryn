-- ==============================================================================
-- Taxoryn Platform - Production Hardening & High-Performance Indexes Migration (V15)
-- Multi-Tenant Practice Management Modular Monolith
-- ==============================================================================

-- 1. Tasks Optimization Indexes
CREATE INDEX IF NOT EXISTS idx_tasks_org_assigned_status ON tasks(organization_id, assigned_to, status);
CREATE INDEX IF NOT EXISTS idx_tasks_org_due_status ON tasks(organization_id, due_date, status);
CREATE INDEX IF NOT EXISTS idx_tasks_org_client_status ON tasks(organization_id, client_id, status);

-- 2. Invoices & Billing Optimization Indexes
CREATE INDEX IF NOT EXISTS idx_invoices_org_status_due ON invoices(organization_id, status, due_date);
CREATE INDEX IF NOT EXISTS idx_invoices_org_client_date ON invoices(organization_id, client_id, invoice_date DESC);

-- 3. GST Filings Optimization Indexes
CREATE INDEX IF NOT EXISTS idx_gst_filings_org_status_due ON gst_return_filings(organization_id, filing_status, due_date);
CREATE INDEX IF NOT EXISTS idx_gst_filings_org_period ON gst_return_filings(organization_id, return_period, return_type);

-- 4. ITR Returns Optimization Indexes
CREATE INDEX IF NOT EXISTS idx_itr_returns_org_status_due ON itr_returns(organization_id, status, due_date);
CREATE INDEX IF NOT EXISTS idx_itr_returns_org_client_ay ON itr_returns(organization_id, client_id, assessment_year DESC);

-- 5. Clients & Employees Performance Indexes
CREATE INDEX IF NOT EXISTS idx_clients_org_status ON clients(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_employees_org_status ON employees(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_client_contacts_client_id ON client_contacts(client_id);

-- 6. Documents & Notifications Indexes
CREATE INDEX IF NOT EXISTS idx_documents_org_client ON documents(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_notifications_org_user_status ON notifications(organization_id, user_id, status);
