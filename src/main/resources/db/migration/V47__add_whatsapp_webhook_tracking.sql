-- ==============================================================================
-- Taxoryn Platform - Migration (V47)
-- WhatsApp Webhook Status Tracking & Media Support
-- ==============================================================================

ALTER TABLE whatsapp_messages 
ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS media_url TEXT;

ALTER TABLE whatsapp_messages DROP CONSTRAINT IF EXISTS whatsapp_messages_status_check;
ALTER TABLE whatsapp_messages ADD CONSTRAINT whatsapp_messages_status_check CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED'));

CREATE INDEX IF NOT EXISTS idx_whatsapp_messages_provider_msg_id ON whatsapp_messages(provider_message_id);
