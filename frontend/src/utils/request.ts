import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse, type AxiosError, type ResponseType } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

interface RequestConfig {
  params?: Record<string, unknown>
  headers?: Record<string, string>
  responseType?: ResponseType
  silent?: boolean
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
    const config = error.config as (InternalAxiosRequestConfig & { silent?: boolean }) | undefined
    const backendMessage = (error.response?.data as { message?: string } | undefined)?.message
    let message = backendMessage || error.message || '请求失败'

    if (error.response) {
      switch (error.response.status) {
        case 401: {
          message = backendMessage || '登录已过期，请重新登录'
          const userStore = useUserStore()
          userStore.logout()
          void router.push('/login')
          break
        }
        case 403:
          message = backendMessage || '没有权限执行此操作'
          break
        case 404:
          message = backendMessage || '请求的资源不存在'
          break
        case 500:
          message = backendMessage || '服务器内部错误'
          break
        default:
          message = backendMessage || '请求失败'
      }
    } else if (error.request) {
      message = '网络连接失败，请检查网络'
    } else {
      message = error.message || '请求配置错误'
    }

    error.message = message
    if (!config?.silent) ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service
