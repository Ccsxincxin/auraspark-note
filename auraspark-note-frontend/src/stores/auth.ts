import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import type { UserInfo } from '@/types'

function getToken(key: string): string {
  return localStorage.getItem(key) || sessionStorage.getItem(key) || ''
}

function setToken(key: string, value: string, remember: boolean) {
  if (remember) {
    localStorage.setItem(key, value)
  } else {
    sessionStorage.setItem(key, value)
  }
}

function removeToken(key: string) {
  localStorage.removeItem(key)
  sessionStorage.removeItem(key)
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(getToken('accessToken'))
  const refreshToken = ref(getToken('refreshToken'))
  const user = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!accessToken.value)

  function saveTokens(access: string, refresh: string, remember = false) {
    accessToken.value = access
    refreshToken.value = refresh
    setToken('accessToken', access, remember)
    setToken('refreshToken', refresh, remember)
  }

  async function loginByPassword(account: string, password: string, rememberMe = false) {
    const { data } = await authApi.loginByPassword(account, password, rememberMe)
    saveTokens(data.data.accessToken, data.data.refreshToken, rememberMe)
  }

  async function loginByCode(target: string, value: string, code: string, rememberMe = false) {
    const { data } = await authApi.loginByCode(target, value, code, rememberMe)
    saveTokens(data.data.accessToken, data.data.refreshToken, rememberMe)
  }

  async function register(
    target: string,
    value: string,
    code: string,
    password: string,
    nickname: string,
  ) {
    const { data } = await authApi.register(target, value, code, password, nickname)
    saveTokens(data.data.accessToken, data.data.refreshToken, false)
  }

  async function fetchUserInfo() {
    const { data } = await authApi.getUserInfo()
    user.value = data.data
  }

  async function logout() {
    try {
      await authApi.logout(refreshToken.value)
    } catch {
      // ignore
    }
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    removeToken('accessToken')
    removeToken('refreshToken')
  }

  return {
    accessToken,
    refreshToken,
    user,
    isLoggedIn,
    loginByPassword,
    loginByCode,
    register,
    fetchUserInfo,
    logout,
  }
})
