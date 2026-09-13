<#
.SYNOPSIS
    Runs targeted cleanup for Practitioner, Client, and Employee data.
.DESCRIPTION
    Executes scripts/cleanup_clients_and_employees.sql against the target PostgreSQL database.
.PARAMETER DbUrl
    PostgreSQL connection string or JDBC URL (optional, defaults to SPRING_DATASOURCE_URL or localhost).
#>

param(
    [string]$DbHost = $(if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }),
    [string]$DbPort = $(if ($env:DB_PORT) { $env:DB_PORT } else { "5432" }),
    [string]$DbName = $(if ($env:DB_NAME) { $env:DB_NAME } else { "taxoryn" }),
    [string]$DbUser = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "postgres" })
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$SqlFile = Join-Path $ScriptDir "cleanup_clients_and_employees.sql"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Taxoryn Targeted Cleanup: Practitioner, Client & Employee" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Host: $DbHost"
Write-Host "Port: $DbPort"
Write-Host "Database: $DbName"
Write-Host "User: $DbUser"
Write-Host "SQL Script: $SqlFile"
Write-Host ""

$confirmation = Read-Host "Are you sure you want to delete ALL clients, employees, practitioners, and their filings/tasks/invoices? (type 'YES' to proceed)"
if ($confirmation -ne "YES") {
    Write-Host "Operation cancelled by user." -ForegroundColor Yellow
    exit 0
}

Write-Host "Executing cleanup script..." -ForegroundColor Green
& psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -f $SqlFile

if ($LASTEXITCODE -eq 0) {
    Write-Host "Cleanup completed successfully!" -ForegroundColor Green
} else {
    Write-Host "Error executing cleanup script. Exit code: $LASTEXITCODE" -ForegroundColor Red
}
