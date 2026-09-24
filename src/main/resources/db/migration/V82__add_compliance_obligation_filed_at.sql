-- Taxoryn Platform Migration V82
-- Add filing timestamp to compliance obligations.
-- ComplianceObligationEntity.filedAt is java.time.Instant.

ALTER TABLE compliance_obligations
    ADD COLUMN filed_at TIMESTAMP WITH TIME ZONE;