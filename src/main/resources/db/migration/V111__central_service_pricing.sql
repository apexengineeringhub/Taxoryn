-- ============================================================
-- V111 - Central Service Pricing
-- ============================================================

-- ============================================================
-- 1. Extend marketplace tax services
-- ============================================================

ALTER TABLE marketplace_tax_services
    ADD COLUMN IF NOT EXISTS suggested_price NUMERIC(15,2),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    ADD COLUMN IF NOT EXISTS billing_type VARCHAR(30);


-- Ensure audit/version defaults for newly inserted services
ALTER TABLE marketplace_tax_services
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE marketplace_tax_services
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE marketplace_tax_services
    ALTER COLUMN version SET DEFAULT 0;


-- ============================================================
-- 2. Ensure category audit/version defaults
-- ============================================================

ALTER TABLE marketplace_tax_service_categories
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE marketplace_tax_service_categories
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE marketplace_tax_service_categories
    ALTER COLUMN version SET DEFAULT 0;


-- ============================================================
-- 3. Add TDS and Tax Notice categories
-- ============================================================

INSERT INTO marketplace_tax_service_categories
(
    id,
    code,
    name,
    description,
    icon,
    sort_order,
    is_active,
    created_at,
    updated_at,
    version
)
VALUES
(
    'c0000001-0000-0000-0000-000000000006',
    'TDS',
    'TDS Services',
    'Quarterly TDS returns, correction statements, and TAN registration.',
    'FileText',
    6,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    'c0000001-0000-0000-0000-000000000007',
    'TAX_NOTICES',
    'Tax Notice Services',
    'Basic review and reply drafting for income tax and GST notices.',
    'Scale',
    7,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
)
ON CONFLICT (code) DO NOTHING;


-- ============================================================
-- 4. Update existing catalog services
-- ============================================================

UPDATE marketplace_tax_services
SET
    suggested_price = 1999,
    currency = 'INR',
    billing_type = 'ONE_TIME'
WHERE code = 'GST_REGISTRATION';

UPDATE marketplace_tax_services
SET
    suggested_price = 999,
    currency = 'INR',
    billing_type = 'PER_RETURN'
WHERE code = 'GST_AMENDMENT';


-- ============================================================
-- 5. Add billable service units
-- ============================================================

