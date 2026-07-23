<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore, type Role } from '@/stores/user'
import { useRouter, useRoute } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const isCollapsed = ref(false)

const roleLabels: Record<Role, string> = {
  farmer: '农户',
  tech: '农技人员',
  coop: '合作社',
  admin: '管理员'
}

const roleColors: Record<Role, string> = {
  farmer: '#52C41A',
  tech: '#1890FF',
  coop: '#FAAD14',
  admin: '#F5222D'
}

const currentPath = computed(() => route.path)

const handleMenuSelect = (path: string) => {
  router.push(path)
}

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// 获取用户名首字作为头像
const avatarText = computed(() => {
  return userStore.user?.name?.charAt(0) || '?'
})
</script>

<template>
  <div class="sidebar" :class="{ 'is-collapsed': isCollapsed }">
    <!-- 品牌区域 -->
    <div class="sidebar-brand">
      <div class="brand-icon">
        <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M16 2C9 2 4 8 4 14c0 4 2 7 5 9v5a2 2 0 002 2h10a2 2 0 002-2v-5c3-2 5-5 5-9 0-6-5-12-12-12z" fill="#52C41A"/>
          <path d="M12 16c0-2.2 1.8-4 4-4s4 1.8 4 4" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
          <path d="M10 22h12M13 26h6" stroke="#fff" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
      </div>
      <transition name="fade">
        <span v-if="!isCollapsed" class="brand-text">云农智诊</span>
      </transition>
    </div>

    <!-- 用户信息 -->
    <div class="sidebar-user" v-if="!isCollapsed">
      <div class="user-avatar" :style="{ borderColor: roleColors[userStore.user?.role || 'farmer'] }">
        {{ avatarText }}
      </div>
      <div class="user-info">
        <div class="user-name">{{ userStore.user?.name || '用户' }}</div>
        <div class="user-role" :style="{ color: roleColors[userStore.user?.role || 'farmer'] }">
          {{ roleLabels[userStore.user?.role || 'farmer'] }}
        </div>
      </div>
    </div>
    <div class="sidebar-user-collapsed" v-else>
      <div class="user-avatar-small" :style="{ borderColor: roleColors[userStore.user?.role || 'farmer'] }">
        {{ avatarText }}
      </div>
    </div>

    <!-- 分割线 -->
    <div class="sidebar-divider"></div>

    <!-- 导航菜单 -->
    <nav class="sidebar-nav">
      <div
        v-for="menu in userStore.menus"
        :key="menu.id"
        class="nav-item"
        :class="{ 'is-active': currentPath === menu.path }"
        @click="handleMenuSelect(menu.path)"
      >
        <div class="nav-item-indicator" v-if="currentPath === menu.path"></div>
        <div class="nav-item-icon">
          <!-- 地块管理 -->
          <svg v-if="menu.icon === 'MapLocation'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/>
            <circle cx="12" cy="10" r="3"/>
          </svg>
          <!-- 种植档案/叶子 -->
          <svg v-else-if="menu.icon === 'Leaf'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 8C8 10 5.9 16.17 3.82 21.34l1.89.66.95-2.3c.48.17.98.3 1.34.3C19 20 22 3 22 3c-1 2-8 2.25-13 3.25S2 11.5 2 13.5s1.75 3.75 1.75 3.75"/>
          </svg>
          <!-- 病害上报/相机 -->
          <svg v-else-if="menu.icon === 'Camera'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/>
            <circle cx="12" cy="13" r="4"/>
          </svg>
          <!-- 任务列表 -->
          <svg v-else-if="menu.icon === 'List'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/>
            <line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>
          </svg>
          <!-- 时钟/待审核 -->
          <svg v-else-if="menu.icon === 'Clock'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
          </svg>
          <!-- 搜索 -->
          <svg v-else-if="menu.icon === 'Search'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <!-- 饼图 -->
          <svg v-else-if="menu.icon === 'PieChart'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21.21 15.89A10 10 0 118 2.83"/><path d="M22 12A10 10 0 0012 2v10z"/>
          </svg>
          <!-- 趋势 -->
          <svg v-else-if="menu.icon === 'TrendingUp' || menu.icon === 'ArrowUp'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/>
          </svg>
          <!-- 用户 -->
          <svg v-else-if="menu.icon === 'User'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>
          </svg>
          <!-- 文档/知识库 -->
          <svg v-else-if="menu.icon === 'BookOpen' || menu.icon === 'Document'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2z"/><path d="M22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7z"/>
          </svg>
          <!-- CPU/模型 -->
          <svg v-else-if="menu.icon === 'Cpu'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="4" y="4" width="16" height="16" rx="2" ry="2"/><rect x="9" y="9" width="6" height="6"/>
            <line x1="9" y1="1" x2="9" y2="4"/><line x1="15" y1="1" x2="15" y2="4"/>
            <line x1="9" y1="20" x2="9" y2="23"/><line x1="15" y1="20" x2="15" y2="23"/>
            <line x1="20" y1="9" x2="23" y2="9"/><line x1="20" y1="14" x2="23" y2="14"/>
            <line x1="1" y1="9" x2="4" y2="9"/><line x1="1" y1="14" x2="4" y2="14"/>
          </svg>
          <!-- 默认 -->
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="1"/>
          </svg>
        </div>
        <transition name="fade">
          <span v-if="!isCollapsed" class="nav-item-text">{{ menu.name }}</span>
        </transition>
      </div>
    </nav>

    <!-- 底部折叠按钮 -->
    <div class="sidebar-footer">
      <div class="collapse-btn" @click="toggleCollapse">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :style="{ transform: isCollapsed ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.25s ease' }">
          <polyline points="11 17 6 12 11 7"/><polyline points="18 17 13 12 18 7"/>
        </svg>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--color-bg-sidebar);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-normal);
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
}

.sidebar.is-collapsed {
  width: var(--sidebar-collapsed-width);
}

/* 品牌区域 */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}

.brand-icon svg {
  width: 100%;
  height: 100%;
}

.brand-text {
  font-size: 20px;
  font-weight: 700;
  color: #FFFFFF;
  letter-spacing: 2px;
  white-space: nowrap;
}

/* 用户信息 */
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar-user-collapsed {
  display: flex;
  justify-content: center;
  padding: 16px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  border: 2px solid;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FFFFFF;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  border: 2px solid;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FFFFFF;
  font-size: 13px;
  font-weight: 600;
}

.user-info {
  overflow: hidden;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #FFFFFF;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 12px;
  font-weight: 500;
  margin-top: 2px;
}

/* 分割线 */
.sidebar-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.06);
  margin: 8px 16px;
}

/* 导航菜单 */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  margin: 2px 8px;
  border-radius: 8px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.65);
  transition: all var(--transition-fast);
  position: relative;
  white-space: nowrap;
}

.nav-item:hover {
  color: #FFFFFF;
  background: rgba(255, 255, 255, 0.08);
}

.nav-item.is-active {
  color: #FFFFFF;
  background: rgba(82, 196, 26, 0.15);
}

.nav-item-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: #52C41A;
  border-radius: 0 3px 3px 0;
}

.nav-item-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-item-icon svg {
  width: 20px;
  height: 20px;
}

.nav-item-text {
  font-size: 14px;
  font-weight: 500;
}

/* 底部折叠 */
.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.5);
  transition: all var(--transition-fast);
}

.collapse-btn:hover {
  color: #FFFFFF;
  background: rgba(255, 255, 255, 0.08);
}

.collapse-btn svg {
  width: 20px;
  height: 20px;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-fast);
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
