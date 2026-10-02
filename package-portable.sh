#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_PATH="$PROJECT_ROOT/sgfl-frontend"
JAR_PATH="$PROJECT_ROOT/target/sgfl-0.0.1-SNAPSHOT.jar"
PACKAGE_PATH="$PROJECT_ROOT/portable-runtime"
PACKAGE_TARGET_PATH="$PACKAGE_PATH/target"
ZIP_PATH="$PROJECT_ROOT/sgfl-portatil.zip"
SKIP_BUILD="${1:-}"

if [[ "$SKIP_BUILD" != "--skip-build" ]]; then
    JAVA_EXECUTABLE="$(command -v java || true)"
    if [[ -z "$JAVA_EXECUTABLE" && -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
        JAVA_EXECUTABLE="$JAVA_HOME/bin/java"
    fi
    [[ -n "$JAVA_EXECUTABLE" ]] || { echo 'Instale Java 17 ou superior e configure JAVA_HOME ou PATH.' >&2; exit 1; }
    command -v node >/dev/null || { echo 'Instale Node.js 20.19+ (ou 22.12+).' >&2; exit 1; }
    command -v npm >/dev/null || { echo 'Instale npm.' >&2; exit 1; }

    if [[ ! -d "$FRONTEND_PATH/node_modules" ]]; then
        (cd "$FRONTEND_PATH" && npm ci)
    fi

    (cd "$FRONTEND_PATH" && VITE_API_URL=/api npm run build)
    (cd "$PROJECT_ROOT" && sh ./mvnw clean package -Pportable -DskipTests)
fi

[[ -f "$JAR_PATH" ]] || { echo 'JAR nao encontrado. Gere-o primeiro ou execute sem --skip-build.' >&2; exit 1; }

mkdir -p "$PACKAGE_TARGET_PATH"
cp "$JAR_PATH" "$PACKAGE_TARGET_PATH/sgfl-0.0.1-SNAPSHOT.jar"
cp "$PROJECT_ROOT/run-local.ps1" "$PACKAGE_PATH/run-local.ps1"
cp "$PROJECT_ROOT/run-local.sh" "$PACKAGE_PATH/run-local.sh"
cp "$PROJECT_ROOT/run-local.bat" "$PACKAGE_PATH/run-local.bat"

cat > "$PACKAGE_PATH/LEIA-ME.txt" <<'EOF'
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
EOF

if command -v zip >/dev/null; then
    if [[ -e "$ZIP_PATH" ]]; then
        rm -- "$ZIP_PATH"
    fi
    (cd "$PROJECT_ROOT" && zip -qr "$ZIP_PATH" \
        portable-runtime/run-local.ps1 \
        portable-runtime/run-local.sh \
        portable-runtime/run-local.bat \
        portable-runtime/LEIA-ME.txt \
        portable-runtime/target/sgfl-0.0.1-SNAPSHOT.jar)
    echo "Arquivo ZIP: $ZIP_PATH"
else
    echo 'Utilitario zip nao encontrado; use a pasta portable-runtime/ diretamente.'
fi

echo "Pasta portatil: $PACKAGE_PATH"
