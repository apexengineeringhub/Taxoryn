-- ==============================================================================
-- Taxoryn Platform - Phase 20 Migration (V100)
-- Practice Promotional Pricing: Promotions Table & Invoice Item Snapshot
-- ==============================================================================

-- 1. Create Promotions Table
CREATE TABLE IF NOT EXISTS promotions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    code VARCHAR(50),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    promotion_type VARCHAR(50) NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    discount_value NUMERIC(15, 2) NOT NULL,
    target_service VARCHAR(50),
    target_client_id UUID,
    valid_from DATE,
    valid_until DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0,
    max_uses INT,
    current_uses INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_promotions_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_promotions_client FOREIGN KEY (target_client_id) REFERENCES clients(id) ON DELETE SET NULL
);

-- 2. Enhance Invoice Items with Immutable Pricing Snapshot Columns
ALTER TABLE invoice_items
    ADD COLUMN IF NOT EXISTS promotion_id UUID,
    ADD COLUMN IF NOT EXISTS promotion_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pricing_type VARCHAR(50) DEFAULT 'STANDARD',
    ADD COLUMN IF NOT EXISTS standard_unit_price NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS discount_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS discount_value NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(15, 2) DEFAULT 0.00;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_invoice_items_promotion_id'
    ) THEN
        ALTER TABLE invoice_items
            ADD CONSTRAINT fk_invoice_items_promotion_id
            FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 3. Indexes for Promotions and Invoicing Performance
CREATE INDEX IF NOT EXISTS idx_promotions_org_active ON promotions(organization_id, active);
CREATE INDEX IF NOT EXISTS idx_promotions_org_type ON promotions(organization_id, promotion_type);
CREATE INDEX IF NOT EXISTS idx_promotions_org_code ON promotions(organization_id, code);
CREATE INDEX IF NOT EXISTS idx_promotions_validity ON promotions(organization_id, active, valid_from, valid_until);
CREATE INDEX IF NOT EXISTS idx_invoice_items_promotion_id ON invoice_items(promotion_id);
