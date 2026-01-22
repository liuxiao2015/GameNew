#!/bin/bash

# Shell script to start individual Spring Boot services

SCRIPT_DIR=$(dirname "$0")
PROJECT_ROOT="${SCRIPT_DIR}/.."

services=(
    "services/service-login"
    "services/service-game"
    "services/service-guild"
    "services/service-chat"
    "services/service-rank"
    "services/service-scheduler"
    "services/service-gm"
    "gateway/gateway-server"
)

echo "Starting all services..."

for service in "${services[@]}"; do
    fullPath="${PROJECT_ROOT}/${service}"
    serviceName=$(basename "$service")
    echo "Starting ${serviceName}..."
    
    # Start service in background with nohup
    cd "$fullPath"
    nohup mvn spring-boot:run > "${serviceName}.log" 2>&1 &
    
    echo "  PID: $!"
    sleep 3
done

echo ""
echo "All services initiated. Check logs:"
for service in "${services[@]}"; do
    serviceName=$(basename "$service")
    echo "  - ${PROJECT_ROOT}/${service}/${serviceName}.log"
done
