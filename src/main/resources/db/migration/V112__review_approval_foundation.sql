-- Generic Stage 2.5 review foundation. Business resources remain owned by their modules.
CREATE TABLE IF NOT EXISTS review_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    resource_type VARCHAR(40) NOT NULL,
    resource_id UUID NOT NULL,
    review_type VARCHAR(40) NOT NULL,
    status VARCHAR(24) NOT NULL,
    requested_by UUID NOT NULL REFERENCES users(id),
    assigned_reviewer_id UUID NOT NULL REFERENCES users(id),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES users(id),
    review_comment TEXT,
    rejection_reason TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_review_org_status ON review_requests(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_review_org_resource ON review_requests(organization_id, resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_review_org_reviewer ON review_requests(organization_id, assigned_reviewer_id);

CREATE TABLE IF NOT EXISTS review_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    review_request_id UUID NOT NULL REFERENCES review_requests(id) ON DELETE CASCADE,
    action VARCHAR(32) NOT NULL,
    actor_id UUID NOT NULL REFERENCES users(id),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    comment TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_review_action_org_request ON review_actions(organization_id, review_request_id);

INSERT INTO permissions (id, code, name, module, description, created_at) VALUES
    ('10000000-0000-0000-0000-000000000401', 'REVIEW_VIEW', 'View Reviews', 'REVIEW', 'View review requests within client scope', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000402', 'REVIEW_SUBMIT', 'Submit Reviews', 'REVIEW', 'Submit eligible compliance work for review', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000403', 'REVIEW_APPROVE', 'Approve Reviews', 'REVIEW', 'Approve assigned review requests', CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000404', 'REVIEW_REJECT', 'Reject Reviews', 'REVIEW', 'Reject assigned review requests with a reason', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'TAXORYN_SUPERADMIN', 'ORG_ADMIN', 'PRACTICE_ADMIN', 'PRACTICE_OWNER', 'PARTNER') AND p.module = 'REVIEW'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('MANAGER', 'TAX_MANAGER') AND p.code IN ('REVIEW_VIEW', 'REVIEW_APPROVE', 'REVIEW_REJECT')
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('TAX_PROFESSIONAL', 'PRACTITIONER', 'SENIOR_TAX_ASSOCIATE') AND p.code IN ('REVIEW_VIEW', 'REVIEW_SUBMIT', 'REVIEW_APPROVE', 'REVIEW_REJECT')
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('STAFF', 'ACCOUNTANT', 'TAX_ASSOCIATE', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'EMPLOYEE') AND p.code IN ('REVIEW_VIEW', 'REVIEW_SUBMIT')
ON CONFLICT DO NOTHING;

UPDATE marketplace_tax_services
SET suggested_price = 1999,
    billing_type = 'ONE_TIME'
WHERE code = 'GST_REGISTRATION';

UPDATE marketplace_tax_services
SET suggested_price = 1499,
    billing_type = 'PER_RETURN'
WHERE code = 'ITR_FILING';

UPDATE marketplace_tax_services
SET suggested_price = 2499,
    billing_type = 'PER_RETURN'
WHERE code = 'TDS_RETURN_FILING';

UPDATE marketplace_tax_services
SET suggested_price = 1499,
    billing_type = 'PER_RETURN'
WHERE code = 'GST_RETURN_FILING';

UPDATE marketplace_tax_services
SET suggested_price = 1999,
    billing_type = 'PER_CASE'
WHERE code = 'ITR_CORRECTION';

UPDATE marketplace_tax_services
SET suggested_price = 1999,
    billing_type = 'PER_RETURN'
WHERE code = 'TDS_COMPLIANCE';

UPDATE marketplace_tax_services
SET suggested_price = 1499,
    billing_type = 'PER_SESSION'
WHERE code = 'GST_ADVISORY';

UPDATE marketplace_tax_services
SET suggested_price = 2999,
    billing_type = 'PER_CASE'
WHERE code = 'ITR_NOTICE_ASSISTANCE';

UPDATE marketplace_tax_services
SET suggested_price = 1999,
    billing_type = 'PER_CASE'
WHERE code = 'ITR_REFUND_ASSISTANCE';

UPDATE marketplace_tax_services
SET suggested_price = 2499,
    billing_type = 'PER_SESSION'
WHERE code = 'ITR_PLANNING';
