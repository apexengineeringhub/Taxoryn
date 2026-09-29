-- ==============================================================================
-- Taxoryn Platform - Phase 2 SuperAdmin Practice-Centric User Management (V104)
-- Performance Indexes for Organization User Count Aggregations and Searches
-- ==============================================================================

-- 1. Composite index for tenant-scoped user status lookups and count aggregations
CREATE INDEX IF NOT EXISTS idx_users_org_status ON users(organization_id, status);

-- 2. Composite index for user name search acceleration
CREATE INDEX IF NOT EXISTS idx_users_names ON users(first_name, last_name);
