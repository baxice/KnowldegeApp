import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomePage.vue'),
    meta: {
      title: '知识管理平台',
      requiresAuth: false
    }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: {
      title: '注册',
      requiresAuth: false
    }
  },
  {
    path: '/admin',
    name: 'Admin',
    redirect: '/admin/users',
    meta: {
      title: '管理中心',
      requiresAuth: true,
      requiresAdmin: true
    },
    children: [
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/admin/UserManagement.vue'),
        meta: {
          title: '用户管理',
          requiresAuth: true,
          requiresAdmin: true
        }
      },
      {
        path: 'logs',
        name: 'LogManagement',
        component: () => import('@/views/admin/LogManagement.vue'),
        meta: {
          title: '日志管理',
          requiresAuth: true,
          requiresAdmin: true
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/',
    meta: {
      title: '页面未找到',
      requiresAuth: false
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 更新页面标题
  document.title = to.meta.title || '知识管理平台'
  
  // 获取用户登录状态
  const userStore = useUserStore()
  const isLoggedIn = userStore.isAuthenticated
  const isAdmin = userStore.user?.role === 'admin' // 假设用户有role字段
  
  console.log('路由导航守卫:', to.path, 'requiresAuth:', to.meta.requiresAuth, 'isLoggedIn:', isLoggedIn)
  
  // 对于需要认证的页面，如果未登录，则重定向到登录页
  if (to.meta.requiresAuth && !isLoggedIn) {
    console.log('需要认证但未登录，重定向到登录页')
    next({ path: '/login', query: { redirect: to.fullPath } })
  } 
  // 对于需要管理员权限的页面，检查权限
  else if (to.meta.requiresAdmin && !isAdmin) {
    console.log('需要管理员权限但权限不足')
    next({ path: '/', query: { error: 'permission_denied' } })
  } 
  else {
    // 其他情况正常放行
    next()
  }
})

export default router 