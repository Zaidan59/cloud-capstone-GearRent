. "$PSScriptRoot\_common.ps1"
Push-Location $RootDir
Write-Host "== Deploying app/ to local Gitea lab =="
$giteaUrl = "http://student:student@127.0.0.1:3001/student/capstone-app.git"
git subtree split --prefix app -b gitea-deploy-tmp
git push -f $giteaUrl gitea-deploy-tmp:main
git branch -D gitea-deploy-tmp
Pop-Location
Write-Host "== Berhasil ter-push ke Gitea! =="
Write-Host "Cek status CI di: http://127.0.0.1:3001/student/capstone-app/actions"
Write-Host "Cek status Argo CD di: https://127.0.0.1:8082"
