-- =========================================================================
-- V27: Early-Stage Tax Data Privacy & Minimum Disclosure Hardening
-- Feature #7A: Protect Sensitive Tax Data During Early Enquiry Stage
-- =========================================================================

ALTER TABLE marketplace_leads
    ADD COLUMN IF NOT EXISTS tax_requirement_id UUID REFERENCES customer_tax_requirements(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS tax_service_id UUID REFERENCES marketplace_tax_services(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS financial_year VARCHAR(20),
    ADD COLUMN IF NOT EXISTS customer_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS early_enquiry_message VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS is_contact_masked BOOLEAN DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_leads_tax_req_id ON marketplace_leads(tax_requirement_id);
CREATE INDEX IF NOT EXISTS idx_leads_tax_service_id ON marketplace_leads(tax_service_id);
CREATE INDEX IF NOT EXISTS idx_leads_financial_year ON marketplace_leads(financial_year);
