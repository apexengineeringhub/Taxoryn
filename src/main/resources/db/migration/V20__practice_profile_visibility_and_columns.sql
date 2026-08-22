-- ==============================================================================
-- Taxoryn Platform — Flyway Migration V20: Practice Profile Domain Model Enhancement
-- Marketplace Practice Profile Foundation: Visibility Status & Domain Alignment
-- ==============================================================================

ALTER TABLE marketplace_profiles
ADD COLUMN IF NOT EXISTS visibility_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT';

-- Backfill visibility_status based on current is_published flag
UPDATE marketplace_profiles
SET visibility_status = CASE 
    WHEN is_published = TRUE THEN 'PUBLISHED' 
    ELSE 'DRAFT' 
END
WHERE visibility_status = 'DRAFT' AND is_published = TRUE;

CREATE INDEX IF NOT EXISTS idx_mp_profiles_visibility ON marketplace_profiles(visibility_status);
