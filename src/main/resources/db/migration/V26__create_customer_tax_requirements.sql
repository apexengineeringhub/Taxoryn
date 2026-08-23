-- =========================================================================
-- V26: Create Customer Tax Requirements
-- Feature #6: Customer Requirement / Tax Need Capture
-- =========================================================================

CREATE TABLE IF NOT EXISTS customer_tax_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES marketplace_customer_profiles(id) ON DELETE CASCADE,
    tax_service_id UUID NOT NULL REFERENCES marketplace_tax_services(id) ON DELETE RESTRICT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    customer_type VARCHAR(50),
    financial_year VARCHAR(20),
    description VARCHAR(2000),
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    search_radius_km INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT chk_cust_tax_req_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'CANCELLED', 'CLOSED')),
    CONSTRAINT chk_cust_tax_req_cust_type CHECK (customer_type IS NULL OR customer_type IN ('SALARIED', 'SELF_EMPLOYED', 'BUSINESS_OWNER', 'FREELANCER', 'INVESTOR', 'OTHER'))
);

-- B-Tree Indexes for rapid filtering, ownership queries, and lifecycle operations
CREATE INDEX IF NOT EXISTS idx_cust_req_customer_id ON customer_tax_requirements(customer_id);
CREATE INDEX IF NOT EXISTS idx_cust_req_status ON customer_tax_requirements(status);
CREATE INDEX IF NOT EXISTS idx_cust_req_tax_service_id ON customer_tax_requirements(tax_service_id);
CREATE INDEX IF NOT EXISTS idx_cust_req_financial_year ON customer_tax_requirements(financial_year);
CREATE INDEX IF NOT EXISTS idx_cust_req_created_at ON customer_tax_requirements(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cust_req_active_lookup ON customer_tax_requirements(customer_id, tax_service_id, financial_year, status);
