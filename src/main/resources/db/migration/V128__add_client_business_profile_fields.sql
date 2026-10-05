-- V128: Add Client Business Profile Fields for Phase 28.2
ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS business_activity VARCHAR(255),
    ADD COLUMN IF NOT EXISTS industry VARCHAR(100),
    ADD COLUMN IF NOT EXISTS business_scale VARCHAR(50),
    ADD COLUMN IF NOT EXISTS state_code VARCHAR(10);

CREATE INDEX IF NOT EXISTS idx_clients_industry ON clients(industry);
CREATE INDEX IF NOT EXISTS idx_clients_state_code ON clients(state_code);
