. "$PSScriptRoot\_common.ps1"
$smoke=Join-Path $RootDir 'smoke-test'
$pushImage="localhost:$($env:REGISTRY_PORT)/capstone/smoke:v1"
$pullImage='registry:5000/capstone/smoke:v1'
docker build -t $pushImage $smoke
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
docker push $pushImage
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
$yaml=(Get-Content (Join-Path $smoke 'k8s.yaml') -Raw).Replace('__SMOKE_IMAGE__',$pullImage)
$yaml | kubectl apply -f -
kubectl -n capstone-smoke rollout status deployment/capstone-smoke --timeout=90s
for($i=0;$i -lt 30;$i++){
  try { $r=Invoke-WebRequest "http://smoke.localhost:$($env:K3D_HTTP_PORT)/" -TimeoutSec 2; Write-Host $r.Content; break }
  catch { Start-Sleep 2 }
}
