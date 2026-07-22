# CoffeeShop 实施计划

> **Goal:** 从零搭建 CoffeeShop 咖啡点单全栈应用（Next.js 14 + Prisma + Tailwind CSS + MySQL）

**Architecture:** App Router 模式，Server Component 负责只读页面，Client Component 处理交互，API Routes 做业务逻辑，SSE+BroadcastChannel 实现实时购物车同步

**Tech Stack:** Next.js 14, TypeScript, Prisma, MySQL, Tailwind CSS, SSE, BroadcastChannel

## Global Constraints

- Next.js 14+ App Router
- Tailwind CSS Mobile First 响应式
- Prisma ORM + MySQL
- 手机号直接登录 (Cookie 30天)
- SSE 实时同步 + BroadcastChannel 跨标签页
- 移动端底部导航 + PC 端顶部导航
- 购物车悬浮球 (移动端)
- 订单状态: pending → paid → making → done

---

### Task 1: 项目初始化

**Files:**
- Create: `package.json`, `tsconfig.json`, `next.config.js`, `tailwind.config.ts`, `postcss.config.js`
- Create: `prisma/schema.prisma`
- Create: `.env`
- Create: `app/globals.css`, `app/layout.tsx`

**Steps:**

- [x] 1. 初始化 Next.js 14 项目
```bash
npx create-next-app@14 . --typescript --tailwind --eslint --app --src-dir=false --import-alias="@/*" --no-turbopack
```

- [x] 2. 安装依赖
```bash
npm install prisma @prisma/client next-auth
```

- [x] 3. 初始化 Prisma
```bash
npx prisma init
```

- [x] 4. 配置 `.env` 中 MySQL 连接串
```
DATABASE_URL="mysql://root:password@localhost:3306/coffeeshop"
```

- [x] 5. 写入 Prisma Schema，运行 `npx prisma db push`

- [x] 6. 写入 seed 脚本并执行 `npx prisma db seed`

- [x] 7. 配置 Tailwind 主题色

- [x] 8. Commit

---

### Task 2: 认证系统 (API + Cookie)

**Files:**
- Create: `lib/auth.ts`
- Create: `app/api/auth/login/route.ts`
- Create: `app/api/auth/logout/route.ts`
- Create: `app/api/auth/me/route.ts`
- Create: `lib/prisma.ts`

**Steps:**

- [ ] 1. 创建 `lib/prisma.ts` — Prisma 单例
- [ ] 2. 创建 `lib/auth.ts` — 登录态检查工具函数
- [ ] 3. 实现 `POST /api/auth/login` — 手机号登录，Set-Cookie (30天)
- [ ] 4. 实现 `POST /api/auth/logout` — 清除 Cookie
- [ ] 5. 实现 `GET /api/auth/me` — 获取当前用户
- [ ] 6. Commit

---

### Task 3: UI 基础组件 + 布局

**Files:**
- Create: `app/layout.tsx` (更新)
- Create: `app/globals.css` (更新)
- Create: `app/page.tsx`
- Create: `components/DesktopNav.tsx`
- Create: `components/MobileNav.tsx`
- Create: `components/CartFab.tsx`
- Create: `components/SessionProvider.tsx`
- Create: `components/SSEProvider.tsx`
- Create: `components/BroadcastProvider.tsx`
- Create: `app/login/page.tsx`

**Steps:**

- [ ] 1. 更新 `globals.css` — 添加自定义 CSS 变量和基础样式
- [ ] 2. 更新根布局 `layout.tsx` — SessionProvider + BroadcastProvider + MobileNav + DesktopNav
- [ ] 3. 实现 `MobileNav.tsx` — 移动端底部四 Tab 导航
- [ ] 4. 实现 `DesktopNav.tsx` — PC 端顶部横向导航
- [ ] 5. 实现 `CartFab.tsx` — 购物车悬浮球（移动端）
- [ ] 6. 实现 `SessionProvider.tsx` — 登录状态 Context
- [ ] 7. 实现 `BroadcastProvider.tsx` — 跨标签页通信 Context
- [ ] 8. 实现 `SSEProvider.tsx` — SSE 连接管理 + Leader 选举
- [ ] 9. 实现 `login/page.tsx` — 手机号登录页面
- [ ] 10. 更新 `app/page.tsx` — 首页自动跳转逻辑
- [ ] 11. Commit

