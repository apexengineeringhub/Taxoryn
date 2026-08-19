-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V9)
-- Document Management Module: Client Vault, Filing Attachments & Storage Metadata
-- ==============================================================================

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID,
    gst_filing_id UUID,
    itr_return_id UUID,
    task_id UUID,
    document_type VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    storage_provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    financial_year VARCHAR(20),
    assessment_year VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    checksum VARCHAR(64),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_documents_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE SET NULL,
    CONSTRAINT fk_documents_gst FOREIGN KEY (gst_filing_id) REFERENCES gst_return_filings(id) ON DELETE SET NULL,
    CONSTRAINT fk_documents_itr FOREIGN KEY (itr_return_id) REFERENCES itr_returns(id) ON DELETE SET NULL,
    CONSTRAINT fk_documents_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_documents_org ON documents(organization_id);
CREATE INDEX IF NOT EXISTS idx_documents_client ON documents(client_id);
CREATE INDEX IF NOT EXISTS idx_documents_gst ON documents(gst_filing_id);
CREATE INDEX IF NOT EXISTS idx_documents_itr ON documents(itr_return_id);
CREATE INDEX IF NOT EXISTS idx_documents_task ON documents(task_id);
CREATE INDEX IF NOT EXISTS idx_documents_type ON documents(document_type);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_storage_key ON documents(storage_key);
