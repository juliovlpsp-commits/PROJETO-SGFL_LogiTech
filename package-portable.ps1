param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$frontendPath = Join-Path $projectRoot 'sgfl-frontend'
$jarPath = Join-Path $projectRoot 'target\sgfl-0.0.1-SNAPSHOT.jar'
$packagePath = Join-Path $projectRoot 'portable-runtime'
$packageTargetPath = Join-Path $packagePath 'target'
$zipPath = Join-Path $projectRoot 'sgfl-portatil.zip'

Set-Location $projectRoot

if (-not $SkipBuild) {
    foreach ($commandName in @('node', 'npm')) {
        if (-not (Get-Command $commandName -ErrorAction SilentlyContinue)) {
            throw "Instale Node.js 20.19+ (ou 22.12+) e npm antes de empacotar. Comando ausente: $commandName"
        }
    }
    $javaNoPath = Get-Command java -ErrorAction SilentlyContinue
    $javaNoHome = $env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))
    if (-not $javaNoPath -and -not $javaNoHome) {
        throw 'Instale Java 17 ou superior e configure JAVA_HOME ou PATH antes de empacotar.'
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
}

if (-not (Test-Path $jarPath)) { throw 'JAR nao encontrado. Gere-o primeiro ou execute sem -SkipBuild.' }

New-Item -ItemType Directory -Force -Path $packageTargetPath | Out-Null
Copy-Item -LiteralPath $jarPath -Destination (Join-Path $packageTargetPath 'sgfl-0.0.1-SNAPSHOT.jar') -Force
Copy-Item -LiteralPath (Join-Path $projectRoot 'run-local.ps1') -Destination $packagePath -Force
Copy-Item -LiteralPath (Join-Path $projectRoot 'run-local.sh') -Destination $packagePath -Force
Copy-Item -LiteralPath (Join-Path $projectRoot 'run-local.bat') -Destination $packagePath -Force

$instructions = @'
SGFL - PACOTE PORTATIL

Requisito: Java 17 ou superior instalado na maquina.
Nao e necessario instalar Node.js, Maven, PostgreSQL ou Docker.

Windows (PowerShell):
  .\run-local.bat

Linux/macOS:
  bash ./run-local.sh

Na primeira execucao, informe o e-mail, nome de usuario e senha do administrador.
Depois, acesse http://localhost:8080 no navegador.

Os dados ficam na pasta data/. Para mover uma instalacao existente, encerre o SGFL
e copie tambem essa pasta. Nao apague data/ se quiser preservar os cadastros.
'@
Set-Content -LiteralPath (Join-Path $packagePath 'LEIA-ME.txt') -Value $instructions -Encoding UTF8

$zipInputs = @(
    (Join-Path $packagePath 'run-local.ps1'),
    (Join-Path $packagePath 'run-local.sh'),
    (Join-Path $packagePath 'run-local.bat'),
    (Join-Path $packagePath 'LEIA-ME.txt'),
    $packageTargetPath
)
Compress-Archive -Path $zipInputs -DestinationPath $zipPath -Force

Write-Host "Pasta portatil: $packagePath" -ForegroundColor Green
Write-Host "Arquivo ZIP: $zipPath" -ForegroundColor Green
