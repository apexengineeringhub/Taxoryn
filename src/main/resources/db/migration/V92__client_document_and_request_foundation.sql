-- ==============================================================================
-- Taxoryn Platform - Phase 13 Migration (V92)
-- Client Document & Request Foundation Enhancements
-- ==============================================================================

-- 1. Enhance Documents Table with Workflow, Request, and Location Scopes
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS workflow_id UUID,
    ADD COLUMN IF NOT EXISTS request_id UUID,
    ADD COLUMN IF NOT EXISTS location_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_documents_workflow'
    ) THEN
        ALTER TABLE documents
            ADD CONSTRAINT fk_documents_workflow
            FOREIGN KEY (workflow_id) REFERENCES compliance_workflows(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_documents_request'
    ) THEN
        ALTER TABLE documents
            ADD CONSTRAINT fk_documents_request
            FOREIGN KEY (request_id) REFERENCES document_requests(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_documents_location'
    ) THEN
        ALTER TABLE documents
            ADD CONSTRAINT fk_documents_location
            FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_documents_workflow_id ON documents(workflow_id);
CREATE INDEX IF NOT EXISTS idx_documents_request_id ON documents(request_id);
CREATE INDEX IF NOT EXISTS idx_documents_location_id ON documents(location_id);
CREATE INDEX IF NOT EXISTS idx_documents_org_workflow ON documents(organization_id, workflow_id);
CREATE INDEX IF NOT EXISTS idx_documents_org_location ON documents(organization_id, location_id);


-- 2. Enhance Document Requests Table with Workflow and Location Scopes
ALTER TABLE document_requests
    ADD COLUMN IF NOT EXISTS workflow_id UUID,
    ADD COLUMN IF NOT EXISTS location_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_document_requests_workflow'
    ) THEN
        ALTER TABLE document_requests
            ADD CONSTRAINT fk_document_requests_workflow
            FOREIGN KEY (workflow_id) REFERENCES compliance_workflows(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_document_requests_location'
    ) THEN
        ALTER TABLE document_requests
            ADD CONSTRAINT fk_document_requests_location
            FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_doc_requests_workflow_id ON document_requests(workflow_id);
CREATE INDEX IF NOT EXISTS idx_doc_requests_location_id ON document_requests(location_id);
CREATE INDEX IF NOT EXISTS idx_doc_requests_org_workflow ON document_requests(organization_id, workflow_id);
CREATE INDEX IF NOT EXISTS idx_doc_requests_org_location ON document_requests(organization_id, location_id);
