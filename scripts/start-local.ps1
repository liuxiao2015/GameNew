# 游戏服务器本地开发环境一键启动脚本 (PowerShell)
# 使用方法: .\scripts\start-local.ps1

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "╔═══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║              游戏服务器本地开发环境一键启动                    ║" -ForegroundColor Cyan
Write-Host "╚═══════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# 切换到项目根目录
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

# 检查 Java
try {
    java -version 2>&1 | Out-Null
} catch {
    Write-Host "[错误] 未找到 Java，请确保已安装 JDK 21 并配置 JAVA_HOME" -ForegroundColor Red
    exit 1
}

# 检查并编译 launcher
$launcherJar = "launcher\target\launcher-1.0.0-SNAPSHOT.jar"
if (-not (Test-Path $launcherJar)) {
    Write-Host "[信息] 正在编译 Launcher..." -ForegroundColor Yellow
    mvn package -DskipTests -q -pl launcher -am
}

Write-Host "[信息] 启动服务器 (本地模式)..." -ForegroundColor Green
Write-Host "[信息] 将自动启动本地 Nacos，请确保 MongoDB 和 Redis 已运行" -ForegroundColor Yellow
Write-Host ""

# 启动
java -jar $launcherJar up --local
