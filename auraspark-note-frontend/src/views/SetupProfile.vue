<template>
  <div class="setup-page">
    <div class="back-btn" @click="router.push('/login')">
      <svg viewBox="0 0 1024 1024" width="28" height="28" fill="#bbb">...</svg>
    </div>
    <div class="setup-container" :class="{ expanded: cropping }">
      <div class="setup-card">
        <h1 class="logo">Auraspark</h1>
        <p class="subtitle">Your stories carry their own light</p>

        <div class="avatar-area">
          <div class="avatar-circle" @click="fileInput?.click()">
            <img v-if="avatarPreview" :src="avatarPreview" class="avatar-img" />
            <span v-else class="avatar-icon">+</span>
          </div>
        </div>

        <div class="nickname-area">
          <span class="nickname-label">{{ t('placeholder.nickname') }}</span>
          <input v-model="nickname" type="text" :placeholder="t('placeholder.setNickname')" class="input" />
        </div>

        <button class="btn-primary" :disabled="loading" @click="handleSave">
          {{ loading ? t('setup.loading') : t('setup.btn') }}
        </button>

        <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/webp" hidden @change="onFileSelect" />
      </div>
      
      <Transition name="slide">
        <div v-if="cropping" class="crop-wrap" key="crop">
          <div class="crop-panel">
            <div class="crop-close" @click="cropping = false">
              <svg viewBox="0 0 1024 1024" width="16" height="16" fill="#bbb"><path d="M585.412525 512.594747L973.601616 124.418586c19.600808-19.600808 19.600808-51.898182 0-71.49899l-2.120404-2.120404c-19.600808-19.600808-51.898182-19.600808-71.49899 0L511.793131 439.518384 123.61697 50.799192c-19.600808-19.600808-51.898182-19.600808-71.49899 0l-2.120404 2.120404c-20.11798 19.600808-20.11798 51.898182 0 71.49899l388.189091 388.189091L49.997576 900.783838c-19.587879 19.600808-19.587879 51.898182 0 71.49899l2.120404 2.120404c19.600808 19.600808 51.898182 19.600808 71.49899 0L511.793131 586.214141l388.189091 388.176162c19.600808 19.600808 51.898182 19.600808 71.49899 0l2.120404-2.120404c19.600808-19.600808 19.600808-51.898182 0-71.49899L585.412525 512.594747z"/></svg>
            </div>
            <p class="crop-title">{{ t('avatar.cropTitle') }}</p>
            <p class="crop-hint">{{ t('avatar.zoomHint') }}</p>
            <AvatarCropper :image-file="cropFile" @preview="onPreview" @change="fileInput?.click()" />
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { updateProfile } from '@/api/auth'
import client from '@/api/client'
import { t, errorMessage } from '@/i18n'
import AvatarCropper from '@/components/AvatarCropper.vue'
import { validateNickname } from '@/utils/validation'

const router = useRouter()
const auth = useAuthStore()

const nickname = ref(!auth.user?.nickname || auth.user.nickname === 'User' ? '' : auth.user.nickname)
const avatarPreview = ref('')
const avatarDataUrl = ref('')
const error = ref('')
watch(error, (val) => { if (val) { ElMessage.error({ message: val, duration: 3000 }); error.value = '' } })
const loading = ref(false)
const cropping = ref(false)
const cropFile = ref<File>()
const fileInput = ref<HTMLInputElement>()

function onFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const f = input.files?.[0]
  if (!f) return
  if (f.size > 10 * 1024 * 1024) { error.value = t('avatar.tooLarge'); return }
  if (!f.type.startsWith('image/')) { error.value = t('avatar.invalidType'); return }
  avatarPreview.value = URL.createObjectURL(f)
  cropFile.value = f
  cropping.value = true
}

function onPreview(dataUrl: string) {
  avatarPreview.value = dataUrl
  avatarDataUrl.value = dataUrl
}

