. "$PSScriptRoot\_common.ps1"
Compose --profile observability up -d prometheus grafana
Compose --profile observability ps prometheus grafana
Write-Host 'Prometheus: http://127.0.0.1:9090'; Write-Host 'Grafana: http://127.0.0.1:3000'
