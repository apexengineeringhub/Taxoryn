-- ==============================================================================
-- Taxoryn Platform - Migration V65
-- Align notice management audit columns (created_by, updated_by) type to VARCHAR(255)
-- ==============================================================================

-- 1. tax_notices
ALTER TABLE tax_notices 
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::TEXT,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::TEXT;

-- 2. notice_responses
ALTER TABLE notice_responses 
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::TEXT,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::TEXT;

-- 3. notice_hearings
ALTER TABLE notice_hearings 
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::TEXT,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::TEXT;

-- 4. notice_activities
ALTER TABLE notice_activities 
    ALTER COLUMN created_by TYPE VARCHAR(255) USING created_by::TEXT,
    ALTER COLUMN updated_by TYPE VARCHAR(255) USING updated_by::TEXT;