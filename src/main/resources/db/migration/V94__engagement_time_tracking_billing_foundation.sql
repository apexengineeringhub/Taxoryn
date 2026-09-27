-- ==============================================================================
-- Taxoryn Platform - Phase 15 Migration (V94)
-- Engagement, Time Tracking & Billing Foundation
-- ==============================================================================

-- 1. Create Engagements Table
CREATE TABLE IF NOT EXISTS engagements (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    location_id UUID REFERENCES locations(id) ON DELETE SET NULL,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    client_service_id UUID REFERENCES client_services(id) ON DELETE SET NULL,
    engagement_code VARCHAR(100),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    start_date DATE,
    end_date DATE,
    assigned_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for Engagements
CREATE INDEX IF NOT EXISTS idx_engagements_org ON engagements(organization_id);
CREATE INDEX IF NOT EXISTS idx_engagements_org_client ON engagements(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_engagements_org_loc ON engagements(organization_id, location_id);
CREATE INDEX IF NOT EXISTS idx_engagements_org_service ON engagements(organization_id, client_service_id);
CREATE INDEX IF NOT EXISTS idx_engagements_org_status ON engagements(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_engagements_org_assigned ON engagements(organization_id, assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_engagements_org_code ON engagements(organization_id, engagement_code);

-- 2. Create Time Entries Table
CREATE TABLE IF NOT EXISTS time_entries (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    location_id UUID REFERENCES locations(id) ON DELETE SET NULL,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    engagement_id UUID REFERENCES engagements(id) ON DELETE SET NULL,
    work_item_id UUID REFERENCES work_items(id) ON DELETE SET NULL,
    task_id UUID REFERENCES tasks(id) ON DELETE SET NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entry_date DATE NOT NULL,
    duration_minutes INT NOT NULL,
    description TEXT,
    billable BOOLEAN NOT NULL DEFAULT true,
    billing_rate NUMERIC(15, 2),
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for Time Entries
CREATE INDEX IF NOT EXISTS idx_time_entries_org ON time_entries(organization_id);
CREATE INDEX IF NOT EXISTS idx_time_entries_org_client ON time_entries(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_time_entries_org_engagement ON time_entries(organization_id, engagement_id);
CREATE INDEX IF NOT EXISTS idx_time_entries_org_work_item ON time_entries(organization_id, work_item_id);
CREATE INDEX IF NOT EXISTS idx_time_entries_org_task ON time_entries(organization_id, task_id);
CREATE INDEX IF NOT EXISTS idx_time_entries_org_user ON time_entries(organization_id, user_id);
CREATE INDEX IF NOT EXISTS idx_time_entries_org_date ON time_entries(organization_id, entry_date);
CREATE INDEX IF NOT EXISTS idx_time_entries_org_status ON time_entries(organization_id, status);

-- 3. Create Billing Profiles Table
CREATE TABLE IF NOT EXISTS billing_profiles (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    engagement_id UUID REFERENCES engagements(id) ON DELETE SET NULL,
    billing_frequency VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    default_rate NUMERIC(15, 2),
    tax_applicable BOOLEAN NOT NULL DEFAULT true,
    active BOOLEAN NOT NULL DEFAULT true,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for Billing Profiles
CREATE INDEX IF NOT EXISTS idx_billing_profiles_org ON billing_profiles(organization_id);
CREATE INDEX IF NOT EXISTS idx_billing_profiles_org_client ON billing_profiles(organization_id, client_id);
CREATE INDEX IF NOT EXISTS idx_billing_profiles_org_engagement ON billing_profiles(organization_id, engagement_id);

-- 4. Enhance Invoices Table with Location, Engagement, and Currency
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS location_id UUID,
    ADD COLUMN IF NOT EXISTS engagement_id UUID,
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'INR';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_invoices_location'
    ) THEN
        ALTER TABLE invoices
            ADD CONSTRAINT fk_invoices_location
            FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_invoices_engagement'
    ) THEN
        ALTER TABLE invoices
            ADD CONSTRAINT fk_invoices_engagement
            FOREIGN KEY (engagement_id) REFERENCES engagements(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_invoices_org_location ON invoices(organization_id, location_id);
CREATE INDEX IF NOT EXISTS idx_invoices_org_engagement ON invoices(organization_id, engagement_id);

-- 5. Enhance Invoice Items (Lines) Table with Work Item and Time Entry References
ALTER TABLE invoice_items
    ADD COLUMN IF NOT EXISTS work_item_id UUID,
    ADD COLUMN IF NOT EXISTS time_entry_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_invoice_items_work_item'
    ) THEN
        ALTER TABLE invoice_items
            ADD CONSTRAINT fk_invoice_items_work_item
            FOREIGN KEY (work_item_id) REFERENCES work_items(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_invoice_items_time_entry'
    ) THEN
        ALTER TABLE invoice_items
            ADD CONSTRAINT fk_invoice_items_time_entry
            FOREIGN KEY (time_entry_id) REFERENCES time_entries(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_invoice_items_work_item ON invoice_items(work_item_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_time_entry ON invoice_items(time_entry_id);
