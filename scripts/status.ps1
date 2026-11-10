. "$PSScriptRoot\_common.ps1"
Compose --profile ci --profile observability ps
k3d cluster list
kubectl get nodes 2>$null
kubectl get pods -A 2>$null
kubectl -n argocd get applications.argoproj.io 2>$null
