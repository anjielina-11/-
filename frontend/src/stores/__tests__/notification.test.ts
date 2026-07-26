// @vitest-environment jsdom

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const { requestGet } = vi.hoisted(() => ({
  requestGet: vi.fn()
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: requestGet
  }
}))

import { useNotificationStore } from '../notification'

describe('Notification Store', () => {
  beforeEach(() => {
    localStorage.clear()
    requestGet.mockReset()
    requestGet.mockRejectedValue(new Error('offline'))
    setActivePinia(createPinia())
  })

  it.each([
    ['非数组 JSON', JSON.stringify({ id: 'broken' })],
    ['包含非法项的数组', JSON.stringify([null, { id: 1 }, 'broken'])]
  ])('离线恢复遇到%s时返回空列表而不是抛出异常', async (_caseName, savedValue) => {
    localStorage.setItem('notifications', savedValue)
    const store = useNotificationStore()

    await expect(store.loadNotifications()).resolves.toBeUndefined()
    expect(store.notifications).toEqual([])
    expect(store.unreadCount).toBe(0)
  })

  it('在线加载识别通知时显示正常中文标题', async () => {
    requestGet.mockResolvedValue({
      list: [{
        id: 'diagnosis-1',
        diseaseName: 'rice_blast',
        reviewStatus: 'pending',
        createdAt: '2026-07-26T09:00:00'
      }],
      total: 1
    })
    const store = useNotificationStore()

    await store.loadNotifications()

    expect(store.notifications[0].title).toBe('病害识别：水稻稻瘟病')
    expect(store.notifications[0].title).not.toContain('?')
  })
})
