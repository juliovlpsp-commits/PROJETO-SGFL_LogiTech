#!/usr/bin/env bash
set -euo pipefail

default_output_dir="$(cd "$(dirname "$0")/../backups" && pwd)"
output_dir="${1:-$default_output_dir}"
mkdir -p "$output_dir"
db_user="$(docker compose exec -T postgres sh -c 'printf %s "$POSTGRES_USER"')"
db_name="$(docker compose exec -T postgres sh -c 'printf %s "$POSTGRES_DB"')"
stamp="$(date +%Y%m%d-%H%M%S)"
backup_file="$output_dir/sgfl-$stamp.dump"

docker compose exec -T postgres pg_dump -Fc -U "$db_user" -d "$db_name" > "$backup_file"
test -s "$backup_file"
if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$backup_file"
else
  shasum -a 256 "$backup_file"
fi
printf 'Backup criado: %s\n' "$backup_file"
