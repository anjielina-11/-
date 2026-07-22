<template>
  <div class="main-layout">
    <el-header class="main-header">
      <div class="header-inner">
        <div class="header-brand" @click="$router.push('/')">
          <div class="brand-icon">
            <svg viewBox="0 0 40 40" width="34" height="34"><circle cx="20" cy="20" r="19" fill="#e65d3e"/><text x="20" y="27" text-anchor="middle" fill="#fff" font-size="22">🍲</text></svg>
          </div>
          <div class="brand-text">
            <span class="brand-name">美味商家</span>
            <span class="brand-tag">点餐系统</span>
          </div>
        </div>

        <nav class="nav-links">
          <router-link to="/" class="nav-item" :class="{ active: route.path === '/' }">
            <span class="nav-icon">🏠</span>首页
          </router-link>
          <router-link to="/products" class="nav-item" :class="{ active: route.path === '/products' }">
            <span class="nav-icon">📋</span>点餐
          </router-link>
          <router-link to="/cart" class="nav-item cart-nav" :class="{ active: route.path === '/cart' }">
            <span class="nav-icon">🛒</span>购物车
            <span v-if="cart.totalCount" class="cart-dot">{{ cart.totalCount }}</span>
          </router-link>
          <router-link to="/orders" class="nav-item" :class="{ active: route.path.startsWith('/orders') }">
            <span class="nav-icon">📦</span>订单
          </router-link>
          <router-link v-if="user.isAdmin" to="/admin" class="nav-item admin-nav" :class="{ active: route.path.startsWith('/admin') }">
            <span class="nav-icon">⚙️</span>后台
          </router-link>
        </nav>

        <div class="user-area">
          <template v-if="user.isLoggedIn">
            <el-dropdown trigger="click" @command="handleCommand">
              <div class="user-badge">
                <el-avatar :size="36" class="user-avatar">
                  <span style="font-size:18px">{{ (user.user.nickname||user.user.username).charAt(0) }}</span>
                </el-avatar>
                <span class="user-name">{{ user.user.nickname || user.user.username }}</span>
                <span class="user-role-dot" :class="{ admin: user.isAdmin }"></span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item disabled>
                    <div style="display:flex;align-items:center;gap:10px;padding:4px 0">
                      <el-avatar :size="40"><span>{{ (user.user.nickname||user.user.username).charAt(0) }}</span></el-avatar>
                      <div>
                        <div style="font-weight:700">{{ user.user.nickname }}</div>
                        <div style="font-size:12px;color:#999">{{ user.isAdmin ? '管理员' : '普通用户' }}</div>
                      </div>
                    </div>
                  </el-dropdown-item>
                  <el-dropdown-item v-if="user.isAdmin" command="admin"><span>⚙️</span> 管理后台</el-dropdown-item>
                  <el-dropdown-item command="logout" divided><span>🚪</span> 退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button round class="btn-login" @click="$router.push('/login')">登录</el-button>
            <el-button round class="btn-register" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <footer class="main-footer">
      <div class="footer-inner">
        <span>© 2026 美味商家 · 让每一餐都值得期待</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'

const route = useRoute()
const user = useUserStore()
const cart = useCartStore()

function handleCommand(cmd) {
  if (cmd === 'logout') { cart.clearCartOnLogout(); user.logout(); window.location.href = '/' }
  else if (cmd === 'admin') { window.location.href = '/admin' }
}
</script>

<style scoped>
.main-layout { min-height: 100vh; display: flex; flex-direction: column; }
.main-header { position: sticky; top: 0; z-index: 100; background: rgba(255,255,255,.95); backdrop-filter: blur(12px); border-bottom: 1px solid #f0e8e0; padding: 0; height: 64px; }
.header-inner { max-width: 1400px; margin: 0 auto; height: 100%; display: flex; align-items: center; padding: 0 24px; gap: 32px; }
.header-brand { display: flex; align-items: center; gap: 10px; cursor: pointer; flex-shrink: 0; }
.brand-icon { line-height: 0; }
.brand-text { display: flex; flex-direction: column; }
.brand-name { font-size: 18px; font-weight: 800; color: #e65d3e; letter-spacing: -0.5px; line-height: 1.1; }
.brand-tag { font-size: 11px; color: #b08b7a; letter-spacing: 1px; }
.nav-links { display: flex; gap: 4px; flex: 1; }
.nav-item {
  display: flex; align-items: center; gap: 4px; padding: 8px 14px; border-radius: 10px;
  font-size: 14px; font-weight: 600; color: var(--food-text-light); transition: all var(--transition);
  position: relative;
}
.nav-item:hover, .nav-item.active { background: #fef3e8; color: var(--food-primary); }
.nav-icon { font-size: 16px; }
.cart-nav { position: relative; }
.cart-dot {
  position: absolute; top: 2px; right: 2px; background: var(--food-primary); color: #fff;
  font-size: 11px; font-weight: 700; min-width: 18px; height: 18px; border-radius: 9px;
  display: flex; align-items: center; justify-content: center; padding: 0 4px;
}
.admin-nav { color: #7c3aed; }
.admin-nav:hover, .admin-nav.active { background: #f3e8ff; color: #7c3aed; }
.user-area { display: flex; align-items: center; gap: 10px; }
.user-badge { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 4px 10px 4px 4px; border-radius: 20px; transition: all var(--transition); }
.user-badge:hover { background: #fef3e8; }
.user-avatar { background: linear-gradient(135deg, #fbbf24, #f59e0b); color: #fff; font-weight: 700; }
.user-name { font-size: 14px; font-weight: 600; max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.user-role-dot { width: 8px; height: 8px; border-radius: 50%; background: #22c55e; }
.user-role-dot.admin { background: #7c3aed; }
.btn-login { --el-button-bg-color: #fff; --el-button-border-color: #e65d3e; --el-button-text-color: #e65d3e; font-weight: 600; }
.btn-register { --el-button-bg-color: #e65d3e; --el-button-border-color: #e65d3e; --el-button-text-color: #fff; font-weight: 600; }
.main-content { flex: 1; padding: 28px 24px; max-width: 1400px; margin: 0 auto; width: 100%; }
.main-footer { background: #fff; border-top: 1px solid #f0e8e0; padding: 20px; text-align: center; color: #b08b7a; font-size: 13px; }

.page-fade-enter-active, .page-fade-leave-active { transition: opacity .2s ease, transform .2s ease; }
.page-fade-enter-from, .page-fade-leave-to { opacity: 0; transform: translateY(6px); }
</style>