async function handleSave() {
  const nickErr = validateNickname(nickname.value)
  if (nickErr) { error.value = nickErr; return }
  loading.value = true
  try {
    let avatarUrl: string | undefined
    if (avatarDataUrl.value) {
      const blob = await fetch(avatarDataUrl.value).then(r => r.blob())
      const form = new FormData()
      form.append('file', blob, 'avatar.jpg')
      const { data } = await client.post('/upload/avatar', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      avatarUrl = data.data.url
    }
    await updateProfile(nickname.value.trim(), avatarUrl, undefined)
    await auth.fetchUserInfo()
    router.push('/home')
  } catch (e: any) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@keyframes fadeSlideUp {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}

.setup-page { display: flex; align-items: center; justify-content: center; height: 100vh; background: #fafafa; overflow: hidden; }
:global(body) { overflow: hidden; }
.setup-container { display: flex; align-items: center; justify-content: center; }
.setup-container.expanded { gap: 32px; }

.setup-card { width: 400px; padding: 48px 48px 40px; border: 1px solid #e8e8e8; animation: fadeSlideUp 0.5s ease-out; display: flex; flex-direction: column; align-items: center; flex-shrink: 0; background: #fff; box-shadow: 0 8px 32px rgba(0,0,0,0.06); position: relative; z-index: 2; }
.back-btn { position: fixed; top: 32px; left: 32px; cursor: pointer; line-height: 0; transition: fill 0.2s; z-index: 10; filter: drop-shadow(0 2px 8px rgba(0,0,0,0.12)); }
.back-btn:hover svg { fill: #000; }
.logo { font-size: 40px; font-weight: 200; letter-spacing: 10px; margin-bottom: 12px; color: #111; }
.subtitle { font-size: 13px; color: #aaa; letter-spacing: 3px; margin-top: 0; margin-bottom: 48px; font-weight: 300; }

.avatar-area { margin-bottom: 36px; }
.avatar-circle { width: 220px; height: 220px; border-radius: 50%; border: 1px solid #e0e0e0; display: flex; align-items: center; justify-content: center; cursor: pointer; margin: 0 auto; box-shadow: 0 8px 32px rgba(0,0,0,0.06); transition: box-shadow 0.4s ease; background: #fafafa; }
.avatar-circle:hover { box-shadow: 0 12px 40px rgba(0,0,0,0.1); }
.avatar-circle:hover .avatar-icon { font-size: 60px; color: #999999; }
.avatar-icon { font-size: 48px; color: #bbb; transition: all 0.4s ease; }
.avatar-img { width: 100%; height: 100%; object-fit: cover; border-radius: 50%; }

.nickname-area { display: flex; flex-direction: column; align-items: center; gap: 8px; width: 100%; margin-bottom: 32px; margin-top: 20px }
.nickname-label { font-size: 16px; color: #333; font-weight: 400; letter-spacing: 1px; }
.input { width: 80%; padding: 14px 16px; border: 1px solid #e8e8e8; font-size: 20px; outline: none; box-sizing: border-box; background: #fafafa; transition: all 0.25s ease; color: #333; letter-spacing: 1px; text-align: center; }
.input:-webkit-autofill,
.input:-webkit-autofill:hover,
.input:-webkit-autofill:focus {
  -webkit-box-shadow: 0 0 0 1000px #fafafa inset;
  -webkit-text-fill-color: #333;
}
.input::placeholder { font-size: 13px; color: #bbb; }
.input:focus { border-color: #e8e8e8; background: #fff; }

.btn-primary { width: 160px; padding: 14px; border: none; background: #000; color: #fff; font-size: 14px; cursor: pointer; transition: all 0.25s ease; letter-spacing: 2px; font-weight: 300; margin-top: 16px; }
.btn-primary:hover { background: #222; }
.btn-primary:disabled { opacity: 0.3; cursor: default; }
.crop-wrap { display: flex; align-items: center; overflow: hidden; }
.crop-close { position: absolute; top: 32px; right: 36px; cursor: pointer; line-height: 0; z-index: 2; transition: fill 0.2s; }
.crop-close svg { display: block; }
.crop-close:hover svg { fill: #000; }
.crop-panel { position: relative; overflow: hidden; border: 1px solid #e8e8e8; padding: 90px 32px 32px; display: flex; flex-direction: column; align-items: center; justify-content: center; background: #fff; box-shadow: 8px 0 24px rgba(0,0,0,0.06); }
.crop-title { position: absolute; top: 52px; left: 32px; font-size: 22px; color: #111; margin: 0; font-weight: 400; letter-spacing: 2px; }
.crop-hint { font-size: 16px; color: #bbb; align-self: flex-start; margin: 4px 0 24px; letter-spacing: 1px; }
.slide-enter-active { transition: all 0.35s ease; }
.slide-leave-active { transition: all 0.3s ease; }
.slide-enter-from { opacity: 0; transform: translateX(-60px) scale(0.92); }
.slide-leave-to { opacity: 0; transform: translateX(-60px) scale(0.92); width: 0; margin: 0; }
</style>
