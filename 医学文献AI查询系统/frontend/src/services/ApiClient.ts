import axios from 'axios'
import type { ApiErrorResponse } from '@/types'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 120000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const data = error.response.data as ApiErrorResponse
      const err = new Error(data.message || '服务请求失败')
      err.name = data.error_code || 'UNKNOWN_ERROR'
      throw err
    }
    if (error.code === 'ECONNABORTED') {
      const err = new Error('请求超时，请稍后重试')
      err.name = 'TIMEOUT'
      throw err
    }
    const err = new Error('网络连接异常，请检查网络')
    err.name = 'NETWORK_ERROR'
    throw err
  }
)

export default apiClient
