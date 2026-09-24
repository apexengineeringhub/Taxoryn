-- Taxoryn Platform Migration V84
-- Add filing date to compliance obligations.
-- ComplianceObligationEntity.filedDate is java.time.LocalDate.

ALTER TABLE compliance_obligations
    ADD COLUMN filed_date DATE;