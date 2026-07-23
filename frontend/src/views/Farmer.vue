<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElDropdown, ElDropdownMenu, ElDropdownItem, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import Sidebar from '@/components/Sidebar.vue'
import NotificationPanel from '@/components/NotificationPanel.vue'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const currentRouteName = computed(() => {
  const nameMap: Record<string, string> = {
    'FarmerFields': '地块管理',
    'FarmerCrops': '种植档案',
    'FarmerDisease': '病害上报',
    'FarmerTasks': '我的任务'
  }
  return nameMap[route.name as string] || '地块管理'
})

const avatarText = computed(() => {
  return userStore.user?.name?.charAt(0) || '农'
})

const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/farmer/profile')
  } else if (command === 'password') {
    ElMessage.info('修改密码功能开发中')
  }
}
</script>

<template>
  <div class="layout">
    <Sidebar />
    <div class="layout-main">
      <header class="layout-header">
        <div class="header-left">
          <div class="breadcrumb">
            <span class="breadcrumb-prefix">农户端</span>
            <span class="breadcrumb-separator">/</span>
            <span class="breadcrumb-current">{{ currentRouteName }}</span>
          </div>
        </div>
        <div class="header-right">
          <NotificationPanel />
          <ElDropdown trigger="click" @command="handleCommand">
            <div class="user-dropdown">
              <div class="user-avatar-header">{{ avatarText }}</div>
              <span class="user-name-header">{{ userStore.user?.name || '用户' }}</span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:14px;height:14px;margin-left:4px"><polyline points="6 9 12 15 18 9"/></svg>
            </div>
            <template #dropdown>
              <ElDropdownMenu>
                <ElDropdownItem command="profile">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;margin-right:8px"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  个人信息
                </ElDropdownItem>
                <ElDropdownItem command="password">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;margin-right:8px"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
                  修改密码
                </ElDropdownItem>
                <ElDropdownItem command="logout" divided>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;margin-right:8px"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                  退出登录
                </ElDropdownItem>
              </ElDropdownMenu>
            </template>
          </ElDropdown>
        </div>
      </header>
      <main class="layout-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.layout-header {
  height: var(--header-height);
  background: var(--color-bg-header);
  border-bottom: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.breadcrumb-prefix {
  color: var(--color-text-secondary);
  font-weight: 500;
}

.breadcrumb-separator {
  color: var(--color-text-placeholder);
}

.breadcrumb-current {
  color: var(--color-text-primary);
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.notification-bell {
  position: relative;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: color var(--transition-fast);
  display: flex;
  align-items: center;
}

.notification-bell:hover {
  color: var(--color-primary);
}

.notification-bell svg {
  width: 20px;
  height: 20px;
}

.notification-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  background: #F5222D;
  border-radius: 50%;
  border: 2px solid var(--color-bg-header);
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}

.user-dropdown:hover {
  background: var(--color-bg-hover);
}

.user-avatar-header {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #52C41A, #2D7D46);
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}

.user-name-header {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.layout-content {
  flex: 1;
  overflow-y: auto;
  background: var(--color-bg-page);
}
</style>
