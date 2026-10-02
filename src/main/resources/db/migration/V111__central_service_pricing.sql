-- TDS services
INSERT INTO marketplace_tax_services (
    id,
    category_id,
    code,
    name,
    description,
    sort_order,
    is_active,
    suggested_price,
    currency,
    billing_type,
    created_at,
    updated_at,
    version
)
VALUES
(
    'a0000001-0000-0000-0000-000000000028',
    (SELECT id
     FROM marketplace_tax_service_categories
     WHERE code = 'TDS'),
    'TDS_RETURN_24Q_26Q',
    'TDS Return — 24Q / 26Q',
    'Quarterly 24Q or 26Q statement preparation and filing. Professional fee only.',
    28,
    TRUE,
    1999.00,
    'INR',
    'PER_QUARTER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    'a0000001-0000-0000-0000-000000000029',
    (SELECT id
     FROM marketplace_tax_service_categories
     WHERE code = 'TDS'),
    'TDS_RETURN_27Q',
    'TDS Return — 27Q',
    'Quarterly 27Q statement preparation and filing. Professional fee only.',
    29,
    TRUE,
    2499.00,
    'INR',
    'PER_QUARTER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    'a0000001-0000-0000-0000-000000000030',
    (SELECT id
     FROM marketplace_tax_service_categories
     WHERE code = 'TDS'),
    'TDS_CORRECTION',
    'TDS Correction Statement',
    'Preparation and filing of a TDS correction statement. Professional fee only.',
    30,
    TRUE,
    1499.00,
    'INR',
    'PER_RETURN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    'a0000001-0000-0000-0000-000000000031',
    (SELECT id
     FROM marketplace_tax_service_categories
     WHERE code = 'TDS'),
    'TAN_REGISTRATION',
    'TAN Registration',
    'TAN registration assistance. Government charges excluded.',
    31,
    TRUE,
    999.00,
    'INR',
    'ONE_TIME',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

-- Tax Notice services
(
    'a0000001-0000-0000-0000-000000000032',
    (SELECT id
     FROM marketplace_tax_service_categories
     WHERE code = 'TAX_NOTICES'),
    'INCOME_TAX_NOTICE_BASIC',
    'Income Tax Notice — Basic Review / Reply',
    'Basic income tax notice review and reply drafting. Professional fee only.',
    32,
    TRUE,
    2999.00,
    'INR',
    'PER_NOTICE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    'a0000001-0000-0000-0000-000000000033',
    (SELECT id
     FROM marketplace_tax_service_categories
     WHERE code = 'TAX_NOTICES'),
    'GST_NOTICE_BASIC',
    'GST Notice — Basic Reply',
    'Basic GST notice review and reply drafting. Professional fee only.',
    33,
    TRUE,
    2999.00,
    'INR',
    'PER_NOTICE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
)
ON CONFLICT (code) DO UPDATE
SET
    category_id = EXCLUDED.category_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    suggested_price = EXCLUDED.suggested_price,
    currency = EXCLUDED.currency,
    billing_type = EXCLUDED.billing_type,
    updated_at = CURRENT_TIMESTAMP;


 ALTER TABLE invoice_items
     ADD COLUMN IF NOT EXISTS catalog_service_code VARCHAR(100);

 ALTER TABLE marketplace_proposals
     ADD COLUMN IF NOT EXISTS catalog_service_code VARCHAR(100);

 CREATE TABLE IF NOT EXISTS practice_service_pricing (
     id UUID PRIMARY KEY,
     organization_id UUID NOT NULL,
     tax_service_id UUID NOT NULL,

     pricing_mode VARCHAR(30) NOT NULL DEFAULT 'DEFAULT',
     custom_price NUMERIC(15,2),

     enabled BOOLEAN NOT NULL DEFAULT TRUE,

     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

     created_by VARCHAR(255),
     updated_by VARCHAR(255),

     version BIGINT NOT NULL DEFAULT 0,

     CONSTRAINT fk_practice_service_pricing_org
         FOREIGN KEY (organization_id)
         REFERENCES organizations(id)
         ON DELETE CASCADE,

     CONSTRAINT fk_practice_service_pricing_service
         FOREIGN KEY (tax_service_id)
         REFERENCES marketplace_tax_services(id)
         ON DELETE CASCADE,

     CONSTRAINT uk_practice_service_pricing_org_service
         UNIQUE (organization_id, tax_service_id),

     CONSTRAINT chk_practice_service_pricing_price
         CHECK (custom_price IS NULL OR custom_price >= 0)
 );

 CREATE INDEX IF NOT EXISTS idx_practice_service_pricing_org
     ON practice_service_pricing (organization_id);

 CREATE INDEX IF NOT EXISTS idx_practice_service_pricing_service
     ON practice_service_pricing (tax_service_id);

