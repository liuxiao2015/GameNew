# Game Server 服务启动脚本 (PowerShell)
# 用法: .\scripts\start-services.ps1 [ServiceName]

param(
    [string]$Service = "all"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Game Server 服务启动" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$services = @{
    "gateway" = @{
        "jar" = "gateway\gateway-server\target\gateway-server-1.0.0-SNAPSHOT.jar"
        "port" = 8080
    }
    "game" = @{
        "jar" = "services\service-game\target\service-game-1.0.0-SNAPSHOT.jar"
        "port" = 8081
    }
    "login" = @{
        "jar" = "services\service-login\target\service-login-1.0.0-SNAPSHOT.jar"
        "port" = 8082
    }
    "guild" = @{
        "jar" = "services\service-guild\target\service-guild-1.0.0-SNAPSHOT.jar"
        "port" = 8083
    }
    "chat" = @{
        "jar" = "services\service-chat\target\service-chat-1.0.0-SNAPSHOT.jar"
        "port" = 8084
    }
    "rank" = @{
        "jar" = "services\service-rank\target\service-rank-1.0.0-SNAPSHOT.jar"
        "port" = 8085
    }
    "scheduler" = @{
        "jar" = "services\service-scheduler\target\service-scheduler-1.0.0-SNAPSHOT.jar"
        "port" = 8086
    }
    "gm" = @{
        "jar" = "services\service-gm\target\service-gm-1.0.0-SNAPSHOT.jar"
        "port" = 8087
    }
}

Set-Location "$PSScriptRoot\.."

function Start-Service($name, $config) {
    $jarPath = $config["jar"]
    $port = $config["port"]
    
    if (-not (Test-Path $jarPath)) {
        Write-Host "[WARN] JAR 文件不存在: $jarPath，请先运行构建" -ForegroundColor Yellow
        return
    }
    
    Write-Host "[INFO] 启动 $name (端口: $port)..." -ForegroundColor Yellow
    
    $env:SERVER_PORT = $port
    Start-Process -FilePath "java" -ArgumentList "-jar", $jarPath -WindowStyle Normal
    
    Write-Host "[SUCCESS] $name 启动中..." -ForegroundColor Green
}

if ($Service -eq "all") {
    Write-Host "[INFO] 启动所有服务..." -ForegroundColor Yellow
    Write-Host ""
    
    foreach ($name in @("gateway", "game", "login", "guild", "chat", "rank", "scheduler", "gm")) {
        Start-Service $name $services[$name]
        Start-Sleep -Seconds 2
    }
} else {
    if ($services.ContainsKey($Service)) {
        Start-Service $Service $services[$Service]
    } else {
        Write-Host "[ERROR] 未知服务: $Service" -ForegroundColor Red
        Write-Host "可用服务: $($services.Keys -join ', ')" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""
Write-Host "[INFO] 服务启动完成，请等待服务完全启动后访问" -ForegroundColor Cyan
