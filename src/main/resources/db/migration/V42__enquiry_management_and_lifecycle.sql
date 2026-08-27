-- =============================================================================
-- Migration V42: Marketplace Trust, Enquiry Management & Lifecycle
-- =============================================================================

-- 1. Extend marketplace_leads with lifecycle fields, timestamps, and reference number
ALTER TABLE marketplace_leads
    ADD COLUMN IF NOT EXISTS reference_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS enquiry_status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(50),
    ADD COLUMN IF NOT EXISTS rejection_note VARCHAR(500),
    ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS received_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS review_id UUID;

-- 2. Extend marketplace_reviews with lead_id foreign reference
ALTER TABLE marketplace_reviews
    ADD COLUMN IF NOT EXISTS lead_id UUID;

-- 3. Backfill reference_number for any existing leads
UPDATE marketplace_leads
SET reference_number = 'TXN-' || EXTRACT(YEAR FROM created_at)::text || '-' || LPAD(SUBSTRING(REPLACE(id::text, '-', ''), 1, 6), 6, '0')
WHERE reference_number IS NULL;

-- 4. Set unique constraint on reference_number
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_marketplace_leads_reference_number'
    ) THEN
        ALTER TABLE marketplace_leads ADD CONSTRAINT uk_marketplace_leads_reference_number UNIQUE (reference_number);
    END IF;
END $$;

-- 5. Add performance indexes for query filters
CREATE INDEX IF NOT EXISTS idx_marketplace_leads_org_status ON marketplace_leads (organization_id, enquiry_status);
CREATE INDEX IF NOT EXISTS idx_marketplace_leads_customer_status ON marketplace_leads (customer_id, enquiry_status);
CREATE INDEX IF NOT EXISTS idx_marketplace_leads_assigned_emp ON marketplace_leads (assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_leads_ref_num ON marketplace_leads (reference_number);
CREATE INDEX IF NOT EXISTS idx_marketplace_reviews_lead ON marketplace_reviews (lead_id);
