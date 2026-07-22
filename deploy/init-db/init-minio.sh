#!/bin/bash
# 初始化 MinIO bucket
set -e

# 等待 MinIO 启动
until curl -sf http://localhost:9000/minio/health/live; do
  echo "等待 MinIO..."
  sleep 2
done

# 配置 mc
mc alias set local http://localhost:9000 minioadmin minioadmin

# 创建 bucket
mc mb local/yunnong-images --ignore-existing

echo "MinIO bucket 初始化完成"
