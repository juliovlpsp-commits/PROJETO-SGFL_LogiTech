#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ! -s "$1" ]]; then
  echo "Uso: $0 caminho/para/sgfl-backup.dump" >&2
  exit 2
fi
backup_file="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
read -r -p 'A restauração substitui os dados do banco configurado. Digite RESTAURAR para continuar: ' confirmation
if [[ "$confirmation" != 'RESTAURAR' ]]; then
  echo 'Restauração cancelada; nenhum serviço foi alterado.'
  exit 0
fi

backup_dir="$(dirname "$backup_file")"
"$(dirname "$0")/backup-postgres.sh" "$backup_dir"
db_user="$(docker compose exec -T postgres sh -c 'printf %s "$POSTGRES_USER"')"
db_name="$(docker compose exec -T postgres sh -c 'printf %s "$POSTGRES_DB"')"
docker compose stop backend
if docker compose exec -T postgres pg_restore --clean --if-exists --no-owner --no-privileges \
  -U "$db_user" -d "$db_name" < "$backup_file"; then
  docker compose start backend
  echo "Restauração concluída a partir de: $backup_file"
else
  status=$?
  docker compose start backend || true
  echo 'A restauração falhou; o banco pode estar parcialmente restaurado.' >&2
  exit "$status"
fi
