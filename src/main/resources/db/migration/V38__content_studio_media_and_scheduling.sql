-- ==============================================================================
-- Taxoryn Platform - Commit 5 Migration (V38)
-- Taxoryn Content & Marketing Studio: Media Library, Scheduling, Rejection & Versioning
-- ==============================================================================

-- 1. Add rejection reason, scheduling, versioning, and media columns to contents table
ALTER TABLE contents
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS scheduled_publish_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS version_number INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS featured_image_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS alt_text VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_content_scheduled_publish_at ON contents(scheduled_publish_at);
CREATE INDEX IF NOT EXISTS idx_content_version_number ON contents(version_number);

-- 2. Create content_versions table for basic version history
CREATE TABLE IF NOT EXISTS content_versions (
    id UUID PRIMARY KEY,
    content_id UUID NOT NULL,
    version_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    body TEXT NOT NULL,
    thumbnail_url VARCHAR(500),
    featured_image_url VARCHAR(500),
    alt_text VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    change_summary VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cv_content FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_content_versions_content_id ON content_versions(content_id);
CREATE INDEX IF NOT EXISTS idx_content_versions_created_at ON content_versions(created_at);

-- 3. Create content_media_assets table for Media Library
CREATE TABLE IF NOT EXISTS content_media_assets (
    id UUID PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    public_url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(255),
    uploaded_by_id UUID,
    uploaded_by_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cma_user FOREIGN KEY (uploaded_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_content_media_created_at ON content_media_assets(created_at);
CREATE INDEX IF NOT EXISTS idx_content_media_content_type ON content_media_assets(content_type);

-- 4. Seed Studio Permissions for RBAC
INSERT INTO permissions (id, code, name, module, description) VALUES
    ('10000000-0000-0000-0000-000000000260', 'MEDIA_VIEW', 'View Media Library', 'CONTENT', 'View and browse uploaded platform media assets'),
    ('10000000-0000-0000-0000-000000000261', 'MEDIA_UPLOAD', 'Upload Media Assets', 'CONTENT', 'Upload images, banners, and thumbnails to Media Library'),
    ('10000000-0000-0000-0000-000000000262', 'MEDIA_DELETE', 'Delete Media Assets', 'CONTENT', 'Remove unused media assets from Media Library'),
    ('10000000-0000-0000-0000-000000000263', 'CONTENT_SCHEDULE', 'Schedule Content Publication', 'CONTENT', 'Schedule approved content for automatic future publication'),
    ('10000000-0000-0000-0000-000000000264', 'CONTENT_REJECT', 'Reject Content with Reason', 'CONTENT', 'Reject submitted content and send back to author with feedback'),
    ('10000000-0000-0000-0000-000000000265', 'CONTENT_RESTORE', 'Restore Archived Content', 'CONTENT', 'Restore archived content back to draft for re-evaluation')
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- Grant Studio permissions to TAXORYN_CONTENT_ADMIN and TAXORYN_SUPERADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000106', id FROM permissions
WHERE code IN ('MEDIA_VIEW', 'MEDIA_UPLOAD', 'MEDIA_DELETE', 'CONTENT_SCHEDULE', 'CONTENT_REJECT', 'CONTENT_RESTORE')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000101', id FROM permissions
WHERE code IN ('MEDIA_VIEW', 'MEDIA_UPLOAD', 'MEDIA_DELETE', 'CONTENT_SCHEDULE', 'CONTENT_REJECT', 'CONTENT_RESTORE')
ON CONFLICT DO NOTHING;
