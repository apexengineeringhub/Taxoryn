-- ==============================================================================
-- Taxoryn Platform - Phase 32.5 Migration (V145)
-- Client Approvals & Consent Foundation
-- ==============================================================================

CREATE TABLE IF NOT EXISTS client_approval_requests (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    approval_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    context_type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    context_id UUID,
    context_title VARCHAR(255),
    document_id UUID,
    content_version VARCHAR(50),
    content_hash VARCHAR(128),
    requested_by_user_id UUID,
    requested_by_name VARCHAR(255),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    responded_by_user_id UUID,
    responded_by_name VARCHAR(255),
    responded_by_email VARCHAR(255),
    responded_at TIMESTAMP WITH TIME ZONE,
    decision_notes TEXT,
    rejection_reason TEXT,
    revoked_by_user_id UUID,
    revoked_by_name VARCHAR(255),
    revoked_at TIMESTAMP WITH TIME ZONE,
    revocation_reason TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Composite indexes for efficient querying and tenant isolation
CREATE INDEX IF NOT EXISTS idx_client_approvals_org_client_status
    ON client_approval_requests (organization_id, client_id, status);

CREATE INDEX IF NOT EXISTS idx_client_approvals_context
    ON client_approval_requests (organization_id, client_id, context_type, context_id);

CREATE INDEX IF NOT EXISTS idx_client_approvals_status_expires
    ON client_approval_requests (status, expires_at);
