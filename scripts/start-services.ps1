# ==================== 游戏服务器启动脚本 ====================
# 使用 service-launcher 统一管理服务
# 
# 用法:
#   .\scripts\start-services.ps1           # 交互模式
#   .\scripts\start-services.ps1 up        # 一键启动
#   .\scripts\start-services.ps1 status    # 查看状态
#   .\scripts\start-services.ps1 down      # 停止所有

param(
    [Parameter(Position=0)]
    [string]$Command = "",
    
    [Parameter(Position=1, ValueFromRemainingArguments=$true)]
    [string[]]$Args
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       游戏服务器启动器 v2.0" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 定位项目根目录
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

# 检查 JAR 是否存在
$LauncherJar = "$ProjectRoot\launcher\target\launcher-1.0.0-SNAPSHOT.jar"

if (-not (Test-Path $LauncherJar)) {
    Write-Host "[INFO] 正在构建 launcher..." -ForegroundColor Yellow
    
    # 使用 mvn 构建
    $buildResult = & mvn package -DskipTests -q -pl launcher -am 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] 构建失败，请检查代码" -ForegroundColor Red
        Write-Host $buildResult
        exit 1
    }
    
    Write-Host "[SUCCESS] 构建完成" -ForegroundColor Green
    Write-Host ""
}

# 检查 Java
$JavaCmd = "java"
if ($env:JAVA_HOME) {
    $JavaCmd = "$env:JAVA_HOME\bin\java.exe"
}

# 构建参数
$AllArgs = @("-jar", $LauncherJar)
if ($Command) {
    $AllArgs += $Command
}
if ($Args) {
    $AllArgs += $Args
}

# 运行启动器
Write-Host "[INFO] 启动服务管理器..." -ForegroundColor Yellow
Write-Host ""

& $JavaCmd $AllArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] 启动器退出异常" -ForegroundColor Red
    exit $LASTEXITCODE
}
