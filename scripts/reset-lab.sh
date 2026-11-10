#!/usr/bin/env bash
source "$(dirname "$0")/_common.sh"
echo 'This removes the k3d cluster AND all persistent Docker volumes for Gitea, PostgreSQL, Keycloak, Registry, Prometheus, and Grafana.'
read -r -p 'Type DELETE-CAPSTONE-LAB to continue: ' answer
if [ "$answer" != 'DELETE-CAPSTONE-LAB' ]; then echo 'Cancelled.'; exit 1; fi
k3d cluster delete "${K3D_CLUSTER_NAME:-capstone}" 2>/dev/null || true
"${COMPOSE[@]}" --profile ci --profile observability down -v --remove-orphans
rm -rf "$INFRA_DIR/.runtime" "$ROOT_DIR/.bootstrap-work"
echo 'Local capstone infrastructure reset complete.'
