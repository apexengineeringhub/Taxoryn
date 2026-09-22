-- V74__tax_notice_operations_configuration.sql
-- Phase 8.3: Configuration-Driven Tax Notice Operations

CREATE TABLE IF NOT EXISTS tax_notice_configurations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
    
    -- Workflow Configuration
    response_review_required BOOLEAN NOT NULL DEFAULT TRUE,
    partner_approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    hearing_tracking_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    response_submission_tracking_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Deadline Defaults
    default_response_due_days INT NOT NULL DEFAULT 30,
    reminder_days_before_due INT NOT NULL DEFAULT 7,
    escalation_days_after_due INT NOT NULL DEFAULT 2,
    
    -- Assignment & Automation
    auto_create_response_task BOOLEAN NOT NULL DEFAULT TRUE,
    default_priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    assignment_required BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Notifications
    notify_on_assignment BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_due_soon BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_overdue BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_submission BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_hearing BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Dashboard Widgets
    show_due_soon BOOLEAN NOT NULL DEFAULT TRUE,
    show_overdue BOOLEAN NOT NULL DEFAULT TRUE,
    show_awaiting_response BOOLEAN NOT NULL DEFAULT TRUE,
    show_awaiting_hearing BOOLEAN NOT NULL DEFAULT TRUE,
    show_awaiting_order BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Customization tracking
    is_customized BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_tnc_org_id ON tax_notice_configurations(organization_id);
