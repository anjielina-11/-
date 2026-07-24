import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

export type Role = 'farmer' | 'tech' | 'coop' | 'admin'

export interface UserInfo {
  id: string
  username: string
  role: Role
  name: string
}

export interface MenuItem {
  id: string
  name: string
  path: string
  icon: string
}

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  refreshToken?: string
  user: UserInfo
}

export const normalizeBackendRole = (role: string): Role => {
  const normalized = role.toUpperCase().replace(/^ROLE_/, '')
  const roleMap: Record<string, Role> = {
    FARMER: 'farmer',
    TECHNICIAN: 'tech',
    TECH: 'tech',
    COOP_MANAGER: 'coop',
    COOP: 'coop',
    ADMIN: 'admin'
  }
  const mapped = roleMap[normalized]
  if (!mapped) throw new Error(`不支持的用户角色: ${role}`)
  return mapped
}

const menuConfig: Record<Role, MenuItem[]> = {
  farmer: [
    { id: '1', name: '地块管理', path: '/farmer/fields', icon: 'MapLocation' },
    { id: '2', name: '种植档案', path: '/farmer/crops', icon: 'Leaf' },
    { id: '3', name: '病害上报', path: '/farmer/disease', icon: 'Camera' },
    { id: '4', name: '我的任务', path: '/farmer/tasks', icon: 'List' }
  ],
  tech: [
    { id: '1', name: '待审核列表', path: '/tech/review', icon: 'Clock' },
    { id: '2', name: '识别结果查询', path: '/tech/results', icon: 'Search' }
  ],
  coop: [
    { id: '1', name: '生产趋势看板', path: '/coop/dashboard', icon: 'PieChart' },
    { id: '2', name: '市场价格监控', path: '/coop/market', icon: 'TrendingUp' }
  ],
  admin: [
    { id: '1', name: '用户管理', path: '/admin/users', icon: 'User' },
    { id: '2', name: '知识库管理', path: '/admin/knowledge', icon: 'BookOpen' },
    { id: '3', name: '模型版本管理', path: '/admin/models', icon: 'Cpu' }
  ]
}

const mockUsers: Record<string, { password: string; user: UserInfo; token: string }> = {
  farmer: {
    password: 'farmer123',
    user: {
      id: '1',
      username: 'farmer',
      role: 'farmer',
      name: '张农户'
    },
    token: 'mock-token-farmer-123456'
  },
  tech: {
    password: 'tech123',
    user: {
      id: '2',
      username: 'tech',
      role: 'tech',
      name: '李农技员'
    },
    token: 'mock-token-tech-654321'
  },
  coop: {
    password: 'coop123',
    user: {
      id: '3',
      username: 'coop',
      role: 'coop',
      name: '王合作社长'
    },
    token: 'mock-token-coop-789012'
  },
  admin: {
    password: 'admin123',
    user: {
      id: '4',
      username: 'admin',
      role: 'admin',
      name: '刘管理员'
    },
    token: 'mock-token-admin-345678'
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const user = ref<UserInfo | null>(null)
  const menus = ref<MenuItem[]>([])

  const isLoggedIn = computed(() => !!token.value)

  const hasRole = (role: Role): boolean => {
    return user.value?.role === role
  }

  const getDefaultPath = (): string => {
    if (!user.value) return '/login'
    const roleMenus = menuConfig[user.value.role]
    return roleMenus.length > 0 ? roleMenus[0].path : '/login'
  }

  const getRoleMenus = (role: Role): MenuItem[] => {
    return menuConfig[role] || []
  }

  const setMenus = (role: Role): void => {
    menus.value = menuConfig[role] || []
    localStorage.setItem('menus', JSON.stringify(menus.value))
  }

  const mockLogin = (params: LoginParams): Promise<LoginResponse> => {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const mockUser = mockUsers[params.username]
        if (mockUser && mockUser.password === params.password) {
          resolve({
            token: mockUser.token,
            user: mockUser.user
          })
        } else {
          reject(new Error('用户名或密码错误'))
        }
      }, 500)
    })
  }

  const login = async (params: LoginParams): Promise<void> => {
    try {
      const useMock = import.meta.env.VITE_MOCK_LOGIN === 'true'
      const data = useMock ? await mockLogin(params) : await request.post<LoginResponse>('/auth/login', params)
      const normalizedUser = { ...data.user, role: normalizeBackendRole(data.user.role) }
      token.value = data.token
      user.value = normalizedUser
      setMenus(normalizedUser.role)
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(normalizedUser))
      if (data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken)
    } catch (error) {
      throw error
    }
  }

  const logout = (): void => {
    token.value = ''
    user.value = null
    menus.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('menus')
    localStorage.removeItem('refreshToken')
  }

  const loadUserInfo = (): void => {
    const savedUser = localStorage.getItem('user')
    const savedMenus = localStorage.getItem('menus')
    if (savedUser) {
      try {
        user.value = JSON.parse(savedUser)
      } catch {
        user.value = null
      }
    }
    if (savedMenus) {
      try {
        menus.value = JSON.parse(savedMenus)
      } catch {
        menus.value = []
      }
    } else if (user.value) {
      setMenus(user.value.role)
    }
  }

  return {
    token,
    user,
    menus,
    isLoggedIn,
    hasRole,
    getDefaultPath,
    getRoleMenus,
    login,
    logout,
    loadUserInfo
  }
})
