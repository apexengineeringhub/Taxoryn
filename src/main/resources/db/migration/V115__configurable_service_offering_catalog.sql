-- V115__configurable_service_offering_catalog.sql
-- P0.6: Configurable Service Offering Catalog (Taxoryn-level & Practice-level)

ALTER TABLE services
    ADD COLUMN IF NOT EXISTS scope VARCHAR(30) NOT NULL DEFAULT 'PRACTICE',
    ADD COLUMN IF NOT EXISTS default_price NUMERIC(15,2),
    ADD COLUMN IF NOT EXISTS billing_unit VARCHAR(50) DEFAULT 'PER_RETURN',
    ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,2) DEFAULT 18.00;

-- Update existing system global services to scope = 'TAXORYN' and set default prices & billing units
UPDATE services SET scope = 'TAXORYN' WHERE organization_id IS NULL;

UPDATE services SET default_price = 1500.00, billing_unit = 'PER_RETURN', tax_rate = 18.00 WHERE service_code = 'GST_COMPLIANCE' AND organization_id IS NULL;
UPDATE services SET default_price = 999.00, billing_unit = 'PER_RETURN', tax_rate = 18.00 WHERE service_code = 'TDS_COMPLIANCE' AND organization_id IS NULL;
UPDATE services SET default_price = 999.00, billing_unit = 'PER_RETURN', tax_rate = 18.00 WHERE service_code = 'ITR_FILING' AND organization_id IS NULL;
UPDATE services SET default_price = 5000.00, billing_unit = 'PER_ENGAGEMENT', tax_rate = 18.00 WHERE service_code = 'TAX_AUDIT' AND organization_id IS NULL;
UPDATE services SET default_price = 2500.00, billing_unit = 'PER_NOTICE', tax_rate = 18.00 WHERE service_code = 'NOTICE_MANAGEMENT' AND organization_id IS NULL;
UPDATE services SET default_price = 3000.00, billing_unit = 'HOURLY', tax_rate = 18.00 WHERE service_code = 'TAX_ADVISORY' AND organization_id IS NULL;
UPDATE services SET default_price = 2000.00, billing_unit = 'MONTHLY', tax_rate = 18.00 WHERE service_code = 'OTHER' AND organization_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_services_scope ON services(scope);
