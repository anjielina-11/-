# 小组协作与 Git 规范

## 1. 成员分工

| 成员 | GitHub | 负责内容 |
|---|---|---|
| 刘子豪 | `anjielina-11` | AI：模型训练、推理、RAG、Agent 与 AI 测试 |
| 叶俊琪 | `yinerzhou-10` | Frontend：页面、交互、状态管理、API 对接与前端测试 |
| 王艺霖 | `wangyiin-66` | Backend：业务接口、数据库、权限、MinIO、部署与后端测试 |

跨模块问题由相关负责人共同联调，例如“图片上传—AI 推理—结果展示”同时涉及三部分。

## 2. 推荐分支

```text
main
├─ feature/frontend-yinerzhou-10
├─ feature/backend-wangyiin-66
└─ feature/ai-anjielina-11
```

个人分支完成并验证后，通过 Pull Request 合并到 `main`。紧急修复可以使用 `fix/<问题>`，文档整理可以使用 `docs/<主题>`。

## 3. 标准操作

```powershell
# 获取最新主分支
git switch main
git pull origin main

# 切换个人分支（首次使用 -c 创建）
git switch -c feature/backend-wangyiin-66

# 查看并提交本人改动
git status
git add backend docs
git commit -m "feat(backend): 完成诊断审核与任务联动"
git push -u origin feature/backend-wangyiin-66
```

其他成员只需替换分支名、目录和提交说明。不要把三个人的代码一次性用同一个账号、同一个提交上传，否则无法展示真实协作过程。

## 4. 提交说明

遵循 Conventional Commits：

| 类型 | 用途 | 示例 |
|---|---|---|
| `feat` | 新功能 | `feat(frontend): add disease upload preview` |
| `fix` | Bug 修复 | `fix(ai): resolve model path from service root` |
| `test` | 测试 | `test(backend): cover farm ownership checks` |
| `docs` | 文档 | `docs: consolidate delivery documents` |
| `refactor` | 不改变功能的重构 | `refactor(frontend): type notification records` |
| `chore` | 构建或仓库维护 | `chore: clean generated artifacts` |

提交前应确认：只包含本人负责范围或已共同确认的联调改动，不提交 `.env`、缓存、构建产物和实验截图。

## 5. Pull Request 检查

1. 标题说明模块和目的。
2. 描述列出改动、验证命令和截图（如涉及页面）。
3. 至少由另一名成员阅读代码后再合并。
4. 冲突应在个人分支解决，不直接在 `main` 上强制覆盖。
5. 合并后其他成员执行 `git pull origin main` 同步。

## 6. 协作证据边界

本地文档只能说明分工与规范，不能替代 GitHub 的真实时间线。答辩时应展示仓库中的分支、成员提交、Pull Request 和 review 页面；不要补造不存在的 Issue、PR 编号或评审记录。
