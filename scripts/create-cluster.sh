#!/usr/bin/env bash
source "$(dirname "$0")/_common.sh"
NAME="${K3D_CLUSTER_NAME:-capstone}"
if k3d cluster list --no-headers 2>/dev/null | awk '{print $1}' | grep -qx "$NAME"; then
  echo "Cluster $NAME already exists."
else
  k3d cluster create "$NAME" \
    --servers 1 --agents 1 \
    --network capstone-net \
    --api-port 6550 \
    -p "${K3D_HTTP_PORT:-8088}:80@loadbalancer" \
    -p "${K3D_HTTPS_PORT:-8443}:443@loadbalancer" \
    --registry-config "$INFRA_DIR/k3d/registries.yaml" \
    --wait
fi
kubectl get nodes -o wide
kubectl get pods -A
