-- ==============================================================================
-- Taxoryn Platform - Commit 6 Migration (V40)
-- Taxoryn Learn SEO & Slug Redirects
-- ==============================================================================

-- 1. Add SEO metadata columns to contents table
ALTER TABLE contents
    ADD COLUMN IF NOT EXISTS seo_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS meta_description VARCHAR(500),
    ADD COLUMN IF NOT EXISTS canonical_url VARCHAR(500);

-- 2. Create content_slug_redirects table for URL stability & 301 redirects
CREATE TABLE IF NOT EXISTS content_slug_redirects (
    id UUID PRIMARY KEY,
    old_slug VARCHAR(255) NOT NULL UNIQUE,
    new_slug VARCHAR(255) NOT NULL,
    content_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_csr_content FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_slug_redirect_old_slug ON content_slug_redirects(old_slug);
CREATE INDEX IF NOT EXISTS idx_slug_redirect_content_id ON content_slug_redirects(content_id);
