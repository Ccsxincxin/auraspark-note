import client from './client'
import type {
  ApiResponse,
  AuthResponse,
  UserInfo,
  PasswordPolicy,
} from '@/types'

export function sendCode(target: string, value: string, type: string) {
  return client.post<ApiResponse<null>>('/auth/send-code', { target, value, type })
}

export function register(
  target: string,
  value: string,
  code: string,
  password: string,
  nickname: string,
) {
  return client.post<ApiResponse<AuthResponse>>('/auth/register', {
    target,
    value,
    code,
    password,
    nickname,
  })
}

export function loginByPassword(account: string, password: string, rememberMe = false) {
  return client.post<ApiResponse<AuthResponse>>('/auth/login/password', {
    account,
    password,
    rememberMe,
  })
}

export function loginByCode(target: string, value: string, code: string, rememberMe = false) {
  return client.post<ApiResponse<AuthResponse>>('/auth/login/code', {
    target,
    value,
    code,
    rememberMe,
  })
}

export function resetPassword(target: string, value: string, code: string, newPassword: string) {
  return client.post<ApiResponse<null>>('/auth/reset-password', {
    target,
    value,
    code,
    newPassword,
  })
}

export function refreshToken(refreshToken: string) {
  return client.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken })
}

export function logout(refreshToken?: string) {
  return client.post<ApiResponse<null>>(
    '/auth/logout',
    { refreshToken },
    {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
      },
    },
  )
}

export function getPasswordPolicy() {
  return client.get<ApiResponse<PasswordPolicy>>('/auth/password-policy')
}

export function getUserInfo() {
  return client.get<ApiResponse<UserInfo>>('/auth/me')
}

export function updateProfile(nickname: string, avatar?: string, bio?: string) {
  return client.put<ApiResponse<null>>('/auth/profile', { nickname, avatar, bio })
}

export function deleteAccount(password: string) {
  return client.delete<ApiResponse<null>>('/auth/account', {
    data: { password },
  })
}
