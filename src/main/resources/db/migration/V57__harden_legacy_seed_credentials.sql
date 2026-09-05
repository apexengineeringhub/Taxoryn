-- ==============================================================================
-- Taxoryn Platform — Phase 9 Security Hardening (V57)
-- Disable Legacy Seed User with Default Password (from V32)
-- ==============================================================================

-- 1. Safely disable legacy SuperAdmin account seeded in V32 and randomize password hash
-- so the hardcoded 'Password123!' credential cannot authenticate in production databases.
UPDATE users
SET status = 'INACTIVE',
    password_hash = '$2a$12$INACTIVE.LEGACY.SEED.ACCOUNT.INVALID.HASH.DO.NOT.USE.XXXX'
WHERE id = '00000000-0000-0000-0000-000000000002'
  AND email = 'superadmin@taxoryn.com';

