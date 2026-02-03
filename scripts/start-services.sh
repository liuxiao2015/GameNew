#!/bin/bash
# ==================== 游戏服务器启动脚本 ====================
# 使用 service-launcher 统一管理服务
# 
# 用法:
#   ./scripts/start-services.sh           # 交互模式
#   ./scripts/start-services.sh up        # 一键启动
#   ./scripts/start-services.sh status    # 查看状态
#   ./scripts/start-services.sh down      # 停止所有

set -e

echo ""
echo "========================================"
echo "       游戏服务器启动器 v2.0"
echo "========================================"
echo ""

# 定位项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# 检查 JAR 是否存在
LAUNCHER_JAR="$PROJECT_ROOT/launcher/target/launcher-1.0.0-SNAPSHOT.jar"

if [ ! -f "$LAUNCHER_JAR" ]; then
    echo "[INFO] 正在构建 launcher..."
    
    mvn package -DskipTests -q -pl launcher -am
    
    if [ $? -ne 0 ]; then
        echo "[ERROR] 构建失败，请检查代码"
        exit 1
    fi
    
    echo "[SUCCESS] 构建完成"
    echo ""
fi

# 检查 Java
JAVA_CMD="java"
if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

# 运行启动器
echo "[INFO] 启动服务管理器..."
echo ""

exec "$JAVA_CMD" -jar "$LAUNCHER_JAR" "$@"
