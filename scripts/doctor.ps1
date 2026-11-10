$ErrorActionPreference = 'Continue'
Write-Host '== Cloud Capstone Infrastructure Doctor =='
$failed = $false
function Check-Tool([string]$Name, [scriptblock]$Cmd) {
  Write-Host -NoNewline ("{0,-18}" -f $Name)
  try { $o = & $Cmd 2>&1; if ($LASTEXITCODE -eq 0 -or $null -eq $LASTEXITCODE) { Write-Host 'OK'; if($o){Write-Host ('  '+($o|Select-Object -First 1))} } else { Write-Host 'MISSING/FAILED'; $script:failed=$true } }
  catch { Write-Host 'MISSING/FAILED'; $script:failed=$true }
}
Check-Tool 'Docker' { docker version }
Check-Tool 'Compose' { docker compose version }
Check-Tool 'Git' { git --version }
Check-Tool 'curl' { curl.exe --version }
Check-Tool 'kubectl' { kubectl version --client }
Check-Tool 'k3d' { k3d version }
try { docker info *> $null; if($LASTEXITCODE -eq 0){Write-Host 'Docker engine: RUNNING'}else{Write-Host 'Docker engine: NOT RUNNING';$failed=$true} } catch {$failed=$true}
if($failed){Write-Host 'Doctor result: ACTION REQUIRED'; exit 1}else{Write-Host 'Doctor result: READY'}
