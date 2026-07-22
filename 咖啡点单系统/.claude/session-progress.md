# 会话任务进度

## 🔴 当前任务：本地部署 Dify

### 进度
- [x] 确认 Docker Desktop 已安装 (29.6.1 + Compose v5.3.0)
- [x] 启用 Windows 功能: Microsoft-Windows-Subsystem-Linux
- [x] 启用 Windows 功能: VirtualMachinePlatform
- [ ] 重启后安装 WSL 内核包 (`C:\Users\wangy\Downloads\wsl_update_x64.msi`)
- [ ] 设置 WSL 2 为默认版本 (`wsl --set-default-version 2`)
- [ ] 启动 Docker Desktop
- [ ] Clone Dify 仓库
- [ ] docker compose up 启动 Dify

### 关键信息
- Docker: `docker --version` → Docker 29.6.1, Docker Compose v5.3.0
- WSL 内核: `C:\Users\wangy\Downloads\wsl_update_x64.msi` (已下载)
- 项目位置: Dify 尚未 clone，待 WSL + Docker 就绪后决定放哪里

### 重启后下一步
1. 验证 `wsl.exe` 存在 → 终端运行 `wsl --status`
2. 双击安装 `wsl_update_x64.msi`
3. 管理员 PowerShell 运行 `wsl --set-default-version 2`
4. 打开 Docker Desktop，确认能启动
5. 回来告诉我结果
