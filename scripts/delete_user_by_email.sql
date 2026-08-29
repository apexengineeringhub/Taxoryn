-- ==============================================================================
-- Taxoryn: Delete User & Related Records by Email (Direct SQL for DBeaver / pgAdmin)
--
-- Instructions:
-- Replace 'test@example.com' with the email address you want to clear and execute!
-- ==============================================================================

-- ==============================================================================
-- 1. INDIVIDUAL CUSTOMER ACCOUNT CLEANUP
-- ==============================================================================
DELETE FROM marketplace_leads 
WHERE lower(client_email) = lower('test@example.com')
   OR customer_id IN (SELECT id FROM marketplace_customer_profiles WHERE lower(email) = lower('test@example.com'));

DELETE FROM customer_tax_requirements 
WHERE customer_id IN (SELECT id FROM marketplace_customer_profiles WHERE lower(email) = lower('test@example.com'));

DELETE FROM marketplace_customer_profiles 
WHERE lower(email) = lower('test@example.com') 
   OR user_id IN (SELECT id FROM users WHERE lower(email) = lower('test@example.com'));

DELETE FROM whatsapp_messages 
WHERE user_id IN (SELECT id FROM users WHERE lower(email) = lower('test@example.com'));

DELETE FROM notifications 
WHERE user_id IN (SELECT id FROM users WHERE lower(email) = lower('test@example.com'));

DELETE FROM user_roles 
WHERE user_id IN (SELECT id FROM users WHERE lower(email) = lower('test@example.com'));

DELETE FROM application_feedback 
WHERE user_id IN (SELECT id FROM users WHERE lower(email) = lower('test@example.com'));

DELETE FROM audit_logs 
WHERE user_id IN (SELECT id FROM users WHERE lower(email) = lower('test@example.com'));

DELETE FROM users 
WHERE lower(email) = lower('test@example.com');


-- ==============================================================================
-- 2. PRACTITIONER / ORGANIZATION CLEANUP (Optional - only if clearing a Practice)
-- ==============================================================================
DELETE FROM marketplace_practice_locations 
WHERE organization_id IN (SELECT id FROM organizations WHERE lower(email) = lower('test@example.com'));

DELETE FROM marketplace_profiles 
WHERE organization_id IN (SELECT id FROM organizations WHERE lower(email) = lower('test@example.com'));

DELETE FROM invoice_payments 
WHERE organization_id IN (SELECT id FROM organizations WHERE lower(email) = lower('test@example.com'));

DELETE FROM invoice_items 
WHERE organization_id IN (SELECT id FROM organizations WHERE lower(email) = lower('test@example.com'));

DELETE FROM invoices 
WHERE organization_id IN (SELECT id FROM organizations WHERE lower(email) = lower('test@example.com'));

DELETE FROM client_portal_users 
WHERE client_id IN (SELECT id FROM clients WHERE organization_id IN (SELECT id FROM organizations WHERE lower(email) = lower('test@example.com')));

DELETE FROM client_contacts 
WHERE client_id IN (SELECT id FROM clients WHERE organization_id IN (SELECT id FROM organizations WHERE lower(email) = lower('test@example.com')));

DELETE FROM clients 
WHERE organization_id IN (SELECT id FROM organizations WHERE lower(email) = lower('test@example.com'));

DELETE FROM employee_roles 
WHERE employee_id IN (SELECT id FROM employees WHERE organization_id IN (SELECT id FROM organizations WHERE lower(email) = lower('test@example.com')));

DELETE FROM employees 
WHERE organization_id IN (SELECT id FROM organizations WHERE lower(email) = lower('test@example.com'));

DELETE FROM subscriptions 
WHERE organization_id IN (SELECT id FROM organizations WHERE lower(email) = lower('test@example.com'));

DELETE FROM organizations 
WHERE lower(email) = lower('test@example.com');

