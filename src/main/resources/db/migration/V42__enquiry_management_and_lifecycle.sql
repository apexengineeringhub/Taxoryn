-- =============================================================================
-- Migration V42: Marketplace Trust, Enquiry Management & Lifecycle
-- =============================================================================

-- 0. Ensure marketplace_leads and marketplace_reviews exist
CREATE TABLE IF NOT EXISTS marketplace_leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    marketplace_profile_id UUID NOT NULL,
    service_id UUID,
    client_name VARCHAR(255) NOT NULL,
    client_email VARCHAR(255) NOT NULL,
    client_phone VARCHAR(20) NOT NULL,
    city VARCHAR(100),
    pan VARCHAR(10),
    gstin VARCHAR(15),
    service_category VARCHAR(100),
    requirement_description TEXT,
    budget_range VARCHAR(50),
    urgency VARCHAR(50) DEFAULT 'STANDARD',
    lead_status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    converted_client_id UUID,
    assigned_employee_id UUID,
    practitioner_notes TEXT,
    customer_id UUID,
    tax_requirement_id UUID,
    tax_service_id UUID,
    financial_year VARCHAR(20),
    customer_type VARCHAR(50),
    early_enquiry_message TEXT,
    is_contact_masked BOOLEAN DEFAULT TRUE,
    source_type VARCHAR(50),
    source_content_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS marketplace_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    marketplace_profile_id UUID NOT NULL,
    reviewer_name VARCHAR(255) NOT NULL,
    reviewer_designation VARCHAR(255),
    reviewer_company VARCHAR(255),
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_title VARCHAR(255),
    review_comment TEXT NOT NULL,
    service_taken VARCHAR(100),
    is_verified_client BOOLEAN DEFAULT TRUE,
    status VARCHAR(50) NOT NULL DEFAULT 'APPROVED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

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
