-- ============================================================================
-- Migration: V67__create_early_access_requests.sql
-- Description: Create early_access_requests table for practice access requests
-- ============================================================================

CREATE TABLE IF NOT EXISTS early_access_requests (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    practice_name VARCHAR(200) NOT NULL,
    phone VARCHAR(30),
    city VARCHAR(100),
    practice_profile VARCHAR(100),
    primary_area VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    source VARCHAR(100) DEFAULT 'MARKETING_WEBSITE',
    notes TEXT,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_early_access_email ON early_access_requests(LOWER(email));
CREATE INDEX IF NOT EXISTS idx_early_access_status ON early_access_requests(status);
CREATE INDEX IF NOT EXISTS idx_early_access_created_at ON early_access_requests(created_at DESC);
