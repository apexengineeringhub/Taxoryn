#!/usr/bin/env bash
# ==============================================================================
# Taxoryn Platform — Database Provisioning & Development Seeding Script (Bash)
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "========================================="
echo "   Taxoryn Database Provisioning Tool   "
echo "========================================="

# 1. Check Docker
echo "[1/4] Checking Docker status..."
if ! docker info >/dev/null 2>&1; then
    echo "Error: Docker is not running or not installed. Please start Docker and retry."
    exit 1
fi
echo "✓ Docker is running."

# 2. Reset if --reset flag provided
if [[ "$*" == *"--reset"* ]]; then
    echo "Resetting existing database volume..."
    docker compose -f "$PROJECT_DIR/docker-compose.yml" down -v
fi

# 3. Start PostgreSQL Container
echo "[2/4] Starting PostgreSQL 16 Alpine container..."
docker compose -f "$PROJECT_DIR/docker-compose.yml" up -d postgres

# 4. Wait for PostgreSQL readiness
echo "[3/4] Waiting for PostgreSQL readiness..."
RETRIES=30
READY=false
while [ $RETRIES -gt 0 ] && [ "$READY" = false ]; do
    sleep 1
    if docker exec taxoryn_postgres pg_isready -U taxoryn_user -d taxoryn_db >/dev/null 2>&1; then
        READY=true
        echo "✓ PostgreSQL is ready and accepting connections."
    else
        RETRIES=$((RETRIES - 1))
        echo "  ... waiting for PostgreSQL ($RETRIES retries left)"
    fi
done

if [ "$READY" = false ]; then
    echo "Error: PostgreSQL container failed to become ready within timeout."
    exit 1
fi

# 5. Apply Initial Consolidated Schema
echo "[4/4] Executing schema initialization (init-db.sql)..."
docker exec -i taxoryn_postgres psql -U taxoryn_user -d taxoryn_db < "$SCRIPT_DIR/init-db.sql"
echo "✓ Schema initialized successfully."

# 6. Apply Dev Seed Data if --seed flag provided
if [[ "$*" == *"--seed"* ]]; then
    echo "Seeding development demo data (seed-dev-data.sql)..."
    docker exec -i taxoryn_postgres psql -U taxoryn_user -d taxoryn_db < "$SCRIPT_DIR/seed-dev-data.sql"
    echo "✓ Seed data inserted successfully!"
    echo ""
    echo "Demo Credentials:"
    echo "  Org Admin:    admin@apextax.com      / Password123!"
    echo "  Manager:      manager@apextax.com    / Password123!"
    echo "  Tax Pro:      taxpro@apextax.com     / Password123!"
    echo "  Accountant:   accountant@apextax.com / Password123!"
    echo "  Staff:        staff@apextax.com      / Password123!"
fi

echo ""
echo "========================================="
echo "✓ Taxoryn Database Setup Complete!"
echo "  Host:      localhost:5432"
echo "  Database:  taxoryn_db"
echo "  Username:  taxoryn_user"
echo "  Password:  taxoryn_secret"
echo "========================================="
