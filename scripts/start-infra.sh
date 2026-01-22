#!/bin/bash
# Game Server 基础设施启动脚本 (Bash)
# 用法: ./scripts/start-infra.sh

echo "========================================"
echo "  Game Server 基础设施启动"
echo "========================================"
echo ""

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "[ERROR] Docker 未安装，请先安装 Docker"
    exit 1
fi

# 检查 Docker 是否运行
if ! docker info &> /dev/null; then
    echo "[ERROR] Docker 未运行，请启动 Docker"
    exit 1
fi

echo "[INFO] 启动基础设施容器..."
cd "$(dirname "$0")/../docker"

# 启动容器
docker-compose up -d

if [ $? -eq 0 ]; then
    echo ""
    echo "[SUCCESS] 基础设施启动成功!"
    echo ""
    echo "服务地址:"
    echo "  - MongoDB:      localhost:27017"
    echo "  - Redis:        localhost:6379"
    echo "  - Nacos:        http://localhost:8848/nacos"
    echo "  - XXL-Job:      http://localhost:8088/xxl-job-admin"
    echo ""
    echo "默认账号密码:"
    echo "  - MongoDB:      game / game123"
    echo "  - Redis:        无用户名 / game123"
    echo "  - Nacos:        nacos / nacos"
    echo "  - XXL-Job:      admin / 123456"
else
    echo "[ERROR] 基础设施启动失败，请检查 Docker 日志"
    exit 1
fi
