-- ==============================================================================
-- Taxoryn Platform - Stage 2.5 Migration (V110)
-- P0.5: Reminder / Automation Foundation
-- Tables: reminders, automation_rules
-- ==============================================================================

-- ============================================================
-- 1. Automation Rules
-- Configurable rules that generate reminders based on events.
-- organization_id = NULL → system default (applies to all orgs)
-- ============================================================
CREATE TABLE IF NOT EXISTS automation_rules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID,               -- NULL = global system default
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    event_type          VARCHAR(80)  NOT NULL,   -- TASK_DUE, TASK_ASSIGNED, TASK_OVERDUE, TASK_CREATED, TASK_COMPLETED, DOCUMENT_UPLOADED, WORK_INSTANCE_CREATED, WORK_INSTANCE_DUE
    days_offset         INTEGER      NOT NULL DEFAULT -1,  -- negative = N days BEFORE event, 0 = same day, positive = N days AFTER
    action_type         VARCHAR(80)  NOT NULL DEFAULT 'CREATE_REMINDER',  -- CREATE_REMINDER, IN_APP_NOTIFICATION
    target_type         VARCHAR(80)  NOT NULL DEFAULT 'TASK_ASSIGNEE',   -- TASK_ASSIGNEE, ENGAGEMENT_OWNER, SPECIFIC_USER, WORK_INSTANCE_ASSIGNEE
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Audit fields (AuditableEntity — no org FK for system defaults)
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    version             BIGINT       NOT NULL DEFAULT 0,
    -- FK: organization (nullable for system defaults)
    CONSTRAINT fk_automation_rules_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_automation_rules_org      ON automation_rules(organization_id);
CREATE INDEX IF NOT EXISTS idx_automation_rules_event    ON automation_rules(event_type, enabled);
CREATE INDEX IF NOT EXISTS idx_automation_rules_enabled  ON automation_rules(enabled);

-- ============================================================
-- 2. Default automation rules (system-level, org = NULL)
-- These appear for all organizations until overridden.
-- ============================================================
INSERT INTO automation_rules (name, description, event_type, days_offset, action_type, target_type, enabled, created_by)
VALUES
    ('Task Due Soon',   'Remind task assignee 2 days before the task due date.',  'TASK_DUE',      -2, 'CREATE_REMINDER', 'TASK_ASSIGNEE', TRUE, 'SYSTEM'),
    ('Task Overdue',    'Notify task assignee when a task passes its due date.',   'TASK_OVERDUE',   0, 'CREATE_REMINDER', 'TASK_ASSIGNEE', TRUE, 'SYSTEM')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. Reminders
-- Individual reminder records — created manually or by automation.
-- ============================================================
CREATE TABLE IF NOT EXISTS reminders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID         NOT NULL,
    -- Content
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    reminder_type       VARCHAR(50)  NOT NULL DEFAULT 'GENERAL',  -- TASK_DUE, TASK_OVERDUE, FOLLOW_UP, DOCUMENT_COLLECTION, GENERAL
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING',   -- PENDING, TRIGGERED, COMPLETED, CANCELLED
    priority            VARCHAR(30)  NOT NULL DEFAULT 'MEDIUM',    -- LOW, MEDIUM, HIGH, URGENT
    -- Relationships
    target_user_id      UUID,       -- who should be reminded (practitioner)
    client_id           UUID,       -- optional client association
    engagement_id       UUID,       -- optional engagement association
    work_instance_id    UUID,       -- optional work instance association
    task_id             UUID,       -- optional task association
    automation_rule_id  UUID,       -- which rule generated this (null = manual)
    -- Reference for automation-generated reminders (for dedup / idempotency)
    reference_type      VARCHAR(64),  -- 'TASK', 'WORK_INSTANCE', etc.
    reference_id        VARCHAR(64),  -- UUID string of the referenced entity
    -- Idempotency: prevents duplicate automation-generated reminders
    -- Pattern: <rule_id>::<reference_type>::<reference_id>::<yyyy-MM-dd>
    idempotency_key     VARCHAR(255) UNIQUE,
    -- Scheduling
    scheduled_at        TIMESTAMPTZ  NOT NULL,
    recurrence_type     VARCHAR(30)  NOT NULL DEFAULT 'NONE',  -- NONE, DAILY, WEEKLY, MONTHLY
    -- Lifecycle timestamps
    triggered_at        TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    notes               TEXT,
    -- Audit fields (TenantAuditableEntity)
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    version             BIGINT       NOT NULL DEFAULT 0,
    -- FK constraints
    CONSTRAINT fk_reminders_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_reminders_task
        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    CONSTRAINT fk_reminders_automation_rule
        FOREIGN KEY (automation_rule_id) REFERENCES automation_rules(id) ON DELETE SET NULL
);

-- Performance indexes
CREATE INDEX IF NOT EXISTS idx_reminders_org             ON reminders(organization_id);
CREATE INDEX IF NOT EXISTS idx_reminders_target_user     ON reminders(target_user_id);
CREATE INDEX IF NOT EXISTS idx_reminders_status          ON reminders(status);
CREATE INDEX IF NOT EXISTS idx_reminders_scheduled_at    ON reminders(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_reminders_task_id         ON reminders(task_id);
CREATE INDEX IF NOT EXISTS idx_reminders_client          ON reminders(client_id);
CREATE INDEX IF NOT EXISTS idx_reminders_engagement      ON reminders(engagement_id);
CREATE INDEX IF NOT EXISTS idx_reminders_work_instance   ON reminders(work_instance_id);
-- Scheduler query: find all PENDING reminders due for processing
CREATE INDEX IF NOT EXISTS idx_reminders_pending_due     ON reminders(status, scheduled_at) WHERE status = 'PENDING';
