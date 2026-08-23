-- =========================================================================
-- V28: Add Missing Optimistic Locking Column to Customer Tax Requirements
-- Fix: CustomerTaxRequirementEntity extends AuditableEntity, which requires
-- a `version` column (@Version) for optimistic locking. V26 created the
-- table without it, causing Hibernate schema validation to fail on startup:
--   "Schema-validation: missing column [version] in table [customer_tax_requirements]"
-- =========================================================================

ALTER TABLE customer_tax_requirements
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
