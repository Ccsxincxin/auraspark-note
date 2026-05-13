import { t } from '@/i18n'

export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
export const PHONE_RE = /^1[3-9]\d{9}$/

export interface PasswordPolicy {
  minLength: number
  requireUppercase: boolean
  requireLowercase: boolean
  requireDigit: boolean
  requireSpecial: boolean
  pattern: string
  message: string
}

export function validateEmail(v: string): string | null {
  return EMAIL_RE.test(v) ? null : t('error.email')
}

export function validatePhone(v: string): string | null {
  return PHONE_RE.test(v) ? null : t('error.phone')
}

export function validateCode(v: string): string | null {
  return /^\d{6}$/.test(v) ? null : t('error.code')
}

export function validateNickname(v: string): string | null {
  if (!v || !v.trim()) return t('error.required') + t('placeholder.nickname')
  if (v.trim().length > 16) return t('error.nicknameLength')
  return null
}

export function validatePassword(v: string, policy?: PasswordPolicy): string | null {
  if (!v) return t('error.required') + t('placeholder.password')
  if (policy) {
    if (v.length < policy.minLength) return `${t('error.required')}${policy.minLength}${t('error.required')}`
    if (policy.requireUppercase && !/[A-Z]/.test(v)) return 'error.password.uppercase'
    if (policy.requireLowercase && !/[a-z]/.test(v)) return 'error.password.lowercase'
    if (policy.requireDigit && !/\d/.test(v)) return 'error.password.digit'
    if (policy.requireSpecial && !/[^a-zA-Z0-9]/.test(v)) return 'error.password.special'
  }
  return null
}
