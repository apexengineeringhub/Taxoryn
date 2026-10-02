CREATE TABLE IF NOT EXISTS practice_leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    lead_type VARCHAR(24) NOT NULL DEFAULT 'INDIVIDUAL',
    name VARCHAR(255) NOT NULL,
    business_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    source VARCHAR(32) NOT NULL DEFAULT 'OTHER',
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    priority VARCHAR(24) NOT NULL DEFAULT 'MEDIUM',
    interested_service_code VARCHAR(100),
    description TEXT,
    assigned_employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    next_follow_up_at TIMESTAMPTZ,
    converted_client_id UUID REFERENCES clients(id) ON DELETE SET NULL,
    converted_at TIMESTAMPTZ,
    converted_by UUID REFERENCES users(id) ON DELETE SET NULL,
    lost_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_practice_leads_org_status ON practice_leads(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_practice_leads_org_assigned ON practice_leads(organization_id, assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_practice_leads_org_followup ON practice_leads(organization_id, next_follow_up_at);
CREATE INDEX IF NOT EXISTS idx_practice_leads_org_created ON practice_leads(organization_id, created_at DESC);

CREATE TABLE IF NOT EXISTS practice_lead_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    lead_id UUID NOT NULL REFERENCES practice_leads(id) ON DELETE CASCADE,
    activity_type VARCHAR(24) NOT NULL,
    subject VARCHAR(255),
    content TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    author_name VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_practice_lead_activities_org_lead_occurred
    ON practice_lead_activities(organization_id, lead_id, occurred_at DESC, created_at DESC);

INSERT INTO permissions (id, code, name, module, description, created_at) VALUES
 ('10000000-0000-0000-0000-000000000505', 'LEAD_VIEW', 'View Practice Leads', 'LEADS', 'View practice prospect leads within assigned scope', CURRENT_TIMESTAMP),
 ('10000000-0000-0000-0000-000000000506', 'LEAD_CREATE', 'Create Practice Leads', 'LEADS', 'Capture practice enquiries and prospects', CURRENT_TIMESTAMP),
 ('10000000-0000-0000-0000-000000000507', 'LEAD_UPDATE', 'Update Practice Leads', 'LEADS', 'Update practice lead details and lifecycle', CURRENT_TIMESTAMP),
 ('10000000-0000-0000-0000-000000000508', 'LEAD_ASSIGN', 'Assign Practice Leads', 'LEADS', 'Assign practice leads to employees', CURRENT_TIMESTAMP),
 ('10000000-0000-0000-0000-000000000509', 'LEAD_CONVERT', 'Convert Practice Leads', 'LEADS', 'Convert qualified leads into clients', CURRENT_TIMESTAMP),
 ('10000000-0000-0000-0000-000000000510', 'LEAD_DELETE', 'Delete Practice Leads', 'LEADS', 'Delete practice leads', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'TAXORYN_SUPERADMIN', 'ORG_ADMIN', 'PRACTICE_ADMIN', 'PRACTICE_OWNER', 'PARTNER')
  AND p.code IN ('LEAD_VIEW', 'LEAD_CREATE', 'LEAD_UPDATE', 'LEAD_ASSIGN', 'LEAD_CONVERT', 'LEAD_DELETE')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('MANAGER', 'TAX_MANAGER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'SENIOR_TAX_ASSOCIATE')
  AND p.code IN ('LEAD_VIEW', 'LEAD_CREATE', 'LEAD_UPDATE', 'LEAD_ASSIGN', 'LEAD_CONVERT')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('STAFF', 'ACCOUNTANT', 'TAX_ASSOCIATE', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'EMPLOYEE')
  AND p.code IN ('LEAD_VIEW', 'LEAD_CREATE', 'LEAD_UPDATE')
ON CONFLICT DO NOTHING;