---

### Task 4: 菜单浏览

**Files:**
- Create: `components/CategoryTabs.tsx`
- Create: `components/MenuCard.tsx`
- Create: `components/SpecSelector.tsx`
- Create: `app/menu/page.tsx`
- Create: `app/menu/[id]/page.tsx`
- Create: `app/api/menu/route.ts`
- Create: `app/api/menu/[id]/route.ts`
- Create: `lib/menu.ts`

**Steps:**

- [ ] 1. 实现 `GET /api/menu` — 菜单列表 API（支持 category 筛选）
- [ ] 2. 实现 `GET /api/menu/[id]` — 商品详情 API
- [ ] 3. 实现 `CategoryTabs.tsx` — 分类横滚标签
- [ ] 4. 实现 `MenuCard.tsx` — 商品卡片组件
- [ ] 5. 实现 `SpecSelector.tsx` — 杯型+温度选择器
- [ ] 6. 实现 `app/menu/page.tsx` — 菜单列表页 (Server Component)
- [ ] 7. 实现 `app/menu/[id]/page.tsx` — 商品详情页
- [ ] 8. Commit

---

### Task 5: 购物车管理

**Files:**
- Create: `app/api/cart/route.ts`
- Create: `app/api/cart/items/route.ts`
- Create: `app/api/cart/items/[id]/route.ts`
- Create: `app/api/cart/sse/route.ts`
- Create: `components/CartDrawer.tsx`
- Create: `components/CartItem.tsx`
- Create: `app/cart/page.tsx`
- Create: `lib/cart.ts`

**Steps:**

- [ ] 1. 实现 `GET /api/cart` — 获取购物车
- [ ] 2. 实现 `POST /api/cart/items` — 添加商品
- [ ] 3. 实现 `PATCH /api/cart/items/[id]` — 更新数量
- [ ] 4. 实现 `DELETE /api/cart/items/[id]` — 删除商品
- [ ] 5. 实现 `DELETE /api/cart` — 清空购物车
- [ ] 6. 实现 `GET /api/cart/sse` — SSE 端点
- [ ] 7. 实现 `CartItem.tsx` — 购物车项组件
- [ ] 8. 实现 `CartDrawer.tsx` — 购物车侧边抽屉（PC）
- [ ] 9. 实现 `app/cart/page.tsx` — 购物车页面
- [ ] 10. 集成 SSE + BroadcastChannel 实时同步
- [ ] 11. Commit

---

### Task 6: 订单管理

**Files:**
- Create: `app/api/orders/route.ts`
- Create: `app/api/orders/[id]/route.ts`
- Create: `components/OrderCard.tsx`
- Create: `app/orders/page.tsx`
- Create: `app/orders/[id]/page.tsx`
- Create: `lib/order.ts`

**Steps:**

- [ ] 1. 实现 `POST /api/orders` — 创建订单
- [ ] 2. 实现 `GET /api/orders` — 订单列表（按 phone 筛选）
- [ ] 3. 实现 `GET /api/orders/[id]` — 订单详情
- [ ] 4. 实现 `PATCH /api/orders/[id]` — 更新状态
- [ ] 5. 实现 `OrderCard.tsx` — 订单卡片
- [ ] 6. 实现 `app/orders/page.tsx` — 订单列表页
- [ ] 7. 实现 `app/orders/[id]/page.tsx` — 订单详情页
- [ ] 8. Commit

---

### Task 7: 管理端 + 完善

**Files:**
- Create: `app/admin/page.tsx`

**Steps:**

- [ ] 1. 实现 `app/admin/page.tsx` — 管理端：全部订单 + 状态推进按钮
- [ ] 2. 全链路端到端测试
- [ ] 3. 边界情况修复
- [ ] 4. Commit
