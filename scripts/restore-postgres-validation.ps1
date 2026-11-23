param([Parameter(Mandatory=$true)][string]$BackupFile)
. "$PSScriptRoot\_common.ps1"

$validationDb = "$($env:POSTGRES_DB)_restore_validation"
$composeFile = Join-Path $InfraDir 'docker-compose.yml'
$full = (Resolve-Path $BackupFile).Path

Compose exec -T postgres psql -U $env:POSTGRES_USER -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS $validationDb;" -c "CREATE DATABASE $validationDb OWNER $($env:POSTGRES_USER);"
if ($LASTEXITCODE -ne 0) { throw 'Failed to recreate validation database' }

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = 'docker'
$psi.Arguments = "compose --env-file `"$EnvFile`" -f `"$composeFile`" exec -T postgres pg_restore -U `"$($env:POSTGRES_USER)`" -d `"$validationDb`" --no-owner"
$psi.UseShellExecute = $false
$psi.RedirectStandardInput = $true
$psi.RedirectStandardError = $true

$p = [Diagnostics.Process]::Start($psi)
$fs = [IO.File]::OpenRead($full)
try {
  $fs.CopyTo($p.StandardInput.BaseStream)
} finally {
  $fs.Close()
  $p.StandardInput.Close()
}
$stderr = $p.StandardError.ReadToEnd()
$p.WaitForExit()
if ($p.ExitCode -ne 0) { throw "pg_restore failed: $stderr" }

Compose exec -T postgres psql -U $env:POSTGRES_USER -d $validationDb -c '\dt'
Write-Host "Restore validation database: $validationDb"
