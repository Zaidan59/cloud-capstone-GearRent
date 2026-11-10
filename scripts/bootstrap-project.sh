#!/usr/bin/env bash
source "$(dirname "$0")/_common.sh"
ROOT="$ROOT_DIR"
TMP="$ROOT/.bootstrap-work"
APP_REPO=capstone-app
GITOPS_REPO=capstone-gitops
API=http://127.0.0.1:3001/api/v1
AUTH="$GITEA_STUDENT_USER:$GITEA_STUDENT_PASSWORD"

mkdir -p "$TMP"
rm -rf "$TMP/$APP_REPO" "$TMP/$GITOPS_REPO"

create_repo() {
  local repo="$1"
  if curl -fsS -u "$AUTH" "$API/repos/$GITEA_STUDENT_USER/$repo" >/dev/null 2>&1; then
    echo "Repository $repo already exists."
  else
    curl -fsS -u "$AUTH" -H 'Content-Type: application/json' \
      -d "{\"name\":\"$repo\",\"private\":false,\"auto_init\":false}" \
      "$API/user/repos" >/dev/null
    echo "Created $repo."
  fi
}
create_repo "$APP_REPO"
create_repo "$GITOPS_REPO"

copy_template() {
  local src="$1" dst="$2" repo="$3"
  cp -R "$src" "$dst"
  find "$dst" -type f -print0 | xargs -0 sed -i \
    -e "s/__GITEA_USER__/$GITEA_STUDENT_USER/g" \
    -e "s/__APP_REPO__/$APP_REPO/g" \
    -e "s/__GITOPS_REPO__/$GITOPS_REPO/g"
  (cd "$dst" && git init -b main && git config user.name 'Capstone Bootstrap' && git config user.email 'bootstrap@example.local' && git add . && git commit -m 'Initial capstone template')
  local b64
  b64=$(printf '%s:%s' "$GITEA_STUDENT_USER" "$GITEA_STUDENT_PASSWORD" | base64 | tr -d '\n')
  (cd "$dst" && git remote add origin "http://127.0.0.1:3001/$GITEA_STUDENT_USER/$repo.git" && git -c http.extraHeader="Authorization: Basic $b64" push -u origin main --force)
}

copy_template "$ROOT/app" "$TMP/$APP_REPO" "$APP_REPO"
copy_template "$ROOT/gitops" "$TMP/$GITOPS_REPO" "$GITOPS_REPO"
rm -rf "$TMP"

# Store GitOps credential as Actions secret. It remains local to the Gitea instance.
curl -fsS -u "$AUTH" -X PUT -H 'Content-Type: application/json' \
  -d "{\"data\":\"$GITEA_STUDENT_PASSWORD\",\"description\":\"Local GitOps repository credential for the classroom CI workflow\"}" \
  "$API/repos/$GITEA_STUDENT_USER/$APP_REPO/actions/secrets/GITOPS_PASSWORD" >/dev/null

echo
echo 'Project repositories are ready:'
echo "  http://127.0.0.1:3001/$GITEA_STUDENT_USER/$APP_REPO"
echo "  http://127.0.0.1:3001/$GITEA_STUDENT_USER/$GITOPS_REPO"
