<template>
  <div class="reset-page">
    <div class="reset-card">
      <h1 class="logo">Auraspark</h1>
      <p class="subtitle">Your stories carry their own light</p>

      <form @submit.prevent="handleReset" autocomplete="off">
        <input type="text" style="display:none" aria-hidden="true" />
        <input type="password" style="display:none" aria-hidden="true" />
        <input v-model="value" type="text" :placeholder="t('placeholder.account')" class="input" required autocomplete="off" readonly onfocus="this.removeAttribute('readonly')" />
        <div class="code-row">
          <input v-model="code" type="text" :placeholder="t('placeholder.code')" class="input" required autocomplete="off" readonly onfocus="this.removeAttribute('readonly')" />
          <button type="button" class="btn-code" :disabled="codeCountdown > 0" @click="handleSendCode">
            {{ codeCountdown > 0 ? `${codeCountdown}s` : t('btn.sendCode') }}
          </button>
        </div>
        <input v-model="newPassword" type="password" :placeholder="t('placeholder.newPassword')" class="input" required autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" @blur="validatePwd" />
        <input v-model="confirmPassword" type="password" :placeholder="t('placeholder.confirmPassword')" class="input" required autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" />

        <p class="policy-hint">{{ policyMessage }}</p>
        <p v-if="message" class="success">{{ message }}</p>

        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? t('reset.loading') : t('reset.btn') }}
        </button>
      </form>

      <div class="links">
        <router-link to="/login">{{ t('reset.back') }}</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { resetPassword, sendCode, getPasswordPolicy } from '@/api/auth'
import { validateEmail, validatePhone, validateCode, validatePassword } from '@/utils/validation'
import type { PasswordPolicy } from '@/utils/validation'
import { t, errorMessage } from '@/i18n'

const value = ref('')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const error = ref('')
watch(error, (val) => { if (val) { ElMessage.error({ message: val, duration: 3000 }); error.value = '' } })
const message = ref('')
const loading = ref(false)
const codeCountdown = ref(0)
const policy = ref<PasswordPolicy | null>({
  minLength: 8, requireUppercase: true, requireLowercase: true, requireDigit: true, requireSpecial: false,
  pattern: '', message: '',
})
const policyMessage = ref(t('password.rule'))

onMounted(async () => {
  try {
    const { data } = await getPasswordPolicy()
    policy.value = data.data
  } catch {
    // use defaults
  }
})

function validatePwd() {
  if (policy.value) {
    const err = validatePassword(newPassword.value, policy.value)
    if (err) error.value = err
    else error.value = ''
  }
}

async function handleReset() {
  error.value = ''
  message.value = ''

  const valErr = value.value.includes('@') ? validateEmail(value.value) : validatePhone(value.value)
  if (valErr) { error.value = valErr; return }
  const codeErr = validateCode(code.value)
  if (codeErr) { error.value = codeErr; return }

  if (policy.value) {
    const pwdErr = validatePassword(newPassword.value, policy.value)
    if (pwdErr) { error.value = pwdErr; return }
  }

  if (newPassword.value !== confirmPassword.value) {
    error.value = t('error.passwordMismatch')
    return
  }

  loading.value = true
  try {
    await resetPassword('email', value.value, code.value, newPassword.value)
    message.value = t('reset.success')
  } catch (e: any) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

async function handleSendCode() {
  const valErr = value.value.includes('@') ? validateEmail(value.value) : validatePhone(value.value)
  if (valErr) { error.value = valErr; return }
  try {
    await sendCode('email', value.value, 'RESET_PASSWORD')
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

.reset-page { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: #fff; }
.reset-card { width: 380px; padding: 48px; border: 1px solid #e0e0e0; animation: fadeSlideUp 0.5s ease-out; }
.logo { font-size: 44px; font-weight: 300; text-align: center; letter-spacing: 8px; margin-bottom: 12px; animation: fadeSlideUp 0.5s ease-out 0.1s both; }
.subtitle { text-align: center; font-size: 16px; color: #888; letter-spacing: 2px; margin-top: -4px; margin-bottom: 40px; animation: fadeSlideUp 0.5s ease-out 0.2s both; }

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

.policy-hint { font-size: 12px; color: #999; margin-top: -8px; margin-bottom: 16px; line-height: 1.4; }

.btn-primary { width: 100%; padding: 12px; border: 1px solid #000; background: #000; color: #fff; font-size: 15px; cursor: pointer; transition: opacity 0.2s; }
.btn-primary:hover { opacity: 0.85; }
.btn-primary:disabled { opacity: 0.5; }
.success { color: #060; font-size: 14px; margin-bottom: 12px; }
.links { margin-top: 24px; text-align: center; }
.links a { font-size: 14px; color: #666; text-decoration: none; transition: color 0.2s; }
.links a:hover { color: #000; }
</style>
