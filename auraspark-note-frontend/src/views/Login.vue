<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="logo">Auraspark</h1>
      <p class="subtitle">Your stories carry their own light</p>

      <div class="tabs">
        <button :class="['tab', { active: tab === 'password' }]" @click="tab = 'password'">{{ t('login.password') }}</button>
        <button :class="['tab', { active: tab === 'code' }]" @click="tab = 'code'">{{ t('login.code') }}</button>
      </div>

      <form @submit.prevent="handleLogin">
        <Transition name="fade" mode="out-in">
          <div v-if="tab === 'password'" key="password">
            <input v-model="account" type="text" :placeholder="t('placeholder.account')" class="input" required />
            <input v-model="password" type="password" :placeholder="t('placeholder.password')" class="input" required />
            <div class="row-between">
              <label class="checkbox-label">
                <input v-model="rememberMe" type="checkbox" />
                <span>{{ t('label.rememberMe') }}</span>
              </label>
              <router-link to="/reset-password" class="forgot-link">{{ t('label.forgotPassword') }}</router-link>
            </div>
          </div>
          <div v-else key="code">
            <div class="target-tabs">
              <button :class="['target-tab', { active: codeTarget === 'email' }]" type="button" @click="codeTarget = 'email'">{{ t('label.email') }}</button>
              <button :class="['target-tab', { active: codeTarget === 'phone' }]" type="button" @click="codeTarget = 'phone'">{{ t('label.phone') }}</button>
            </div>
            <input v-model="codeValue" :type="codeTarget === 'email' ? 'email' : 'tel'" :placeholder="codeTarget === 'email' ? t('placeholder.email') : t('placeholder.phone')" class="input" required />
            <div class="code-row">
              <input v-model="codeCode" type="text" :placeholder="t('placeholder.code')" class="input" required />
              <button type="button" class="btn-code" :disabled="codeCountdown > 0" @click="handleSendCode('LOGIN')">
                {{ codeCountdown > 0 ? `${codeCountdown}s` : t('btn.sendCode') }}
              </button>
            </div>
            <div class="row-center">
              <label class="checkbox-label">
                <input v-model="rememberMe" type="checkbox" />
                <span>{{ t('label.rememberMe') }}</span>
              </label>
            </div>
          </div>
        </Transition>

        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? t('login.loading') : t('login.btn') }}
        </button>
      </form>

      <div class="links">
        <router-link to="/register">{{ t('login.noAccount') }}</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { sendCode } from '@/api/auth'
import { validateEmail, validatePhone, validateCode } from '@/utils/validation'
import { t, errorMessage } from '@/i18n'

const router = useRouter()
const auth = useAuthStore()

const tab = ref<'password' | 'code'>('password')
const account = ref('')
const password = ref('')
const rememberMe = ref(false)
const codeTarget = ref<'email' | 'phone'>('email')
const codeValue = ref('')
const codeCode = ref('')
const error = ref('')
watch(error, (val) => { if (val) { ElMessage.error({ message: val, duration: 3000 }); error.value = '' } })
const loading = ref(false)
const codeCountdown = ref(0)

function validateAccount(v: string): string | null {
  if (!v) return t('error.required')
  if (v.includes('@')) return validateEmail(v)
  return validatePhone(v)
}

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    if (tab.value === 'password') {
      const err = validateAccount(account.value)
      if (err) { error.value = err; loading.value = false; return }
      await auth.loginByPassword(account.value, password.value, rememberMe.value)
    } else {
      const err1 = codeTarget.value === 'email' ? validateEmail(codeValue.value) : validatePhone(codeValue.value)
      if (err1) { error.value = err1; loading.value = false; return }
      const err2 = validateCode(codeCode.value)
      if (err2) { error.value = err2; loading.value = false; return }
      await auth.loginByCode(codeTarget.value, codeValue.value, codeCode.value, rememberMe.value)
    }
    await auth.fetchUserInfo()
    router.push('/home')
  } catch (e: any) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

