-- ==============================================================================
-- Taxoryn Platform - Commit 1 Migration (V35)
-- Taxoryn Learn: Content Foundation Schema (Articles, Videos, Guides, FAQs, Tax Updates)
-- ==============================================================================

-- 1. Create content_tags table for normalized taxonomy
CREATE TABLE IF NOT EXISTS content_tags (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_content_tags_slug ON content_tags(slug);

-- 2. Create contents table for central knowledge & learning entity
CREATE TABLE IF NOT EXISTS contents (
    id UUID PRIMARY KEY,
    content_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    summary TEXT,
    body TEXT NOT NULL,
    thumbnail_url VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    category_id UUID,
    tax_service_id UUID,
    scope VARCHAR(50) NOT NULL DEFAULT 'PLATFORM',
    author_id UUID,
    reviewer_id UUID,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_content_category FOREIGN KEY (category_id) REFERENCES marketplace_tax_service_categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_content_tax_service FOREIGN KEY (tax_service_id) REFERENCES marketplace_tax_services(id) ON DELETE SET NULL,
    CONSTRAINT fk_content_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_content_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 3. Create content_tag_mappings table
CREATE TABLE IF NOT EXISTS content_tag_mappings (
    content_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (content_id, tag_id),
    CONSTRAINT fk_ctm_content FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE,
    CONSTRAINT fk_ctm_tag FOREIGN KEY (tag_id) REFERENCES content_tags(id) ON DELETE CASCADE
);

-- 4. Create Performance and Query Indexes
CREATE INDEX IF NOT EXISTS idx_content_slug ON contents(slug);
CREATE INDEX IF NOT EXISTS idx_content_status ON contents(status);
CREATE INDEX IF NOT EXISTS idx_content_type ON contents(content_type);
CREATE INDEX IF NOT EXISTS idx_content_category_id ON contents(category_id);
CREATE INDEX IF NOT EXISTS idx_content_tax_service_id ON contents(tax_service_id);
CREATE INDEX IF NOT EXISTS idx_content_published_at ON contents(published_at);
CREATE INDEX IF NOT EXISTS idx_content_author_id ON contents(author_id);
CREATE INDEX IF NOT EXISTS idx_content_reviewer_id ON contents(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_content_created_at ON contents(created_at);

-- 5. Seed Core Content Permissions for RBAC
INSERT INTO permissions (id, code, name, module, description) VALUES
    ('10000000-0000-0000-0000-000000000250', 'CONTENT_CREATE', 'Create Platform Content', 'CONTENT', 'Draft knowledge base articles, videos, guides, and tax updates'),
    ('10000000-0000-0000-0000-000000000251', 'CONTENT_EDIT', 'Edit Platform Content', 'CONTENT', 'Modify and update drafted or reviewed platform content'),
    ('10000000-0000-0000-0000-000000000252', 'CONTENT_SUBMIT_REVIEW', 'Submit Content for Review', 'CONTENT', 'Submit drafted content for peer and regulatory review'),
    ('10000000-0000-0000-0000-000000000253', 'CONTENT_REVIEW', 'Review Platform Content', 'CONTENT', 'Inspect and review submitted content for accuracy'),
    ('10000000-0000-0000-0000-000000000254', 'CONTENT_APPROVE', 'Approve Platform Content', 'CONTENT', 'Approve reviewed content for publication'),
    ('10000000-0000-0000-0000-000000000255', 'CONTENT_ARCHIVE', 'Archive Platform Content', 'CONTENT', 'Archive obsolete or outdated content records')
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- Grant to TAXORYN_CONTENT_ADMIN and TAXORYN_SUPERADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000106', id FROM permissions
WHERE code IN ('CONTENT_CREATE', 'CONTENT_EDIT', 'CONTENT_SUBMIT_REVIEW', 'CONTENT_REVIEW', 'CONTENT_APPROVE', 'CONTENT_ARCHIVE')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000101', id FROM permissions
WHERE code IN ('CONTENT_CREATE', 'CONTENT_EDIT', 'CONTENT_SUBMIT_REVIEW', 'CONTENT_REVIEW', 'CONTENT_APPROVE', 'CONTENT_ARCHIVE')
ON CONFLICT DO NOTHING;

-- Also seed essential starter Content Tags
INSERT INTO content_tags (id, name, slug) VALUES
    ('30000000-0000-0000-0000-000000000001', 'GST', 'gst'),
    ('30000000-0000-0000-0000-000000000002', 'GST Return', 'gst-return'),
    ('30000000-0000-0000-0000-000000000003', 'Income Tax', 'income-tax'),
    ('30000000-0000-0000-0000-000000000004', 'ITR Filing', 'itr-filing'),
    ('30000000-0000-0000-0000-000000000005', 'TDS', 'tds'),
    ('30000000-0000-0000-0000-000000000006', 'Tax Deductions', 'tax-deductions'),
    ('30000000-0000-0000-0000-000000000007', 'Salaried', 'salaried'),
    ('30000000-0000-0000-0000-000000000008', 'Small Business', 'small-business'),
    ('30000000-0000-0000-0000-000000000009', 'Startup', 'startup'),
    ('30000000-0000-0000-0000-000000000010', 'Beginner Guide', 'beginner-guide')
ON CONFLICT (slug) DO NOTHING;
