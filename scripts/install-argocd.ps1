. "$PSScriptRoot\_common.ps1"
$ver=$env:ARGOCD_VERSION; if(-not $ver){$ver='3.5.6'}
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argocd --server-side --force-conflicts -f "https://raw.githubusercontent.com/argoproj/argo-cd/v$ver/manifests/install.yaml"
kubectl -n argocd rollout status deployment/argocd-server --timeout=180s
kubectl apply -f (Join-Path $RootDir 'templates\argocd\application.yaml')
kubectl -n argocd get applications.argoproj.io
