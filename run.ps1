# =============================================================
# MerogieMS Auto-Restart Launcher
# =============================================================
# HOW TO USE:
#   1. Build the server first (run: .\mvnw.cmd package -DskipTests)
#   2. Open IntelliJ's Terminal tab (bottom of the IDE)
#   3. Run this script:  .\run.ps1
#   4. To stop the server permanently: press Ctrl+C in this terminal
# =============================================================

$JAR = "target\Cosmic.jar"
$JVM_ARGS = @(
    "-Dpolyglot.engine.WarnInterpreterOnly=false",
    "-Xms512m",
    "-Xmx4g"
)

while ($true) {
    if (-not (Test-Path $JAR)) {
        Write-Host "[Launcher] ERROR: $JAR not found. Build the project first:" -ForegroundColor Red
        Write-Host "           .\mvnw.cmd package -DskipTests" -ForegroundColor Yellow
        exit 1
    }

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host ""
    Write-Host "===============================================" -ForegroundColor Cyan
    Write-Host "  MerogieMS Starting — $timestamp" -ForegroundColor Cyan
    Write-Host "  Press Ctrl+C to fully stop the server." -ForegroundColor DarkGray
    Write-Host "===============================================" -ForegroundColor Cyan
    Write-Host ""

    # Run the server — this blocks until the server stops
    & java @JVM_ARGS -jar $JAR

    $exitCode = $LASTEXITCODE
    $stopTime = Get-Date -Format "HH:mm:ss"

    Write-Host ""
    Write-Host "-----------------------------------------------" -ForegroundColor Yellow
    Write-Host "  Server stopped at $stopTime (exit code: $exitCode)" -ForegroundColor Yellow
    Write-Host "  Restarting in 20 seconds... (Ctrl+C to cancel)" -ForegroundColor Yellow
    Write-Host "-----------------------------------------------" -ForegroundColor Yellow

    # Count down so it's visible that it's waiting
    for ($i = 20; $i -gt 0; $i--) {
        Write-Host "`r  Restarting in $i seconds... " -NoNewline -ForegroundColor DarkYellow
        Start-Sleep -Seconds 1
    }

    Write-Host ""
}
