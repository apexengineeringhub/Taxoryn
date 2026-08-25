-- Extends the single Application Feedback engine for customers, practitioners,
-- and practice employees. Practice identity is derived from the JWT-backed
-- organization context in the service layer; it is never accepted from clients.
ALTER TABLE application_feedback
    ADD COLUMN actor_type VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER',
    ADD COLUMN practice_id UUID,
    ADD COLUMN context_type VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER_PORTAL';

ALTER TABLE application_feedback
    ADD CONSTRAINT fk_application_feedback_practice
        FOREIGN KEY (practice_id) REFERENCES organizations(id) ON DELETE SET NULL;

CREATE INDEX idx_application_feedback_practice_id ON application_feedback(practice_id);
CREATE INDEX idx_application_feedback_actor_type ON application_feedback(actor_type);
CREATE INDEX idx_application_feedback_practice_actor_created
    ON application_feedback(practice_id, actor_type, created_at DESC);
