# PowerShell script to generate .proto files from DTOs

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = Join-Path $ScriptDir ".."

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       Proto 文件生成工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 编译 proto-generator
Write-Host "编译 proto-generator..." -ForegroundColor Yellow
Push-Location $ProjectRoot
mvn -pl tools/proto-generator -am clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Pop-Location

# 编译 service-api
Write-Host "编译 service-api..." -ForegroundColor Yellow
Push-Location $ProjectRoot
mvn -pl services/service-api -am compile -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Pop-Location

# 运行生成器
Write-Host "生成 .proto 文件..." -ForegroundColor Yellow
$generatorJar = Join-Path $ProjectRoot "tools/proto-generator/target/proto-generator-1.0.0-SNAPSHOT.jar"
$outputDir = Join-Path $ProjectRoot "services/service-api/src/main/proto/generated"
$classpath = Join-Path $ProjectRoot "services/service-api/target/classes"

java -jar $generatorJar `
    --scan-packages=com.game.api `
    --output-dir=$outputDir `
    --classpath=$classpath

Write-Host ""
Write-Host "完成!" -ForegroundColor Green
Write-Host "生成的文件位于: $outputDir" -ForegroundColor Cyan
