import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动附加 Admin Header
api.interceptors.request.use(config => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  if (user.id && user.role === 1) {
    config.headers['X-User-Id'] = user.id
  }
  return config
})

// ==================== 用户 ====================
export const login = (data) => api.post('/user/login', data)
export const register = (data) => api.post('/user/register', data)

// ==================== 分类 ====================
export const getCategories = () => api.get('/category')
export const createCategory = (data) => api.post('/category', data)
export const deleteCategory = (id) => api.delete(`/category/${id}`)

// ==================== 商品 ====================
export const getProducts = () => api.get('/product')
export const createProduct = (data) => api.post('/product', data)
export const updateProduct = (id, data) => api.put(`/product/${id}`, data)
export const deleteProduct = (id) => api.delete(`/product/${id}`)

// ==================== 订单 ====================
export const createOrder = (data) => api.post('/order', data)
export const getUserOrders = (userId) => api.get(`/order/user/${userId}`)
export const getOrderDetail = (id) => api.get(`/order/${id}`)
export const getAllOrders = () => api.get('/order')
export const deleteOrder = (id) => api.delete(`/order/${id}`)

// ==================== 大屏 ====================
export const getDashboard = () => api.get('/dashboard')

export default api
