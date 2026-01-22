# Game Server 基础设施启动脚本 (PowerShell)
# 用法: .\scripts\start-infra.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Game Server 基础设施启动" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Docker
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] Docker 未安装，请先安装 Docker Desktop" -ForegroundColor Red
    exit 1
}

# 检查 Docker 是否运行
$dockerStatus = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Docker 未运行，请启动 Docker Desktop" -ForegroundColor Red
    exit 1
}

Write-Host "[INFO] 启动基础设施容器..." -ForegroundColor Yellow
Set-Location "$PSScriptRoot\..\docker"

# 启动容器
docker-compose up -d

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[SUCCESS] 基础设施启动成功!" -ForegroundColor Green
    Write-Host ""
    Write-Host "服务地址:" -ForegroundColor Cyan
    Write-Host "  - MongoDB:      localhost:27017" -ForegroundColor White
    Write-Host "  - Redis:        localhost:6379" -ForegroundColor White
    Write-Host "  - Nacos:        http://localhost:8848/nacos" -ForegroundColor White
    Write-Host "  - XXL-Job:      http://localhost:8088/xxl-job-admin" -ForegroundColor White
    Write-Host ""
    Write-Host "默认账号密码:" -ForegroundColor Cyan
    Write-Host "  - MongoDB:      game / game123" -ForegroundColor White
    Write-Host "  - Redis:        无用户名 / game123" -ForegroundColor White
    Write-Host "  - Nacos:        nacos / nacos" -ForegroundColor White
    Write-Host "  - XXL-Job:      admin / 123456" -ForegroundColor White
} else {
    Write-Host "[ERROR] 基础设施启动失败，请检查 Docker 日志" -ForegroundColor Red
    exit 1
}
