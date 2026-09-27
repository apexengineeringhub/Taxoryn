-- =========================================================================
-- Flyway Migration V97: TDS Compliance Workspace Foundation
--
-- Phase 18 introduces:
-- 1. Enhancements to tds_profiles table for location scoping and active flag.
-- 2. Reverse linkage columns on compliance_obligations and compliance_workflows
--    referencing tds_profiles(id).
-- 3. Tenant-scoped performance indexes.
-- =========================================================================

-- 1. Enhance tds_profiles table
ALTER TABLE tds_profiles ADD COLUMN IF NOT EXISTS location_id UUID;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tds_profiles_location'
    ) THEN
        ALTER TABLE tds_profiles
            ADD CONSTRAINT fk_tds_profiles_location
            FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL;
    END IF;
END $$;

ALTER TABLE tds_profiles ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_tds_profiles_org_loc ON tds_profiles(organization_id, location_id);
CREATE INDEX IF NOT EXISTS idx_tds_profiles_org_active ON tds_profiles(organization_id, active);

-- 2. Add tds_profile_id to compliance_obligations
ALTER TABLE compliance_obligations ADD COLUMN IF NOT EXISTS tds_profile_id UUID;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_obligations_tds_profile'
    ) THEN
        ALTER TABLE compliance_obligations
            ADD CONSTRAINT fk_compliance_obligations_tds_profile
            FOREIGN KEY (tds_profile_id) REFERENCES tds_profiles(id) ON DELETE SET NULL;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_compliance_obligations_tds_profile ON compliance_obligations(organization_id, tds_profile_id);

-- 3. Add tds_profile_id to compliance_workflows
ALTER TABLE compliance_workflows ADD COLUMN IF NOT EXISTS tds_profile_id UUID;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_workflows_tds_profile'
    ) THEN
        ALTER TABLE compliance_workflows
            ADD CONSTRAINT fk_compliance_workflows_tds_profile
            FOREIGN KEY (tds_profile_id) REFERENCES tds_profiles(id) ON DELETE SET NULL;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_compliance_workflows_tds_profile ON compliance_workflows(organization_id, tds_profile_id);
