Write-Host 'Argo CD UI: https://127.0.0.1:8082 - press Ctrl+C to stop.'
kubectl -n argocd port-forward svc/argocd-server 8082:443
