-- Taxoryn Platform Migration V83
-- Add filing actor to compliance obligations.
-- ComplianceObligationEntity.filedBy is String.

ALTER TABLE compliance_obligations
    ADD COLUMN filed_by VARCHAR(255);