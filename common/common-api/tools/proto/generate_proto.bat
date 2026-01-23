@echo off
REM ============================================================
REM Proto 协议生成脚本 (Windows)
REM ============================================================
REM 
REM 用法:
REM   generate_proto.bat [java|ts|js|go|all]
REM
REM 参数:
REM   java  - 生成 Java 代码 (Maven 自动处理)
REM   ts    - 生成 TypeScript 代码
REM   js    - 生成 JavaScript 代码  
REM   go    - 生成 Go 代码
REM   all   - 生成所有语言
REM
REM 依赖:
REM   - protoc (Protocol Buffers 编译器)
REM   - protoc-gen-ts (TypeScript 插件, npm install -g ts-protoc-gen)
REM   - pbjs/pbts (JavaScript 插件, npm install -g protobufjs)
REM
REM ============================================================

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..\..\..\..
set PROTO_DIR=%SCRIPT_DIR%..\src\main\proto
set PROTO_FILE=%PROTO_DIR%\game.proto
set OUTPUT_DIR=%SCRIPT_DIR%output

REM 创建输出目录
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
if not exist "%OUTPUT_DIR%\java" mkdir "%OUTPUT_DIR%\java"
if not exist "%OUTPUT_DIR%\ts" mkdir "%OUTPUT_DIR%\ts"
if not exist "%OUTPUT_DIR%\js" mkdir "%OUTPUT_DIR%\js"
if not exist "%OUTPUT_DIR%\go" mkdir "%OUTPUT_DIR%\go"

set TARGET=%1
if "%TARGET%"=="" set TARGET=all

echo ============================================================
echo  Proto 协议生成器
echo ============================================================
echo  Proto 文件: %PROTO_FILE%
echo  输出目录: %OUTPUT_DIR%
echo  目标语言: %TARGET%
echo ============================================================

if "%TARGET%"=="java" goto :java
if "%TARGET%"=="ts" goto :ts
if "%TARGET%"=="js" goto :js
if "%TARGET%"=="go" goto :go
if "%TARGET%"=="all" goto :all
echo 未知目标: %TARGET%
goto :end

:all
call :java
call :ts
call :js
call :go
goto :end

:java
echo.
echo [Java] 使用 Maven 自动生成...
cd /d "%PROJECT_ROOT%"
call mvn compile -pl common/common-api -am -DskipTests -q
echo [Java] 完成! 输出目录: common\common-api\target\generated-sources\protobuf
goto :eof

:ts
echo.
echo [TypeScript] 生成中...
where protoc >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未找到 protoc, 请安装 Protocol Buffers
    goto :eof
)
REM 使用 protobuf-ts
protoc --proto_path="%PROTO_DIR%" --ts_out="%OUTPUT_DIR%\ts" "%PROTO_FILE%" 2>nul
if %errorlevel% neq 0 (
    echo [提示] 使用 pbjs 生成 TypeScript 定义...
    call npx pbjs -t static-module -w es6 -o "%OUTPUT_DIR%\ts\game.js" "%PROTO_FILE%"
    call npx pbts -o "%OUTPUT_DIR%\ts\game.d.ts" "%OUTPUT_DIR%\ts\game.js"
)
echo [TypeScript] 完成! 输出目录: %OUTPUT_DIR%\ts
goto :eof

:js
echo.
echo [JavaScript] 生成中...
where npx >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未找到 npx, 请安装 Node.js
    goto :eof
)
call npx pbjs -t static-module -w es6 -o "%OUTPUT_DIR%\js\game.js" "%PROTO_FILE%"
call npx pbjs -t json -o "%OUTPUT_DIR%\js\game.json" "%PROTO_FILE%"
echo [JavaScript] 完成! 输出目录: %OUTPUT_DIR%\js
goto :eof

:go
echo.
echo [Go] 生成中...
where protoc >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未找到 protoc, 请安装 Protocol Buffers
    goto :eof
)
protoc --proto_path="%PROTO_DIR%" --go_out="%OUTPUT_DIR%\go" "%PROTO_FILE%"
echo [Go] 完成! 输出目录: %OUTPUT_DIR%\go
goto :eof

:end
echo.
echo ============================================================
echo  生成完成!
echo ============================================================
echo.
echo  前端同步方法:
echo    1. 复制 game.proto 到前端项目
echo    2. 或复制 output 目录下对应语言的生成文件
echo.
echo  Proto 文件位置:
echo    %PROTO_FILE%
echo.
endlocal
