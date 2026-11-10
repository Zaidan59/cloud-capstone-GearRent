#!/usr/bin/env bash
source "$(dirname "$0")/_common.sh"
SMOKE="$ROOT_DIR/smoke-test"
IMAGE_PUSH="localhost:${REGISTRY_PORT:-5001}/capstone/smoke:v1"
IMAGE_PULL="registry:5000/capstone/smoke:v1"
docker build -t "$IMAGE_PUSH" "$SMOKE"
docker push "$IMAGE_PUSH"
sed "s#__SMOKE_IMAGE__#$IMAGE_PULL#g" "$SMOKE/k8s.yaml" | kubectl apply -f -
kubectl -n capstone-smoke rollout status deployment/capstone-smoke --timeout=90s
for _ in $(seq 1 30); do
  if curl -fsS http://smoke.localhost:${K3D_HTTP_PORT:-8088}/ >/dev/null 2>&1; then break; fi
  sleep 2
done
curl -fsS http://smoke.localhost:${K3D_HTTP_PORT:-8088}/
echo
