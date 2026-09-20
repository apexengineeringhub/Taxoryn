-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V68)
-- Document Exchange Enhancements: Bidirectional Requests, Document Delivery, and Acknowledgement Fulfillment
-- ==============================================================================

-- 1. Add exchange direction, exchange type, and document delivery fields to document_requests
ALTER TABLE document_requests
    ADD COLUMN IF NOT EXISTS exchange_type VARCHAR(50) NOT NULL DEFAULT 'DOCUMENT_REQUEST',
    ADD COLUMN IF NOT EXISTS direction VARCHAR(50) NOT NULL DEFAULT 'PRACTITIONER_TO_CLIENT',
    ADD COLUMN IF NOT EXISTS category VARCHAR(50),
    ADD COLUMN IF NOT EXISTS tax_period VARCHAR(50),
    ADD COLUMN IF NOT EXISTS delivered_document_id UUID,
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS declined_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS decline_reason TEXT;

-- 2. Add foreign key for delivered document
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_doc_req_delivered_doc'
    ) THEN
        ALTER TABLE document_requests
            ADD CONSTRAINT fk_doc_req_delivered_doc
            FOREIGN KEY (delivered_document_id) REFERENCES documents(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 3. Indexes for efficient directional and exchange filtering
CREATE INDEX IF NOT EXISTS idx_doc_req_direction ON document_requests(organization_id, direction);
CREATE INDEX IF NOT EXISTS idx_doc_req_exchange_type ON document_requests(organization_id, exchange_type);
CREATE INDEX IF NOT EXISTS idx_doc_req_delivered_doc ON document_requests(delivered_document_id);
