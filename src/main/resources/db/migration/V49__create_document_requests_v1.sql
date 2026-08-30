-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V49)
-- Client Document Request V1 Module: Multi-item Document Requests, Workflow, and Item Review
-- ==============================================================================

-- 1. Sequence for human-readable request reference numbers (e.g. REQ-2026-000101)
CREATE SEQUENCE IF NOT EXISTS doc_request_seq START WITH 1001 INCREMENT BY 1;

-- 2. Document Requests (Parent table)
CREATE TABLE IF NOT EXISTS document_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    request_number VARCHAR(50) NOT NULL UNIQUE,
    purpose VARCHAR(255) NOT NULL,
    due_date DATE,
    message TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'SENT',
    financial_year VARCHAR(20),
    assessment_year VARCHAR(20),
    requested_by_user_id UUID,
    sent_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_doc_req_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_req_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_req_user FOREIGN KEY (requested_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_doc_req_org_client ON document_requests(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_doc_req_org_status ON document_requests(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_doc_req_due_date ON document_requests(due_date);

-- 3. Document Request Items (Child checklist items)
CREATE TABLE IF NOT EXISTS document_request_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    document_type VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    title VARCHAR(255) NOT NULL,
    description TEXT,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    uploaded_document_id UUID,
    uploaded_at TIMESTAMPTZ,
    reviewed_by_user_id UUID,
    reviewed_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_doc_item_request FOREIGN KEY (request_id) REFERENCES document_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_item_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_item_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_item_doc FOREIGN KEY (uploaded_document_id) REFERENCES documents(id) ON DELETE SET NULL,
    CONSTRAINT fk_doc_item_reviewer FOREIGN KEY (reviewed_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_doc_item_request ON document_request_items(request_id);
CREATE INDEX IF NOT EXISTS idx_doc_item_org_client ON document_request_items(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_doc_item_status ON document_request_items(status);
CREATE INDEX IF NOT EXISTS idx_doc_item_doc ON document_request_items(uploaded_document_id);