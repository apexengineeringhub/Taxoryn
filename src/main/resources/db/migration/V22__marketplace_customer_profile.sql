-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V22)
-- Marketplace Customer Account, Profile, and Customer Interaction Associations
-- ==============================================================================

-- 1. Allow users.organization_id and audit_logs.organization_id to be nullable for direct Marketplace Customers and platform events
ALTER TABLE users ALTER COLUMN organization_id DROP NOT NULL;
ALTER TABLE audit_logs ALTER COLUMN organization_id DROP NOT NULL;

-- 2. Create Marketplace Customer Profiles table
CREATE TABLE IF NOT EXISTS marketplace_customer_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    customer_type VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL',
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    display_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    profile_photo_url VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    preferred_language VARCHAR(50) DEFAULT 'English',
    business_name VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_mkt_cust_user_id ON marketplace_customer_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_mkt_cust_email ON marketplace_customer_profiles(email);
CREATE INDEX IF NOT EXISTS idx_mkt_cust_city_state ON marketplace_customer_profiles(city, state);
CREATE INDEX IF NOT EXISTS idx_mkt_cust_status ON marketplace_customer_profiles(status);

-- 3. Add customer_id link to Marketplace Lead, Consultation, Review, Proposal tables
ALTER TABLE marketplace_leads ADD COLUMN IF NOT EXISTS customer_id UUID;
CREATE INDEX IF NOT EXISTS idx_mkt_leads_customer_id ON marketplace_leads(customer_id);

ALTER TABLE marketplace_consultations ADD COLUMN IF NOT EXISTS customer_id UUID;
CREATE INDEX IF NOT EXISTS idx_mkt_consultations_customer_id ON marketplace_consultations(customer_id);

ALTER TABLE marketplace_reviews ADD COLUMN IF NOT EXISTS customer_id UUID;
CREATE INDEX IF NOT EXISTS idx_mkt_reviews_customer_id ON marketplace_reviews(customer_id);

ALTER TABLE marketplace_proposals ADD COLUMN IF NOT EXISTS customer_id UUID;
CREATE INDEX IF NOT EXISTS idx_mkt_proposals_customer_id ON marketplace_proposals(customer_id);

-- 4. Seed Permission & System Role for Marketplace Customer
INSERT INTO permissions (id, code, name, module, description)
VALUES
    ('10000000-0000-0000-0000-000000000060', 'MARKETPLACE_CUSTOMER_ACCESS', 'Marketplace Customer Access', 'MARKETPLACE', 'Access to Taxoryn Marketplace Customer profile, dashboard, and requests')
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles (id, organization_id, code, name, description, is_system_role)
VALUES
    ('20000000-0000-0000-0000-000000000020', NULL, 'MARKETPLACE_CUSTOMER', 'Marketplace Customer', 'Individual or Business seeking tax, GST, ITR, and corporate advisory services through the Taxoryn Marketplace', TRUE)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'MARKETPLACE_CUSTOMER' AND p.code = 'MARKETPLACE_CUSTOMER_ACCESS'
ON CONFLICT DO NOTHING;
