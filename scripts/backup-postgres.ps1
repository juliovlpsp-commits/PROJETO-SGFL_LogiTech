param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\backups")
)

$ErrorActionPreference = 'Stop'
$outputPath = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null

$userResult = & docker compose exec -T postgres printenv POSTGRES_USER
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível ler POSTGRES_USER do serviço Postgres.' }
$databaseResult = & docker compose exec -T postgres printenv POSTGRES_DB
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível ler POSTGRES_DB do serviço Postgres.' }
$dbUser = ($userResult | Out-String).Trim()
$dbName = ($databaseResult | Out-String).Trim()
if ([string]::IsNullOrWhiteSpace($dbUser) -or [string]::IsNullOrWhiteSpace($dbName)) {
    throw 'POSTGRES_USER ou POSTGRES_DB está vazio no serviço Postgres.'
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupFile = Join-Path $outputPath "sgfl-$timestamp.dump"
$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = 'docker'
$startInfo.Arguments = 'compose exec -T postgres pg_dump -Fc -U "{0}" -d "{1}"' -f $dbUser, $dbName
$startInfo.UseShellExecute = $false
$startInfo.RedirectStandardOutput = $true
$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo

try {
    if (-not $process.Start()) { throw 'Não foi possível iniciar pg_dump.' }
    $fileStream = [System.IO.File]::Open($backupFile, [System.IO.FileMode]::CreateNew)
    try { $process.StandardOutput.BaseStream.CopyTo($fileStream) }
    finally { $fileStream.Dispose() }
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "pg_dump terminou com código $($process.ExitCode)." }
    if ((Get-Item -LiteralPath $backupFile).Length -eq 0) { throw 'O arquivo de backup ficou vazio.' }
} catch {
    Remove-Item -LiteralPath $backupFile -Force -ErrorAction SilentlyContinue
    throw
} finally {
    $process.Dispose()
}

$hash = (Get-FileHash -LiteralPath $backupFile -Algorithm SHA256).Hash
Write-Output "Backup criado: $backupFile"
Write-Output "SHA-256: $hash"
