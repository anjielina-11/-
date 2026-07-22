<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-brand" @click="$router.push('/')">
        <span class="brand-emoji">🍲</span>
        <div>
          <div class="brand-name">美味商家</div>
          <div class="brand-sub">管理后台</div>
        </div>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/admin/screen" class="sidebar-item" :class="{ active: route.path === '/admin/screen' }">
          <span class="s-icon">📊</span>数据大屏
        </router-link>
        <router-link to="/admin/category" class="sidebar-item" :class="{ active: route.path === '/admin/category' }">
          <span class="s-icon">📂</span>分类管理
        </router-link>
        <router-link to="/admin/product" class="sidebar-item" :class="{ active: route.path === '/admin/product' }">
          <span class="s-icon">🍽️</span>商品管理
        </router-link>
        <router-link to="/admin/order" class="sidebar-item" :class="{ active: route.path === '/admin/order' }">
          <span class="s-icon">📦</span>订单管理
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <div class="admin-card">
          <el-avatar :size="34" style="background:linear-gradient(135deg,#fbbf24,#f59e0b);font-weight:700">
            {{ (user.user.nickname||user.user.username).charAt(0) }}
          </el-avatar>
          <div class="admin-info">
            <div class="admin-name">{{ user.user.nickname }}</div>
            <div class="admin-role">管理员</div>
          </div>
          <el-button class="logout-btn" @click="handleLogout" title="退出登录">🚪</el-button>
        </div>
      </div>
    </aside>
    <main class="admin-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'

const route = useRoute()
const user = useUserStore()
const cart = useCartStore()
function handleLogout() { cart.clearCartOnLogout(); user.logout(); window.location.href = '/' }
</script>

<style scoped>
.admin-layout { display: flex; height: 100vh; }
.admin-sidebar { width: 230px; background: linear-gradient(180deg, #1c1917 0%, #292524 100%); display: flex; flex-direction: column; }
.sidebar-brand { display: flex; align-items: center; gap: 10px; padding: 22px 20px; cursor: pointer; border-bottom: 1px solid rgba(255,255,255,.08); }
.brand-emoji { font-size: 28px; }
.brand-name { color: #fff; font-weight: 800; font-size: 16px; }
.brand-sub { color: #a8a29e; font-size: 11px; letter-spacing: 1px; }
.sidebar-nav { flex: 1; padding: 12px; display: flex; flex-direction: column; gap: 2px; }
.sidebar-item {
  display: flex; align-items: center; gap: 10px; padding: 12px 16px; border-radius: 10px;
  color: #a8a29e; font-size: 14px; font-weight: 600; transition: all .2s;
}
.sidebar-item:hover { background: rgba(255,255,255,.08); color: #fafaf9; }
.sidebar-item.active { background: rgba(230,93,62,.2); color: #f97316; }
.s-icon { font-size: 18px; }
.sidebar-footer { padding: 16px; border-top: 1px solid rgba(255,255,255,.08); }
.admin-card { display: flex; align-items: center; gap: 10px; }
.admin-info { flex: 1; }
.admin-name { color: #fafaf9; font-size: 14px; font-weight: 600; }
.admin-role { color: #78716c; font-size: 11px; }
.logout-btn { background: none; border: none; font-size: 18px; cursor: pointer; opacity: 0.6; transition: opacity .2s; padding: 4px; }
.logout-btn:hover { opacity: 1; }
.admin-main { flex: 1; background: #faf7f2; padding: 28px; overflow-y: auto; }
.fade-enter-active, .fade-leave-active { transition: opacity .2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