async function handleSendCode(type: string) {
  const err = codeTarget.value === 'email' ? validateEmail(codeValue.value) : validatePhone(codeValue.value)
  if (err) { error.value = err; return }
  try {
    await sendCode(codeTarget.value, codeValue.value, type)
    codeCountdown.value = 60
    const timer = setInterval(() => { codeCountdown.value--; if (codeCountdown.value <= 0) clearInterval(timer) }, 1000)
  } catch (e: any) {
    error.value = errorMessage(e)
  }
}
</script>

<style scoped>
@keyframes fadeSlideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.fade-enter-from { opacity: 0; transform: translateX(12px); }
.fade-leave-to { opacity: 0; transform: translateX(-12px); }

.login-page { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: #fff; }
.login-card { width: 380px; padding: 48px; border: 1px solid #e0e0e0; animation: fadeSlideUp 0.5s ease-out; }
.logo { font-size: 44px; font-weight: 300; text-align: center; letter-spacing: 8px; margin-bottom: 12px; animation: fadeSlideUp 0.5s ease-out 0.1s both; }
.subtitle { text-align: center; font-size: 16px; color: #888; letter-spacing: 2px; margin-top: -4px; margin-bottom: 40px; animation: fadeSlideUp 0.5s ease-out 0.2s both; }

.tabs { display: flex; border-bottom: 1px solid #e0e0e0; margin-bottom: 24px; }
.tab { flex: 1; padding: 10px 0; border: none; background: none; font-size: 14px; color: #999; cursor: pointer; border-bottom: 2px solid transparent; transition: all 0.2s; }
.tab.active { color: #000; border-bottom-color: #000; }

.target-tabs { display: flex; gap: 0; margin-bottom: 16px; border: 1px solid #e0e0e0; }
.target-tab { flex: 1; padding: 8px; border: none; background: none; font-size: 14px; color: #999; cursor: pointer; transition: all 0.2s; }
.target-tab.active { color: #000; background: #f5f5f5; }

.input { width: 100%; padding: 12px 14px; border: 1px solid #e0e0e0; font-size: 15px; outline: none; margin-bottom: 16px; box-sizing: border-box; background: #fff; color: #000; transition: border-color 0.2s; }
.input:-webkit-autofill,
.input:-webkit-autofill:hover,
.input:-webkit-autofill:focus {
  -webkit-box-shadow: 0 0 0 1000px #fff inset;
  -webkit-text-fill-color: #000;
}
.input:focus { border-color: #000; }
.code-row { display: flex; gap: 8px; }
.code-row .input { flex: 1; }
.btn-code { white-space: nowrap; min-width: 120px; padding: 12px 14px; border: 1px solid #e0e0e0; background: #fff; color: #000; font-size: 14px; cursor: pointer; height: 44px; box-sizing: border-box; transition: border-color 0.2s; }
.btn-code:hover { border-color: #000; }
.btn-code:disabled { color: #ccc; }

.row-between { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.row-between .checkbox-label { margin-bottom: 0; }
.row-center { display: flex; justify-content: center; margin-bottom: 16px; }
.forgot-link { font-size: 14px; color: #666; text-decoration: none; transition: color 0.2s; }
.forgot-link:hover { color: #000; }
.checkbox-label { display: flex; align-items: center; gap: 6px; font-size: 14px; color: #666; margin-bottom: 16px; }

.btn-primary { width: 100%; padding: 12px; border: 1px solid #000; background: #000; color: #fff; font-size: 15px; cursor: pointer; transition: opacity 0.2s; }
.btn-primary:hover { opacity: 0.85; }
.btn-primary:disabled { opacity: 0.5; }
.links { margin-top: 24px; text-align: center; }
.links a { font-size: 14px; color: #666; text-decoration: none; transition: color 0.2s; }
.links a:hover { color: #000; }
</style>
