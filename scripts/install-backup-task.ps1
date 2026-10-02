param(
    [Parameter(Mandatory = $true)]
    [string]$ExternalDirectory,

    [ValidatePattern('^([01]\d|2[0-3]):[0-5]\d$')]
    [string]$At = '02:00'
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$destination = [System.IO.Path]::GetFullPath($ExternalDirectory)
$repositoryPrefix = $repositoryRoot.TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar

if ([string]::Equals($destination.TrimEnd('\', '/'), $repositoryRoot.TrimEnd('\', '/'), [System.StringComparison]::OrdinalIgnoreCase) -or
    $destination.StartsWith($repositoryPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Escolha uma pasta fora do repositório para que a cópia sobreviva à troca de máquina.'
}

$backupScript = Join-Path $PSScriptRoot 'backup-postgres.ps1'
$powershell = (Get-Command powershell.exe -ErrorAction Stop).Source
$arguments = '-NoProfile -ExecutionPolicy Bypass -File "{0}" -ExternalDirectory "{1}"' -f $backupScript, $destination
$action = New-ScheduledTaskAction -Execute $powershell -Argument $arguments -WorkingDirectory $repositoryRoot
$scheduleTime = [DateTime]::Today.Add([TimeSpan]::Parse($At))
$trigger = New-ScheduledTaskTrigger -Daily -At $scheduleTime
$settings = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable `
    -ExecutionTimeLimit (New-TimeSpan -Hours 2) `
    -MultipleInstances IgnoreNew
$principal = New-ScheduledTaskPrincipal `
    -UserId ([System.Security.Principal.WindowsIdentity]::GetCurrent().Name) `
    -LogonType Interactive `
    -RunLevel Limited

Register-ScheduledTask `
    -TaskName 'SGFL - Backup PostgreSQL externo' `
    -Description 'Cria um dump PostgreSQL e verifica a cópia SHA-256 no destino externo.' `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Principal $principal `
    -Force | Out-Null

Write-Output "Tarefa diária registrada para ${At}: $destination"
Write-Output "Ela usa a sessão atual do Windows; Docker Desktop e o destino precisam estar acessíveis quando executar."
