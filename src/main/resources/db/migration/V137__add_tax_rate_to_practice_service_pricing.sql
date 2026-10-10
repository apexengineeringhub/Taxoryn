-- V137__add_tax_rate_to_practice_service_pricing.sql
-- Allow practitioners to configure custom GST tax rates for standard services

ALTER TABLE practice_service_pricing
    ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,2);
