# ==============================================================================
# Script to Copy Local MySQL Database to Aiven Cloud MySQL
# ==============================================================================

Write-Host "Syncing Local MySQL -> Aiven Cloud SQL..." -ForegroundColor Cyan

# Load credentials from .env
if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
        }
    }
}

$AivenHost = "mysql-a04d93a-dheepakadhithya-15ee.e.aivencloud.com"
$AivenPort = "26835"
$AivenUser = "avnadmin"
$AivenPassword = $env:SPRING_DATASOURCE_PASSWORD
$LocalPassword = "7010816140123@aA"

$MysqlDumpPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"
$MysqlPath     = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

if (-not (Test-Path $MysqlDumpPath)) {
    Write-Error "mysqldump.exe not found at $MysqlDumpPath"
    exit 1
}

Write-Host "1. Exporting local database 'chemical'..." -ForegroundColor Yellow
& $MysqlDumpPath -u root "-p$LocalPassword" --no-tablespaces --set-gtid-purged=OFF chemical | Set-Content -Path "chemical_backup.sql" -Encoding utf8

Write-Host "2. Uploading data to Aiven Cloud MySQL 'defaultdb'..." -ForegroundColor Yellow
Get-Content -Path "chemical_backup.sql" | & $MysqlPath -h $AivenHost -P $AivenPort -u $AivenUser "-p$AivenPassword" defaultdb

Write-Host "3. Verifying record counts in Aiven Cloud..." -ForegroundColor Yellow
& $MysqlPath -h $AivenHost -P $AivenPort -u $AivenUser "-p$AivenPassword" defaultdb -e "SELECT 'chemicals' AS `table`, COUNT(*) AS total FROM chemicals UNION ALL SELECT 'app_users', COUNT(*) FROM app_users UNION ALL SELECT 'reading', COUNT(*) FROM reading;"

Write-Host "Sync completed successfully!" -ForegroundColor Green
