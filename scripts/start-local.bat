@echo off
chcp 65001 >nul 2>&1
title 游戏服务器启动器

echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║              游戏服务器本地开发环境一键启动                    ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

cd /d %~dp0..

REM 检查 Java
where java >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 Java，请确保已安装 JDK 21 并配置 JAVA_HOME
    pause
    exit /b 1
)

REM 检查 launcher.jar
if not exist "launcher\target\launcher-1.0.0-SNAPSHOT.jar" (
    echo [信息] 正在编译 Launcher...
    call mvn package -DskipTests -q -pl launcher -am
)

echo [信息] 启动服务器 (本地模式)...
echo [信息] 将自动启动本地 Nacos，请确保 MongoDB 和 Redis 已运行
echo.

java -jar launcher\target\launcher-1.0.0-SNAPSHOT.jar up --local

pause
