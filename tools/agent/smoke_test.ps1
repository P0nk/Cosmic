#Requires -Version 5.1
<#
.SYNOPSIS
  Builds and boots Cosmic natively (no Docker), waits for "Cosmic is now online", checks the login + channel
  ports, scans the log for errors/exceptions, then stops the server.

.DESCRIPTION
  Prerequisite (one-time, human setup): MySQL Community Server for Windows installed and running, with an empty
  `cosmic` schema created (`CREATE DATABASE cosmic;`). The server creates its own tables/seed data on first boot.

  Safe for parallel agents in different git worktrees: every worktree talks to the SAME local MySQL schema and
  the SAME fixed ports (8484, 7575-7577), so a named Mutex serialises runs instead of letting them collide.

.EXAMPLE
  powershell tools/agent/smoke_test.ps1

.EXAMPLE
  $env:KEEP_UP = "1"; powershell tools/agent/smoke_test.ps1      # leave the server running for manual poking
#>
$ErrorActionPreference = "Stop"

$repoRoot = (& git rev-parse --show-toplevel).Trim()
Set-Location $repoRoot

$timeoutSec = if ($env:SMOKE_TIMEOUT) { [int]$env:SMOKE_TIMEOUT } else { 420 }
$allowErrors = $env:SMOKE_ALLOW_ERRORS -eq "1"
$keepUp = $env:KEEP_UP -eq "1"

New-Item -ItemType Directory -Force -Path "target" | Out-Null
$outLog = "target\smoke-server.out.log"
$errLog = "target\smoke-server.err.log"
Remove-Item -ErrorAction SilentlyContinue $outLog, $errLog

$mutex = New-Object System.Threading.Mutex($false, "Global\CosmicSmokeTest")
Write-Host "[smoke] waiting for lock ..."
[void]$mutex.WaitOne()
Write-Host "[smoke] lock acquired"

$proc = $null
$fail = $false
try {
    $db = Test-NetConnection -ComputerName "127.0.0.1" -Port 3306 -WarningAction SilentlyContinue
    if (-not $db.TcpTestSucceeded) {
        throw "MySQL not reachable on 127.0.0.1:3306. Install MySQL for Windows, start it, and create the " +
              "'cosmic' schema first (see AGENTS.md)."
    }

    Write-Host "[smoke] building (.\mvnw.cmd -B package) ..."
    & .\mvnw.cmd -q -B package
    if ($LASTEXITCODE -ne 0) { throw "mvnw.cmd package failed (exit $LASTEXITCODE)" }

    $jar = Get-ChildItem "target\Cosmic*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $jar) { throw "no jar found in target\ after build" }

    Write-Host "[smoke] starting server ($($jar.Name)) ..."
    $proc = Start-Process -FilePath "java" `
        -ArgumentList @("-Xmx2048m", "-Dwz-path=wz", "-jar", $jar.FullName) `
        -RedirectStandardOutput $outLog -RedirectStandardError $errLog `
        -WindowStyle Hidden -PassThru

    $start = Get-Date
    $online = $false
    while (((Get-Date) - $start).TotalSeconds -lt $timeoutSec) {
        if ($proc.HasExited) {
            throw "server process exited early (exit code $($proc.ExitCode)) - see $outLog / $errLog"
        }
        if ((Test-Path $outLog) -and (Select-String -Path $outLog -Pattern "Cosmic is now online" -Quiet)) {
            $online = $true
            break
        }
        Start-Sleep -Seconds 3
    }
    if (-not $online) { throw "server not online after ${timeoutSec}s - see $outLog" }
    Write-Host "[smoke] server online after $([int]((Get-Date) - $start).TotalSeconds)s"

    foreach ($port in 8484, 7575, 7576, 7577) {
        $t = Test-NetConnection -ComputerName "127.0.0.1" -Port $port -WarningAction SilentlyContinue
        if ($t.TcpTestSucceeded) {
            Write-Host "[smoke] port $port OK"
        } else {
            Write-Host "[smoke] FAIL: port $port not accepting connections"
            $fail = $true
        }
    }

    $errors = Select-String -Path $outLog, $errLog -Pattern " ERROR |Exception" -ErrorAction SilentlyContinue |
        Select-Object -First 40
    if ($errors) {
        Write-Host "[smoke] errors/exceptions during startup (first 40):"
        $errors | ForEach-Object { Write-Host "  $($_.Line)" }
        if (-not $allowErrors) { $fail = $true }
    }
}
catch {
    Write-Host "[smoke] FAIL: $($_.Exception.Message)"
    $fail = $true
}
finally {
    if ($proc -and -not $proc.HasExited -and -not $keepUp) {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
    $mutex.ReleaseMutex()
}

if ($fail) {
    Write-Host "[smoke] FAIL (full log: $outLog / $errLog)"
    exit 1
}
Write-Host "[smoke] PASS"
exit 0
