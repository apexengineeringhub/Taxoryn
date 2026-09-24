-- ==============================================================================
-- Taxoryn Platform - Phase 16 Migration (V80)
-- Tax Notice Resolution Workflow & Follow-Up Enhancements
-- ==============================================================================

-- 1. Extend tax_notices with Risk Level, Waiting for Client, Follow-up & Linkages
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'tax_notices' AND column_name = 'risk_level') THEN
        ALTER TABLE tax_notices ADD COLUMN risk_level VARCHAR(50) NOT NULL DEFAULT 'MEDIUM';
        CREATE INDEX IF NOT EXISTS idx_tax_notices_risk_level ON tax_notices(risk_level);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'tax_notices' AND column_name = 'waiting_for_client') THEN
        ALTER TABLE tax_notices ADD COLUMN waiting_for_client BOOLEAN NOT NULL DEFAULT FALSE;
        CREATE INDEX IF NOT EXISTS idx_tax_notices_waiting_for_client ON tax_notices(waiting_for_client);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'tax_notices' AND column_name = 'waiting_reason') THEN
        ALTER TABLE tax_notices ADD COLUMN waiting_reason TEXT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'tax_notices' AND column_name = 'waiting_requested_at') THEN
        ALTER TABLE tax_notices ADD COLUMN waiting_requested_at TIMESTAMP WITH TIME ZONE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'tax_notices' AND column_name = 'waiting_requested_by') THEN
        ALTER TABLE tax_notices ADD COLUMN waiting_requested_by UUID REFERENCES users(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'tax_notices' AND column_name = 'expected_response_date') THEN
        ALTER TABLE tax_notices ADD COLUMN expected_response_date DATE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'tax_notices' AND column_name = 'follow_up_date') THEN
        ALTER TABLE tax_notices ADD COLUMN follow_up_date DATE;
        CREATE INDEX IF NOT EXISTS idx_tax_notices_follow_up_date ON tax_notices(follow_up_date);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'tax_notices' AND column_name = 'follow_up_notes') THEN
        ALTER TABLE tax_notices ADD COLUMN follow_up_notes TEXT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'tax_notices' AND column_name = 'compliance_obligation_id') THEN
        ALTER TABLE tax_notices ADD COLUMN compliance_obligation_id UUID REFERENCES compliance_obligations(id) ON DELETE SET NULL;
        CREATE INDEX IF NOT EXISTS idx_tax_notices_compliance_obligation_id ON tax_notices(compliance_obligation_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'tax_notices' AND column_name = 'client_service_id') THEN
        ALTER TABLE tax_notices ADD COLUMN client_service_id UUID REFERENCES client_services(id) ON DELETE SET NULL;
        CREATE INDEX IF NOT EXISTS idx_tax_notices_client_service_id ON tax_notices(client_service_id);
    END IF;
END $$;
