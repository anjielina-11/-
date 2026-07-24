import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn().mockResolvedValue({
      token: 'real-jwt',
      user: { id: 'u-1', username: 'tech1', role: 'tech', name: '李技术员' }
    })
  }
}))

import { normalizeBackendRole, useUserStore } from '../user'

describe('User Store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it.each([
    ['ROLE_FARMER', 'farmer'],
    ['ROLE_TECHNICIAN', 'tech'],
    ['ROLE_COOP_MANAGER', 'coop'],
    ['ROLE_ADMIN', 'admin'],
    ['technician', 'tech'],
    ['coop_manager', 'coop']
  ])('将后端角色 %s 映射为前端角色 %s', (backendRole, expectedRole) => {
    expect(normalizeBackendRole(backendRole)).toBe(expectedRole)
  })

  it('真实登录后保存 token、用户和菜单', async () => {
    const store = useUserStore()

    await store.login({ username: 'tech1', password: 'secret123' })

    expect(store.token).toBe('real-jwt')
    expect(store.user?.role).toBe('tech')
    expect(store.menus[0]?.path).toBe('/tech/review')
    expect(localStorage.getItem('token')).toBe('real-jwt')
  })
})
