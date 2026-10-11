-- =========================================================================
-- V139__marketplace_review_moderation_and_constraints.sql
-- Taxoryn Stabilization Phase 3.4 — Practitioner Reviews & Ratings
--
-- Adds moderation metadata columns and unique/performance indexes
-- to marketplace_reviews to prevent duplicate reviews and support
-- administrative moderation lifecycle.
-- =========================================================================

-- 1. Add moderation audit columns to marketplace_reviews
ALTER TABLE marketplace_reviews
    ADD COLUMN IF NOT EXISTS moderated_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS moderated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS moderation_note VARCHAR(500);

-- 2. Partial unique index on lead_id to ensure a completed enquiry/lead
--    can only be reviewed once concurrently and idempotently
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'uq_mp_reviews_lead_id'
    ) THEN
        CREATE UNIQUE INDEX uq_mp_reviews_lead_id
        ON marketplace_reviews (lead_id)
        WHERE lead_id IS NOT NULL;
    END IF;
END $$;

-- 3. Composite performance index for public profile review lookups by status
CREATE INDEX IF NOT EXISTS idx_mp_reviews_profile_status_created
ON marketplace_reviews (marketplace_profile_id, status, created_at DESC);
