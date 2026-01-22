# Game Server 构建脚本 (PowerShell)
# 用法: .\scripts\build.ps1 [-SkipTests]

param(
    [switch]$SkipTests
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Game Server 构建" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Set-Location "$PSScriptRoot\.."

$mvnArgs = "clean package"
if ($SkipTests) {
    $mvnArgs += " -DskipTests"
    Write-Host "[INFO] 跳过测试" -ForegroundColor Yellow
}

Write-Host "[INFO] 开始构建..." -ForegroundColor Yellow
Write-Host ""

mvn $mvnArgs.Split(" ")

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[SUCCESS] 构建成功!" -ForegroundColor Green
    Write-Host ""
    Write-Host "生成的 JAR 文件:" -ForegroundColor Cyan
    Get-ChildItem -Path . -Recurse -Filter "*.jar" | Where-Object { $_.DirectoryName -like "*target*" -and $_.Name -notlike "*-sources*" -and $_.Name -notlike "*-javadoc*" } | ForEach-Object {
        Write-Host "  - $($_.FullName)" -ForegroundColor White
    }
} else {
    Write-Host ""
    Write-Host "[ERROR] 构建失败!" -ForegroundColor Red
    exit 1
}