INSERT INTO marketplace_tax_services
(
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
    'a0000001-0000-0000-0000-000000000019',
    'c0000001-0000-0000-0000-000000000001',
    'ITR_1',
    'ITR-1 Filing — Salaried',
    'Preparation and filing of a salaried individual ITR-1. Professional fee only.',
    19,
    TRUE,
    999,
    'INR',
    'PER_RETURN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000020',
    'c0000001-0000-0000-0000-000000000001',
    'ITR_2',
    'ITR-2 Filing — Capital Gains / Other Income',
    'Preparation and filing of ITR-2 with capital gains or other income. Professional fee only.',
    20,
    TRUE,
    1999,
    'INR',
    'PER_RETURN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000021',
    'c0000001-0000-0000-0000-000000000001',
    'ITR_3',
    'ITR-3 Filing — Business / Profession',
    'Preparation and filing of a business or professional ITR-3. Professional fee only.',
    21,
    TRUE,
    2999,
    'INR',
    'PER_RETURN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000022',
    'c0000001-0000-0000-0000-000000000001',
    'ITR_4',
    'ITR-4 Filing — Presumptive Taxation',
    'Preparation and filing of ITR-4 under presumptive taxation. Professional fee only.',
    22,
    TRUE,
    1499,
    'INR',
    'PER_RETURN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000023',
    'c0000001-0000-0000-0000-000000000001',
    'NRI_ITR',
    'NRI Income Tax Return',
    'Preparation and filing of an NRI income tax return. Professional fee only.',
    23,
    TRUE,
    4999,
    'INR',
    'PER_RETURN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000024',
    'c0000001-0000-0000-0000-000000000002',
    'GST_MONTHLY_RETURN',
    'GST Monthly Return — GSTR-1 + GSTR-3B',
    'Monthly preparation and filing of GSTR-1 and GSTR-3B. Professional fee only.',
    24,
    TRUE,
    1499,
    'INR',
    'PER_MONTH',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000025',
    'c0000001-0000-0000-0000-000000000002',
    'GST_QUARTERLY_RETURN',
    'GST Quarterly Return',
    'Quarterly GST return preparation and filing. Professional fee only.',
    25,
    TRUE,
    2499,
    'INR',
    'PER_QUARTER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000026',
    'c0000001-0000-0000-0000-000000000002',
    'GST_NIL_RETURN',
    'GST Nil Return',
    'Nil GST return preparation and filing. Professional fee only.',
    26,
    TRUE,
    499,
    'INR',
    'PER_RETURN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000027',
    'c0000001-0000-0000-0000-000000000002',
    'GSTR9_ANNUAL_RETURN',
    'GSTR-9 Annual Return',
    'Annual GSTR-9 preparation and filing. Professional fee only.',
    27,
    TRUE,
    4999,
    'INR',
    'PER_RETURN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000028',
    'c0000001-0000-0000-0000-000000000006',
    'TDS_RETURN_24Q_26Q',
    'TDS Return — 24Q / 26Q',
    'Quarterly 24Q or 26Q statement preparation and filing. Professional fee only.',
    28,
    TRUE,
    1999,
    'INR',
    'PER_QUARTER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000029',
    'c0000001-0000-0000-0000-000000000006',
    'TDS_RETURN_27Q',
    'TDS Return — 27Q',
    'Quarterly 27Q statement preparation and filing. Professional fee only.',
    29,
    TRUE,
    2499,
    'INR',
    'PER_QUARTER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000030',
    'c0000001-0000-0000-0000-000000000006',
    'TDS_CORRECTION',
    'TDS Correction Statement',
    'Preparation and filing of a TDS correction statement. Professional fee only.',
    30,
    TRUE,
    1499,
    'INR',
    'PER_RETURN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000031',
    'c0000001-0000-0000-0000-000000000006',
    'TAN_REGISTRATION',
    'TAN Registration',
    'TAN registration assistance. Government charges excluded.',
    31,
    TRUE,
    999,
    'INR',
    'ONE_TIME',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000032',
    'c0000001-0000-0000-0000-000000000007',
    'INCOME_TAX_NOTICE_BASIC',
    'Income Tax Notice — Basic Review / Reply',
    'Basic income tax notice review and reply drafting. Professional fee only.',
    32,
    TRUE,
    2999,
    'INR',
    'PER_NOTICE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000033',
    'c0000001-0000-0000-0000-000000000007',
    'GST_NOTICE_BASIC',
    'GST Notice — Basic Reply',
    'Basic GST notice review and reply drafting. Professional fee only.',
    33,
    TRUE,
    2999,
    'INR',
    'PER_NOTICE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000034',
    'c0000001-0000-0000-0000-000000000003',
    'INCOME_TAX_CONSULTATION',
    'Income Tax Consultation',
    'Individual income tax consultation. Professional fee only.',
    34,
    TRUE,
    1499,
    'INR',
    'PER_SESSION',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),

(
    'a0000001-0000-0000-0000-000000000035',
    'c0000001-0000-0000-0000-000000000003',
    'TAX_PLANNING_CONSULTATION',
    'Tax Planning Consultation',
    'Personal or business tax planning consultation. Professional fee only.',
    35,
    TRUE,
    2999,
    'INR',
    'PER_SESSION',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
)
ON CONFLICT (code) DO NOTHING;


-- ============================================================
-- 6. Practice-level service pricing
-- ============================================================

CREATE TABLE IF NOT EXISTS practice_service_pricing
(
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    tax_service_id UUID NOT NULL
        REFERENCES marketplace_tax_services(id)
        ON DELETE CASCADE,

    pricing_mode VARCHAR(20) NOT NULL DEFAULT 'DEFAULT'
        CHECK (pricing_mode IN ('DEFAULT', 'CUSTOM')),

    custom_price NUMERIC(15,2)
        CHECK (custom_price IS NULL OR custom_price >= 0),

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    created_by VARCHAR(255),

    updated_by VARCHAR(255),

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_practice_service_pricing
        UNIQUE (organization_id, tax_service_id),

    CONSTRAINT ck_practice_service_custom_price
        CHECK (
            pricing_mode <> 'CUSTOM'
            OR custom_price IS NOT NULL
        )
);


-- ============================================================
-- 7. Index
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_practice_service_pricing_org
    ON practice_service_pricing(organization_id);


-- ============================================================
-- 8. Link catalog service to invoice items
-- ============================================================

ALTER TABLE invoice_items
    ADD COLUMN IF NOT EXISTS catalog_service_code VARCHAR(100);


-- ============================================================
-- 9. Link catalog service to marketplace proposals
-- ============================================================

ALTER TABLE marketplace_proposals
    ADD COLUMN IF NOT EXISTS catalog_service_code VARCHAR(100);