import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('@/views/Login.vue'),
  },
  {
    path: '/register',
    component: () => import('@/views/Register.vue'),
  },
  {
    path: '/reset-password',
    component: () => import('@/views/ResetPassword.vue'),
  },
  {
    path: '/setup-profile',
    component: () => import('@/views/SetupProfile.vue'),
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    meta: { auth: true },
    redirect: '/home',
    children: [
      { path: 'home', component: () => import('@/views/Home.vue') },
      { path: 'notes', component: () => import('@/views/Notes.vue') },
      { path: 'files', component: () => import('@/views/Files.vue') },
      { path: 'chat', component: () => import('@/views/Chat.vue') },
      { path: 'profile', component: () => import('@/views/Profile.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const publicPaths = ['/login', '/register', '/reset-password', '/setup-profile']

router.beforeEach(async (to, _from, next) => {
  const auth = useAuthStore()

  if (to.meta.auth && !auth.isLoggedIn) {
    next('/login')
    return
  }

  if (auth.isLoggedIn) {
    try {
      await auth.fetchUserInfo()
    } catch {
      await auth.logout()
      next('/login')
      return
    }

    if (!publicPaths.includes(to.path) && auth.user) {
      const nickname = auth.user.nickname
      if (!nickname || nickname === 'User' || nickname.length < 2 || !auth.user.avatar) {
        next('/setup-profile')
        return
      }
    }
  }

  next()
})

export default router
