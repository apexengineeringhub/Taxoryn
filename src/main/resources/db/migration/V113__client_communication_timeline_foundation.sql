-- Extend the existing client interaction log into the paginated communication timeline.
ALTER TABLE client_notes
    ADD COLUMN IF NOT EXISTS occurred_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS visibility VARCHAR(24) NOT NULL DEFAULT 'INTERNAL',
    ADD COLUMN IF NOT EXISTS follow_up_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS follow_up_date TIMESTAMPTZ;

UPDATE client_notes SET occurred_at = created_at WHERE occurred_at IS NULL;
ALTER TABLE client_notes ALTER COLUMN occurred_at SET NOT NULL;
ALTER TABLE client_notes ALTER COLUMN occurred_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE client_notes ALTER COLUMN title DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_client_notes_org_client_occurred
    ON client_notes(organization_id, client_id, occurred_at DESC, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_client_notes_org_client_type
    ON client_notes(organization_id, client_id, note_type);

INSERT INTO permissions (id, code, name, module, description, created_at) VALUES
    ('10000000-0000-0000-0000-000000000501', 'CLIENT_COMMUNICATION_VIEW', 'View Client Communications', 'CLIENTS', 'View communication timeline entries within client portfolio scope', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000502', 'CLIENT_COMMUNICATION_CREATE', 'Create Client Communications', 'CLIENTS', 'Record client communication timeline entries', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000503', 'CLIENT_COMMUNICATION_UPDATE', 'Update Client Communications', 'CLIENTS', 'Update manually recorded client communications', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000504', 'CLIENT_COMMUNICATION_DELETE', 'Delete Client Communications', 'CLIENTS', 'Delete manually recorded client communications', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'TAXORYN_SUPERADMIN', 'ORG_ADMIN', 'PRACTICE_ADMIN', 'PRACTICE_OWNER', 'PARTNER')
  AND p.code IN ('CLIENT_COMMUNICATION_VIEW', 'CLIENT_COMMUNICATION_CREATE', 'CLIENT_COMMUNICATION_UPDATE', 'CLIENT_COMMUNICATION_DELETE')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('MANAGER', 'TAX_MANAGER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'SENIOR_TAX_ASSOCIATE')
  AND p.code IN ('CLIENT_COMMUNICATION_VIEW', 'CLIENT_COMMUNICATION_CREATE', 'CLIENT_COMMUNICATION_UPDATE')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('STAFF', 'ACCOUNTANT', 'TAX_ASSOCIATE', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'EMPLOYEE')
  AND p.code IN ('CLIENT_COMMUNICATION_VIEW', 'CLIENT_COMMUNICATION_CREATE')
ON CONFLICT DO NOTHING;
