#!/bin/bash
# ============================================================
# Proto 协议生成脚本 (Linux/macOS)
# ============================================================
# 
# 用法:
#   ./generate_proto.sh [java|ts|js|go|all]
#
# 参数:
#   java  - 生成 Java 代码 (Maven 自动处理)
#   ts    - 生成 TypeScript 代码
#   js    - 生成 JavaScript 代码  
#   go    - 生成 Go 代码
#   all   - 生成所有语言
#
# 依赖:
#   - protoc (Protocol Buffers 编译器)
#   - protoc-gen-ts (TypeScript 插件)
#   - pbjs/pbts (JavaScript 插件)
#
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../../../.."
PROTO_DIR="$SCRIPT_DIR/../src/main/proto"
PROTO_FILE="$PROTO_DIR/game.proto"
OUTPUT_DIR="$SCRIPT_DIR/output"

# 创建输出目录
mkdir -p "$OUTPUT_DIR"/{java,ts,js,go}

TARGET="${1:-all}"

echo "============================================================"
echo " Proto 协议生成器"
echo "============================================================"
echo " Proto 文件: $PROTO_FILE"
echo " 输出目录: $OUTPUT_DIR"
echo " 目标语言: $TARGET"
echo "============================================================"

generate_java() {
    echo ""
    echo "[Java] 使用 Maven 自动生成..."
    cd "$PROJECT_ROOT"
    mvn compile -pl common/common-api -am -DskipTests -q
    echo "[Java] 完成! 输出目录: common/common-api/target/generated-sources/protobuf"
}

generate_ts() {
    echo ""
    echo "[TypeScript] 生成中..."
    if ! command -v protoc &> /dev/null; then
        echo "[错误] 未找到 protoc, 请安装 Protocol Buffers"
        return
    fi
    
    # 尝试使用 protobuf-ts
    if protoc --proto_path="$PROTO_DIR" --ts_out="$OUTPUT_DIR/ts" "$PROTO_FILE" 2>/dev/null; then
        echo "[TypeScript] 完成! 输出目录: $OUTPUT_DIR/ts"
    else
        echo "[提示] 使用 pbjs 生成 TypeScript 定义..."
        npx pbjs -t static-module -w es6 -o "$OUTPUT_DIR/ts/game.js" "$PROTO_FILE"
        npx pbts -o "$OUTPUT_DIR/ts/game.d.ts" "$OUTPUT_DIR/ts/game.js"
        echo "[TypeScript] 完成! 输出目录: $OUTPUT_DIR/ts"
    fi
}

generate_js() {
    echo ""
    echo "[JavaScript] 生成中..."
    if ! command -v npx &> /dev/null; then
        echo "[错误] 未找到 npx, 请安装 Node.js"
        return
    fi
    
    npx pbjs -t static-module -w es6 -o "$OUTPUT_DIR/js/game.js" "$PROTO_FILE"
    npx pbjs -t json -o "$OUTPUT_DIR/js/game.json" "$PROTO_FILE"
    echo "[JavaScript] 完成! 输出目录: $OUTPUT_DIR/js"
}

generate_go() {
    echo ""
    echo "[Go] 生成中..."
    if ! command -v protoc &> /dev/null; then
        echo "[错误] 未找到 protoc, 请安装 Protocol Buffers"
        return
    fi
    
    protoc --proto_path="$PROTO_DIR" --go_out="$OUTPUT_DIR/go" "$PROTO_FILE"
    echo "[Go] 完成! 输出目录: $OUTPUT_DIR/go"
}

case "$TARGET" in
    java)
        generate_java
        ;;
    ts)
        generate_ts
        ;;
    js)
        generate_js
        ;;
    go)
        generate_go
        ;;
    all)
        generate_java
        generate_ts
        generate_js
        generate_go
        ;;
    *)
        echo "未知目标: $TARGET"
        echo "用法: $0 [java|ts|js|go|all]"
        exit 1
        ;;
esac

echo ""
echo "============================================================"
echo " 生成完成!"
echo "============================================================"
echo ""
echo " 前端同步方法:"
echo "   1. 复制 game.proto 到前端项目"
echo "   2. 或复制 output 目录下对应语言的生成文件"
echo ""
echo " Proto 文件位置:"
echo "   $PROTO_FILE"
echo ""
