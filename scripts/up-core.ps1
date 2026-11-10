. "$PSScriptRoot\_common.ps1"
New-Item -ItemType Directory -Force -Path (Join-Path $InfraDir '.runtime') | Out-Null
Compose up -d gitea postgres keycloak registry
Compose ps
Write-Host 'Gitea: http://127.0.0.1:3001'
Write-Host 'Keycloak: http://127.0.0.1:8081'
Write-Host 'Registry: http://127.0.0.1:5001/v2/'
