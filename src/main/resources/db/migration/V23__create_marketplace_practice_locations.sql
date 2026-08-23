-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V23)
-- Multiple Practice Locations for Tax Practice Marketplace Profiles
-- ==============================================================================

-- 1. Create Marketplace Practice Locations table
CREATE TABLE IF NOT EXISTS marketplace_practice_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    marketplace_profile_id UUID NOT NULL REFERENCES marketplace_profiles(id) ON DELETE CASCADE,
    location_name VARCHAR(150) NOT NULL,
    address_line_1 VARCHAR(255) NOT NULL,
    address_line_2 VARCHAR(255),
    landmark VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    state VARCHAR(100) NOT NULL,
    state_code VARCHAR(10),
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    country_code VARCHAR(10) NOT NULL DEFAULT 'IN',
    pincode VARCHAR(20) NOT NULL,
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- 2. Indexes for efficient lookup, multi-tenant isolation, and geographic queries
CREATE INDEX IF NOT EXISTS idx_mkt_locations_profile ON marketplace_practice_locations(marketplace_profile_id);
CREATE INDEX IF NOT EXISTS idx_mkt_locations_org ON marketplace_practice_locations(organization_id);
CREATE INDEX IF NOT EXISTS idx_mkt_locations_city_state ON marketplace_practice_locations(city, state);
CREATE INDEX IF NOT EXISTS idx_mkt_locations_pincode ON marketplace_practice_locations(pincode);
CREATE INDEX IF NOT EXISTS idx_mkt_locations_active ON marketplace_practice_locations(is_active);

-- 3. Data Migration: Safely migrate existing practice profile location data to primary locations
INSERT INTO marketplace_practice_locations (
    id,
    organization_id,
    marketplace_profile_id,
    location_name,
    address_line_1,
    city,
    state,
    pincode,
    country,
    country_code,
    is_primary,
    is_active,
    created_at,
    updated_at,
    version
)
SELECT
    gen_random_uuid(),
    mp.organization_id,
    mp.id,
    'Main Office',
    COALESCE(NULLIF(TRIM(mp.address), ''), 'Main Office'),
    COALESCE(NULLIF(TRIM(mp.city), ''), 'Bengaluru'),
    COALESCE(NULLIF(TRIM(mp.state), ''), 'Karnataka'),
    COALESCE(NULLIF(TRIM(mp.pincode), ''), '560001'),
    'India',
    'IN',
    TRUE,
    TRUE,
    NOW(),
    NOW(),
    0
FROM marketplace_profiles mp
WHERE (mp.city IS NOT NULL AND TRIM(mp.city) <> '')
   OR (mp.state IS NOT NULL AND TRIM(mp.state) <> '')
   OR (mp.address IS NOT NULL AND TRIM(mp.address) <> '')
ON CONFLICT DO NOTHING;
