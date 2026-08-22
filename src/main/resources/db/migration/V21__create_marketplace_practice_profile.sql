-- ==============================================================================
-- Taxoryn Platform — Flyway Migration V21: Create Marketplace Practice Profile
-- Marketplace Practice Profile Foundation Schema, Constraints, Statuses, and Indexes
-- ==============================================================================

-- 1. Create Marketplace Practice Profile Table
CREATE TABLE IF NOT EXISTS marketplace_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    slug VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    headline VARCHAR(255),
    bio TEXT,
    professional_type VARCHAR(50) NOT NULL DEFAULT 'CHARTERED_ACCOUNTANT',
    experience_years INT DEFAULT 5,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(255),
    website_url VARCHAR(255),
    avatar_url TEXT,
    banner_url TEXT,
    specializations TEXT,
    languages_spoken VARCHAR(255) DEFAULT 'English, Hindi',
    starting_fee NUMERIC(15, 2) DEFAULT 999.00,
    hourly_rate NUMERIC(15, 2) DEFAULT 1500.00,
    average_rating NUMERIC(3, 2) DEFAULT 4.90,
    total_reviews INT DEFAULT 0,
    total_clients_served INT DEFAULT 0,
    visibility_status VARCHAR(50) NOT NULL DEFAULT 'PRIVATE',
    verification_status VARCHAR(50) NOT NULL DEFAULT 'NOT_SUBMITTED',
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    consultation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    consultation_fee NUMERIC(15, 2) DEFAULT 499.00,
    consultation_duration_minutes INT DEFAULT 30,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_mp_profile_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT uq_mp_profile_organization_id UNIQUE (organization_id),
    CONSTRAINT uq_mp_profile_slug UNIQUE (slug)
);

-- 2. Performance & Discovery Indexes
CREATE INDEX IF NOT EXISTS idx_mp_profiles_org_id ON marketplace_profiles(organization_id);
CREATE INDEX IF NOT EXISTS idx_mp_profiles_slug ON marketplace_profiles(slug);
CREATE INDEX IF NOT EXISTS idx_mp_profiles_visibility_status ON marketplace_profiles(visibility_status);
CREATE INDEX IF NOT EXISTS idx_mp_profiles_verification_status ON marketplace_profiles(verification_status);
CREATE INDEX IF NOT EXISTS idx_mp_profiles_city_state ON marketplace_profiles(city, state);
CREATE INDEX IF NOT EXISTS idx_mp_profiles_published_active ON marketplace_profiles(is_published, visibility_status);
CREATE INDEX IF NOT EXISTS idx_mp_profiles_created_at ON marketplace_profiles(created_at);
