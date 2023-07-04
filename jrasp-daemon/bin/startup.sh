#!/bin/bash
# MAX_RETRIES 表示最大重试次数
MAX_RETRIES=3
# RETRY_INTERVAL 表示重试间隔时间
RETRY_INTERVAL=20
# RETRY_COUNT 表示当前重试次数
RETRY_COUNT=0

while true; do
    ./jrasp-daemon
    if [ $? -eq 0 ]; then
        echo "jrasp-daemon started successfully"
        break
    else
        RETRY_COUNT=$((RETRY_COUNT+1))
        if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
            echo "Failed to start jrasp-daemon after $MAX_RETRIES attempts"
            exit 1
        fi
        echo "Failed to start jrasp-daemon, retrying in $RETRY_INTERVAL seconds..."
        sleep $RETRY_INTERVAL
    fi
done
