#!/usr/bin/env bash
source "$(dirname "$0")/_common.sh"
OUT="$ROOT_DIR/backups"
mkdir -p "$OUT"
TS=$(date +%Y%m%d-%H%M%S)
FILE="$OUT/${POSTGRES_DB}-${TS}.dump"
"${COMPOSE[@]}" exec -T postgres pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc > "$FILE"
echo "Created $FILE"
