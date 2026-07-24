import { defineStore } from 'pinia'
import { ref } from 'vue'
import { diseaseDisplayName, normalizeReviewStatus } from '@/utils/domainMappers'

export interface Notification {
  id: string
  title: string
  content: string
  type: 'info' | 'success' | 'warning' | 'error'
  read: boolean
  createdAt: string
  link?: string
}

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<Notification[]>([])

  const unreadCount = ref(0)

  const loadNotifications = async () => {
    try {
      const { default: request } = await import('@/utils/request')
      const { useUserStore } = await import('@/stores/user')
      const role = useUserStore().user?.role
      const page = await request.get<{ list: any[]; total: number }>('/diagnosis?size=20')
      notifications.value = page.list.map((item: any) => ({
          id: item.id,
          title: item.diseaseName ? `诊断结果: ${diseaseDisplayName(item.diseaseName)}` : '新诊断记录',
          content: reviewStatusText(item.reviewStatus),
          type: notificationType(item.reviewStatus),
          read: false,
          createdAt: item.createdAt || new Date().toISOString(),
          link: role === 'tech' ? `/tech/results?id=${item.id}` : undefined
        }))
    } catch {
      // 离线时从 localStorage 恢复
      const saved = localStorage.getItem('notifications')
      if (saved) {
        try {
          notifications.value = JSON.parse(saved)
        } catch {
          notifications.value = []
        }
      }
    }
    updateUnreadCount()
  }

  const reviewStatusText = (status?: string): string => {
    const labels = {
      pending: '识别结果待人工审核',
      approved: '识别结果已审核通过',
      rejected: '识别结果已驳回',
      failed: '图片识别失败，请检查后重试'
    }
    return labels[normalizeReviewStatus(status)]
  }

  const notificationType = (status?: string): Notification['type'] => {
    const types = {
      pending: 'warning',
      approved: 'success',
      rejected: 'error',
      failed: 'error'
    } as const
    return types[normalizeReviewStatus(status)]
  }

  const updateUnreadCount = () => {
    unreadCount.value = notifications.value.filter(n => !n.read).length
  }

  const markAsRead = (id: string) => {
    const notification = notifications.value.find(n => n.id === id)
    if (notification) {
      notification.read = true
      updateUnreadCount()
      saveToStorage()
    }
  }

  const markAllAsRead = () => {
    notifications.value.forEach(n => {
      n.read = true
    })
    updateUnreadCount()
    saveToStorage()
  }

  const remove = (id: string) => {
    const index = notifications.value.findIndex(n => n.id === id)
    if (index > -1) {
      notifications.value.splice(index, 1)
      updateUnreadCount()
      saveToStorage()
    }
  }

  const saveToStorage = () => {
    localStorage.setItem('notifications', JSON.stringify(notifications.value))
  }

  const addNotification = (notification: Omit<Notification, 'id' | 'read'>) => {
    const newNotification: Notification = {
      ...notification,
      id: Date.now().toString(),
      read: false
    }
    notifications.value.unshift(newNotification)
    updateUnreadCount()
    saveToStorage()
  }

  return {
    notifications,
    unreadCount,
    loadNotifications,
    markAsRead,
    markAllAsRead,
    remove,
    addNotification
  }
})
