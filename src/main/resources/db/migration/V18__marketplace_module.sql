-- ==============================================================================
-- Taxoryn Platform — Flyway Migration V18: Customer Marketplace & Discovery Module
-- Multi-Tenant Practice Marketplace, Lead-to-Client CRM Pipeline & Admin Governance
-- ==============================================================================

-- 1. Marketplace Public Profiles (Tax Firm Directory Listings)
CREATE TABLE IF NOT EXISTS marketplace_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    headline VARCHAR(255),
    bio TEXT,
    professional_type VARCHAR(50) NOT NULL DEFAULT 'CHARTERED_ACCOUNTANT',
    experience_years INT DEFAULT 5,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(255),
    website_url VARCHAR(255),
    avatar_url TEXT,
    banner_url TEXT,
    specializations TEXT,
    languages_spoken VARCHAR(255) DEFAULT 'English, Hindi',
    starting_fee NUMERIC(15, 2) DEFAULT 999.00,
    hourly_rate NUMERIC(15, 2) DEFAULT 1500.00,
    average_rating NUMERIC(3, 2) DEFAULT 4.90,
    total_reviews INT DEFAULT 0,
    total_clients_served INT DEFAULT 0,
    verification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    consultation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    consultation_fee NUMERIC(15, 2) DEFAULT 499.00,
    consultation_duration_minutes INT DEFAULT 30,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_mp_profile_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mp_profiles_org ON marketplace_profiles(organization_id);
CREATE INDEX IF NOT EXISTS idx_mp_profiles_city ON marketplace_profiles(city);
CREATE INDEX IF NOT EXISTS idx_mp_profiles_published ON marketplace_profiles(is_published);
CREATE INDEX IF NOT EXISTS idx_mp_profiles_prof_type ON marketplace_profiles(professional_type);

-- 2. Marketplace Service Packages (Fixed-Fee & Retainer Offerings)
CREATE TABLE IF NOT EXISTS marketplace_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    marketplace_profile_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    price NUMERIC(15, 2) NOT NULL,
    pricing_type VARCHAR(50) NOT NULL DEFAULT 'FIXED',
    delivery_days INT DEFAULT 3,
    deliverables TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_mp_services_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_mp_services_profile FOREIGN KEY (marketplace_profile_id) REFERENCES marketplace_profiles(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mp_services_org ON marketplace_services(organization_id);
CREATE INDEX IF NOT EXISTS idx_mp_services_profile ON marketplace_services(marketplace_profile_id);
CREATE INDEX IF NOT EXISTS idx_mp_services_category ON marketplace_services(category);

-- 3. Marketplace Inbound Leads & Requirements
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_mp_leads_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_mp_leads_profile FOREIGN KEY (marketplace_profile_id) REFERENCES marketplace_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_mp_leads_service FOREIGN KEY (service_id) REFERENCES marketplace_services(id) ON DELETE SET NULL,
    CONSTRAINT fk_mp_leads_converted_client FOREIGN KEY (converted_client_id) REFERENCES clients(id) ON DELETE SET NULL,
    CONSTRAINT fk_mp_leads_assigned_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_mp_leads_org ON marketplace_leads(organization_id);
CREATE INDEX IF NOT EXISTS idx_mp_leads_profile ON marketplace_leads(marketplace_profile_id);
CREATE INDEX IF NOT EXISTS idx_mp_leads_status ON marketplace_leads(lead_status);

-- 4. Marketplace Consultations & Appointments
CREATE TABLE IF NOT EXISTS marketplace_consultations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    marketplace_profile_id UUID NOT NULL,
    lead_id UUID,
    client_name VARCHAR(255) NOT NULL,
    client_email VARCHAR(255) NOT NULL,
    client_phone VARCHAR(20) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    consultation_mode VARCHAR(50) NOT NULL DEFAULT 'VIDEO',
    meeting_link VARCHAR(500),
    booking_date DATE NOT NULL,
    start_time VARCHAR(10) NOT NULL,
    end_time VARCHAR(10) NOT NULL,
    fee_amount NUMERIC(15, 2) DEFAULT 0.00,
    payment_status VARCHAR(50) DEFAULT 'PAID',
    consultation_status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    assigned_employee_id UUID,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_mp_consultations_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_mp_consultations_profile FOREIGN KEY (marketplace_profile_id) REFERENCES marketplace_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_mp_consultations_lead FOREIGN KEY (lead_id) REFERENCES marketplace_leads(id) ON DELETE SET NULL,
    CONSTRAINT fk_mp_consultations_emp FOREIGN KEY (assigned_employee_id) REFERENCES employees(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_mp_consultations_org ON marketplace_consultations(organization_id);
CREATE INDEX IF NOT EXISTS idx_mp_consultations_profile ON marketplace_consultations(marketplace_profile_id);
CREATE INDEX IF NOT EXISTS idx_mp_consultations_date ON marketplace_consultations(booking_date);

-- 5. Marketplace Reviews & Ratings
CREATE TABLE IF NOT EXISTS marketplace_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    marketplace_profile_id UUID NOT NULL,
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
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_mp_reviews_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_mp_reviews_profile FOREIGN KEY (marketplace_profile_id) REFERENCES marketplace_profiles(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mp_reviews_org ON marketplace_reviews(organization_id);
CREATE INDEX IF NOT EXISTS idx_mp_reviews_profile ON marketplace_reviews(marketplace_profile_id);

-- 6. Practitioner KYC & Credential Verifications
CREATE TABLE IF NOT EXISTS marketplace_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    marketplace_profile_id UUID NOT NULL,
    professional_body VARCHAR(100) NOT NULL,
    membership_number VARCHAR(100) NOT NULL,
    cop_number VARCHAR(100),
    firm_registration_number VARCHAR(100),
    document_url TEXT,
    verification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    verified_at TIMESTAMPTZ,
    verified_by VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_mp_verifications_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_mp_verifications_profile FOREIGN KEY (marketplace_profile_id) REFERENCES marketplace_profiles(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mp_verifications_org ON marketplace_verifications(organization_id);
CREATE INDEX IF NOT EXISTS idx_mp_verifications_profile ON marketplace_verifications(marketplace_profile_id);
CREATE INDEX IF NOT EXISTS idx_mp_verifications_status ON marketplace_verifications(verification_status);
