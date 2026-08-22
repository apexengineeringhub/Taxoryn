-- ============================================================================
-- V19: Marketplace Onboarding & KYC Pipeline Module
-- Strict Multi-Stage Separation: Lead -> Proposal -> Onboarding -> Client Master -> Client Portal
-- ============================================================================

-- 1. Proposals Table
CREATE TABLE IF NOT EXISTS marketplace_proposals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    marketplace_profile_id UUID NOT NULL,
    lead_id UUID NOT NULL,
    service_id UUID,
    proposal_title VARCHAR(255) NOT NULL,
    scope_of_work TEXT NOT NULL,
    deliverables TEXT,
    fee_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    pricing_type VARCHAR(50) NOT NULL DEFAULT 'FIXED', -- FIXED, MONTHLY_RETAINER, HOURLY
    estimated_timeline_days INT DEFAULT 7,
    proposal_status VARCHAR(50) NOT NULL DEFAULT 'SENT', -- DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED
    access_token VARCHAR(100) NOT NULL UNIQUE,
    valid_until DATE,
    rejection_reason TEXT,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_prop_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_prop_prof FOREIGN KEY (marketplace_profile_id) REFERENCES marketplace_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_prop_lead FOREIGN KEY (lead_id) REFERENCES marketplace_leads(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_prop_org ON marketplace_proposals(organization_id);
CREATE INDEX IF NOT EXISTS idx_prop_lead ON marketplace_proposals(lead_id);
CREATE INDEX IF NOT EXISTS idx_prop_token ON marketplace_proposals(access_token);

-- 2. Onboardings Table (Staging Area BEFORE Client Master Creation)
CREATE TABLE IF NOT EXISTS marketplace_onboardings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    marketplace_profile_id UUID NOT NULL,
    lead_id UUID NOT NULL,
    proposal_id UUID,
    access_token VARCHAR(100) NOT NULL UNIQUE,
    client_name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    client_email VARCHAR(255) NOT NULL,
    client_phone VARCHAR(20) NOT NULL,
    entity_type VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL', -- INDIVIDUAL, COMPANY, LLP, FIRM, HUF, TRUST
    pan VARCHAR(10),
    gstin VARCHAR(15),
    tan VARCHAR(10),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    onboarding_status VARCHAR(50) NOT NULL DEFAULT 'INITIATED', -- INITIATED, DOCUMENTS_PENDING, UNDER_REVIEW, APPROVED, REJECTED
    engagement_letter_signed BOOLEAN NOT NULL DEFAULT FALSE,
    engagement_signed_at TIMESTAMPTZ,
    engagement_letter_url TEXT,
    fee_agreement_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_employee_id UUID,
    promoted_client_id UUID, -- NULL until approved & promoted to Client Master!
    portal_user_id UUID, -- NULL until Client Portal user is provisioned!
    reviewer_notes TEXT,
    rejection_reason TEXT,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_onb_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_onb_prof FOREIGN KEY (marketplace_profile_id) REFERENCES marketplace_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_onb_lead FOREIGN KEY (lead_id) REFERENCES marketplace_leads(id) ON DELETE CASCADE,
    CONSTRAINT fk_onb_client FOREIGN KEY (promoted_client_id) REFERENCES clients(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_onb_org ON marketplace_onboardings(organization_id);
CREATE INDEX IF NOT EXISTS idx_onb_lead ON marketplace_onboardings(lead_id);
CREATE INDEX IF NOT EXISTS idx_onb_token ON marketplace_onboardings(access_token);
CREATE INDEX IF NOT EXISTS idx_onb_status ON marketplace_onboardings(onboarding_status);

-- 3. Onboarding KYC Documents Table
CREATE TABLE IF NOT EXISTS marketplace_onboarding_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    onboarding_id UUID NOT NULL,
    document_type VARCHAR(100) NOT NULL, -- PAN_CARD, AADHAAR_CARD, CERTIFICATE_OF_INCORPORATION, GST_CERTIFICATE, ADDRESS_PROOF, BOARD_RESOLUTION, CANCELLED_CHEQUE, OTHER
    document_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    file_size_bytes BIGINT DEFAULT 0,
    content_type VARCHAR(100),
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    verification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, VERIFIED, REJECTED
    rejection_reason TEXT,
    verified_at TIMESTAMPTZ,
    verified_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_onb_doc FOREIGN KEY (onboarding_id) REFERENCES marketplace_onboardings(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_onb_doc_onb ON marketplace_onboarding_documents(onboarding_id);
