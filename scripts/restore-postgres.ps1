param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,
    [string]$BackupDirectory = (Join-Path $PSScriptRoot "..\backups")
)

$ErrorActionPreference = 'Stop'
$dumpPath = (Resolve-Path -LiteralPath $BackupFile).Path
if ((Get-Item -LiteralPath $dumpPath).Length -eq 0) { throw 'O arquivo de backup está vazio.' }

Write-Warning 'A restauração substitui objetos e dados do banco configurado no Docker Compose.'
$confirmacao = Read-Host 'Digite RESTAURAR para continuar'
if ($confirmacao -cne 'RESTAURAR') {
    Write-Output 'Restauração cancelada; nenhum serviço foi alterado.'
    exit 0
}

& (Join-Path $PSScriptRoot 'backup-postgres.ps1') -OutputDirectory $BackupDirectory
if ($LASTEXITCODE -ne 0) { throw 'O backup de segurança anterior à restauração falhou.' }

$userResult = & docker compose exec -T postgres printenv POSTGRES_USER
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível ler POSTGRES_USER do serviço Postgres.' }
$databaseResult = & docker compose exec -T postgres printenv POSTGRES_DB
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível ler POSTGRES_DB do serviço Postgres.' }
$dbUser = ($userResult | Out-String).Trim()
$dbName = ($databaseResult | Out-String).Trim()

& docker compose stop backend
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível parar o backend antes da restauração.' }

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = 'docker'
$startInfo.Arguments = 'compose exec -T postgres pg_restore --clean --if-exists --no-owner --no-privileges -U "{0}" -d "{1}"' -f $dbUser, $dbName
$startInfo.UseShellExecute = $false
$startInfo.RedirectStandardInput = $true
$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo

try {
    if (-not $process.Start()) { throw 'Não foi possível iniciar pg_restore.' }
    $fileStream = [System.IO.File]::OpenRead($dumpPath)
    try { $fileStream.CopyTo($process.StandardInput.BaseStream) }
    finally { $fileStream.Dispose(); $process.StandardInput.Close() }
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "pg_restore terminou com código $($process.ExitCode). O banco pode estar parcialmente restaurado." }
} finally {
    $process.Dispose()
    & docker compose start backend
}

Write-Output "Restauração concluída a partir de: $dumpPath"
