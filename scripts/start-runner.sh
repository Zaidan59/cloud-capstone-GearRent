#!/usr/bin/env bash
source "$(dirname "$0")/_common.sh"
TOKEN_FILE="$INFRA_DIR/.runtime/runner-token"
if [ ! -s "$TOKEN_FILE" ]; then
  echo 'Runner token missing. Run bootstrap-gitea.sh first.' >&2; exit 1
fi
"${COMPOSE[@]}" --profile ci build gitea-runner
"${COMPOSE[@]}" --profile ci up -d gitea-runner
"${COMPOSE[@]}" --profile ci ps gitea-runner
