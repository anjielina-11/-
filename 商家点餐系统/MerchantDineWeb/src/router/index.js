import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'products', name: 'Products', component: () => import('../views/Products.vue') },
      { path: 'cart', name: 'Cart', component: () => import('../views/Cart.vue') },
      { path: 'checkout', name: 'Checkout', component: () => import('../views/Checkout.vue'), meta: { auth: true } },
      { path: 'orders', name: 'Orders', component: () => import('../views/Orders.vue'), meta: { auth: true } },
      { path: 'order-detail/:id', name: 'OrderDetail', component: () => import('../views/OrderDetail.vue'), meta: { auth: true } }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { admin: true },
    children: [
      { path: '', redirect: '/admin/screen' },
      { path: 'screen', name: 'AdminScreen', component: () => import('../views/admin/Screen.vue') },
      { path: 'category', name: 'AdminCategory', component: () => import('../views/admin/CategoryManage.vue') },
      { path: 'product', name: 'AdminProduct', component: () => import('../views/admin/ProductManage.vue') },
      { path: 'order', name: 'AdminOrder', component: () => import('../views/admin/OrderManage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  const isLoggedIn = !!user.id
  const isAdmin = user.role === 1

  if (to.meta.admin && !isAdmin) {
    return next('/login')
  }
  if (to.meta.auth && !isLoggedIn) {
    return next('/login')
  }
  next()
})

export default router
