-- ==============================================================================
-- Taxoryn Platform - Flyway Migration V44
-- Ensure Complete Schema Tables: Application Feedback & Marketplace CRM
-- ==============================================================================

-- 1. Ensure application_feedback table exists
CREATE TABLE IF NOT EXISTS application_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    feedback_type VARCHAR(30) NOT NULL DEFAULT 'FEATURE_REQUEST',
    category VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
    rating INTEGER,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    page_path VARCHAR(500),
    feature_name VARCHAR(100),
    source VARCHAR(40) NOT NULL DEFAULT 'WEB',
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    practice_id UUID,
    actor_type VARCHAR(30) NOT NULL DEFAULT 'ANONYMOUS',
    actor_id UUID,
    actor_name VARCHAR(150),
    actor_email VARCHAR(255),
    organization_name VARCHAR(255),
    client_name VARCHAR(255),
    duplicate_of_id UUID,
    resolution_note VARCHAR(4000),
    resolved_by UUID,
    resolved_at TIMESTAMPTZ,
    closed_by UUID,
    closed_at TIMESTAMPTZ,
    assigned_team VARCHAR(50),
    assigned_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Ensure all columns exist in application_feedback if table was partially created
ALTER TABLE application_feedback
    ADD COLUMN IF NOT EXISTS practice_id UUID,
    ADD COLUMN IF NOT EXISTS actor_type VARCHAR(30) NOT NULL DEFAULT 'ANONYMOUS',
    ADD COLUMN IF NOT EXISTS actor_id UUID,
    ADD COLUMN IF NOT EXISTS actor_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS actor_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS organization_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS client_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS duplicate_of_id UUID,
    ADD COLUMN IF NOT EXISTS resolution_note VARCHAR(4000),
    ADD COLUMN IF NOT EXISTS resolved_by UUID,
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS closed_by UUID,
    ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS assigned_team VARCHAR(50),
    ADD COLUMN IF NOT EXISTS assigned_user_id UUID,
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_application_feedback_user_id ON application_feedback(user_id);
CREATE INDEX IF NOT EXISTS idx_application_feedback_type ON application_feedback(feedback_type);
CREATE INDEX IF NOT EXISTS idx_application_feedback_status ON application_feedback(status);
CREATE INDEX IF NOT EXISTS idx_application_feedback_created_at ON application_feedback(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_application_feedback_practice_id ON application_feedback(practice_id);
CREATE INDEX IF NOT EXISTS idx_application_feedback_actor_type ON application_feedback(actor_type);

-- 2. Ensure feedback_assignments table exists
CREATE TABLE IF NOT EXISTS feedback_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID NOT NULL,
    team VARCHAR(50) NOT NULL,
    assigned_user_id UUID,
    assigned_by UUID,
    reason VARCHAR(500),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_feedback_assignments_feedback ON feedback_assignments(feedback_id, is_active);
CREATE INDEX IF NOT EXISTS idx_feedback_assignments_user ON feedback_assignments(assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_feedback_assignments_team ON feedback_assignments(team);

-- 3. Ensure feedback_notes table exists
CREATE TABLE IF NOT EXISTS feedback_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID NOT NULL,
    author_id UUID,
    author_name VARCHAR(150),
    note VARCHAR(4000) NOT NULL,
    visibility VARCHAR(30) NOT NULL DEFAULT 'INTERNAL',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_feedback_notes_feedback ON feedback_notes(feedback_id, created_at ASC);

-- 4. Ensure feedback_status_history table exists
CREATE TABLE IF NOT EXISTS feedback_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by UUID,
    changed_by_name VARCHAR(150),
    reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_feedback_status_history_feedback ON feedback_status_history(feedback_id, created_at ASC);

-- 5. Ensure engineering_issues table exists
CREATE TABLE IF NOT EXISTS engineering_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID NOT NULL UNIQUE,
    issue_code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'HIGH',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_team VARCHAR(50) NOT NULL DEFAULT 'ENGINEERING',
    creator_user_id UUID,
    external_system VARCHAR(50),
    external_issue_id VARCHAR(100),
    external_issue_url VARCHAR(500),
    external_status VARCHAR(50),
    last_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_engineering_issues_code ON engineering_issues(issue_code);
CREATE INDEX IF NOT EXISTS idx_engineering_issues_status ON engineering_issues(status);

-- 6. Ensure marketplace_leads table exists
CREATE TABLE IF NOT EXISTS marketplace_leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    marketplace_profile_id UUID NOT NULL,
    service_id UUID,
    client_name VARCHAR(255) NOT NULL,
    client_email VARCHAR(255) NOT NULL,
    client_phone VARCHAR(20) NOT NULL,
    city VARCHAR(100),
    pan VARCHAR(10),
    gstin VARCHAR(15),
    service_category VARCHAR(100),
    requirement_description TEXT,
    budget_range VARCHAR(50),
    urgency VARCHAR(50) DEFAULT 'STANDARD',
    lead_status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    converted_client_id UUID,
    assigned_employee_id UUID,
    practitioner_notes TEXT,
    customer_id UUID,
    tax_requirement_id UUID,
    tax_service_id UUID,
    financial_year VARCHAR(20),
    customer_type VARCHAR(50),
    early_enquiry_message TEXT,
    is_contact_masked BOOLEAN DEFAULT TRUE,
    source_type VARCHAR(50),
    source_content_id UUID,
    reference_number VARCHAR(30),
    enquiry_status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    rejection_reason VARCHAR(50),
    rejection_note VARCHAR(500),
    cancellation_reason VARCHAR(500),
    received_at TIMESTAMP WITH TIME ZONE,
    accepted_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    review_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- 7. Ensure marketplace_reviews table exists
CREATE TABLE IF NOT EXISTS marketplace_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    marketplace_profile_id UUID NOT NULL,
    lead_id UUID,
    reviewer_name VARCHAR(255) NOT NULL,
    reviewer_designation VARCHAR(255),
    reviewer_company VARCHAR(255),
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_title VARCHAR(255),
    review_comment TEXT NOT NULL,
    service_taken VARCHAR(100),
    is_verified_client BOOLEAN DEFAULT TRUE,
    status VARCHAR(50) NOT NULL DEFAULT 'APPROVED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- 8. Ensure marketplace_enquiry_messages table exists
CREATE TABLE IF NOT EXISTS marketplace_enquiry_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enquiry_id UUID NOT NULL,
    sender_type VARCHAR(30) NOT NULL,
    sender_user_id UUID,
    sender_name VARCHAR(150) NOT NULL,
    message_body TEXT NOT NULL,
    attachments_json TEXT,
    is_read_by_customer BOOLEAN NOT NULL DEFAULT FALSE,
    is_read_by_practice BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE marketplace_enquiry_messages
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_enq_msg_enquiry_created ON marketplace_enquiry_messages(enquiry_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_enq_msg_customer_unread ON marketplace_enquiry_messages(enquiry_id, is_read_by_customer);
CREATE INDEX IF NOT EXISTS idx_enq_msg_practice_unread ON marketplace_enquiry_messages(enquiry_id, is_read_by_practice);
