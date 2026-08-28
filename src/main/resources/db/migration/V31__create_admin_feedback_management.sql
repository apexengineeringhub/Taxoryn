-- ==============================================================================
-- Taxoryn Platform - Phase 0 Migration (V31)
-- Admin Feedback Management & Internal Operations Lifecycle
-- ==============================================================================

-- 1. Extend application_feedback table for resolution, closure, assignment, and deduplication
ALTER TABLE application_feedback
    ADD COLUMN IF NOT EXISTS duplicate_of_id UUID,
    ADD COLUMN IF NOT EXISTS resolution_note VARCHAR(4000),
    ADD COLUMN IF NOT EXISTS resolved_by UUID,
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS closed_by UUID,
    ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS assigned_team VARCHAR(50),
    ADD COLUMN IF NOT EXISTS assigned_user_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_application_feedback_duplicate_of') THEN
        ALTER TABLE application_feedback
            ADD CONSTRAINT fk_application_feedback_duplicate_of
            FOREIGN KEY (duplicate_of_id) REFERENCES application_feedback(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_application_feedback_resolved_by') THEN
        ALTER TABLE application_feedback
            ADD CONSTRAINT fk_application_feedback_resolved_by
            FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_application_feedback_closed_by') THEN
        ALTER TABLE application_feedback
            ADD CONSTRAINT fk_application_feedback_closed_by
            FOREIGN KEY (closed_by) REFERENCES users(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_application_feedback_assigned_user') THEN
        ALTER TABLE application_feedback
            ADD CONSTRAINT fk_application_feedback_assigned_user
            FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_application_feedback_duplicate_of ON application_feedback(duplicate_of_id);
CREATE INDEX IF NOT EXISTS idx_application_feedback_assigned_team ON application_feedback(assigned_team);
CREATE INDEX IF NOT EXISTS idx_application_feedback_assigned_user ON application_feedback(assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_application_feedback_priority ON application_feedback(priority);

-- 2. Create feedback_assignments table
CREATE TABLE IF NOT EXISTS feedback_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID NOT NULL,
    team VARCHAR(50) NOT NULL,
    assigned_user_id UUID,
    assigned_by UUID,
    reason VARCHAR(500),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_feedback_assignments_feedback FOREIGN KEY (feedback_id) REFERENCES application_feedback(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_assignments_user FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_feedback_assignments_by FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL
);

ALTER TABLE feedback_assignments
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_feedback_assignments_feedback ON feedback_assignments(feedback_id, is_active);
CREATE INDEX IF NOT EXISTS idx_feedback_assignments_user ON feedback_assignments(assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_feedback_assignments_team ON feedback_assignments(team);

-- 3. Create feedback_notes table
CREATE TABLE IF NOT EXISTS feedback_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID NOT NULL,
    author_id UUID,
    author_name VARCHAR(150),
    note VARCHAR(4000) NOT NULL,
    visibility VARCHAR(30) NOT NULL DEFAULT 'INTERNAL',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_feedback_notes_feedback FOREIGN KEY (feedback_id) REFERENCES application_feedback(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_notes_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL
);

ALTER TABLE feedback_notes
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_feedback_notes_feedback ON feedback_notes(feedback_id, created_at ASC);

-- 4. Create feedback_status_history table
CREATE TABLE IF NOT EXISTS feedback_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by UUID,
    changed_by_name VARCHAR(150),
    reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_feedback_status_history_feedback FOREIGN KEY (feedback_id) REFERENCES application_feedback(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_status_history_changed_by FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL
);

ALTER TABLE feedback_status_history
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_feedback_status_history_feedback ON feedback_status_history(feedback_id, created_at ASC);

-- 5. Create engineering_issues table (1 : 0..1 relationship with application_feedback)
CREATE TABLE IF NOT EXISTS engineering_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID NOT NULL UNIQUE,
    issue_code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'HIGH',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_team VARCHAR(50) NOT NULL DEFAULT 'ENGINEERING',
    creator_user_id UUID,
    external_system VARCHAR(50),
    external_issue_id VARCHAR(100),
    external_issue_url VARCHAR(500),
    external_status VARCHAR(50),
    last_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_engineering_issues_feedback FOREIGN KEY (feedback_id) REFERENCES application_feedback(id) ON DELETE CASCADE,
    CONSTRAINT fk_engineering_issues_creator FOREIGN KEY (creator_user_id) REFERENCES users(id) ON DELETE SET NULL
);

ALTER TABLE engineering_issues
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_engineering_issues_code ON engineering_issues(issue_code);
CREATE INDEX IF NOT EXISTS idx_engineering_issues_status ON engineering_issues(status);

-- 6. Insert Admin Feedback Management Permissions
INSERT INTO permissions (id, code, name, module, description) VALUES
    ('10000000-0000-0000-0000-000000000081', 'FEEDBACK_VIEW', 'View Admin Feedback', 'FEEDBACK', 'Allows browsing and searching application feedback across all actors'),
    ('10000000-0000-0000-0000-000000000082', 'FEEDBACK_REVIEW', 'Review Feedback', 'FEEDBACK', 'Allows starting review, adding internal notes and triage'),
    ('10000000-0000-0000-0000-000000000083', 'FEEDBACK_ASSIGN', 'Assign Feedback', 'FEEDBACK', 'Allows assigning feedback to internal teams and team members'),
    ('10000000-0000-0000-0000-000000000084', 'FEEDBACK_RESOLVE', 'Resolve & Close Feedback', 'FEEDBACK', 'Allows resolving, closing, rejecting, or marking feedback as duplicate'),
    ('10000000-0000-0000-0000-000000000085', 'FEEDBACK_ESCALATE', 'Escalate to Engineering', 'FEEDBACK', 'Allows creating internal engineering issues from feedback'),
    ('10000000-0000-0000-0000-000000000086', 'FEEDBACK_MANAGE', 'Full Feedback Administration', 'FEEDBACK', 'Full management access for application feedback operations')
ON CONFLICT (code) DO NOTHING;

-- 7. Grant Feedback Permissions to SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000001', id FROM permissions
WHERE code IN ('FEEDBACK_VIEW', 'FEEDBACK_REVIEW', 'FEEDBACK_ASSIGN', 'FEEDBACK_RESOLVE', 'FEEDBACK_ESCALATE', 'FEEDBACK_MANAGE')
ON CONFLICT DO NOTHING;
