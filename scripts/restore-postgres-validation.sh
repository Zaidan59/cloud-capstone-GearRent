#!/usr/bin/env bash
source "$(dirname "$0")/_common.sh"
FILE="${1:?Usage: restore-postgres-validation.sh backups/file.dump}"
VALIDATION_DB="${POSTGRES_DB}_restore_validation"
"${COMPOSE[@]}" exec -T postgres psql -U "$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1 \
  -c "DROP DATABASE IF EXISTS ${VALIDATION_DB};" \
  -c "CREATE DATABASE ${VALIDATION_DB} OWNER ${POSTGRES_USER};"
cat "$FILE" | "${COMPOSE[@]}" exec -T postgres pg_restore -U "$POSTGRES_USER" -d "$VALIDATION_DB" --no-owner
"${COMPOSE[@]}" exec -T postgres psql -U "$POSTGRES_USER" -d "$VALIDATION_DB" -c '\\dt'
echo "Restore validation database: $VALIDATION_DB"
