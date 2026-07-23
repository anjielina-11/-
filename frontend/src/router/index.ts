import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore, type Role } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/farmer',
    name: 'Farmer',
    component: () => import('@/views/Farmer.vue'),
    meta: { requiresAuth: true, role: 'farmer' as Role },
    redirect: '/farmer/fields',
    children: [
      {
        path: 'fields',
        name: 'FarmerFields',
        component: () => import('@/views/FarmList.vue'),
        meta: { requiresAuth: true, role: 'farmer' as Role }
      },
      {
        path: 'crops',
        name: 'FarmerCrops',
        component: () => import('@/views/FarmerCrops.vue'),
        meta: { requiresAuth: true, role: 'farmer' as Role }
      },
      {
        path: 'disease',
        name: 'FarmerDisease',
        component: () => import('@/views/DiseaseUpload.vue'),
        meta: { requiresAuth: true, role: 'farmer' as Role }
      },
      {
        path: 'tasks',
        name: 'FarmerTasks',
        component: () => import('@/views/FarmerTask.vue'),
        meta: { requiresAuth: true, role: 'farmer' as Role }
      }
    ]
  },
  {
    path: '/tech',
    name: 'Tech',
    component: () => import('@/views/Tech.vue'),
    meta: { requiresAuth: true, role: 'tech' as Role },
    redirect: '/tech/review',
    children: [
      {
        path: 'review',
        name: 'TechReview',
        component: () => import('@/views/TechWorkbench.vue'),
        meta: { requiresAuth: true, role: 'tech' as Role }
      },
      {
        path: 'results',
        name: 'TechResults',
        component: () => import('@/views/ResultDetail.vue'),
        meta: { requiresAuth: true, role: 'tech' as Role }
      }
    ]
  },
  {
    path: '/coop',
    name: 'Coop',
    component: () => import('@/views/Coop.vue'),
    meta: { requiresAuth: true, role: 'coop' as Role },
    redirect: '/coop/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'CoopDashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { requiresAuth: true, role: 'coop' as Role }
      },
      {
        path: 'market',
        name: 'CoopMarket',
        component: () => import('@/views/CoopMarket.vue'),
        meta: { requiresAuth: true, role: 'coop' as Role }
      }
    ]
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAuth: true, role: 'admin' as Role },
    redirect: '/admin/users',
    children: [
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/AdminUsers.vue'),
        meta: { requiresAuth: true, role: 'admin' as Role }
      },
      {
        path: 'knowledge',
        name: 'AdminKnowledge',
        component: () => import('@/views/AdminKnowledge.vue'),
        meta: { requiresAuth: true, role: 'admin' as Role }
      },
      {
        path: 'models',
        name: 'AdminModels',
        component: () => import('@/views/AdminModels.vue'),
        meta: { requiresAuth: true, role: 'admin' as Role }
      }
    ]
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  if (!userStore.isLoggedIn) {
    if (to.meta.requiresAuth) {
      next('/login')
    } else {
      next()
    }
    return
  }

  if (to.path === '/login') {
    next(userStore.getDefaultPath())
    return
  }

  if (to.meta.requiresAuth && to.meta.role) {
    const requiredRole = to.meta.role as Role
    if (!userStore.hasRole(requiredRole)) {
      next(userStore.getDefaultPath())
      return
    }
  }

  next()
})

export default router