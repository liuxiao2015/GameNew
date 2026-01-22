# ==================== Nacos 配置导入脚本 (PowerShell) ====================
# 用于将本地配置文件导入到 Nacos 配置中心

param(
    [string]$NacosAddr = "localhost:8848",
    [string]$Namespace = "game-server",
    [string]$Group = "GAME_SERVER",
    [string]$Username = "nacos",
    [string]$Password = "nacos"
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ConfigDir = Join-Path $ScriptDir "..\config\nacos"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       Nacos 配置导入工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Nacos 地址: $NacosAddr"
Write-Host "命名空间: $Namespace"
Write-Host "配置分组: $Group"
Write-Host "配置目录: $ConfigDir"
Write-Host ""

# 检查配置目录
if (-not (Test-Path $ConfigDir)) {
    Write-Host "错误: 配置目录不存在: $ConfigDir" -ForegroundColor Red
    exit 1
}

# 获取登录 Token
function Get-NacosToken {
    $loginUrl = "http://$NacosAddr/nacos/v1/auth/login"
    $body = @{
        username = $Username
        password = $Password
    }
    
    try {
        $response = Invoke-RestMethod -Uri $loginUrl -Method POST -Body $body
        return $response.accessToken
    } catch {
        Write-Host "警告: 登录失败，尝试无认证模式" -ForegroundColor Yellow
        return $null
    }
}

# 创建命名空间
function Create-Namespace {
    param($Token)
    
    $url = "http://$NacosAddr/nacos/v1/console/namespaces"
    $headers = @{}
    if ($Token) {
        $headers["accessToken"] = $Token
    }
    
    # 检查命名空间是否存在
    try {
        $response = Invoke-RestMethod -Uri $url -Method GET -Headers $headers
        $exists = $response.data | Where-Object { $_.namespace -eq $Namespace }
        
        if (-not $exists) {
            Write-Host "创建命名空间: $Namespace" -ForegroundColor Yellow
            $body = @{
                customNamespaceId = $Namespace
                namespaceName = "Game Server"
                namespaceDesc = "游戏服务器配置"
            }
            Invoke-RestMethod -Uri $url -Method POST -Body $body -Headers $headers | Out-Null
            Write-Host "命名空间创建成功" -ForegroundColor Green
        } else {
            Write-Host "命名空间已存在: $Namespace" -ForegroundColor Green
        }
    } catch {
        Write-Host "命名空间操作失败: $_" -ForegroundColor Red
    }
}

# 导入单个配置文件
function Import-Config {
    param(
        [string]$FilePath,
        [string]$Token
    )
    
    $fileName = [System.IO.Path]::GetFileName($FilePath)
    $content = Get-Content -Path $FilePath -Raw -Encoding UTF8
    
    $url = "http://$NacosAddr/nacos/v1/cs/configs"
    $headers = @{
        "Content-Type" = "application/x-www-form-urlencoded"
    }
    if ($Token) {
        $headers["accessToken"] = $Token
    }
    
    $body = @{
        dataId = $fileName
        group = $Group
        tenant = $Namespace
        content = $content
        type = "yaml"
    }
    
    try {
        $response = Invoke-RestMethod -Uri $url -Method POST -Body $body -Headers $headers
        if ($response -eq "true" -or $response -eq $true) {
            Write-Host "  [OK] $fileName" -ForegroundColor Green
        } else {
            Write-Host "  [FAIL] $fileName - $response" -ForegroundColor Red
        }
    } catch {
        Write-Host "  [ERROR] $fileName - $_" -ForegroundColor Red
    }
}

# 主流程
Write-Host "正在获取认证 Token..." -ForegroundColor Yellow
$token = Get-NacosToken

Write-Host "正在检查命名空间..." -ForegroundColor Yellow
Create-Namespace -Token $token

Write-Host ""
Write-Host "开始导入配置文件..." -ForegroundColor Yellow
Write-Host "----------------------------------------"

$configFiles = Get-ChildItem -Path $ConfigDir -Filter "*.yaml"
foreach ($file in $configFiles) {
    Import-Config -FilePath $file.FullName -Token $token
}

Write-Host "----------------------------------------"
Write-Host ""
Write-Host "配置导入完成!" -ForegroundColor Green
Write-Host ""
Write-Host "请访问 Nacos 控制台确认配置:" -ForegroundColor Cyan
Write-Host "http://$NacosAddr/nacos" -ForegroundColor Cyan
Write-Host ""
