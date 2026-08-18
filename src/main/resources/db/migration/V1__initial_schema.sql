-- ==============================================================================
-- Taxoryn Platform - Phase 0 Master Schema Initial Migration (V1)
-- Multi-Tenant Practice Management Foundation
-- ==============================================================================

-- Enable UUID extension if available
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Organizations (Tenants)
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    trade_name VARCHAR(255),
    pan VARCHAR(10),
    gstin VARCHAR(15),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    subscription_plan VARCHAR(50) NOT NULL DEFAULT 'STARTER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_organizations_status ON organizations(status);
CREATE INDEX idx_organizations_email ON organizations(email);

-- 2. Permissions
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    module VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_permissions_module ON permissions(module);

-- 3. Roles
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_roles_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX idx_roles_organization_id ON roles(organization_id);
CREATE INDEX idx_roles_code ON roles(code);

-- 4. Role Permissions Mapping
CREATE TABLE role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- 5. Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    phone VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_users_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT uq_users_org_email UNIQUE (organization_id, email)
);

CREATE INDEX idx_users_organization_id ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);

-- 6. User Roles Mapping
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 7. Employees (Tenant Specific)
CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id UUID,
    employee_code VARCHAR(50) NOT NULL,
    designation VARCHAR(100),
    department VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    joining_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_employees_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_employees_org_code UNIQUE (organization_id, employee_code)
);

CREATE INDEX idx_employees_organization_id ON employees(organization_id);
CREATE INDEX idx_employees_user_id ON employees(user_id);

-- 8. Clients (Tenant Specific)
CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_type VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL',
    display_name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    pan VARCHAR(10),
    gstin VARCHAR(15),
    email VARCHAR(255),
    phone VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_clients_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX idx_clients_organization_id ON clients(organization_id);
CREATE INDEX idx_clients_pan ON clients(pan);
CREATE INDEX idx_clients_gstin ON clients(gstin);

-- 9. Tasks (Tenant Specific)
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    client_id UUID,
    assigned_to UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    task_category VARCHAR(100) NOT NULL DEFAULT 'OTHER',
    status VARCHAR(50) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tasks_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE SET NULL,
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_tasks_organization_id ON tasks(organization_id);
CREATE INDEX idx_tasks_client_id ON tasks(client_id);
CREATE INDEX idx_tasks_assigned_to ON tasks(assigned_to);
CREATE INDEX idx_tasks_status ON tasks(status);

-- 10. Audit Logs (Tenant Specific)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255),
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_organization_id ON audit_logs(organization_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- ==============================================================================
-- System Seed Data: Default Permissions & System Roles
-- ==============================================================================

-- System Permissions
INSERT INTO permissions (id, code, name, module, description) VALUES
    ('10000000-0000-0000-0000-000000000001', 'ORG_READ', 'View Organization Details', 'ORGANIZATION', 'Allows viewing organization profile and settings'),
    ('10000000-0000-0000-0000-000000000002', 'ORG_WRITE', 'Manage Organization Details', 'ORGANIZATION', 'Allows updating organization profile and settings'),
    ('10000000-0000-0000-0000-000000000003', 'USER_READ', 'View Users', 'USER', 'Allows viewing team members'),
    ('10000000-0000-0000-0000-000000000004', 'USER_WRITE', 'Manage Users', 'USER', 'Allows creating and editing team members'),
    ('10000000-0000-0000-0000-000000000005', 'ROLE_READ', 'View Roles and Permissions', 'ROLE', 'Allows viewing roles and assigned permissions'),
    ('10000000-0000-0000-0000-000000000006', 'ROLE_WRITE', 'Manage Roles and Permissions', 'ROLE', 'Allows managing custom roles and permissions'),
    ('10000000-0000-0000-0000-000000000007', 'EMPLOYEE_READ', 'View Employees', 'EMPLOYEE', 'Allows viewing employee master data'),
    ('10000000-0000-0000-0000-000000000008', 'EMPLOYEE_WRITE', 'Manage Employees', 'EMPLOYEE', 'Allows modifying employee records'),
    ('10000000-0000-0000-0000-000000000009', 'CLIENT_READ', 'View Clients', 'CLIENT', 'Allows viewing client master list and details'),
    ('10000000-0000-0000-0000-000000000010', 'CLIENT_WRITE', 'Manage Clients', 'CLIENT', 'Allows onboarding and updating client profiles'),
    ('10000000-0000-0000-0000-000000000011', 'TASK_READ', 'View Tasks', 'TASK', 'Allows viewing assigned tasks'),
    ('10000000-0000-0000-0000-000000000012', 'TASK_WRITE', 'Manage Tasks', 'TASK', 'Allows creating, assigning and updating tasks'),
    ('10000000-0000-0000-0000-000000000013', 'GST_READ', 'View GST Returns & Data', 'GST', 'Allows viewing GST filing status and computations'),
    ('10000000-0000-0000-0000-000000000014', 'GST_WRITE', 'Manage GST Filings', 'GST', 'Allows preparing and filing GST returns'),
    ('10000000-0000-0000-0000-000000000015', 'ITR_READ', 'View ITR Computations', 'ITR', 'Allows viewing income tax return computations'),
    ('10000000-0000-0000-0000-000000000016', 'ITR_WRITE', 'Manage ITR Filings', 'ITR', 'Allows drafting and filing ITR forms'),
    ('10000000-0000-0000-0000-000000000017', 'BILLING_READ', 'View Invoices & Billing', 'BILLING', 'Allows viewing invoices and payments'),
    ('10000000-0000-0000-0000-000000000018', 'BILLING_WRITE', 'Manage Billing & Invoicing', 'BILLING', 'Allows generating invoices and recording payments'),
    ('10000000-0000-0000-0000-000000000019', 'AUDIT_READ', 'View Audit Trail', 'AUDIT', 'Allows inspecting security and change audit trails')
ON CONFLICT (code) DO NOTHING;

-- System Roles (Null organization_id indicates global system archetype roles)
INSERT INTO roles (id, organization_id, code, name, description, is_system_role) VALUES
    ('20000000-0000-0000-0000-000000000001', NULL, 'SUPER_ADMIN', 'Platform Super Administrator', 'Full platform administrative access across all tenants', TRUE),
    ('20000000-0000-0000-0000-000000000002', NULL, 'ORG_ADMIN', 'Organization Administrator', 'Full administrative authority within an organization', TRUE),
    ('20000000-0000-0000-0000-000000000003', NULL, 'CA_PARTNER', 'Chartered Accountant Partner', 'Full access to client files, filings, approvals and reports', TRUE),
    ('20000000-0000-0000-0000-000000000004', NULL, 'MANAGER', 'Practice Manager', 'Manages tasks, staff assignments, review workflows', TRUE),
    ('20000000-0000-0000-0000-000000000005', NULL, 'ASSOCIATE', 'Tax Associate / Senior Staff', 'Drafts returns, performs computations and client tasks', TRUE),
    ('20000000-0000-0000-0000-000000000006', NULL, 'STAFF', 'Articled Assistant / Junior Staff', 'Data entry, document collection and basic task execution', TRUE),
    ('20000000-0000-0000-0000-000000000007', NULL, 'CLIENT', 'Client User', 'Accesses client portal to upload documents and view status', TRUE)
ON CONFLICT (id) DO NOTHING;

-- Map All Permissions to ORG_ADMIN and CA_PARTNER
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000002', id FROM permissions
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000003', id FROM permissions
ON CONFLICT DO NOTHING;
