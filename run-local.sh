#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_PATH="$PROJECT_ROOT/sgfl-frontend"
JAR_PATH="$PROJECT_ROOT/target/sgfl-0.0.1-SNAPSHOT.jar"
DATA_PATH="$PROJECT_ROOT/data/sgfl.mv.db"
NO_BUILD="${1:-}"
JAVA_EXECUTABLE="$(command -v java || true)"

if [[ -z "$JAVA_EXECUTABLE" && -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    JAVA_EXECUTABLE="$JAVA_HOME/bin/java"
fi
[[ -n "$JAVA_EXECUTABLE" ]] || { echo 'Instale Java 17 ou superior e configure JAVA_HOME ou PATH.' >&2; exit 1; }
JAVA_VERSION_OUTPUT="$("$JAVA_EXECUTABLE" -version 2>&1)" || { echo 'Nao foi possivel iniciar o Java.' >&2; exit 1; }
JAVA_MAJOR="$(printf '%s\n' "$JAVA_VERSION_OUTPUT" | sed -nE 's/.*version "([0-9]+).*/\1/p' | head -n 1)"
[[ -n "$JAVA_MAJOR" ]] || { echo 'Nao foi possivel identificar a versao do Java.' >&2; exit 1; }
(( JAVA_MAJOR >= 17 )) || { echo 'Este aplicativo requer Java 17 ou superior.' >&2; exit 1; }

cd "$PROJECT_ROOT"

if [[ "$NO_BUILD" != "--no-build" && -d "$FRONTEND_PATH" && -f "$PROJECT_ROOT/mvnw" ]]; then
    [[ -n "$JAVA_EXECUTABLE" ]] || { echo 'Instale Java 17 ou superior e configure JAVA_HOME ou PATH.' >&2; exit 1; }
    command -v node >/dev/null || { echo 'Instale Node.js para compilar o frontend.' >&2; exit 1; }
    command -v npm >/dev/null || { echo 'Instale npm para compilar o frontend.' >&2; exit 1; }

    if [[ ! -d "$FRONTEND_PATH/node_modules" ]]; then
        (cd "$FRONTEND_PATH" && npm ci)
    fi

    (cd "$FRONTEND_PATH" && VITE_API_URL=/api npm run build)
    sh ./mvnw clean package -Pportable -DskipTests
elif [[ "$NO_BUILD" != "--no-build" ]]; then
    echo 'Codigo-fonte nao encontrado; iniciando o JAR ja empacotado.'
fi

if [[ ! -f "$JAR_PATH" ]]; then
    echo 'O JAR nao existe. Rode novamente sem --no-build para compilar o projeto.' >&2
    exit 1
fi

mkdir -p "$PROJECT_ROOT/data"
export SPRING_PROFILES_ACTIVE=local
export DB_URL='jdbc:h2:file:./data/sgfl;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE'
export DB_USERNAME=sa
export DB_PASSWORD=''
export DB_DIALECT=org.hibernate.dialect.H2Dialect
export FLYWAY_ENABLED=false
export JWT_SECRET="$(head -c 48 /dev/urandom | base64 | tr -d '\n')"
export BOOTSTRAP_ADMIN_ENABLED=false
export BOOTSTRAP_ADMIN_EMAIL=''
export BOOTSTRAP_ADMIN_USERNAME=''
export BOOTSTRAP_ADMIN_PASSWORD=''

if [[ ! -f "$DATA_PATH" ]]; then
    echo 'Primeira execucao: crie o administrador inicial.'
    read -r -p 'E-mail do administrador: ' BOOTSTRAP_ADMIN_EMAIL
    [[ -n "$BOOTSTRAP_ADMIN_EMAIL" ]] || { echo 'O e-mail e obrigatorio.' >&2; exit 1; }
    read -r -p "Nome de usuario [$BOOTSTRAP_ADMIN_EMAIL]: " BOOTSTRAP_ADMIN_USERNAME
    BOOTSTRAP_ADMIN_USERNAME="${BOOTSTRAP_ADMIN_USERNAME:-$BOOTSTRAP_ADMIN_EMAIL}"

    while true; do
        read -r -s -p 'Senha do administrador (minimo 8 caracteres): ' BOOTSTRAP_ADMIN_PASSWORD
        echo
        if [[ ${#BOOTSTRAP_ADMIN_PASSWORD} -ge 8 ]]; then break; fi
        echo 'A senha precisa ter pelo menos 8 caracteres.'
    done

    export BOOTSTRAP_ADMIN_ENABLED=true
    export BOOTSTRAP_ADMIN_EMAIL BOOTSTRAP_ADMIN_USERNAME BOOTSTRAP_ADMIN_PASSWORD
fi

echo 'SGFL disponivel em http://localhost:8080. Pressione Ctrl+C para encerrar.'
[[ -n "$JAVA_EXECUTABLE" ]] || { echo 'Java 17 ou superior nao encontrado. Configure JAVA_HOME ou PATH.' >&2; exit 1; }
exec "$JAVA_EXECUTABLE" -jar "$JAR_PATH"
