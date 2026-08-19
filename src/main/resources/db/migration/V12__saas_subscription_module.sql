-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V12)
-- SaaS Subscription Module: Organization Subscriptions, Plans & Usage Limits
-- ==============================================================================

-- 1. Subscriptions Table
CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    plan VARCHAR(50) NOT NULL DEFAULT 'STARTER',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    billing_interval VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    renewal_date DATE NOT NULL DEFAULT (CURRENT_DATE + INTERVAL '30 days'),
    max_users INT NOT NULL DEFAULT 5,
    max_clients INT NOT NULL DEFAULT 25,
    max_storage_bytes BIGINT NOT NULL DEFAULT 5368709120, -- 5 GB
    price NUMERIC(15, 2) NOT NULL DEFAULT 999.00,
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_subscriptions_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT uq_subscriptions_org UNIQUE (organization_id)
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_org ON subscriptions(organization_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_plan ON subscriptions(plan);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_renewal ON subscriptions(renewal_date);

-- 2. Backfill existing organizations with a default STARTER subscription if missing
INSERT INTO subscriptions (
    id, organization_id, plan, status, billing_interval,
    start_date, renewal_date, max_users, max_clients, max_storage_bytes, price, auto_renew
)
SELECT
    gen_random_uuid(),
    o.id,
    'STARTER',
    'ACTIVE',
    'MONTHLY',
    CURRENT_DATE,
    (CURRENT_DATE + INTERVAL '30 days'),
    5,
    25,
    5368709120,
    999.00,
    TRUE
FROM organizations o
WHERE NOT EXISTS (
    SELECT 1 FROM subscriptions s WHERE s.organization_id = o.id
)
ON CONFLICT (organization_id) DO NOTHING;
