-- ==============================================================================
-- Taxoryn Platform - Phase 2 Migration (V54)
-- Complete document-level linkage parity for TDS (matching existing GST/ITR pattern)
-- ==============================================================================

-- 1. Add TDS return linkage column to documents (mirrors gst_filing_id / itr_return_id)
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS tds_return_id UUID REFERENCES tds_returns(id) ON DELETE SET NULL;

-- 2. Index for lookups
CREATE INDEX IF NOT EXISTS idx_documents_tds ON documents(tds_return_id);
