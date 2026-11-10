#!/usr/bin/env bash
source "$(dirname "$0")/_common.sh"
RUNTIME="$INFRA_DIR/.runtime"
mkdir -p "$RUNTIME"

"${COMPOSE[@]}" up -d gitea

echo 'Waiting for Gitea...'
for _ in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:3001/api/healthz >/dev/null 2>&1; then break; fi
  sleep 2
done
curl -fsS http://127.0.0.1:3001/api/healthz >/dev/null

if ! "${COMPOSE[@]}" exec -T -u git gitea gitea admin user list 2>/dev/null | grep -q "$GITEA_ADMIN_USER"; then
  "${COMPOSE[@]}" exec -T -u git gitea gitea admin user create \
    --username "$GITEA_ADMIN_USER" --password "$GITEA_ADMIN_PASSWORD" \
    --email "$GITEA_ADMIN_EMAIL" --admin --must-change-password=false
fi

if ! "${COMPOSE[@]}" exec -T -u git gitea gitea admin user list 2>/dev/null | grep -q "$GITEA_STUDENT_USER"; then
  "${COMPOSE[@]}" exec -T -u git gitea gitea admin user create \
    --username "$GITEA_STUDENT_USER" --password "$GITEA_STUDENT_PASSWORD" \
    --email "$GITEA_STUDENT_EMAIL" --must-change-password=false
fi

TOKEN=$(curl -fsS -u "$GITEA_ADMIN_USER:$GITEA_ADMIN_PASSWORD" \
  -X POST http://127.0.0.1:3001/api/v1/admin/actions/runners/registration-token | jq -r '.token')
if [ -z "$TOKEN" ] || [ "$TOKEN" = null ]; then
  echo 'Could not obtain runner registration token.' >&2; exit 1
fi
printf '%s' "$TOKEN" > "$RUNTIME/runner-token"
chmod 600 "$RUNTIME/runner-token"

echo 'Gitea bootstrap complete.'
echo "Admin user:   $GITEA_ADMIN_USER"
echo "Student user: $GITEA_STUDENT_USER"
echo 'Runner token stored locally in infrastructure/.runtime/runner-token (do not submit it).'
