#!/usr/bin/env bash
source "$(dirname "$0")/_common.sh"
mkdir -p "$INFRA_DIR/.runtime"
"${COMPOSE[@]}" up -d gitea postgres keycloak registry
"${COMPOSE[@]}" ps

echo
printf '%s\n' \
  'Gitea:    http://127.0.0.1:3001' \
  'Keycloak: http://127.0.0.1:8081' \
  'Registry: http://127.0.0.1:5001/v2/' \
  'Postgres: 127.0.0.1:5432'
