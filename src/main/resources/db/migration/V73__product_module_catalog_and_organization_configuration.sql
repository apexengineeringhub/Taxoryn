-- V73__product_module_catalog_and_organization_configuration.sql
-- Establishes the Product Module Catalog and Organization Module Configuration Foundation

-- 1. Product Module Catalog (Global Master)
CREATE TABLE IF NOT EXISTS product_modules (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL, -- 'CORE', 'TAX', 'PRACTICE_OPERATIONS', 'NETWORK_GROWTH'
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'BETA', 'DEPRECATED'
    enabled_by_default BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_pm_code ON product_modules(code);
CREATE INDEX IF NOT EXISTS idx_pm_category ON product_modules(category);
CREATE INDEX IF NOT EXISTS idx_pm_status ON product_modules(status);

-- 2. Organization Module Configuration (Tenant Scoped)
CREATE TABLE IF NOT EXISTS organization_modules (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    module_code VARCHAR(50) NOT NULL REFERENCES product_modules(code) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_org_module UNIQUE (organization_id, module_code)
);

CREATE INDEX IF NOT EXISTS idx_om_org_id ON organization_modules(organization_id);
CREATE INDEX IF NOT EXISTS idx_om_module_code ON organization_modules(module_code);
CREATE INDEX IF NOT EXISTS idx_om_org_enabled ON organization_modules(organization_id, enabled);

-- 3. Seed Initial Product Module Catalog
INSERT INTO product_modules (id, code, name, description, category, status, enabled_by_default, display_order, version, created_at, updated_at)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'CLIENTS', 'Client Management', 'Core client profiles, statutory numbers, and portfolio assignment.', 'CORE', 'ACTIVE', TRUE, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000002', 'TASKS', 'Task Management', 'Practice worklists, tasks, deadlines, and employee assignment.', 'CORE', 'ACTIVE', TRUE, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000003', 'DOCUMENTS', 'Document Management', 'Document vault, category folders, and malware-scanned storage.', 'CORE', 'ACTIVE', TRUE, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000004', 'DOCUMENT_REQUESTS', 'Document Requests', 'Client document checklists and self-service upload requests.', 'CORE', 'ACTIVE', TRUE, 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000005', 'CLIENT_PORTAL', 'Client Portal', 'Taxpayer self-service portal, messaging, and document sharing.', 'CORE', 'ACTIVE', TRUE, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000006', 'NOTIFICATIONS', 'Notification Center', 'Email, SMS, WhatsApp, and in-app compliance alerts.', 'CORE', 'ACTIVE', TRUE, 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000007', 'AUDIT', 'Audit & Activity Logs', 'Immutable compliance audit logs and activity tracking.', 'CORE', 'ACTIVE', TRUE, 7, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000008', 'GST', 'GST Compliance', 'GSTR-1, 3B, 9 return filing tracking and monthly tax summary.', 'TAX', 'ACTIVE', TRUE, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000009', 'ITR', 'Income Tax Returns', 'ITR-1 to 7 computation, form filing, and acknowledgement vault.', 'TAX', 'ACTIVE', TRUE, 9, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000010', 'TDS', 'TDS Compliance', '24Q, 26Q, 27Q quarterly returns, challan verification, and Form 16.', 'TAX', 'ACTIVE', TRUE, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000011', 'TAX_NOTICES', 'Tax Notice Management', 'Assessment notices, hearing schedules, and response drafting.', 'TAX', 'ACTIVE', TRUE, 11, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000012', 'BILLING', 'Billing & Invoicing', 'Professional fee invoicing, receipts, and payment tracking.', 'PRACTICE_OPERATIONS', 'ACTIVE', TRUE, 12, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000013', 'REPORTS', 'Central Reporting', 'Practice productivity, tax workload, and realization reports.', 'PRACTICE_OPERATIONS', 'ACTIVE', TRUE, 13, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000014', 'MARKETPLACE', 'Practice Marketplace', 'Public practice profile, lead generation, and marketplace presence.', 'NETWORK_GROWTH', 'ACTIVE', FALSE, 14, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;
