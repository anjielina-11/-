import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse, type AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

interface RequestConfig {
  params?: Record<string, unknown>
  headers?: Record<string, string>
}

interface CustomAxiosInstance extends AxiosInstance {
  get<T = unknown>(url: string, config?: RequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: RequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: RequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: RequestConfig): Promise<T>
}

interface ApiResponse<T> {
  code: number
  message?: string | null
  data: T
}

const isApiResponse = <T>(value: unknown): value is ApiResponse<T> => {
  return typeof value === 'object' && value !== null &&
    typeof (value as Partial<ApiResponse<T>>).code === 'number' &&
    'data' in value
}

export const unwrapApiResponse = <T>(value: ApiResponse<T> | T): T => {
  if (!isApiResponse<T>(value)) return value as T
  if (value.code !== 0) throw new Error(value.message || `业务请求失败（code=${value.code}）`)
  return value.data
}

const service = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
}) as CustomAxiosInstance

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) config.headers.Authorization = `Bearer ${userStore.token}`
    return config
  },
  error => Promise.reject(error)
)

service.interceptors.response.use(
  (response: AxiosResponse) => unwrapApiResponse(response.data),
  (error: AxiosError) => {
    if (error.response) {
      const message = (error.response.data as { message?: string })?.message
      switch (error.response.status) {
        case 401: {
        ElMessage.error(message || '登录已过期，请重新登录')
          const userStore = useUserStore()
          userStore.logout()
          void router.push('/login')
          break
        }
        case 403:
        ElMessage.error(message || '没有权限执行此操作')
          break
        case 404:
        ElMessage.error(message || '请求的资源不存在')
          break
        case 500:
        ElMessage.error(message || '服务器内部错误')
          break
        default:
        ElMessage.error(message || '请求失败')
      }
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      ElMessage.error(error.message || '请求配置错误')
    }
    return Promise.reject(error)
  }
)

export default service
