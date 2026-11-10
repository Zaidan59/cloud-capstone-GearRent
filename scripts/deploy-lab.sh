#!/usr/bin/env bash
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

echo "== Deploying app/ to local Gitea lab =="
GITEA_URL="http://student:student@127.0.0.1:3001/student/capstone-app.git"

git subtree split --prefix app -b gitea-deploy-tmp
git push -f "$GITEA_URL" gitea-deploy-tmp:main
git branch -D gitea-deploy-tmp

echo
echo "== Berhasil ter-push ke Gitea! =="
echo "Cek status CI di: http://127.0.0.1:3001/student/capstone-app/actions"
echo "Cek status Argo CD di: https://127.0.0.1:8082"
