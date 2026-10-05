-- Migration: V130__add_client_service_relationship_fields.sql
-- Description: Enhances client_services with service_offering_id, agreed_price, and status lifecycle metadata

ALTER TABLE client_services
    ADD COLUMN IF NOT EXISTS service_offering_id UUID REFERENCES services(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS agreed_price NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS status_changed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS status_changed_by UUID,
    ADD COLUMN IF NOT EXISTS status_change_reason VARCHAR(500);

-- Backfill service_offering_id matching services.service_code where possible
UPDATE client_services cs
SET service_offering_id = s.id
FROM services s
WHERE cs.service_offering_id IS NULL
  AND cs.service_type = s.service_code;

-- Backfill status_changed_at with updated_at / created_at for existing rows
UPDATE client_services
SET status_changed_at = COALESCE(updated_at, created_at, NOW())
WHERE status_changed_at IS NULL;

-- Indexes for performance and lifecycle queries
CREATE INDEX IF NOT EXISTS idx_client_services_service_offering ON client_services(service_offering_id);
CREATE INDEX IF NOT EXISTS idx_client_services_org_offering ON client_services(organization_id, service_offering_id);
CREATE INDEX IF NOT EXISTS idx_client_services_status_changed_at ON client_services(status_changed_at);
