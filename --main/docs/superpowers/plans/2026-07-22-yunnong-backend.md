# 云南农业智能诊断平台 — 业务后端实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完整构建 Spring Boot 3.5 业务后端，包含 14 张表数据库迁移、10 个模块 45+ REST API、JWT + RBAC 认证、MinIO 文件上传、Redis 缓存、Docker Compose 部署。

**Architecture:** 分层架构 — Controller → Service → Mapper(MyBatis-Plus)。公共层提供统一响应(R)、分页(PageResult)、全局异常处理。Security 层用 Spring Security + JWT Filter 实现无状态认证。每个业务模块独立 package，含 entity/dto/mapper/service/controller。

**Tech Stack:** Java 21 + Spring Boot 3.5 + MyBatis-Plus 3.5 + PostgreSQL 16 + pgvector + PostGIS + Redis 7 + MinIO + jjwt + SpringDoc OpenAPI + Flyway + Lombok + MapStruct + Hutool

## Global Constraints

- Java 21 (系统已安装)，Spring Boot 3.5.x，MyBatis-Plus 3.5.x
- 数据库 PostgreSQL 16 + PostGIS 3 + pgvector（Docker 部署）
- 所有表主键用 UUID（Java `String` 类型，数据库 `UUID` 类型）
- 所有 API 统一前缀 `/api/v1`，统一响应格式 `R<T>`
- JWT 无状态认证，角色 4 种：`ROLE_FARMER/ROLE_TECHNICIAN/ROLE_COOP_MANAGER/ROLE_ADMIN`
- 所有表必须带 `created_at`、`updated_at` 审计字段
- API 文档用 SpringDoc OpenAPI (Swagger 3)

---
