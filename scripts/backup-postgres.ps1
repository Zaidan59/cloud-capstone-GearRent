. "$PSScriptRoot\_common.ps1"
$out = Join-Path $RootDir 'backups'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$ts = Get-Date -Format 'yyyyMMdd-HHmmss'
$file = Join-Path $out "$($env:POSTGRES_DB)-$ts.dump"
$composeFile = Join-Path $InfraDir 'docker-compose.yml'

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = 'docker'
$psi.Arguments = "compose --env-file `"$EnvFile`" -f `"$composeFile`" exec -T postgres pg_dump -U `"$($env:POSTGRES_USER)`" -d `"$($env:POSTGRES_DB)`" -Fc"
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.UseShellExecute = $false

$p = [Diagnostics.Process]::Start($psi)
$fs = [IO.File]::Create($file)
try {
  $p.StandardOutput.BaseStream.CopyTo($fs)
} finally {
  $fs.Close()
}
$stderr = $p.StandardError.ReadToEnd()
$p.WaitForExit()
if ($p.ExitCode -ne 0 -or (Get-Item $file).Length -eq 0) {
  Remove-Item $file -Force -ErrorAction SilentlyContinue
  throw "pg_dump failed: $stderr"
}
Write-Host "Created $file ($((Get-Item $file).Length) bytes)"
