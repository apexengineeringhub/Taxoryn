-- ==============================================================================
-- Taxoryn Platform - Phase 20 Migration (V99)
-- Billing & Practice Operations Foundation: Invoicing, Receipts & Receivables
-- ==============================================================================

-- 1. Enhance Invoices Table with Discount
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS discount NUMERIC(15, 2) NOT NULL DEFAULT 0.00;

-- 2. Enhance Invoice Payments Table with Receipt Number and Location
ALTER TABLE invoice_payments
    ADD COLUMN IF NOT EXISTS receipt_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS location_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_invoice_payments_location'
    ) THEN
        ALTER TABLE invoice_payments
            ADD CONSTRAINT fk_invoice_payments_location
            FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 3. Enhance Invoice Items Table with Service ID
ALTER TABLE invoice_items
    ADD COLUMN IF NOT EXISTS service_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_invoice_items_service_id'
    ) THEN
        ALTER TABLE invoice_items
            ADD CONSTRAINT fk_invoice_items_service_id
            FOREIGN KEY (service_id) REFERENCES client_services(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 4. Indexes for Invoices and Payments Optimization
CREATE INDEX IF NOT EXISTS idx_invoices_org_status_due ON invoices(organization_id, status, due_date);
CREATE INDEX IF NOT EXISTS idx_invoice_payments_org_receipt ON invoice_payments(organization_id, receipt_number);
CREATE INDEX IF NOT EXISTS idx_invoice_payments_org_loc ON invoice_payments(organization_id, location_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_service_id ON invoice_items(service_id);
