# ============================================
# 云南农业智能诊断平台 - 前端 Dockerfile
# 多阶段构建: Node build → Nginx serve
# ============================================

FROM node:20-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci --include=dev --ignore-scripts 2>/dev/null || npm install

COPY . .
RUN npm run build

# --- 运行阶段 ---
FROM nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:80/ || exit 1
