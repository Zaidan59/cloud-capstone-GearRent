#!/usr/bin/env bash
set -euo pipefail
echo 'Argo CD UI: https://127.0.0.1:8082'
echo 'Press Ctrl+C to stop port forwarding.'
kubectl -n argocd port-forward svc/argocd-server 8082:443
