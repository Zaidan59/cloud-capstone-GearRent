. "$PSScriptRoot\_common.ps1"
$api='http://127.0.0.1:3001/api/v1'; $user=$env:GITEA_STUDENT_USER; $pass=$env:GITEA_STUDENT_PASSWORD
$auth=[Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$user`:$pass")); $headers=@{Authorization="Basic $auth"; 'Content-Type'='application/json'}
$repos=@('capstone-app','capstone-gitops')
foreach($repo in $repos){
  try { Invoke-RestMethod "$api/repos/$user/$repo" -Headers $headers | Out-Null }
  catch { Invoke-RestMethod "$api/user/repos" -Method Post -Headers $headers -Body (@{name=$repo;private=$false;auto_init=$false}|ConvertTo-Json) | Out-Null }
}
$tmp=Join-Path $RootDir '.bootstrap-work'; New-Item -ItemType Directory -Force -Path $tmp | Out-Null
function Push-Template($source,$repo){
  $dst=Join-Path $tmp $repo; if(Test-Path $dst){Remove-Item -Recurse -Force $dst}; Copy-Item -Recurse $source $dst
  Get-ChildItem -Recurse -File $dst | ForEach-Object { $c=Get-Content $_.FullName -Raw; $c=$c.Replace('__GITEA_USER__',$user).Replace('__APP_REPO__','capstone-app').Replace('__GITOPS_REPO__','capstone-gitops'); Set-Content $_.FullName $c }
  Push-Location $dst; git init -b main; git config user.name 'Capstone Bootstrap'; git config user.email 'bootstrap@example.local'; git add .; git commit -m 'Initial capstone template'; git remote add origin "http://127.0.0.1:3001/$user/$repo.git"; git -c "http.extraHeader=Authorization: Basic $auth" push -u origin main --force; Pop-Location
}
Push-Template (Join-Path $RootDir 'app') 'capstone-app'
Push-Template (Join-Path $RootDir 'gitops') 'capstone-gitops'
Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
Invoke-RestMethod "$api/repos/$user/capstone-app/actions/secrets/GITOPS_PASSWORD" -Method Put -Headers $headers -Body (@{data=$pass;description='Local GitOps repository credential'}|ConvertTo-Json) | Out-Null
Write-Host 'Repositories bootstrapped.'
