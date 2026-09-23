-- V75__client_services_engagement.sql
-- Phase 11: Client Service Master & Client-Service Engagement Foundation

CREATE TABLE IF NOT EXISTS client_services (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    service_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    start_date DATE,
    end_date DATE,
    assigned_employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    billing_frequency VARCHAR(50) DEFAULT 'MONTHLY',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_client_services_org_client ON client_services(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_client_services_org_service ON client_services(organization_id, service_type);
CREATE INDEX IF NOT EXISTS idx_client_services_assigned_emp ON client_services(assigned_employee_id);
