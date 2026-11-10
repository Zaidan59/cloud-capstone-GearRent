. "$PSScriptRoot\_common.ps1"
$token=Join-Path $InfraDir '.runtime\runner-token'
if(-not (Test-Path $token)){ throw 'Runner token missing. Run bootstrap-gitea.ps1 first.' }
Compose --profile ci build gitea-runner
Compose --profile ci up -d gitea-runner
Compose --profile ci ps gitea-runner
