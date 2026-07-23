import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface Notification {
  id: string
  title: string
  content: string
  type: 'info' | 'success' | 'warning' | 'error'
  read: boolean
  createdAt: string
  link?: string
}

const mockNotifications: Notification[] = [
  {
    id: '1',
    title: '病害识别结果',
    content: '您提交的病害图片已识别完成，结果为水稻稻瘟病，置信度85%',
    type: 'info',
    read: false,
    createdAt: '2026-07-23 10:30',
    link: '/farmer/disease'
  },
  {
    id: '2',
    title: '农技建议',
    content: '农技人员已审核您的病害上报并提供防治建议，请查看详情',
    type: 'success',
    read: false,
    createdAt: '2026-07-23 09:15',
    link: '/farmer/disease'
  },
  {
    id: '3',
    title: '农事任务提醒',
    content: '您有3项任务即将到期，请及时处理',
    type: 'warning',
    read: true,
    createdAt: '2026-07-22 16:45',
    link: '/farmer/tasks'
  },
  {
    id: '4',
    title: '天气预警',
    content: '未来3天将有暴雨天气，请做好农田排水准备',
    type: 'warning',
    read: true,
    createdAt: '2026-07-22 08:00'
  },
  {
    id: '5',
    title: '系统更新',
    content: '平台已更新至v1.2版本，新增市场价格监控功能',
    type: 'info',
    read: true,
    createdAt: '2026-07-21 12:00'
  }
]

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<Notification[]>([])

  const unreadCount = ref(0)

  const loadNotifications = () => {
    const saved = localStorage.getItem('notifications')
    if (saved) {
      try {
        notifications.value = JSON.parse(saved)
      } catch {
        notifications.value = mockNotifications
      }
    } else {
      notifications.value = mockNotifications
    }
    updateUnreadCount()
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