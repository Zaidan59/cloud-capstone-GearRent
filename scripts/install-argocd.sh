#!/usr/bin/env bash
source "$(dirname "$0")/_common.sh"
VER="${ARGOCD_VERSION:-3.5.6}"
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argocd --server-side --force-conflicts \
  -f "https://raw.githubusercontent.com/argoproj/argo-cd/v${VER}/manifests/install.yaml"
kubectl -n argocd rollout status deployment/argocd-server --timeout=180s
kubectl apply -f "$ROOT_DIR/templates/argocd/application.yaml"
kubectl -n argocd get applications.argoproj.io

echo
echo 'Argo CD installed. Run argocd-ui.sh in another terminal for the UI.'
