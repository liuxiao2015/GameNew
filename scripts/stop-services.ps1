# PowerShell script to stop all running services

Write-Host "Stopping all game services..."

# Find all Java processes
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue

if ($null -eq $javaProcesses) {
    Write-Host "No running Java services found."
} else {
    foreach ($process in $javaProcesses) {
        Write-Host "Stopping process: $($process.Id) - $($process.ProcessName)"
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    }
    Write-Host "All services stopped."
}
