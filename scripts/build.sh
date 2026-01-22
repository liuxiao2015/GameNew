#!/bin/bash

# Shell script to build the entire Maven project

SCRIPT_DIR=$(dirname "$0")
PROJECT_ROOT="${SCRIPT_DIR}/.."

echo "Building the project..."
cd "$PROJECT_ROOT"

mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo "Project built successfully."
else
    echo "Project build failed."
    exit 1
fi
