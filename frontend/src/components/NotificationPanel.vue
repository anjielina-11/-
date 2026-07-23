<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElBadge, ElDropdown, ElDropdownItem, ElMessage } from 'element-plus'
import { useNotificationStore } from '@/stores/notification'
import { useRouter } from 'vue-router'

const notificationStore = useNotificationStore()
const router = useRouter()

const showPanel = ref(false)

onMounted(() => {
  notificationStore.loadNotifications()
})

const handleCommand = (command: string) => {
  if (command === 'markAllRead') {
    notificationStore.markAllAsRead()
    ElMessage.success('已全部标为已读')
  }
}

const handleNotificationClick = (notification: { link?: string; id: string }) => {
  notificationStore.markAsRead(notification.id)
  if (notification.link) {
    router.push(notification.link)
  }
}

const getTypeIcon = (type: string) => {
  switch (type) {
    case 'success':
      return 'check-circle'
    case 'warning':
      return 'alert-circle'
    case 'error':
      return 'x-circle'
    default:
      return 'info'
  }
}

const getTypeColor = (type: string) => {
  switch (type) {
    case 'success':
      return '#52C41A'
    case 'warning':
      return '#FAAD14'
    case 'error':
      return '#F5222D'
    default:
      return '#1890FF'
  }
}
</script>

<template>
  <ElDropdown
    trigger="click"
    @command="handleCommand"
    @visible-change="showPanel = $event"
  >
    <div class="notification-trigger">
      <ElBadge :value="notificationStore.unreadCount" :hidden="notificationStore.unreadCount === 0">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/>
          <path d="M13.73 21a2 2 0 01-3.46 0"/>
        </svg>
      </ElBadge>
    </div>
    <template #dropdown>
      <div class="notification-dropdown">
        <div class="dropdown-header">
          <span class="dropdown-title">消息通知</span>
          <span class="dropdown-count">{{ notificationStore.notifications.length }} 条消息</span>
        </div>
        
        <div class="dropdown-body" v-if="notificationStore.notifications.length > 0">
          <div
            v-for="notification in notificationStore.notifications"
            :key="notification.id"
            class="notification-item"
            :class="{ 'is-read': notification.read }"
            @click="handleNotificationClick(notification)"
          >
            <div class="notification-icon" :style="{ color: getTypeColor(notification.type) }">
              <svg v-if="getTypeIcon(notification.type) === 'check-circle'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
              <svg v-else-if="getTypeIcon(notification.type) === 'alert-circle'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              <svg v-else-if="getTypeIcon(notification.type) === 'x-circle'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="15" y1="9" x2="9" y2="15"/>
                <line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="16" x2="12" y2="12"/>
                <line x1="12" y1="8" x2="12.01" y2="8"/>
              </svg>
            </div>
            <div class="notification-content">
              <div class="notification-title">{{ notification.title }}</div>
              <div class="notification-text">{{ notification.content }}</div>
              <div class="notification-time">{{ notification.createdAt }}</div>
            </div>
            <div v-if="!notification.read" class="notification-dot"></div>
          </div>
        </div>
        
        <div class="dropdown-empty" v-else>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 01-3.46 0"/>
          </svg>
          <p>暂无消息</p>
        </div>
        
        <div class="dropdown-footer">
          <ElDropdownItem command="markAllRead">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:14px;height:14px;margin-right:6px">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            全部标为已读
          </ElDropdownItem>
        </div>
      </div>
    </template>
  </ElDropdown>
</template>

<style scoped>
.notification-trigger {
  position: relative;
  cursor: pointer;
  color: var(--color-text-secondary, #6B7280);
  transition: color var(--transition-fast, 0.15s ease);
  display: flex;
  align-items: center;
}

.notification-trigger:hover {
  color: var(--color-primary, #2D7D46);
}

.notification-trigger svg {
  width: 20px;
  height: 20px;
}

.notification-dropdown {
  width: 320px;
  padding: 0;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--color-border-light, #E5E7EB);
}

.dropdown-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary, #1F2937);
}

.dropdown-count {
  font-size: 12px;
  color: var(--color-text-placeholder, #9CA3AF);
}

.dropdown-body {
  max-height: 400px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background var(--transition-fast, 0.15s ease);
}

.notification-item:hover {
  background: var(--color-bg-hover, #F9FAFB);
}

.notification-item.is-read {
  opacity: 0.7;
}

.notification-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.notification-icon svg {
  width: 20px;
  height: 20px;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary, #1F2937);
  margin-bottom: 4px;
}

.notification-text {
  font-size: 12px;
  color: var(--color-text-secondary, #6B7280);
  line-height: 1.5;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-time {
  font-size: 11px;
  color: var(--color-text-placeholder, #9CA3AF);
}

.notification-dot {
  width: 6px;
  height: 6px;
  background: #F5222D;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}

.dropdown-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px;
  color: var(--color-text-placeholder, #9CA3AF);
}

.dropdown-empty svg {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
  opacity: 0.4;
}

.dropdown-empty p {
  font-size: 13px;
  margin: 0;
}

.dropdown-footer {
  padding: 8px;
  border-top: 1px solid var(--color-border-light, #E5E7EB);
}
</style>