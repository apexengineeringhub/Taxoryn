-- V70: Create customer_email_verification_tokens table for self-service customer registration email verification
CREATE TABLE IF NOT EXISTS customer_email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by_ip VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_customer_email_verif_tokens_token_hash ON customer_email_verification_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_customer_email_verif_tokens_user_id ON customer_email_verification_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_customer_email_verif_tokens_email ON customer_email_verification_tokens(email);
CREATE INDEX IF NOT EXISTS idx_customer_email_verif_tokens_expires_at ON customer_email_verification_tokens(expires_at);
