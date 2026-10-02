param(
    [switch]$NoBuild
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$frontendPath = Join-Path $projectRoot 'sgfl-frontend'
$jarPath = Join-Path $projectRoot 'target\sgfl-0.0.1-SNAPSHOT.jar'
$dataPath = Join-Path $projectRoot 'data\sgfl.mv.db'
$javaExecutable = $null

if ($env:JAVA_HOME) {
    $javaInHome = Join-Path $env:JAVA_HOME 'bin\java.exe'
    if (Test-Path $javaInHome) { $javaExecutable = $javaInHome }
}
if (-not $javaExecutable) {
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) { $javaExecutable = $javaCommand.Source }
}
if (-not $javaExecutable) { throw 'Instale Java 17 ou superior e configure JAVA_HOME ou PATH.' }
$javaVersionText = (& $javaExecutable -version 2>&1 | Out-String)
if ($javaVersionText -notmatch 'version "(?<major>\d+)') {
    throw 'Nao foi possivel identificar a versao do Java instalado.'
}
if ([int]$Matches.major -lt 17) { throw 'Este aplicativo requer Java 17 ou superior.' }

Set-Location $projectRoot

if (-not $NoBuild -and (Test-Path $frontendPath) -and (Test-Path (Join-Path $projectRoot 'mvnw.cmd'))) {
    foreach ($commandName in @('node', 'npm')) {
        if (-not (Get-Command $commandName -ErrorAction SilentlyContinue)) {
            throw "Instale Node.js 20.19+ (ou 22.12+) e npm antes de continuar. Comando ausente: $commandName"
        }
    }

    Push-Location $frontendPath
    $apiUrlAnterior = $env:VITE_API_URL
    try {
        if (-not (Test-Path (Join-Path $frontendPath 'node_modules'))) {
            & npm.cmd ci
            if ($LASTEXITCODE -ne 0) { throw 'Falha ao instalar as dependencias do frontend.' }
        }

        $env:VITE_API_URL = '/api'
        & npm.cmd run build
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar o frontend.' }
    } finally {
        if ($null -eq $apiUrlAnterior) {
            Remove-Item Env:VITE_API_URL -ErrorAction SilentlyContinue
        } else {
            $env:VITE_API_URL = $apiUrlAnterior
        }
        Pop-Location
    }

    & (Join-Path $projectRoot 'mvnw.cmd') clean package -Pportable -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao empacotar o backend portatil.' }
} elseif (-not $NoBuild) {
    Write-Host 'Codigo-fonte nao encontrado; iniciando o JAR ja empacotado.' -ForegroundColor Cyan
}

if (-not (Test-Path $jarPath)) {
    throw 'O JAR ainda nao existe. Rode novamente sem -NoBuild para compilar o projeto.'
}

$dataDirectory = Split-Path -Parent $dataPath
New-Item -ItemType Directory -Force -Path $dataDirectory | Out-Null
$primeiroUso = -not (Test-Path $dataPath)

$variaveis = @(
    'SPRING_PROFILES_ACTIVE', 'JWT_SECRET', 'DB_URL', 'DB_USERNAME', 'DB_PASSWORD',
    'DB_DIALECT', 'BOOTSTRAP_ADMIN_ENABLED', 'BOOTSTRAP_ADMIN_EMAIL',
    'BOOTSTRAP_ADMIN_USERNAME', 'BOOTSTRAP_ADMIN_PASSWORD', 'FLYWAY_ENABLED'
)
$valoresAnteriores = @{}
foreach ($nome in $variaveis) {
    $valoresAnteriores[$nome] = [Environment]::GetEnvironmentVariable($nome, 'Process')
}

try {
    $bytesAleatorios = New-Object byte[] 48
    $gerador = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $gerador.GetBytes($bytesAleatorios)
    } finally {
        $gerador.Dispose()
    }

    $env:SPRING_PROFILES_ACTIVE = 'local'
    $env:JWT_SECRET = [Convert]::ToBase64String($bytesAleatorios)
    $env:DB_URL = 'jdbc:h2:file:./data/sgfl;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE'
    $env:DB_USERNAME = 'sa'
    $env:DB_PASSWORD = ''
    $env:DB_DIALECT = 'org.hibernate.dialect.H2Dialect'
    $env:FLYWAY_ENABLED = 'false'
    $env:BOOTSTRAP_ADMIN_ENABLED = 'false'

    if ($primeiroUso) {
        Write-Host 'Primeira execucao: crie o administrador inicial.' -ForegroundColor Cyan
        $emailAdmin = Read-Host 'E-mail do administrador'
        if ([string]::IsNullOrWhiteSpace($emailAdmin)) {
            throw 'O e-mail do administrador e obrigatorio na primeira execucao.'
        }

        $usuarioAdmin = Read-Host "Nome de usuario [$emailAdmin]"
        if ([string]::IsNullOrWhiteSpace($usuarioAdmin)) { $usuarioAdmin = $emailAdmin }

        do {
            $senhaSegura = Read-Host 'Senha do administrador (minimo 8 caracteres)' -AsSecureString
            if ($senhaSegura.Length -lt 8) {
                Write-Host 'A senha precisa ter pelo menos 8 caracteres.' -ForegroundColor Yellow
            }
        } while ($senhaSegura.Length -lt 8)

        $ponteiroSenha = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($senhaSegura)
        try {
            $env:BOOTSTRAP_ADMIN_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ponteiroSenha)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ponteiroSenha)
        }

        $env:BOOTSTRAP_ADMIN_EMAIL = $emailAdmin.Trim()
        $env:BOOTSTRAP_ADMIN_USERNAME = $usuarioAdmin.Trim()
        $env:BOOTSTRAP_ADMIN_ENABLED = 'true'
    }

    Write-Host 'SGFL disponivel em http://localhost:8080. Pressione Ctrl+C para encerrar.' -ForegroundColor Green
    if (-not $javaExecutable) { throw 'Java 17 ou superior nao foi encontrado. Configure JAVA_HOME ou PATH.' }
    & $javaExecutable -jar $jarPath
    if ($LASTEXITCODE -ne 0) { throw 'O SGFL encerrou com erro.' }
} finally {
    foreach ($nome in $variaveis) {
        $valorAnterior = $valoresAnteriores[$nome]
        if ($null -eq $valorAnterior) {
            Remove-Item "Env:$nome" -ErrorAction SilentlyContinue
        } else {
            Set-Item "Env:$nome" $valorAnterior
        }
    }
}
