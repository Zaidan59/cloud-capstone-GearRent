. "$PSScriptRoot\_common.ps1"
$runtime = Join-Path $InfraDir '.runtime'; New-Item -ItemType Directory -Force -Path $runtime | Out-Null
Compose up -d gitea
for($i=0;$i -lt 60;$i++){ try { Invoke-RestMethod 'http://127.0.0.1:3001/api/healthz' -TimeoutSec 2 | Out-Null; break } catch { Start-Sleep 2 } }
$adminUser=$env:GITEA_ADMIN_USER; $adminPass=$env:GITEA_ADMIN_PASSWORD; $studentUser=$env:GITEA_STUDENT_USER; $studentPass=$env:GITEA_STUDENT_PASSWORD
$list = Compose exec -T -u git gitea gitea admin user list 2>$null | Out-String
if($list -notmatch [regex]::Escape($adminUser)){ Compose exec -T -u git gitea gitea admin user create --username $adminUser --password $adminPass --email $env:GITEA_ADMIN_EMAIL --admin --must-change-password=false }
$list = Compose exec -T -u git gitea gitea admin user list 2>$null | Out-String
if($list -notmatch [regex]::Escape($studentUser)){ Compose exec -T -u git gitea gitea admin user create --username $studentUser --password $studentPass --email $env:GITEA_STUDENT_EMAIL --must-change-password=false }
$pair="$adminUser`:$adminPass"; $b64=[Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
$r=Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:3001/api/v1/admin/actions/runners/registration-token' -Headers @{Authorization="Basic $b64"}
Set-Content -NoNewline -Path (Join-Path $runtime 'runner-token') -Value $r.token
Write-Host 'Gitea bootstrap complete.'
