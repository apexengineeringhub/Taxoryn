-- ==============================================================================
-- Taxoryn Platform - Phase 1 Migration (V62)
-- Tax Notice Management: Notice Cases, Responses, Hearings, Activities & Extensions
-- ==============================================================================

-- 1. Create Granular Notice System Permissions
ALTER TABLE permissions ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

INSERT INTO permissions (id, code, name, module, description, created_at) VALUES
    ('10000000-0000-0000-0000-000000000301', 'NOTICE_VIEW', 'View Tax Notices', 'NOTICE', 'Allows viewing tax notice cases, dashboard, timeline and documents', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000302', 'NOTICE_CREATE', 'Create Tax Notice Case', 'NOTICE', 'Allows logging new tax notices from Income Tax, GST, or TDS departments', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000303', 'NOTICE_UPDATE', 'Update Tax Notice', 'NOTICE', 'Allows updating notice details, deadlines, sections, and metadata', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000304', 'NOTICE_ASSIGN', 'Assign Tax Notice', 'NOTICE', 'Allows assigning responsible staff, reviewers, and partners to notices', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000305', 'NOTICE_RESPONSE_CREATE', 'Draft Notice Response', 'NOTICE', 'Allows drafting and revising written replies and submissions', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000306', 'NOTICE_RESPONSE_REVIEW', 'Review Notice Response', 'NOTICE', 'Allows internal maker-checker review of response drafts', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000307', 'NOTICE_APPROVE', 'Partner Approve Notice Response', 'NOTICE', 'Allows executive partner sign-off and approval on tax notice responses', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000308', 'NOTICE_SUBMIT', 'Record Notice Submission', 'NOTICE', 'Allows recording portal filing and acknowledgement of response', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000309', 'NOTICE_CLOSE', 'Resolve & Close Notice', 'NOTICE', 'Allows resolving, dropping demand, or closing tax notice cases', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000310', 'NOTICE_DELETE', 'Delete Notice Case', 'NOTICE', 'Allows deleting or archiving invalid/draft tax notice cases', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- 2. Associate Notice Permissions with Default Practice Roles
-- 2.1 SUPER_ADMIN / TAXORYN_SUPERADMIN (Full permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code IN ('SUPER_ADMIN', 'TAXORYN_SUPERADMIN')
  AND p.module = 'NOTICE'
ON CONFLICT DO NOTHING;

-- 2.2 ORG_ADMIN / PRACTICE_ADMIN / PRACTICE_OWNER / PARTNER (Full notice management)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code IN ('ORG_ADMIN', 'PRACTICE_ADMIN', 'PRACTICE_OWNER', 'PARTNER')
  AND p.module = 'NOTICE'
ON CONFLICT DO NOTHING;

-- 2.3 MANAGER / TAX_MANAGER (Notice operational management + review)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code IN ('MANAGER', 'TAX_MANAGER')
  AND p.code IN ('NOTICE_VIEW', 'NOTICE_CREATE', 'NOTICE_UPDATE', 'NOTICE_ASSIGN', 'NOTICE_RESPONSE_CREATE', 'NOTICE_RESPONSE_REVIEW', 'NOTICE_SUBMIT', 'NOTICE_CLOSE')
ON CONFLICT DO NOTHING;

-- 2.4 TAX_PROFESSIONAL / PRACTITIONER / SENIOR_TAX_ASSOCIATE (Notice handling + review)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code IN ('TAX_PROFESSIONAL', 'PRACTITIONER', 'SENIOR_TAX_ASSOCIATE')
  AND p.code IN ('NOTICE_VIEW', 'NOTICE_CREATE', 'NOTICE_UPDATE', 'NOTICE_RESPONSE_CREATE', 'NOTICE_RESPONSE_REVIEW', 'NOTICE_SUBMIT')
ON CONFLICT DO NOTHING;

-- 2.5 STAFF / ACCOUNTANT / TAX_ASSOCIATE / ARTICLE_ASSISTANT (Notice execution + drafting)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code IN ('STAFF', 'ACCOUNTANT', 'TAX_ASSOCIATE', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'EMPLOYEE')
  AND p.code IN ('NOTICE_VIEW', 'NOTICE_CREATE', 'NOTICE_UPDATE', 'NOTICE_RESPONSE_CREATE')
ON CONFLICT DO NOTHING;

-- 2.6 VIEWER (Read-only)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'VIEWER'
  AND p.code = 'NOTICE_VIEW'
ON CONFLICT DO NOTHING;

-- 3. Create tax_notices Table first
CREATE TABLE IF NOT EXISTS tax_notices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    notice_number VARCHAR(100) NOT NULL,
    din_number VARCHAR(100),
    department VARCHAR(50) NOT NULL,
    notice_type VARCHAR(100) NOT NULL,
    section VARCHAR(100),
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    assessment_year VARCHAR(20),
    financial_year VARCHAR(20),
    tax_period VARCHAR(50),
    demand_amount NUMERIC(15, 2),
    notice_date DATE,
    received_date DATE NOT NULL,
    response_due_date DATE NOT NULL,
    hearing_date DATE,
    hearing_time VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    assigned_employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    reviewer_employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    partner_employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    issuing_authority VARCHAR(255),
    issuing_officer_name VARCHAR(150),
    portal_acknowledgement_number VARCHAR(100),
    submission_mode VARCHAR(50),
    submitted_at TIMESTAMP WITH TIME ZONE,
    closure_date DATE,
    closure_remarks TEXT,
    internal_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_tax_notices_org_notice_number UNIQUE (organization_id, notice_number)
);

CREATE INDEX IF NOT EXISTS idx_tax_notices_org_id ON tax_notices(organization_id);
CREATE INDEX IF NOT EXISTS idx_tax_notices_client_id ON tax_notices(client_id);
CREATE INDEX IF NOT EXISTS idx_tax_notices_status ON tax_notices(status);
CREATE INDEX IF NOT EXISTS idx_tax_notices_priority ON tax_notices(priority);
CREATE INDEX IF NOT EXISTS idx_tax_notices_department ON tax_notices(department);
CREATE INDEX IF NOT EXISTS idx_tax_notices_response_due_date ON tax_notices(response_due_date);
CREATE INDEX IF NOT EXISTS idx_tax_notices_assigned_employee ON tax_notices(assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_tax_notices_din ON tax_notices(din_number);

-- 4. Extend tasks Table with notice_id
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'tasks' AND column_name = 'notice_id') THEN
        ALTER TABLE tasks ADD COLUMN notice_id UUID REFERENCES tax_notices(id) ON DELETE SET NULL;
        CREATE INDEX IF NOT EXISTS idx_tasks_notice_id ON tasks(notice_id);
    END IF;
END $$;

-- 5. Extend documents Table with notice_id
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'documents' AND column_name = 'notice_id') THEN
        ALTER TABLE documents ADD COLUMN notice_id UUID REFERENCES tax_notices(id) ON DELETE SET NULL;
        CREATE INDEX IF NOT EXISTS idx_documents_notice_id ON documents(notice_id);
    END IF;
