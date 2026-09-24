-- Taxoryn Platform Migration V84
-- Align compliance workflow checklist items with AuditableEntity.
--
-- V79 created created_at / updated_at but the table predates the
-- current AuditableEntity definition, which also requires:
--   created_by
--   updated_by
--   version

ALTER TABLE compliance_workflow_checklist_items
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);

ALTER TABLE compliance_workflow_checklist_items
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

ALTER TABLE compliance_workflow_checklist_items
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
