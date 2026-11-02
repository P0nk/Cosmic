#Requires -Version 5.1
<#
.SYNOPSIS
  Integration check for the headless test client: boots the server via smoke_test.ps1 (KEEP_UP=1), runs a test-client
  scenario against it, asserts on the server log, then stops the server.

.DESCRIPTION
  Default scenario: login-enter-map (admin/admin -> character list -> select -> enter map -> log out).
  The server is started with USE_DEBUG_SHOW_RCVD_PACKET=true (via a generated COSMIC_LOCAL_CONFIG overlay in target/,
  which also carries over your existing config.local.yaml) so its log shows PLAYER_LOGGEDIN (opcode 20) being handled.

.PARAMETER Scenario
  Scenario name (see: java -cp target/Cosmic.jar;target/test-classes testclient.TestClientMain --list).

.PARAMETER NoServer
  Do not start/stop a server; run the scenario against whatever is already listening on 8484 (no log assertions).

.PARAMETER KeepServer
  Leave the server running afterwards (stop it yourself). Account/Password: login to use (default admin/admin; an
  unknown name auto-registers, which exercises the "account has no character" path).

.EXAMPLE
  powershell tools/agent/run_testclient.ps1
.EXAMPLE
  powershell tools/agent/run_testclient.ps1 -NoServer
#>
param(
    [string]$Scenario = "login-enter-map",
    [switch]$NoServer,
    [switch]$KeepServer,
    [string]$Account = "admin",
    [string]$Password = "admin"
)
$ErrorActionPreference = "Stop"

$repoRoot = (& git rev-parse --show-toplevel).Trim()
Set-Location $repoRoot

if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem "C:\Program Files\Java\jdk-*" -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "java" }
if ($env:JAVA_HOME) { $env:PATH = (Join-Path $env:JAVA_HOME "bin") + ";" + $env:PATH }  # smoke_test.ps1 starts plain "java"

$outLog = "target\smoke-server.out.log"
$serverPid = $null
$fail = $false

try {
    if (-not $NoServer) {
        # Overlay config: the user's local override (DB password etc.) plus the debug flag that logs received opcodes.
        New-Item -ItemType Directory -Force -Path "target" | Out-Null
        $localCandidates = @($env:COSMIC_LOCAL_CONFIG, (Join-Path $repoRoot "config.local.yaml"))
        $common = (& git rev-parse --path-format=absolute --git-common-dir).Trim()
        $localCandidates += (Join-Path (Split-Path $common -Parent) "config.local.yaml")
        $existing = $localCandidates | Where-Object { $_ -and (Test-Path $_ -PathType Leaf) } | Select-Object -First 1
        $lines = @()
        if ($existing) { $lines = @(Get-Content $existing) }
        $idx = [array]::IndexOf(@($lines | ForEach-Object { $_.TrimEnd() }), "server:")
        $flag = "  USE_DEBUG_SHOW_RCVD_PACKET: true"
        if ($idx -ge 0) {
            $lines = @($lines[0..$idx]) + $flag + @($lines[($idx + 1)..($lines.Count - 1)])
        } else {
            $lines = $lines + "server:" + $flag
        }
        $overlay = Join-Path $repoRoot "target\testclient-config.yaml"
        [System.IO.File]::WriteAllLines($overlay, [string[]]$lines, (New-Object System.Text.UTF8Encoding($false)))
        $env:SMOKE_SERVER_LOCAL_CONFIG = $overlay

        $env:KEEP_UP = "1"
        Write-Host "[testclient] starting server via smoke_test.ps1 (KEEP_UP=1) ..."
        & powershell -NoProfile -ExecutionPolicy Bypass -File "tools\agent\smoke_test.ps1"
        if ($LASTEXITCODE -ne 0) { throw "smoke_test.ps1 failed (exit $LASTEXITCODE)" }
        $conn = Get-NetTCPConnection -LocalPort 8484 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($conn) { $serverPid = $conn.OwningProcess }
    }

    Write-Host "[testclient] running scenario '$Scenario' ..."
    $clientOut = & $java -cp "target\Cosmic.jar;target\test-classes" testclient.TestClientMain $Scenario --user $Account --pass $Password 2>&1
    $clientExit = $LASTEXITCODE
    $clientOut | ForEach-Object { Write-Host $_ }
    if ($clientExit -ne 0) { throw "scenario '$Scenario' failed (exit $clientExit)" }

    if (-not $NoServer -and $Scenario -eq "login-enter-map") {
        # Give the server a moment to finish logging the disconnect; poll rather than sleep a fixed time.
        $deadline = (Get-Date).AddSeconds(10)
        do {
            $enter = Select-String -Path $outLog -Pattern "Received packet id 20$" -ErrorAction SilentlyContinue
            if ($enter) { break }
            Start-Sleep -Milliseconds 250
        } while ((Get-Date) -lt $deadline)
        if (-not $enter) { throw "server log has no 'Received packet id 20' (PLAYER_LOGGEDIN) line in $outLog" }
        Write-Host "[testclient] server log OK: $($enter[0].Line.Trim())"
        $deadline = (Get-Date).AddSeconds(10)
        do {
            $saved = Select-String -Path $outLog -Pattern "Attempting to save chr" -ErrorAction SilentlyContinue
            if ($saved) { break }
            Start-Sleep -Milliseconds 250
        } while ((Get-Date) -lt $deadline)
        if (-not $saved) { throw "server never saved the character after we closed the socket (unclean logout?)" }
        Write-Host "[testclient] server log OK (clean logout): $($saved[0].Line.Trim())"
        $bad = Select-String -Path $outLog, "target\smoke-server.err.log" -Pattern "Error in packet handler" -ErrorAction SilentlyContinue
        if ($bad) { throw "server logged a packet-handler error: $($bad[0].Line.Trim())" }
        Write-Host "[testclient] server log OK: no 'Error in packet handler' lines"
    }
}
catch {
    Write-Host "[testclient] FAIL: $($_.Exception.Message)"
    $fail = $true
}
finally {
    if ($serverPid -and -not $KeepServer) { Stop-Process -Id $serverPid -Force -ErrorAction SilentlyContinue }
}

if ($fail) { exit 1 }
Write-Host "[testclient] PASS"
exit 0
