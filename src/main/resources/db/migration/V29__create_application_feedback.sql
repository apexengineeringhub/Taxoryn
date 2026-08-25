-- Application Feedback is product feedback about Taxoryn itself. It is intentionally
-- separate from marketplace_reviews, which contains verified tax-practice reviews.
CREATE TABLE application_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    feedback_type VARCHAR(30) NOT NULL,
    category VARCHAR(40) NOT NULL,
    rating INTEGER,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    page_path VARCHAR(500),
    feature_name VARCHAR(100),
    source VARCHAR(40) NOT NULL DEFAULT 'WEB',
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_application_feedback_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_application_feedback_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_application_feedback_user_id ON application_feedback(user_id);
CREATE INDEX idx_application_feedback_type ON application_feedback(feedback_type);
CREATE INDEX idx_application_feedback_status ON application_feedback(status);
CREATE INDEX idx_application_feedback_created_at ON application_feedback(created_at DESC);
