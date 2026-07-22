# 商家点餐系统

## 项目结构

```
商家点餐系统/
├── docs/                          # 项目文档
│   ├── 商家点餐可视化系统-API接口文档.docx
│   └── 商家点餐可视化系统 - 前端需求文档.docx
│
├── MerchantDineServer/            # 后端 Spring Boot
│   ├── sql/                       # 数据库脚本
│   │   ├── BuildingDatabasesAndTables.sql
│   │   └── initData.sql
│   ├── pom.xml
│   └── src/main/java/com/merchant/ordering/
│       ├── common/        # 公共组件（Result、异常处理）
│       ├── config/        # MyBatis-Plus 配置
│       ├── controller/    # REST 控制器
│       ├── dto/           # 请求/响应 DTO
│       ├── entity/        # 数据库实体
│       ├── mapper/        # MyBatis Mapper
│       ├── service/       # 业务逻辑
│       └── util/          # 工具类（MD5）
│
└── MerchantDineWeb/               # 前端 Vue 3
    └── src/
        ├── api/            # Axios 请求封装
        ├── assets/         # 静态资源
        ├── layouts/        # 布局组件
        ├── router/         # 路由配置
        ├── stores/         # Pinia 状态管理
        └── views/          # 页面组件
            └── admin/      # 管理后台页面
```

## 技术栈

| 层级 | 技术 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot | 2.7.18 |
| ORM | MyBatis-Plus | 3.5.3.1 |
| 数据库 | MySQL | 8.0 |
| 前端框架 | Vue | 3.4 |
| 构建工具 | Vite | 5.1 |
| UI 组件库 | Element Plus | 2.6 |
| 状态管理 | Pinia | 2.1 |
| 图表库 | ECharts | 5.5 |

## 快速启动

### 1. 数据库
```bash
mysql -u app_user -p < MerchantDineServer/sql/BuildingDatabasesAndTables.sql
mysql -u app_user -p < MerchantDineServer/sql/initData.sql
```

### 2. 后端
```bash
cd MerchantDineServer
./mvnw spring-boot:run
# → http://localhost:8080
```

### 3. 前端
```bash
cd MerchantDineWeb
npm install
npm run dev
# → http://localhost:5173
```

## 体验账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 管理员 | admin | 123456 |
| 普通用户 | user | 123456 |
