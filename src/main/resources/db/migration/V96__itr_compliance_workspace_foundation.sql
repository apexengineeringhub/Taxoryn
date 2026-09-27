-- =========================================================================
-- Flyway Migration V96: ITR Compliance Workspace Foundation
--
-- Phase 17 introduces:
-- 1. Enhancements to itr_profiles table for location scoping, assessment year,
--    assessment category, and applicable return type.
-- 2. Reverse linkage columns on compliance_obligations and compliance_workflows
--    referencing itr_profiles(id).
-- 3. Tenant-scoped performance indexes.
-- =========================================================================

-- 1. Enhance itr_profiles table
ALTER TABLE itr_profiles ADD COLUMN IF NOT EXISTS location_id UUID;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_itr_profiles_location'
    ) THEN
        ALTER TABLE itr_profiles
            ADD CONSTRAINT fk_itr_profiles_location
            FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL;
    END IF;
END $$;

ALTER TABLE itr_profiles ADD COLUMN IF NOT EXISTS default_assessment_year VARCHAR(20);
ALTER TABLE itr_profiles ADD COLUMN IF NOT EXISTS assessment_category VARCHAR(50);
ALTER TABLE itr_profiles ADD COLUMN IF NOT EXISTS applicable_return_type VARCHAR(50);
ALTER TABLE itr_profiles ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_itr_profiles_org_loc ON itr_profiles(organization_id, location_id);
CREATE INDEX IF NOT EXISTS idx_itr_profiles_org_ay ON itr_profiles(organization_id, default_assessment_year);
CREATE INDEX IF NOT EXISTS idx_itr_profiles_org_active ON itr_profiles(organization_id, active);

-- 2. Add itr_profile_id to compliance_obligations
ALTER TABLE compliance_obligations ADD COLUMN IF NOT EXISTS itr_profile_id UUID;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_obligations_itr_profile'
    ) THEN
        ALTER TABLE compliance_obligations
            ADD CONSTRAINT fk_compliance_obligations_itr_profile
            FOREIGN KEY (itr_profile_id) REFERENCES itr_profiles(id) ON DELETE SET NULL;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_compliance_obligations_itr_profile ON compliance_obligations(organization_id, itr_profile_id);

-- 3. Add itr_profile_id to compliance_workflows
ALTER TABLE compliance_workflows ADD COLUMN IF NOT EXISTS itr_profile_id UUID;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compliance_workflows_itr_profile'
    ) THEN
        ALTER TABLE compliance_workflows
            ADD CONSTRAINT fk_compliance_workflows_itr_profile
            FOREIGN KEY (itr_profile_id) REFERENCES itr_profiles(id) ON DELETE SET NULL;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_compliance_workflows_itr_profile ON compliance_workflows(organization_id, itr_profile_id);
