#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd -P)"
default_output_dir="$script_dir/../backups"
output_dir="${1:-$default_output_dir}"
external_dir="${2:-}"
mkdir -p "$output_dir"
output_dir="$(cd "$output_dir" && pwd -P)"
db_user="$(docker compose exec -T postgres sh -c 'printf %s "$POSTGRES_USER"')"
db_name="$(docker compose exec -T postgres sh -c 'printf %s "$POSTGRES_DB"')"
stamp="$(date +%Y%m%d-%H%M%S)"
backup_file="$output_dir/sgfl-$stamp.dump"

docker compose exec -T postgres pg_dump -Fc -U "$db_user" -d "$db_name" > "$backup_file"
test -s "$backup_file"
if command -v sha256sum >/dev/null 2>&1; then
  local_hash="$(sha256sum "$backup_file" | cut -d ' ' -f 1)"
else
  local_hash="$(shasum -a 256 "$backup_file" | cut -d ' ' -f 1)"
fi
printf 'SHA-256: %s\n' "$local_hash"
printf 'Backup criado: %s\n' "$backup_file"

if [[ -n "$external_dir" ]]; then
  mkdir -p "$external_dir"
  external_real="$(cd "$external_dir" && pwd -P)"
  if [[ "$output_dir" == "$external_real" ]]; then
    echo 'O destino externo precisa ser diferente da pasta local de backups.' >&2
    exit 1
  fi
  external_file="$external_real/$(basename "$backup_file")"
  cp -- "$backup_file" "$external_file"
  if command -v sha256sum >/dev/null 2>&1; then
    external_hash="$(sha256sum "$external_file" | cut -d ' ' -f 1)"
  else
    external_hash="$(shasum -a 256 "$external_file" | cut -d ' ' -f 1)"
  fi
  if [[ "$external_hash" != "$local_hash" ]]; then
    rm -f -- "$external_file"
    echo 'A cópia externa não passou na verificação SHA-256.' >&2
    exit 1
  fi
  printf 'Cópia externa verificada: %s\n' "$external_file"
fi
