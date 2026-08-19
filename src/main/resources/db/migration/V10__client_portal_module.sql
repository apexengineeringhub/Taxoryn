-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V10)
-- Client Portal Module: Client Portal Authentication, Roles, Notifications & Checklist
-- ==============================================================================

-- 1. Add client_id link to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS client_id UUID;
ALTER TABLE users ADD CONSTRAINT fk_users_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_users_client ON users(client_id);

-- 2. Insert Client Portal Permissions
INSERT INTO permissions (id, code, name, module, description)
VALUES
    ('10000000-0000-0000-0000-000000000051', 'CLIENT_PORTAL_ACCESS', 'Client Portal Access', 'PORTAL', 'Access to Taxoryn Client Portal dashboard and services'),
    ('10000000-0000-0000-0000-000000000052', 'CLIENT_PORTAL_DOCUMENT_UPLOAD', 'Client Document Upload', 'PORTAL', 'Upload client compliance documents to the vault'),
    ('10000000-0000-0000-0000-000000000053', 'CLIENT_PORTAL_DOCUMENT_VIEW', 'Client Document View', 'PORTAL', 'View and download client vault documents'),
    ('10000000-0000-0000-0000-000000000054', 'CLIENT_PORTAL_PROFILE_VIEW', 'Client Profile View', 'PORTAL', 'View client business profile and registration details'),
    ('10000000-0000-0000-0000-000000000055', 'CLIENT_PORTAL_PROFILE_UPDATE', 'Client Profile Update', 'PORTAL', 'Update client contact information and address'),
    ('10000000-0000-0000-0000-000000000056', 'CLIENT_PORTAL_STATUS_VIEW', 'Client Compliance Status View', 'PORTAL', 'View GST and ITR return filing progress and status')
ON CONFLICT (code) DO NOTHING;

-- 3. Insert Client Portal System Roles
INSERT INTO roles (id, organization_id, code, name, description, is_system_role)
VALUES
    ('20000000-0000-0000-0000-000000000014', NULL, 'CLIENT_ADMIN', 'Client Administrator', 'Primary client contact with full access to client portal, documents, and profile management', TRUE),
    ('20000000-0000-0000-0000-000000000015', NULL, 'CLIENT_USER', 'Client User', 'Client staff with access to view compliance status and upload documents', TRUE)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 4. Map Permissions to CLIENT_ADMIN and CLIENT_USER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CLIENT_ADMIN' AND p.code IN (
    'CLIENT_PORTAL_ACCESS',
    'CLIENT_PORTAL_DOCUMENT_UPLOAD',
    'CLIENT_PORTAL_DOCUMENT_VIEW',
    'CLIENT_PORTAL_PROFILE_VIEW',
    'CLIENT_PORTAL_PROFILE_UPDATE',
    'CLIENT_PORTAL_STATUS_VIEW'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CLIENT_USER' AND p.code IN (
    'CLIENT_PORTAL_ACCESS',
    'CLIENT_PORTAL_DOCUMENT_UPLOAD',
    'CLIENT_PORTAL_DOCUMENT_VIEW',
    'CLIENT_PORTAL_PROFILE_VIEW',
    'CLIENT_PORTAL_STATUS_VIEW'
)
ON CONFLICT DO NOTHING;

-- 5. Client Notifications Table
CREATE TABLE IF NOT EXISTS client_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    action_url VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_client_notifications_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_notifications_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_client_notifications_org ON client_notifications(organization_id);
CREATE INDEX IF NOT EXISTS idx_client_notifications_client ON client_notifications(client_id);
CREATE INDEX IF NOT EXISTS idx_client_notifications_unread ON client_notifications(client_id, is_read);

-- 6. Client Document Requests / Checklist Table
CREATE TABLE IF NOT EXISTS client_document_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATE,
    financial_year VARCHAR(20),
    assessment_year VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    uploaded_document_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_doc_requests_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_requests_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_requests_doc FOREIGN KEY (uploaded_document_id) REFERENCES documents(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_doc_requests_org ON client_document_requests(organization_id);
CREATE INDEX IF NOT EXISTS idx_doc_requests_client ON client_document_requests(client_id);
CREATE INDEX IF NOT EXISTS idx_doc_requests_status ON client_document_requests(status);
