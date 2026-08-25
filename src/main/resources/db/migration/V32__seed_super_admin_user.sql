-- ==============================================================================
-- Taxoryn Platform — Super Admin Seed & Feedback Permission Grants
-- ==============================================================================

-- 1. Ensure Taxoryn Platform Root Organization exists
INSERT INTO organizations (id, name, legal_name, email, phone, status, subscription_plan)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Taxoryn Platform Operations',
    'Taxoryn Platform Technologies Pvt Ltd',
    'admin@taxoryn.com',
    '+918000000001',
    'ACTIVE',
    'ENTERPRISE'
) ON CONFLICT (id) DO NOTHING;

-- 2. Insert Super Admin User (Password: Password123!)
-- BCrypt 12-round hash for Password123!
INSERT INTO users (id, organization_id, email, password_hash, first_name, last_name, phone, status)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'superadmin@taxoryn.com',
    '$2a$12$KkQ0c8Y9fL7mH5nJ6oP.YeKj1h7f2c8a9m4p6q8t0v2x4z6b8d0f2',
    'Taxoryn',
    'SuperAdmin',
    '+918000000001',
    'ACTIVE'
) ON CONFLICT (id) DO NOTHING;

-- 3. Assign SUPER_ADMIN & ORG_ADMIN roles to superadmin@taxoryn.com
INSERT INTO user_roles (user_id, role_id)
VALUES
    ('00000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001'), -- SUPER_ADMIN
    ('00000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002')  -- ORG_ADMIN
ON CONFLICT DO NOTHING;

-- 4. Also grant all Feedback permissions to ORG_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000002', id FROM permissions
WHERE code IN ('FEEDBACK_VIEW', 'FEEDBACK_REVIEW', 'FEEDBACK_ASSIGN', 'FEEDBACK_RESOLVE', 'FEEDBACK_ESCALATE', 'FEEDBACK_MANAGE')
ON CONFLICT DO NOTHING;
