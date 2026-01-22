#!/bin/bash

# Shell script to generate .proto files from DTOs

SCRIPT_DIR=$(dirname "$0")
PROJECT_ROOT="${SCRIPT_DIR}/.."

echo "========================================"
echo "       Proto 文件生成工具"
echo "========================================"
echo ""

# 编译 proto-generator
echo "编译 proto-generator..."
cd "$PROJECT_ROOT"
mvn -pl tools/proto-generator -am clean package -DskipTests -q
if [ $? -ne 0 ]; then
    echo "编译失败!"
    exit 1
fi

# 编译 service-api
echo "编译 service-api..."
mvn -pl services/service-api -am compile -DskipTests -q
if [ $? -ne 0 ]; then
    echo "编译失败!"
    exit 1
fi

# 运行生成器
echo "生成 .proto 文件..."
GENERATOR_JAR="${PROJECT_ROOT}/tools/proto-generator/target/proto-generator-1.0.0-SNAPSHOT.jar"
OUTPUT_DIR="${PROJECT_ROOT}/services/service-api/src/main/proto/generated"
CLASSPATH="${PROJECT_ROOT}/services/service-api/target/classes"

java -jar "$GENERATOR_JAR" \
    --scan-packages=com.game.api \
    --output-dir="$OUTPUT_DIR" \
    --classpath="$CLASSPATH"

echo ""
echo "完成!"
echo "生成的文件位于: $OUTPUT_DIR"
