# 项目文档

本目录只保留课程交付和后续维护需要的正式文档。

| 文档 | 内容 | 演示时用途 |
|---|---|---|
| [requirements.md](requirements.md) | 用户故事、用例图、功能与非功能需求 | 说明“为什么做、需要做什么” |
| [architecture.md](architecture.md) | 架构图、ER 图、时序图、部署图 | 说明“系统怎么实现” |
| [api.md](api.md) | 接口版本、请求参数、错误码和鉴权矩阵 | 联调或答辩追问时查阅 |
| [database-design.md](database-design.md) | 主外键、唯一约束、索引、事务和 Flyway | 回答数据库设计问题 |
| [testing-and-acceptance.md](testing-and-acceptance.md) | 前后端、接口、DB、AI、性能和现场验收 | 证明项目可运行、闭环可验证 |
| [security-and-audit.md](security-and-audit.md) | 数据合规、权限、上传安全、AI 风险与审计 | 对应安全与审计评分项 |
| [collaboration.md](collaboration.md) | 三人分工、分支、Issue、PR 和 Review 规范 | 展示小组协作方式与剩余操作 |
| [test-evidence/](test-evidence/) | 关键流程截图和性能结果 | 测试报告辅助证据 |

## 推荐阅读顺序

1. 根目录 `README.md`：启动项目并了解演示流程。
2. `requirements.md`：从用户故事和用例解释项目目标。
3. `architecture.md` 与 `database-design.md`：说明系统、数据和 AI 如何协作。
4. `testing-and-acceptance.md`：按端到端步骤现场演示。
5. `api.md`、`security-and-audit.md`：回答接口、安全和审计追问。
6. `collaboration.md`：展示三人真实 GitHub 协作证据。

实验过程截图、浏览器 snapshot、临时网络日志和失败调试文件不属于正式交付文档，不放在仓库根目录。