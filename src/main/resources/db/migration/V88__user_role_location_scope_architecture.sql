-- ==============================================================================
-- Taxoryn Platform - Phase 2 Migration (V88)
-- User, Role & Location Access Scope Architecture
-- ==============================================================================

-- 1. Seed/Ensure System Roles: PRACTICE_ADMIN, PRACTITIONER, STAFF
INSERT INTO roles (
    id,
    organization_id,
    code,
    name,
    description,
    is_system_role,
    created_at,
    updated_at,
    created_by,
    updated_by,
    version
) VALUES
    ('20000000-0000-0000-0000-000000000110', NULL, 'PRACTICE_ADMIN', 'Practice Administrator', 'Full administrative authority within a practice tenant', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM', 0),
    ('20000000-0000-0000-0000-000000000114', NULL, 'PRACTITIONER', 'Tax Practitioner', 'Professional practitioner executing compliance workflows across assigned locations', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM', 0),
    ('20000000-0000-0000-0000-000000000006', NULL, 'STAFF', 'Articled Assistant / Junior Staff', 'Data entry, document collection and basic task execution in assigned locations', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM', 0)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'SYSTEM';

-- Ensure PRACTITIONER permissions (view and work with client filings, documents, and tasks)
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000114', id FROM permissions
WHERE code IN (
    'ORGANIZATION_VIEW', 'USER_VIEW', 'EMPLOYEE_VIEW',
    'CLIENT_VIEW', 'CLIENT_CREATE', 'CLIENT_UPDATE',
    'TASK_VIEW', 'TASK_CREATE', 'TASK_UPDATE',
    'GST_VIEW', 'GST_CREATE', 'GST_UPDATE',
    'ITR_VIEW', 'ITR_CREATE', 'ITR_UPDATE',
    'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD',
    'ROLE_READ'
)
ON CONFLICT DO NOTHING;

-- Ensure STAFF permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000006', id FROM permissions
WHERE code IN (
    'ORGANIZATION_VIEW', 'USER_VIEW',
    'CLIENT_VIEW',
    'TASK_VIEW', 'TASK_UPDATE',
    'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD'
)
ON CONFLICT DO NOTHING;

-- 2. Create user_locations Table for Direct User-to-Location Assignment within Tenant Boundary
CREATE TABLE IF NOT EXISTS user_locations (
    user_id UUID NOT NULL,
    location_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, location_id),
    CONSTRAINT fk_ul_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ul_location FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE CASCADE,
    CONSTRAINT fk_ul_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ul_user ON user_locations(user_id);
CREATE INDEX IF NOT EXISTS idx_ul_location ON user_locations(location_id);
CREATE INDEX IF NOT EXISTS idx_ul_org ON user_locations(organization_id);
