# ============================================
# 云南农业智能诊断平台 - 业务后端 Dockerfile
# 多阶段构建: Maven build → JDK runtime
# ============================================

FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# 复制 pom.xml，利用 Docker 缓存层下载依赖
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY src src
RUN mvn package -DskipTests -B

# 使用 Spring Boot Layertools 提取分层（优化最终镜像缓存）
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# --- 运行阶段 ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S yunong && adduser -S yunong -G yunong
RUN apk add --no-cache curl

# 按变更频率从低到高复制层
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./

USER yunong
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
