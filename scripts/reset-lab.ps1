. "$PSScriptRoot\_common.ps1"
$ans=Read-Host 'This deletes the cluster and persistent Docker volumes. Type DELETE-CAPSTONE-LAB'
if($ans -ne 'DELETE-CAPSTONE-LAB'){Write-Host 'Cancelled.'; exit 1}
k3d cluster delete $env:K3D_CLUSTER_NAME 2>$null
Compose --profile ci --profile observability down -v --remove-orphans
Remove-Item -Recurse -Force (Join-Path $InfraDir '.runtime') -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force (Join-Path $RootDir '.bootstrap-work') -ErrorAction SilentlyContinue
