-- ==============================================================================
-- Taxoryn Platform — Phase 10 Production Hygiene (V66)
-- Purge Residual Demo & Test Data in Production Environments
-- ==============================================================================

-- 1. Safely remove known demo organizations and all cascaded records
-- (Foreign keys with ON DELETE CASCADE will automatically clean associated entities)
DELETE FROM organizations
WHERE email IN (
    'contact@apextax.com',
    'admin@apextax.com',
    'pawanadv@gmail.com',
    'contact@taxconsultancy.demo',
    'demo@taxoryn.com'
)
OR name IN (
    'Apex Tax Advisors LLP',
    'MAA MUNDESHWARI TAX CONSULTANCY',
    'Demo Tax Practice'
);

-- 2. Safely remove known demo customer users
DELETE FROM users
WHERE email IN (
    'rahul.sharma.tax@gmail.com',
    'priya.patel.biz@gmail.com',
    'vikram.mehta.corp@gmail.com',
    'ananya.deshmukh.consulting@gmail.com',
    'suresh.menon.retail@gmail.com'
);

-- 3. Safely remove test feedback from demo accounts
DELETE FROM application_feedback
WHERE user_email IN (
    'superadmin@taxoryn.com',
    'contact@apextax.com',
    'admin@apextax.com',
    'pawanadv@gmail.com',
    'rahul.sharma.tax@gmail.com'
);
