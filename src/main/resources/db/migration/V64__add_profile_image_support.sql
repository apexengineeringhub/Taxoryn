-- V64: Add profile image and avatar support for users, employees, and clients
-- Enables secure, tenant-isolated profile photo storage and retrieval.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(1000);

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(1000);

ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_users_avatar_url ON users(avatar_url);
CREATE INDEX IF NOT EXISTS idx_employees_avatar_url ON employees(avatar_url);
CREATE INDEX IF NOT EXISTS idx_clients_avatar_url ON clients(avatar_url);
