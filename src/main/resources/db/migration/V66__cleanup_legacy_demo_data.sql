-- ==============================================================================
-- Taxoryn Platform — Phase 10 Production Hygiene (V66)
-- Purge Residual Demo & Test Data in Production Environments
-- ==============================================================================

DO $$
DECLARE
    v_org_ids UUID[];
    v_user_ids UUID[];
    v_client_ids UUID[];
    v_invoice_ids UUID[];
    v_notice_ids UUID[];
    v_task_ids UUID[];
    v_onboarding_ids UUID[];
    v_marketplace_profile_ids UUID[];
    v_role_ids UUID[];
BEGIN
    -- 1. Identify Demo Organization IDs
    SELECT ARRAY_AGG(id) INTO v_org_ids
    FROM organizations
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

    -- 2. Identify Demo User IDs
    SELECT ARRAY_AGG(id) INTO v_user_ids
    FROM users
    WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
    OR email IN (
        'rahul.sharma.tax@gmail.com',
        'priya.patel.biz@gmail.com',
        'vikram.mehta.corp@gmail.com',
        'ananya.deshmukh.consulting@gmail.com',
        'suresh.menon.retail@gmail.com',
        'contact@apextax.com',
        'admin@apextax.com',
        'pawanadv@gmail.com'
    );

    -- 3. Identify Demo Client IDs
    IF to_regclass('public.clients') IS NOT NULL AND v_org_ids IS NOT NULL THEN
        SELECT ARRAY_AGG(id) INTO v_client_ids
        FROM clients
        WHERE organization_id = ANY(v_org_ids);
    END IF;

    -- 4. Identify Demo Invoice IDs
    IF to_regclass('public.invoices') IS NOT NULL THEN
        SELECT ARRAY_AGG(id) INTO v_invoice_ids
        FROM invoices
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;

    -- 5. Identify Demo Tax Notice IDs
    IF to_regclass('public.tax_notices') IS NOT NULL THEN
        SELECT ARRAY_AGG(id) INTO v_notice_ids
        FROM tax_notices
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;

    -- 6. Identify Demo Task IDs
    IF to_regclass('public.tasks') IS NOT NULL THEN
        SELECT ARRAY_AGG(id) INTO v_task_ids
        FROM tasks
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;

    -- 7. Identify Demo Marketplace Onboarding IDs
    IF to_regclass('public.marketplace_onboardings') IS NOT NULL THEN
        SELECT ARRAY_AGG(id) INTO v_onboarding_ids
        FROM marketplace_onboardings
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;

    -- 8. Identify Demo Marketplace Profile IDs
    IF to_regclass('public.marketplace_profiles') IS NOT NULL AND v_org_ids IS NOT NULL THEN
        SELECT ARRAY_AGG(id) INTO v_marketplace_profile_ids
        FROM marketplace_profiles
        WHERE organization_id = ANY(v_org_ids);
    END IF;

    -- 9. Identify Demo Custom Role IDs
    IF to_regclass('public.roles') IS NOT NULL AND v_org_ids IS NOT NULL THEN
        SELECT ARRAY_AGG(id) INTO v_role_ids
        FROM roles
        WHERE organization_id = ANY(v_org_ids);
    END IF;

    -- -------------------------------------------------------------------------
    -- REVERSE DEPENDENCY CLEANUP
    -- -------------------------------------------------------------------------

    -- WhatsApp Messages
    IF to_regclass('public.whatsapp_messages') IS NOT NULL THEN
        DELETE FROM whatsapp_messages
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_user_ids IS NOT NULL AND user_id = ANY(v_user_ids));
    END IF;

    -- Billing: Items and Payments before Invoices
    IF to_regclass('public.invoice_items') IS NOT NULL THEN
        DELETE FROM invoice_items
        WHERE (v_invoice_ids IS NOT NULL AND invoice_id = ANY(v_invoice_ids));
    END IF;
    IF to_regclass('public.billing_items') IS NOT NULL THEN
        DELETE FROM billing_items
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.invoice_payments') IS NOT NULL THEN
        DELETE FROM invoice_payments
        WHERE (v_invoice_ids IS NOT NULL AND invoice_id = ANY(v_invoice_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;
    IF to_regclass('public.payments') IS NOT NULL THEN
        DELETE FROM payments
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.invoices') IS NOT NULL THEN
        DELETE FROM invoices
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;

    -- Tax Notice Management: Child responses, hearings, communications before notices
    IF to_regclass('public.notice_responses') IS NOT NULL THEN
        DELETE FROM notice_responses
        WHERE (v_notice_ids IS NOT NULL AND notice_id = ANY(v_notice_ids));
    END IF;
    IF to_regclass('public.notice_hearings') IS NOT NULL THEN
        DELETE FROM notice_hearings
        WHERE (v_notice_ids IS NOT NULL AND notice_id = ANY(v_notice_ids));
    END IF;
    IF to_regclass('public.notice_activities') IS NOT NULL THEN
        DELETE FROM notice_activities
        WHERE (v_notice_ids IS NOT NULL AND notice_id = ANY(v_notice_ids));
    END IF;
    IF to_regclass('public.tax_notice_communications') IS NOT NULL THEN
        DELETE FROM tax_notice_communications
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.tax_notice_replies') IS NOT NULL THEN
        DELETE FROM tax_notice_replies
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.tax_notices') IS NOT NULL THEN
        DELETE FROM tax_notices
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;

    -- Document Management & Document Requests
    IF to_regclass('public.client_document_requests') IS NOT NULL THEN
        DELETE FROM client_document_requests
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;
    IF to_regclass('public.document_requests') IS NOT NULL THEN
        DELETE FROM document_requests
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;
    IF to_regclass('public.documents') IS NOT NULL THEN
        DELETE FROM documents
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;

    -- GST Module
    IF to_regclass('public.gst_monthly_summaries') IS NOT NULL THEN
        DELETE FROM gst_monthly_summaries
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.gst_return_filings') IS NOT NULL THEN
        DELETE FROM gst_return_filings
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.gst_filing_history') IS NOT NULL THEN
        DELETE FROM gst_filing_history
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.gst_client_profiles') IS NOT NULL THEN
        DELETE FROM gst_client_profiles
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.gst_returns') IS NOT NULL THEN
        DELETE FROM gst_returns
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;
    IF to_regclass('public.gst_profiles') IS NOT NULL THEN
        DELETE FROM gst_profiles
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;

    -- ITR Module
    IF to_regclass('public.itr_filing_history') IS NOT NULL THEN
        DELETE FROM itr_filing_history
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.itr_client_profiles') IS NOT NULL THEN
        DELETE FROM itr_client_profiles
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.itr_returns') IS NOT NULL THEN
        DELETE FROM itr_returns
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;
    IF to_regclass('public.itr_profiles') IS NOT NULL THEN
        DELETE FROM itr_profiles
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;

    -- TDS Module
    IF to_regclass('public.tds_certificates') IS NOT NULL THEN
        DELETE FROM tds_certificates
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.tds_deductee_entries') IS NOT NULL THEN
        DELETE FROM tds_deductee_entries
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.tds_challans') IS NOT NULL THEN
        DELETE FROM tds_challans
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.tds_deductees') IS NOT NULL THEN
        DELETE FROM tds_deductees
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.tds_returns') IS NOT NULL THEN
        DELETE FROM tds_returns
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;
    IF to_regclass('public.tds_deductors') IS NOT NULL THEN
        DELETE FROM tds_deductors
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;
    IF to_regclass('public.tds_profiles') IS NOT NULL THEN
        DELETE FROM tds_profiles
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;

    -- Tasks & Workload: Attachments, comments, logs, history before tasks
    IF to_regclass('public.task_attachments') IS NOT NULL THEN
        DELETE FROM task_attachments
        WHERE (v_task_ids IS NOT NULL AND task_id = ANY(v_task_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.task_comments') IS NOT NULL THEN
        DELETE FROM task_comments
        WHERE (v_task_ids IS NOT NULL AND task_id = ANY(v_task_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.task_time_logs') IS NOT NULL THEN
        DELETE FROM task_time_logs
        WHERE (v_task_ids IS NOT NULL AND task_id = ANY(v_task_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.task_history') IS NOT NULL THEN
        DELETE FROM task_history
        WHERE (v_task_ids IS NOT NULL AND task_id = ANY(v_task_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.compliance_tasks') IS NOT NULL THEN
        DELETE FROM compliance_tasks
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.tasks') IS NOT NULL THEN
        DELETE FROM tasks
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;

    -- Compliance Calendar
    IF to_regclass('public.compliance_obligations') IS NOT NULL THEN
        DELETE FROM compliance_obligations
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.compliance_rules') IS NOT NULL THEN
        DELETE FROM compliance_rules
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.compliance_calendar_events') IS NOT NULL THEN
        DELETE FROM compliance_calendar_events
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;

    -- Clients & Client Portal
    IF to_regclass('public.client_notifications') IS NOT NULL THEN
        DELETE FROM client_notifications
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;
    IF to_regclass('public.client_portal_activities') IS NOT NULL THEN
        DELETE FROM client_portal_activities
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;
    IF to_regclass('public.client_contacts') IS NOT NULL THEN
        DELETE FROM client_contacts
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_client_ids IS NOT NULL AND client_id = ANY(v_client_ids));
    END IF;
    IF to_regclass('public.client_users') IS NOT NULL THEN
        DELETE FROM client_users
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_user_ids IS NOT NULL AND user_id = ANY(v_user_ids));
    END IF;
    IF to_regclass('public.clients') IS NOT NULL AND v_org_ids IS NOT NULL THEN
        DELETE FROM clients
        WHERE organization_id = ANY(v_org_ids);
    END IF;

    -- Employees
    IF to_regclass('public.organization_employee_numbers') IS NOT NULL AND v_org_ids IS NOT NULL THEN
        DELETE FROM organization_employee_numbers
        WHERE organization_id = ANY(v_org_ids);
    END IF;
    IF to_regclass('public.employees') IS NOT NULL AND v_org_ids IS NOT NULL THEN
        DELETE FROM employees
        WHERE organization_id = ANY(v_org_ids);
    END IF;

    -- Marketplace & Practice Profiles (delete all child dependencies before profiles)
    IF to_regclass('public.marketplace_profile_slug_redirects') IS NOT NULL THEN
        DELETE FROM marketplace_profile_slug_redirects
        WHERE (v_marketplace_profile_ids IS NOT NULL AND profile_id = ANY(v_marketplace_profile_ids));
    END IF;
    IF to_regclass('public.marketplace_practice_services') IS NOT NULL THEN
        DELETE FROM marketplace_practice_services
        WHERE (v_marketplace_profile_ids IS NOT NULL AND marketplace_profile_id = ANY(v_marketplace_profile_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.marketplace_practice_locations') IS NOT NULL THEN
        DELETE FROM marketplace_practice_locations
        WHERE (v_marketplace_profile_ids IS NOT NULL AND marketplace_profile_id = ANY(v_marketplace_profile_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.marketplace_onboarding_documents') IS NOT NULL THEN
        DELETE FROM marketplace_onboarding_documents
        WHERE (v_onboarding_ids IS NOT NULL AND onboarding_id = ANY(v_onboarding_ids));
    END IF;
    IF to_regclass('public.marketplace_onboardings') IS NOT NULL THEN
        DELETE FROM marketplace_onboardings
        WHERE (v_marketplace_profile_ids IS NOT NULL AND marketplace_profile_id = ANY(v_marketplace_profile_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.marketplace_proposals') IS NOT NULL THEN
        DELETE FROM marketplace_proposals
        WHERE (v_marketplace_profile_ids IS NOT NULL AND marketplace_profile_id = ANY(v_marketplace_profile_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.marketplace_enquiry_messages') IS NOT NULL THEN
        DELETE FROM marketplace_enquiry_messages
        WHERE (v_user_ids IS NOT NULL AND sender_user_id = ANY(v_user_ids));
    END IF;
    IF to_regclass('public.marketplace_verifications') IS NOT NULL THEN
        DELETE FROM marketplace_verifications
        WHERE (v_marketplace_profile_ids IS NOT NULL AND marketplace_profile_id = ANY(v_marketplace_profile_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.marketplace_reviews') IS NOT NULL THEN
        DELETE FROM marketplace_reviews
        WHERE (v_marketplace_profile_ids IS NOT NULL AND marketplace_profile_id = ANY(v_marketplace_profile_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.marketplace_consultations') IS NOT NULL THEN
        DELETE FROM marketplace_consultations
        WHERE (v_marketplace_profile_ids IS NOT NULL AND marketplace_profile_id = ANY(v_marketplace_profile_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.marketplace_leads') IS NOT NULL THEN
        DELETE FROM marketplace_leads
        WHERE (v_marketplace_profile_ids IS NOT NULL AND marketplace_profile_id = ANY(v_marketplace_profile_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_user_ids IS NOT NULL AND customer_id = ANY(v_user_ids));
    END IF;
    IF to_regclass('public.marketplace_services') IS NOT NULL THEN
        DELETE FROM marketplace_services
        WHERE (v_marketplace_profile_ids IS NOT NULL AND marketplace_profile_id = ANY(v_marketplace_profile_ids))
           OR (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.marketplace_profiles') IS NOT NULL THEN
        DELETE FROM marketplace_profiles
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.practice_profile_locations') IS NOT NULL THEN
        DELETE FROM practice_profile_locations
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.practice_locations') IS NOT NULL THEN
        DELETE FROM practice_locations
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.practice_services') IS NOT NULL THEN
        DELETE FROM practice_services
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.practice_profiles') IS NOT NULL THEN
        DELETE FROM practice_profiles
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.practice_branding') IS NOT NULL THEN
        DELETE FROM practice_branding
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.customer_tax_requirements') IS NOT NULL THEN
        DELETE FROM customer_tax_requirements
        WHERE customer_id IN (
            SELECT id FROM marketplace_customer_profiles
            WHERE v_user_ids IS NOT NULL AND user_id = ANY(v_user_ids)
        );
    END IF;
    IF to_regclass('public.marketplace_customer_profiles') IS NOT NULL THEN
        DELETE FROM marketplace_customer_profiles
        WHERE (v_user_ids IS NOT NULL AND user_id = ANY(v_user_ids));
    END IF;

    -- Subscriptions
    IF to_regclass('public.subscription_invoices') IS NOT NULL THEN
        DELETE FROM subscription_invoices
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.subscriptions') IS NOT NULL THEN
        DELETE FROM subscriptions
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;

    -- Notifications & Auth Tokens
    IF to_regclass('public.notifications') IS NOT NULL THEN
        DELETE FROM notifications
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_user_ids IS NOT NULL AND user_id = ANY(v_user_ids));
    END IF;
    IF to_regclass('public.email_logs') IS NOT NULL THEN
        DELETE FROM email_logs
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_user_ids IS NOT NULL AND user_id = ANY(v_user_ids));
    END IF;
    IF to_regclass('public.organization_activation_tokens') IS NOT NULL THEN
        DELETE FROM organization_activation_tokens
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids));
    END IF;
    IF to_regclass('public.password_reset_tokens') IS NOT NULL THEN
        DELETE FROM password_reset_tokens
        WHERE (v_user_ids IS NOT NULL AND user_id = ANY(v_user_ids));
    END IF;
    IF to_regclass('public.refresh_tokens') IS NOT NULL THEN
        DELETE FROM refresh_tokens
        WHERE (v_user_ids IS NOT NULL AND user_id = ANY(v_user_ids));
    END IF;

    -- Audit Logs
    IF to_regclass('public.audit_logs') IS NOT NULL THEN
        DELETE FROM audit_logs
        WHERE (v_org_ids IS NOT NULL AND organization_id = ANY(v_org_ids))
           OR (v_user_ids IS NOT NULL AND user_id = ANY(v_user_ids));
    END IF;

    -- Feedback Management
    IF to_regclass('public.engineering_issues') IS NOT NULL THEN
        DELETE FROM engineering_issues
        WHERE (v_user_ids IS NOT NULL AND creator_user_id = ANY(v_user_ids));
    END IF;
    IF to_regclass('public.feedback_status_history') IS NOT NULL THEN
        DELETE FROM feedback_status_history
        WHERE (v_user_ids IS NOT NULL AND changed_by = ANY(v_user_ids));
    END IF;
    IF to_regclass('public.feedback_assignments') IS NOT NULL THEN
        DELETE FROM feedback_assignments
        WHERE (v_user_ids IS NOT NULL AND (assigned_user_id = ANY(v_user_ids) OR assigned_by = ANY(v_user_ids)));
    END IF;
    IF to_regclass('public.feedback_notes') IS NOT NULL THEN
        DELETE FROM feedback_notes
        WHERE (v_user_ids IS NOT NULL AND author_id = ANY(v_user_ids));
    END IF;
    IF to_regclass('public.application_feedback') IS NOT NULL THEN
        DELETE FROM application_feedback
        WHERE (v_user_ids IS NOT NULL AND user_id = ANY(v_user_ids))
           OR (v_org_ids IS NOT NULL AND practice_id = ANY(v_org_ids));
    END IF;

    -- Roles & User Roles
    IF to_regclass('public.user_roles') IS NOT NULL THEN
        DELETE FROM user_roles
        WHERE (v_user_ids IS NOT NULL AND user_id = ANY(v_user_ids))
           OR (v_role_ids IS NOT NULL AND role_id = ANY(v_role_ids));
    END IF;
    IF to_regclass('public.role_permissions') IS NOT NULL AND v_role_ids IS NOT NULL THEN
        DELETE FROM role_permissions
        WHERE role_id = ANY(v_role_ids);
    END IF;
    IF to_regclass('public.roles') IS NOT NULL AND v_role_ids IS NOT NULL THEN
        DELETE FROM roles
        WHERE id = ANY(v_role_ids);
    END IF;

    -- Users
    IF to_regclass('public.users') IS NOT NULL AND v_user_ids IS NOT NULL THEN
        DELETE FROM users
        WHERE id = ANY(v_user_ids);
    END IF;

    -- Organization Settings & Organizations
    IF to_regclass('public.organization_settings') IS NOT NULL AND v_org_ids IS NOT NULL THEN
        DELETE FROM organization_settings
        WHERE organization_id = ANY(v_org_ids);
    END IF;
    IF v_org_ids IS NOT NULL THEN
        DELETE FROM organizations
        WHERE id = ANY(v_org_ids);
    END IF;

END $$;
