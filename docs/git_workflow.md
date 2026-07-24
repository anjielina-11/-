# Git 协作记录

> 云南农业智能诊断平台项目组 · 2026年7月

> 说明：下列 Issue/PR 编号来自团队协作记录。本地仓库无法独立证明远程平台中的链接、评审人和合并状态；最终提交时应附 GitHub/GitLab 页面截图或导出记录，禁止把本文件当作唯一证明。

---

## 一、分支策略

本项目采用 **GitHub Flow** 分支策略：

```
main ─────────────────────────────────────────────▶ (生产就绪)
  │
  ├── feat/frontend-pages ────▶ merged (前端16页面)
  ├── feat/ai-model-training ─▶ merged (模型训练+数据)
  ├── feat/api-integration ───▶ merged (前后端API对接)
  ├── feat/notification ──────▶ merged (通知系统+个人中心)
  ├── fix/jwt-principal ──────▶ merged (JWT类型修复)
  └── feat/backend-core ──────▶ merged (后端完整骨架)
```

## 二、Pull Request 记录

| PR | 分支 → main | 内容 | 评审 | 状态 |
|----|-------------|------|:--:|:--:|
| #7 | `feat/api-integration` | 完成前端API对接，替换 mock 为真实接口调用 | ✅ | merged |
| #6 | `feat/notification` | 通知系统 + 个人中心页面 | ✅ | merged |
| #5 | `feat/model-data` | 补全18类训练数据、训练脚本、data_loader | ✅ | merged |
| #4 | `fix/jwt-principal` | 修复 JWT principal 类型错误 | ✅ | merged |
| #3 | `docs/env-requirements` | 环境要求清单文档 | ✅ | merged |
| #2 | `chore/project-structure` | 项目结构整理 + Bug修复 | ✅ | merged |
| #1 | `feat/frontend-pages` | 前端功能开发 — 角色权限、种植档案、任务看板等 | ✅ | merged |

## 三、代码评审 (Code Review) 要点

| 评审维度 | 检查项 |
|----------|--------|
| 代码规范 | 命名、注释、代码风格一致性 |
| 安全 | 无硬编码密码/密钥、SQL 注入防护、XSS 防护 |
| 性能 | 无 N+1 查询、合理使用缓存、数据库索引 |
| 测试 | 核心逻辑有单元测试、边界条件覆盖 |
| 文档 | API 变更同步更新接口文档、README 更新 |

## 四、Issue 追踪

| Issue | 标题 | 标签 | 状态 |
|-------|------|------|:--:|
| #12 | 补全部署文档和一键启动脚本 | documentation | ✅ closed |
| #11 | 前端 API 对接替换 mock 数据 | enhancement | ✅ closed |
| #10 | 补充 RAG 知识库农技文档 | ai-service | ✅ closed |
| #9 | 模型评测报告和混淆矩阵 | ai-service | ✅ closed |
| #8 | 补后端集成测试和 E2E 测试 | testing | ✅ closed |
| #7 | 统一 Docker Compose 一键部署 | deploy | ✅ closed |
| #6 | 前后端接口联调 — 诊断上传流程 | bug | ✅ closed |
| #5 | JWT principal 类型错误修复 | bug | ✅ closed |
| #4 | 前端通知系统实现 | feature | ✅ closed |
| #3 | 个人中心页面开发 | feature | ✅ closed |
| #2 | 管理员密码 hash 修复 | bug | ✅ closed |
| #1 | 项目初始化和基础架构搭建 | epic | ✅ closed |

## 五、提交规范

遵循 Conventional Commits：
- `feat:` 新功能
- `fix:` Bug 修复
- `docs:` 文档变更
- `chore:` 构建/配置/工具
- `merge:` 分支合并
- `refactor:` 代码重构
- `test:` 测试相关
