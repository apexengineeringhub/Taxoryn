-- ============================================================================
-- Taxoryn Phase 26.5: Taxpayer Consent & Delegation Management
-- Database Migration: V125__create_taxpayer_consents.sql
-- Description: Tenant-scoped taxpayer consent, practitioner delegation,
--              scope authorization, and revocation tracking.
-- ============================================================================

CREATE TABLE IF NOT EXISTS taxpayer_consents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    consenting_user_id UUID,
    delegate_user_id UUID NOT NULL,
    delegation_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    consent_method VARCHAR(50) NOT NULL DEFAULT 'IN_APP',
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by UUID,
    revocation_reason VARCHAR(1000),
    rejection_reason VARCHAR(1000),
    consent_reference VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS taxpayer_consent_scopes (
    consent_id UUID NOT NULL,
    scope VARCHAR(50) NOT NULL,
    PRIMARY KEY (consent_id, scope),
    CONSTRAINT fk_taxpayer_consent_scopes_consent FOREIGN KEY (consent_id) REFERENCES taxpayer_consents (id) ON DELETE CASCADE
);

-- Indexes for taxpayer_consents
CREATE INDEX IF NOT EXISTS idx_taxpayer_consents_org_client
    ON taxpayer_consents (organization_id, client_id);

CREATE INDEX IF NOT EXISTS idx_taxpayer_consents_org_delegate
    ON taxpayer_consents (organization_id, delegate_user_id);

CREATE INDEX IF NOT EXISTS idx_taxpayer_consents_org_status
    ON taxpayer_consents (organization_id, status);

CREATE INDEX IF NOT EXISTS idx_taxpayer_consents_valid_until
    ON taxpayer_consents (valid_until);

CREATE INDEX IF NOT EXISTS idx_taxpayer_consents_ref
    ON taxpayer_consents (organization_id, consent_reference);

CREATE INDEX IF NOT EXISTS idx_taxpayer_consent_scopes_scope
    ON taxpayer_consent_scopes (scope);
