# CoffeeShop 咖啡点单系统 — 设计文档

> 日期：2026-07-20 | 状态：待审批

## 一、项目概述

一款面向咖啡店的线上点单全栈应用，覆盖移动端 H5 和 PC 端官网，支持菜单浏览、购物车管理、订单创建与管理。

### 技术栈

| 层 | 技术 |
|---|------|
| 框架 | Next.js 14+ (App Router) |
| 样式 | Tailwind CSS (Mobile First 响应式) |
| ORM | Prisma |
| 数据库 | MySQL |
| 实时通信 | SSE + BroadcastChannel |
| 认证 | 手机号直接登录，Cookie 持久化 30 天 |

---

## 二、路由结构

| 路由 | 渲染类型 | 说明 |
|------|---------|------|
| `/` | Server | 首页：已登录→菜单，未登录→登录引导 |
| `/login` | Client | 手机号登录页 |
| `/menu` | Server | 菜单浏览：分类筛选 + 商品列表 |
| `/menu/[id]` | Server + Client | 商品详情 + 规格选择弹窗 |
| `/cart` | Client | 购物车管理 |
| `/orders` | Client | 我的订单列表 |
| `/orders/[id]` | Server | 订单详情 |
| `/admin` | Client | 管理端：全部订单 + 状态推进 |
| `/api/auth/*` | API | 登录/登出/状态检查 |
| `/api/menu/*` | API | 菜单 CRUD |
| `/api/cart/*` | API | 购物车 CRUD + SSE |
| `/api/orders/*` | API | 订单 CRUD + 状态变更 |

---

## 三、数据模型 (Prisma Schema)

### 规格加价规则

| 杯型 | 加价 |
|------|------|
| S | ¥0 |
| M | ¥3 |
| L | ¥6 |

温度（hot/iced）不影响价格。

### 表关系

```
User (1) ──< (N) Order
User (1) ──< (N) Cart
Menu (1) ──< (N) CartItem
Menu (1) ──< (N) OrderItem
Cart (1) ──< (N) CartItem
Order (1) ──< (N) OrderItem
```

### 关键设计
- `Cart.phone` 登录时绑定，`Cart.sessionId` 未登录时使用
- `CartItem` 冗余 `name/imageUrl/unitPrice` 防止商品修改后影响购物车
- `OrderItem` 快照 `price` 保证历史订单价格不变
- 订单状态流转：`pending → paid → making → done`

---

## 四、UI 设计

### 移动端 (< 768px)
- 底部固定导航栏：首页 / 菜单 / 购物车 / 我的
- 购物车悬浮球（右下角，显示数量徽标）
- 分类横滚 Tab
- 商品卡片网格（2 列）
- 规格选择用 Bottom Sheet

### PC 端 (≥ 768px)
- 顶部横向导航栏，隐藏底部 Tab
- 左侧分类导航 + 右侧商品网格（3 列）
- 规格选择用内联 Radio Group
- 购物车侧边抽屉

### 设计系统

| Token | 值 |
|-------|-----|
| 主色 | `green-800` (#166534) |
| 背景 | `amber-50` (#FFFBEB) |
| 强调色 | `amber-500` (#F59E0B) |
| 卡片 | 白色 `rounded-2xl shadow-sm` |
| 字体 | 系统默认中文字体 |

---

## 五、组件树

```
app/
├── layout.tsx          → 根布局 (SessionProvider + SSEProvider)
├── page.tsx            → 首页
├── login/page.tsx      → 登录页
├── menu/
│   ├── page.tsx        → 菜单列表
│   └── [id]/page.tsx   → 商品详情
├── cart/page.tsx       → 购物车
├── orders/
│   ├── page.tsx        → 订单列表
│   └── [id]/page.tsx   → 订单详情
├── admin/page.tsx      → 管理端
└── components/
    ├── MobileNav.tsx    → 移动端底部导航
    ├── DesktopNav.tsx   → PC 端顶部导航
    ├── CartFab.tsx      → 购物车悬浮球
    ├── MenuCard.tsx     → 商品卡片
    ├── CategoryTabs.tsx → 分类筛选
    ├── SpecSelector.tsx → 规格选择器
    ├── CartDrawer.tsx   → 购物车抽屉
    ├── OrderCard.tsx    → 订单卡片
    ├── SSEProvider.tsx  → SSE 连接管理
    └── BroadcastProvider.tsx → 跨标签页通信
```

---

## 六、实时通信设计

### SSE 架构
```
Server (SSE endpoint: /api/cart/sse)
    │
    │  ┌──── 心跳 30s ────┐
    ▼  ▼                  ▼
┌──────────┐     ┌──────────┐     ┌──────────┐
│ Tab A    │     │ Tab B    │     │ Tab C    │
│ (Leader) │     │ (Follower)│    │ (Follower)│
│ SSE ◄─── │     │ BC ◄─────│     │ BC ◄─────│
│ BC ──►   │     │          │     │          │
└──────────┘     └──────────┘     └──────────┘
```

- Leader 选举：通过 BroadcastChannel 协商，只有一个 Tab 建立 SSE
- SSE 推送事件类型：`cart-update`、`heartbeat`
- BroadcastChannel 频道名：`coffeeshop-cart`
- 心跳间隔：30 秒

---

## 七、API 设计

| 方法 | 路由 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 手机号登录，Set-Cookie |
| POST | `/api/auth/logout` | 登出，清除 Cookie |
| GET | `/api/auth/me` | 获取当前用户 |
| GET | `/api/menu` | 菜单列表（支持 ?category=） |
| GET | `/api/menu/[id]` | 商品详情 |
| GET | `/api/cart` | 获取购物车 |
| POST | `/api/cart/items` | 添加商品到购物车 |
| PATCH | `/api/cart/items/[id]` | 修改数量 |
| DELETE | `/api/cart/items/[id]` | 删除商品 |
| DELETE | `/api/cart` | 清空购物车 |
| GET | `/api/cart/sse` | SSE 连接 |
| POST | `/api/orders` | 创建订单 |
| GET | `/api/orders` | 订单列表（?phone=） |
| GET | `/api/orders/[id]` | 订单详情 |
| PATCH | `/api/orders/[id]` | 更新订单状态（管理端） |

---

## 八、Seed 数据

开发环境预置 8 款商品覆盖 4 个分类：
- 咖啡：美式、拿铁、卡布奇诺
- 茶：抹茶拿铁、伯爵红茶
- 糕点：可颂、提拉米苏
- 特调：冷萃冰咖啡
