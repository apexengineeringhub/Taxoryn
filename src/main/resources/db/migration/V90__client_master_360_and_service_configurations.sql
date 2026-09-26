-- V90__client_master_360_and_service_configurations.sql
-- Phase 12: Client Master, Client 360 Foundation, and Client Service Configurations

-- 1. Enhance client_services with responsible_user_id and location_id
ALTER TABLE client_services
    ADD COLUMN IF NOT EXISTS responsible_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS location_id UUID REFERENCES locations(id) ON DELETE SET NULL;

-- 2. Performance indexes for service queries and scope resolution
CREATE INDEX IF NOT EXISTS idx_client_services_resp_user ON client_services(responsible_user_id);
CREATE INDEX IF NOT EXISTS idx_client_services_location ON client_services(location_id);
CREATE INDEX IF NOT EXISTS idx_client_services_org_status ON client_services(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_clients_org_status ON clients(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_clients_org_code ON clients(organization_id, client_code);
