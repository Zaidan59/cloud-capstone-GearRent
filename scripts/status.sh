#!/usr/bin/env bash
source "$(dirname "$0")/_common.sh"
echo '== Docker Compose =='
"${COMPOSE[@]}" --profile ci --profile observability ps || true
echo; echo '== k3d =='; k3d cluster list || true
echo; echo '== Kubernetes =='; kubectl get nodes 2>/dev/null || true; kubectl get pods -A 2>/dev/null || true
echo; echo '== Argo CD =='; kubectl -n argocd get applications.argoproj.io 2>/dev/null || true
