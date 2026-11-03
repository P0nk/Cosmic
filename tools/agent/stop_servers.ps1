#Requires -Version 5.1
<#
.SYNOPSIS
  Stops leftover Cosmic servers: kills only java processes whose command line contains "Cosmic.jar" (plus their children).
  Never touches other java programs (IDEs, Maven, other tools).

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools/agent/stop_servers.ps1
#>
function Get-CosmicJava {
    @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
        Where-Object { $_.CommandLine -and $_.CommandLine -match 'Cosmic\.jar' })
}
$procs = Get-CosmicJava
if ($procs.Count -eq 0) { Write-Host "[stop] no Cosmic.jar java processes running."; exit 0 }
foreach ($p in $procs) {
    Write-Host "[stop] killing PID $($p.ProcessId): $($p.CommandLine)"
    & taskkill /PID $p.ProcessId /T /F 2>&1 | Out-Null   # /T = whole tree (launcher shim + real java)
}
Start-Sleep -Seconds 1
$left = Get-CosmicJava
if ($left.Count -gt 0) { Write-Host "[stop] WARN: $($left.Count) still running"; exit 1 }
Write-Host "[stop] done."
exit 0