END $$;

-- 6. Extend document_requests Table with notice_id
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'document_requests' AND column_name = 'notice_id') THEN
        ALTER TABLE document_requests ADD COLUMN notice_id UUID REFERENCES tax_notices(id) ON DELETE SET NULL;
        CREATE INDEX IF NOT EXISTS idx_document_requests_notice_id ON document_requests(notice_id);
    END IF;
END $$;

-- 7. Create notice_responses Table
CREATE TABLE IF NOT EXISTS notice_responses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    notice_id UUID NOT NULL REFERENCES tax_notices(id) ON DELETE CASCADE,
    response_version INT NOT NULL DEFAULT 1,
    response_title VARCHAR(255) NOT NULL,
    response_summary TEXT,
    legal_grounds TEXT,
    facts_of_case TEXT,
    prepared_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    review_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    review_comments TEXT,
    submission_reference VARCHAR(100),
    submitted_at TIMESTAMP WITH TIME ZONE,
    acknowledgement_number VARCHAR(100),
    acknowledgement_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_notice_responses_notice_version UNIQUE (notice_id, response_version)
);

CREATE INDEX IF NOT EXISTS idx_notice_responses_org_id ON notice_responses(organization_id);
CREATE INDEX IF NOT EXISTS idx_notice_responses_notice_id ON notice_responses(notice_id);
CREATE INDEX IF NOT EXISTS idx_notice_responses_review_status ON notice_responses(review_status);

-- 8. Create notice_hearings Table
CREATE TABLE IF NOT EXISTS notice_hearings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    notice_id UUID NOT NULL REFERENCES tax_notices(id) ON DELETE CASCADE,
    hearing_date DATE NOT NULL,
    hearing_time VARCHAR(20),
    hearing_mode VARCHAR(50) NOT NULL DEFAULT 'VIRTUAL_VC',
    hearing_link VARCHAR(500),
    authority_name VARCHAR(255),
    officer_name VARCHAR(150),
    designated_employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    designated_partner_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    proceedings_summary TEXT,
    outcome_summary TEXT,
    next_action TEXT,
    next_hearing_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notice_hearings_org_id ON notice_hearings(organization_id);
CREATE INDEX IF NOT EXISTS idx_notice_hearings_notice_id ON notice_hearings(notice_id);
CREATE INDEX IF NOT EXISTS idx_notice_hearings_date ON notice_hearings(hearing_date);

-- 9. Create notice_activities Table
CREATE TABLE IF NOT EXISTS notice_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    notice_id UUID NOT NULL REFERENCES tax_notices(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    performed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    performer_name VARCHAR(150),
    description TEXT NOT NULL,
    from_state VARCHAR(50),
    to_state VARCHAR(50),
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notice_activities_org_id ON notice_activities(organization_id);
CREATE INDEX IF NOT EXISTS idx_notice_activities_notice_id ON notice_activities(notice_id);
CREATE INDEX IF NOT EXISTS idx_notice_activities_created_at ON notice_activities(created_at DESC);
