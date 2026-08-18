<#
.SYNOPSIS
    Taxoryn Database Provisioning & Development Seeding Script (PowerShell)

.DESCRIPTION
    1. Checks Docker availability
    2. Starts PostgreSQL container via docker compose
    3. Waits for PostgreSQL readiness
    4. Automatically applies init schema and optional dev seed data
#>

param (
    [switch]$SeedData,
    [switch]$Reset
)

$ErrorActionPreference = "Stop"
$ProjectDir = Split-Path -Parent $PSScriptRoot

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "   Taxoryn Database Provisioning Tool   " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Check Docker
Write-Host "[1/4] Checking Docker status..." -ForegroundColor Yellow
try {
    docker info | Out-Null
    Write-Host "✓ Docker is running." -ForegroundColor Green
} catch {
    Write-Error "Docker is not running or not installed. Please start Docker Desktop and retry."
}

# 2. Reset if requested
if ($Reset) {
    Write-Host "Resetting existing database volume..." -ForegroundColor Magenta
    docker compose -f "$ProjectDir/docker-compose.yml" down -v
}

# 3. Start PostgreSQL Container
Write-Host "[2/4] Starting PostgreSQL 16 Alpine container..." -ForegroundColor Yellow
docker compose -f "$ProjectDir/docker-compose.yml" up -d postgres

# 4. Wait for PostgreSQL readiness
Write-Host "[3/4] Waiting for PostgreSQL readiness..." -ForegroundColor Yellow
$retries = 30
$ready = $false
while ($retries -gt 0 -and -not $ready) {
    Start-Sleep -Seconds 1
    $status = docker exec taxoryn_postgres pg_isready -U taxoryn_user -d taxoryn_db 2>&1
    if ($status -match "accepting connections") {
        $ready = $true
        Write-Host "✓ PostgreSQL is ready and accepting connections." -ForegroundColor Green
    } else {
        $retries--
        Write-Host "  ... waiting for PostgreSQL ($retries retries left)" -ForegroundColor Gray
    }
}

if (-not $ready) {
    Write-Error "PostgreSQL container failed to become ready within timeout."
}

# 5. Apply Initial Consolidated Schema
Write-Host "[4/4] Executing schema initialization (init-db.sql)..." -ForegroundColor Yellow
Get-Content "$PSScriptRoot/init-db.sql" -Raw | docker exec -i taxoryn_postgres psql -U taxoryn_user -d taxoryn_db
Write-Host "✓ Schema initialized successfully." -ForegroundColor Green

# 6. Apply Dev Seed Data if requested
if ($SeedData) {
    Write-Host "Seeding development demo data (seed-dev-data.sql)..." -ForegroundColor Magenta
    Get-Content "$PSScriptRoot/seed-dev-data.sql" -Raw | docker exec -i taxoryn_postgres psql -U taxoryn_user -d taxoryn_db
    Write-Host "✓ Seed data inserted successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Demo Credentials:" -ForegroundColor Cyan
    Write-Host "  Org Admin:    admin@apextax.com      / Password123!" -ForegroundColor White
    Write-Host "  Manager:      manager@apextax.com    / Password123!" -ForegroundColor White
    Write-Host "  Tax Pro:      taxpro@apextax.com     / Password123!" -ForegroundColor White
    Write-Host "  Accountant:   accountant@apextax.com / Password123!" -ForegroundColor White
    Write-Host "  Staff:        staff@apextax.com      / Password123!" -ForegroundColor White
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Green
Write-Host "✓ Taxoryn Database Setup Complete!" -ForegroundColor Green
Write-Host "  Host:      localhost:5432" -ForegroundColor White
Write-Host "  Database:  taxoryn_db" -ForegroundColor White
Write-Host "  Username:  taxoryn_user" -ForegroundColor White
Write-Host "  Password:  taxoryn_secret" -ForegroundColor White
Write-Host "=========================================" -ForegroundColor Green
