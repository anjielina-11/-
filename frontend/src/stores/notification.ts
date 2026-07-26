import { defineStore } from 'pinia'
import { ref } from 'vue'
import { diseaseDisplayName, normalizeReviewStatus } from '@/utils/domainMappers'

interface DiagnosisNotificationSource {
  id: string
  diseaseName?: string | null
  reviewStatus?: string | null
  createdAt?: string | null
}

export interface Notification {
  id: string
  title: string
  content: string
  type: 'info' | 'success' | 'warning' | 'error'
  read: boolean
  createdAt: string
  link?: string
}

const isNotification = (value: unknown): value is Notification => {
  if (!value || typeof value !== 'object') return false

  const item = value as Partial<Notification>
  return typeof item.id === 'string'
    && typeof item.title === 'string'
    && typeof item.content === 'string'
    && ['info', 'success', 'warning', 'error'].includes(item.type ?? '')
    && typeof item.read === 'boolean'
    && typeof item.createdAt === 'string'
    && (item.link === undefined || typeof item.link === 'string')
}

const restoreNotifications = (saved: string | null): Notification[] => {
  if (!saved) return []

  try {
    const parsed: unknown = JSON.parse(saved)
    return Array.isArray(parsed) ? parsed.filter(isNotification) : []
  } catch {
    return []
  }
}

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<Notification[]>([])

  const unreadCount = ref(0)

  const loadNotifications = async () => {
    try {
      const { default: request } = await import('@/utils/request')
      const { useUserStore } = await import('@/stores/user')
      const role = useUserStore().user?.role
      const page = await request.get<{ list: DiagnosisNotificationSource[]; total: number }>('/diagnosis?size=20')
      notifications.value = page.list.map(item => ({
        id: item.id,
        title: item.diseaseName ? `病害识别：${diseaseDisplayName(item.diseaseName)}` : '病害识别结果',
        content: reviewStatusText(item.reviewStatus ?? undefined),
        type: notificationType(item.reviewStatus ?? undefined),
        read: false,
        createdAt: item.createdAt || new Date().toISOString(),
        link: role === 'tech' ? `/tech/results?id=${item.id}` : undefined
      }))
    } catch {
      notifications.value = restoreNotifications(localStorage.getItem('notifications'))
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
