. "$PSScriptRoot\_common.ps1"
$name=$env:K3D_CLUSTER_NAME; if(-not $name){$name='capstone'}
$existing = k3d cluster list --no-headers 2>$null | Select-String ("^"+[regex]::Escape($name)+"\s")
if(-not $existing){
  k3d cluster create $name --servers 1 --agents 1 --network capstone-net --api-port 6550 `
    -p "$($env:K3D_HTTP_PORT):80@loadbalancer" -p "$($env:K3D_HTTPS_PORT):443@loadbalancer" `
    --registry-config (Join-Path $InfraDir 'k3d\registries.yaml') --wait
}
kubectl get nodes -o wide
kubectl get pods -A
