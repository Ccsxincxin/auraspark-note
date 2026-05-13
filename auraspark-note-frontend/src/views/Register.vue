<template>
  <div class="register-page">
    <div class="register-card">
      <h1 class="logo">Auraspark</h1>
      <p class="subtitle">Your stories carry their own light</p>

      <Transition name="fade" mode="out-in">
        <div v-if="step === 'choose'" key="choose">
          <p class="step-hint">{{ t('register.step') }}</p>
          <button class="btn-choice" @click="selectTarget('email')">{{ t('register.email') }}</button>
          <button class="btn-choice" @click="selectTarget('phone')">{{ t('register.phone') }}</button>
        </div>

        <form v-else key="form" @submit.prevent="handleRegister" autocomplete="off">
          <input type="text" style="display:none" aria-hidden="true" />
          <input type="password" style="display:none" aria-hidden="true" />
          <input v-model="value" type="text" :placeholder="targetType === 'email' ? t('placeholder.email') : t('placeholder.phone')" class="input" required autocomplete="off" readonly onfocus="this.removeAttribute('readonly')" />
          <div class="code-row">
            <input v-model="code" type="text" :placeholder="t('placeholder.code')" class="input" required autocomplete="off" readonly onfocus="this.removeAttribute('readonly')" />
            <button type="button" class="btn-code" :disabled="codeCountdown > 0" @click="handleSendCode">
              {{ codeCountdown > 0 ? `${codeCountdown}s` : t('btn.sendCode') }}
            </button>
          </div>
          <input v-model="password" type="password" :placeholder="t('placeholder.password')" class="input" required autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" />
          <input v-model="confirmPassword" type="password" :placeholder="t('placeholder.confirmPassword')" class="input" required autocomplete="new-password" readonly onfocus="this.removeAttribute('readonly')" />

          <p class="policy-hint">{{ policyMessage }}</p>

          <button type="submit" class="btn-primary" :disabled="loading">
            {{ loading ? t('register.loading') : t('register.btn') }}
          </button>
          <button type="button" class="btn-back" @click="step = 'choose'">{{ t('btn.back') }}</button>
        </form>
      </Transition>

      <div class="links">
        <router-link to="/login">{{ t('register.hasAccount') }}</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { sendCode, getPasswordPolicy } from '@/api/auth'
import { validateEmail, validatePhone, validateCode, validatePassword } from '@/utils/validation'
import type { PasswordPolicy } from '@/utils/validation'
import { t, errorMessage } from '@/i18n'

const router = useRouter()
const auth = useAuthStore()

const step = ref<'choose' | 'form'>('choose')
const targetType = ref<'email' | 'phone'>('email')
const value = ref('')
const password = ref('')
const confirmPassword = ref('')
const code = ref('')
const error = ref('')
watch(error, (val) => { if (val) { ElMessage.error({ message: val, duration: 3000 }); error.value = '' } })
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

function selectTarget(type: 'email' | 'phone') {
  targetType.value = type
  step.value = 'form'
}

function validatePwd() {
  if (policy.value) {
    const err = validatePassword(password.value, policy.value)
    if (err) error.value = err
    else error.value = ''
  }
}

async function handleRegister() {
  error.value = ''

  const valErr = targetType.value === 'email' ? validateEmail(value.value) : validatePhone(value.value)
  if (valErr) { error.value = valErr; return }
  const codeErr = validateCode(code.value)
  if (codeErr) { error.value = codeErr; return }

  if (policy.value) {
    const pwdErr = validatePassword(password.value, policy.value)
    if (pwdErr) { error.value = pwdErr; return }
  }

  if (password.value !== confirmPassword.value) {
    error.value = t('error.passwordMismatch')
    return
  }

  loading.value = true
  try {
    await auth.register(targetType.value, value.value, code.value, password.value, '')
    await auth.fetchUserInfo()
    router.push('/setup-profile')
  } catch (e: any) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

async function handleSendCode() {
  const valErr = targetType.value === 'email' ? validateEmail(value.value) : validatePhone(value.value)
  if (valErr) { error.value = valErr; return }
  try {
    await sendCode(targetType.value, value.value, 'REGISTER')
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

.register-page { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: #fff; }
.register-card { width: 380px; padding: 48px; border: 1px solid #e0e0e0; animation: fadeSlideUp 0.5s ease-out; }
.logo { font-size: 44px; font-weight: 300; text-align: center; letter-spacing: 8px; margin-bottom: 12px; animation: fadeSlideUp 0.5s ease-out 0.1s both; }
.subtitle { text-align: center; font-size: 16px; color: #888; letter-spacing: 2px; margin-top: -4px; margin-bottom: 40px; animation: fadeSlideUp 0.5s ease-out 0.2s both; }

.step-hint { text-align: center; font-size: 14px; color: #999; margin-bottom: 24px; }
.btn-choice { width: 100%; padding: 14px; border: 1px solid #e0e0e0; background: #fff; color: #000; font-size: 15px; cursor: pointer; margin-bottom: 12px; transition: border-color 0.2s; }
.btn-choice:hover { border-color: #000; }

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

.btn-primary { width: 100%; padding: 12px; border: 1px solid #000; background: #000; color: #fff; font-size: 15px; cursor: pointer; transition: opacity 0.2s; margin-bottom: 8px; }
.btn-primary:hover { opacity: 0.85; }
.btn-primary:disabled { opacity: 0.5; }
.btn-back { width: 100%; padding: 12px; border: 1px solid #e0e0e0; background: #fff; color: #666; font-size: 14px; cursor: pointer; transition: border-color 0.2s; }
.btn-back:hover { border-color: #000; color: #000; }
.links { margin-top: 24px; text-align: center; }
.links a { font-size: 14px; color: #666; text-decoration: none; transition: color 0.2s; }
.links a:hover { color: #000; }
</style>
