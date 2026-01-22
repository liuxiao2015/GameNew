#!/bin/bash

# Shell script to stop all running services

echo "Stopping all game services..."

# Find and kill all Java processes containing spring-boot
pids=$(ps aux | grep 'spring-boot:run' | grep -v grep | awk '{print $2}')

if [ -z "$pids" ]; then
    echo "No running services found."
else
    for pid in $pids; do
        echo "Stopping process: $pid"
        kill -15 $pid 2>/dev/null
    done
    echo "All services stopped."
fi
