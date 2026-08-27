-- ==============================================================================
-- Taxoryn Platform — Flyway Migration V41: Practice Profile SEO & Slug Redirects
-- Adds SEO title, meta description, canonical URL, working hours to marketplace profiles,
-- and creates zero-chain 301 alias redirect table for stable public practice URLs.
-- ==============================================================================

-- 1. Add SEO and public metadata columns to marketplace_profiles
ALTER TABLE marketplace_profiles ADD COLUMN IF NOT EXISTS seo_title VARCHAR(255);
ALTER TABLE marketplace_profiles ADD COLUMN IF NOT EXISTS meta_description TEXT;
ALTER TABLE marketplace_profiles ADD COLUMN IF NOT EXISTS canonical_url VARCHAR(255);
ALTER TABLE marketplace_profiles ADD COLUMN IF NOT EXISTS working_hours VARCHAR(255);

-- 2. Create Marketplace Practice Profile Slug Redirects Table
CREATE TABLE IF NOT EXISTS marketplace_profile_slug_redirects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    old_slug VARCHAR(255) NOT NULL,
    new_slug VARCHAR(255) NOT NULL,
    profile_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mp_redirect_old_slug UNIQUE (old_slug),
    CONSTRAINT fk_mp_redirect_profile FOREIGN KEY (profile_id) REFERENCES marketplace_profiles(id) ON DELETE CASCADE
);

-- 3. Create Indexes for High-Speed Routing & Zero Redirect Chains
CREATE INDEX IF NOT EXISTS idx_mp_redirect_old_slug ON marketplace_profile_slug_redirects(old_slug);
CREATE INDEX IF NOT EXISTS idx_mp_redirect_new_slug ON marketplace_profile_slug_redirects(new_slug);
CREATE INDEX IF NOT EXISTS idx_mp_redirect_profile_id ON marketplace_profile_slug_redirects(profile_id);
