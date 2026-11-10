. "$PSScriptRoot\_common.ps1"
param([Parameter(Mandatory=$true)][string]$BackupFile)
$validationDb="$($env:POSTGRES_DB)_restore_validation"
Compose exec -T postgres psql -U $env:POSTGRES_USER -d postgres -c "DROP DATABASE IF EXISTS $validationDb;"
Compose exec -T postgres psql -U $env:POSTGRES_USER -d postgres -c "CREATE DATABASE $validationDb OWNER $($env:POSTGRES_USER);"
$full=(Resolve-Path $BackupFile).Path
$psi=New-Object Diagnostics.ProcessStartInfo
$psi.FileName='docker'; $psi.UseShellExecute=$false; $psi.RedirectStandardInput=$true
@('compose','--env-file',$EnvFile,'-f',(Join-Path $InfraDir 'docker-compose.yml'),'exec','-T','postgres','pg_restore','-U',$env:POSTGRES_USER,'-d',$validationDb,'--no-owner') | ForEach-Object { $psi.ArgumentList.Add($_) }
$p=[Diagnostics.Process]::Start($psi); $fs=[IO.File]::OpenRead($full); $fs.CopyTo($p.StandardInput.BaseStream); $fs.Close(); $p.StandardInput.Close(); $p.WaitForExit(); if($p.ExitCode -ne 0){throw 'pg_restore failed'}
Compose exec -T postgres psql -U $env:POSTGRES_USER -d $validationDb -c '\dt'
Write-Host "Restore validation database: $validationDb"
