-- ==============================================================================
-- Taxoryn Platform — Flyway Migration V20: Practice Profile Domain Model Enhancement
-- Marketplace Practice Profile Foundation: Two Separate Status Concepts (Visibility & Verification)
-- Visibility: PRIVATE, PUBLIC, SUSPENDED
-- Verification: NOT_SUBMITTED, PENDING, VERIFIED, REJECTED
-- ==============================================================================

ALTER TABLE marketplace_profiles
ADD COLUMN IF NOT EXISTS visibility_status VARCHAR(50) NOT NULL DEFAULT 'PRIVATE';

-- Backfill visibility_status based on current is_published flag
UPDATE marketplace_profiles
SET visibility_status = CASE 
    WHEN is_published = TRUE THEN 'PUBLIC' 
    ELSE 'PRIVATE' 
END;

ALTER TABLE marketplace_profiles
ALTER COLUMN verification_status SET DEFAULT 'NOT_SUBMITTED';

CREATE INDEX IF NOT EXISTS idx_mp_profiles_visibility ON marketplace_profiles(visibility_status);
CREATE INDEX IF NOT EXISTS idx_mp_profiles_verification ON marketplace_profiles(verification_status);
