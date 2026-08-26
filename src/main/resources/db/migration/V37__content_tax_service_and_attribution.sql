-- =============================================================================
-- Migration V37: Content Tax Service Mappings & Marketplace Attribution
-- =============================================================================

CREATE TABLE IF NOT EXISTS content_tax_service_mappings (
    content_id UUID NOT NULL REFERENCES contents(id) ON DELETE CASCADE,
    tax_service_id UUID NOT NULL REFERENCES marketplace_tax_services(id) ON DELETE CASCADE,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (content_id, tax_service_id)
);

CREATE INDEX IF NOT EXISTS idx_content_ts_content_id ON content_tax_service_mappings(content_id);
CREATE INDEX IF NOT EXISTS idx_content_ts_service_id ON content_tax_service_mappings(tax_service_id);

-- Backfill existing singular tax_service_id into the mappings table
INSERT INTO content_tax_service_mappings (content_id, tax_service_id, sort_order)
SELECT id, tax_service_id, 0
FROM contents
WHERE tax_service_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- Marketplace Customer Requirement & Lead Source Attribution
ALTER TABLE customer_tax_requirements ADD COLUMN IF NOT EXISTS source_type VARCHAR(50);
ALTER TABLE customer_tax_requirements ADD COLUMN IF NOT EXISTS source_content_id UUID;

ALTER TABLE marketplace_leads ADD COLUMN IF NOT EXISTS source_type VARCHAR(50);
ALTER TABLE marketplace_leads ADD COLUMN IF NOT EXISTS source_content_id UUID;

CREATE INDEX IF NOT EXISTS idx_cust_tax_req_source ON customer_tax_requirements(source_type, source_content_id);
CREATE INDEX IF NOT EXISTS idx_mkt_leads_source ON marketplace_leads(source_type, source_content_id);
