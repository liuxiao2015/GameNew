@echo off
chcp 65001 >nul 2>&1
echo ========================================
echo       Nacos 启动脚本
echo ========================================
echo.

REM 检查 nacos 目录是否存在
if exist "C:\Work\GameNew\tools\nacos\bin\startup.cmd" (
    echo [√] Nacos 已解压，正在启动...
    cd /d "C:\Work\GameNew\tools\nacos\bin"
    call startup.cmd -m standalone
) else (
    echo [!] Nacos 未解压，正在解压中...
    
    REM 使用 PowerShell 解压
    powershell -Command "Expand-Archive -Path 'C:\Work\GameNew\tools\nacos-server-2.3.0.zip' -DestinationPath 'C:\Work\GameNew\tools' -Force"
    
    if exist "C:\Work\GameNew\tools\nacos\bin\startup.cmd" (
        echo [√] 解压完成，正在启动...
        cd /d "C:\Work\GameNew\tools\nacos\bin"
        call startup.cmd -m standalone
    ) else (
        echo [X] 解压失败，请手动解压 nacos-server-2.3.0.zip
    )
)
