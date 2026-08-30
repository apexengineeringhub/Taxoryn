param (
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Email
)

$ErrorActionPreference = "Stop"
$CleanEmail = $Email.Trim().ToLower()

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Taxoryn Email Cleanup & Reset Tool     " -ForegroundColor Cyan
Write-Host " Target Email: $CleanEmail" -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Generate SQL with replaced email
$SqlScript = (Get-Content -Path "$PSScriptRoot/delete_user_by_email.sql" -Raw).Replace("test@example.com", $CleanEmail)

# 2. Check if Docker container taxoryn_postgres is running
$dockerRunning = $false
try {
    $containerStatus = docker inspect -f '{{.State.Running}}' taxoryn_postgres 2>$null
    if ($containerStatus -eq "true") {
        $dockerRunning = $true
    }
} catch {
    $dockerRunning = $false
}

if ($dockerRunning) {
    Write-Host "[1/2] Executing cleanup via Docker container (taxoryn_postgres)..." -ForegroundColor Yellow
    $SqlScript | docker exec -i taxoryn_postgres psql -U taxoryn_user -d taxoryn_db
    Write-Host ""
    Write-Host "Cleanup complete! Email '$CleanEmail' is ready for re-registration." -ForegroundColor Green
} else {
    Write-Host "[1/2] Docker container not detected. Attempting local psql connection..." -ForegroundColor Yellow
    $SqlScript | psql -U taxoryn_user -d taxoryn_db
    Write-Host ""
    Write-Host "Cleanup complete! Email '$CleanEmail' is ready for re-registration." -ForegroundColor Green
}

