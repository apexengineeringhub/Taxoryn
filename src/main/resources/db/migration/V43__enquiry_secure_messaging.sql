-- V43__enquiry_secure_messaging.sql
-- Enables secure, auditable messaging between Customers and Certified Practices / Assigned Practitioners for Enquiries

CREATE TABLE IF NOT EXISTS marketplace_enquiry_messages (
    id UUID PRIMARY KEY,
    enquiry_id UUID NOT NULL REFERENCES marketplace_leads(id) ON DELETE CASCADE,
    sender_type VARCHAR(30) NOT NULL,
    sender_user_id UUID,
    sender_name VARCHAR(150) NOT NULL,
    message_body TEXT NOT NULL,
    attachments_json TEXT,
    is_read_by_customer BOOLEAN NOT NULL DEFAULT FALSE,
    is_read_by_practice BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_enq_msg_enquiry_created ON marketplace_enquiry_messages(enquiry_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_enq_msg_customer_unread ON marketplace_enquiry_messages(enquiry_id, is_read_by_customer);
CREATE INDEX IF NOT EXISTS idx_enq_msg_practice_unread ON marketplace_enquiry_messages(enquiry_id, is_read_by_practice);
