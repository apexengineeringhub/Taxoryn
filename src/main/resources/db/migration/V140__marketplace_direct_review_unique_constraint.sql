-- =========================================================================
-- V140__marketplace_direct_review_unique_constraint.sql
-- Taxoryn Stabilization Phase 3.4.1 — Review Privacy, Concurrency & Tenant Isolation
--
-- Adds a partial unique index on (customer_id, marketplace_profile_id)
-- for non-lead direct reviews (lead_id IS NULL AND customer_id IS NOT NULL)
-- to prevent concurrent duplicate review submissions at the database level.
-- =========================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'uq_mp_reviews_customer_no_lead'
    ) THEN
        CREATE UNIQUE INDEX uq_mp_reviews_customer_no_lead
        ON marketplace_reviews (customer_id, marketplace_profile_id)
        WHERE lead_id IS NULL AND customer_id IS NOT NULL;
    END IF;
END $$;
