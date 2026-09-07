-- V59: Create organization_activation_tokens table for secure, single-use, time-limited organization activation
CREATE TABLE IF NOT EXISTS organization_activation_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by_ip VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_org_activation_tokens_token_hash ON organization_activation_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_org_activation_tokens_user_id ON organization_activation_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_org_activation_tokens_org_id ON organization_activation_tokens(organization_id);
CREATE INDEX IF NOT EXISTS idx_org_activation_tokens_expires_at ON organization_activation_tokens(expires_at);
