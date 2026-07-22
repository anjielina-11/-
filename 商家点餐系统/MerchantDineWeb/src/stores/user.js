import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, register as apiRegister } from '../api'

export const useUserStore = defineStore('user', () => {
  const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))

  const isLoggedIn = computed(() => !!user.value.id)
  const isAdmin = computed(() => user.value.role === 1)

  async function loginAction(credentials) {
    const res = await apiLogin(credentials)
    const u = res.data.data
    user.value = u
    localStorage.setItem('user', JSON.stringify(u))
    return u
  }

  async function registerAction(data) {
    const res = await apiRegister(data)
    return res.data
  }

  function logout() {
    user.value = { id: null, username: '', nickname: '', phone: '', role: 0, status: 1 }
    localStorage.removeItem('user')
  }

  return { user, isLoggedIn, isAdmin, loginAction, registerAction, logout }
})
