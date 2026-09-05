-- ==============================================================================
-- Taxoryn Platform - Migration V58
-- Drop obsolete Hibernate-generated enum CHECK constraints across domain tables
-- ==============================================================================

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT conname, cl.relname 
        FROM pg_constraint c 
        JOIN pg_class cl ON cl.oid = c.conrelid 
        WHERE c.contype = 'c' 
          AND cl.relname IN (
            'notifications',
            'documents',
            'document_requests',
            'document_request_items',
            'client_document_requests',
            'tasks',
            'invoices',
            'invoice_items',
            'gst_return_filings',
            'itr_returns',
            'tds_returns',
            'clients',
            'users',
            'roles',
            'audit_logs'
          )
    ) LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I;', r.relname, r.conname);
    END LOOP;
END $$;

-- Explicit drops as fallback for exact known constraint names
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_notification_type_check;
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_severity_check;
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_category_check;
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_email_status_check;
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_sms_status_check;
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_whatsapp_status_check;

ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_document_type_check;
ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_storage_provider_check;
ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_status_check;
ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_scan_status_check;

ALTER TABLE document_requests DROP CONSTRAINT IF EXISTS document_requests_status_check;

ALTER TABLE document_request_items DROP CONSTRAINT IF EXISTS document_request_items_document_type_check;
ALTER TABLE document_request_items DROP CONSTRAINT IF EXISTS document_request_items_status_check;

ALTER TABLE client_document_requests DROP CONSTRAINT IF EXISTS client_document_requests_document_type_check;
ALTER TABLE client_document_requests DROP CONSTRAINT IF EXISTS client_document_requests_status_check;

ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_status_check;
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_priority_check;

ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_status_check;
