#!/bin/bash

# ==================== Nacos 配置导入脚本 (Bash) ====================
# 用于将本地配置文件导入到 Nacos 配置中心

# 默认参数
NACOS_ADDR="${NACOS_ADDR:-localhost:8848}"
NAMESPACE="${NACOS_NAMESPACE:-game-server}"
GROUP="${NACOS_GROUP:-GAME_SERVER}"
USERNAME="${NACOS_USER:-nacos}"
PASSWORD="${NACOS_PASS:-nacos}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_DIR="${SCRIPT_DIR}/../config/nacos"

echo "========================================"
echo "       Nacos 配置导入工具"
echo "========================================"
echo ""
echo "Nacos 地址: $NACOS_ADDR"
echo "命名空间: $NAMESPACE"
echo "配置分组: $GROUP"
echo "配置目录: $CONFIG_DIR"
echo ""

# 检查配置目录
if [ ! -d "$CONFIG_DIR" ]; then
    echo "错误: 配置目录不存在: $CONFIG_DIR"
    exit 1
fi

# 检查 curl 是否可用
if ! command -v curl &> /dev/null; then
    echo "错误: 需要 curl 工具"
    exit 1
fi

# 获取登录 Token
get_token() {
    local response
    response=$(curl -s -X POST "http://${NACOS_ADDR}/nacos/v1/auth/login" \
        -d "username=${USERNAME}" \
        -d "password=${PASSWORD}")
    
    # 提取 accessToken
    echo "$response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4
}

# 创建命名空间
create_namespace() {
    local token=$1
    local url="http://${NACOS_ADDR}/nacos/v1/console/namespaces"
    local auth=""
    
    if [ -n "$token" ]; then
        auth="&accessToken=${token}"
    fi
    
    # 检查命名空间是否存在
    local exists
    exists=$(curl -s "${url}?${auth}" | grep -c "\"namespace\":\"${NAMESPACE}\"")
    
    if [ "$exists" -eq 0 ]; then
        echo "创建命名空间: $NAMESPACE"
        curl -s -X POST "$url" \
            -d "customNamespaceId=${NAMESPACE}" \
            -d "namespaceName=Game Server" \
            -d "namespaceDesc=游戏服务器配置${auth}" > /dev/null
        echo "命名空间创建成功"
    else
        echo "命名空间已存在: $NAMESPACE"
    fi
}

# 导入单个配置文件
import_config() {
    local file_path=$1
    local token=$2
    local file_name=$(basename "$file_path")
    local content
    content=$(cat "$file_path")
    
    local url="http://${NACOS_ADDR}/nacos/v1/cs/configs"
    local auth=""
    
    if [ -n "$token" ]; then
        auth="&accessToken=${token}"
    fi
    
    local response
    response=$(curl -s -X POST "$url" \
        -d "dataId=${file_name}" \
        -d "group=${GROUP}" \
        -d "tenant=${NAMESPACE}" \
        --data-urlencode "content=${content}" \
        -d "type=yaml${auth}")
    
    if [ "$response" = "true" ]; then
        echo "  [OK] $file_name"
    else
        echo "  [FAIL] $file_name - $response"
    fi
}

# 主流程
echo "正在获取认证 Token..."
TOKEN=$(get_token)

if [ -n "$TOKEN" ]; then
    echo "认证成功"
else
    echo "警告: 认证失败，尝试无认证模式"
fi

echo "正在检查命名空间..."
create_namespace "$TOKEN"

echo ""
echo "开始导入配置文件..."
echo "----------------------------------------"

for file in "$CONFIG_DIR"/*.yaml; do
    if [ -f "$file" ]; then
        import_config "$file" "$TOKEN"
    fi
done

echo "----------------------------------------"
echo ""
echo "配置导入完成!"
echo ""
echo "请访问 Nacos 控制台确认配置:"
echo "http://${NACOS_ADDR}/nacos"
echo ""
