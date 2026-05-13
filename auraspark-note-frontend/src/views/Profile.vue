<template>
  <div class="profile-page">
    <div class="profile-card">
      <h2>{{ t('profile.title') }}</h2>

      <div v-if="editing">
        <AvatarCropper @uploaded="onAvatarUploaded" />
        <input v-model="editNickname" type="text" :placeholder="t('placeholder.setNickname')" class="input" />
        <input v-model="editBio" type="text" :placeholder="t('placeholder.bio')" class="input" />
        <div class="modal-actions">
          <button class="btn-secondary" @click="cancelEdit">{{ t('profile.cancel') }}</button>
          <button class="btn-primary" :disabled="saving" @click="saveProfile">{{ saving ? t('setup.loading') : t('profile.save') }}</button>
        </div>
      </div>

      <div v-else-if="user" class="info-grid">
        <div class="avatar-section">
          <img v-if="user.avatar" :src="user.avatar" class="avatar" alt="avatar" />
          <div v-else class="avatar-placeholder">{{ user.nickname?.[0] || '?' }}</div>
          <button class="btn-edit" @click="startEdit">{{ t('profile.edit') }}</button>
        </div>
        <div class="info-row"><span class="label">{{ t('placeholder.nickname') }}</span><span class="value">{{ user.nickname }}</span></div>
        <div class="info-row"><span class="label">Email</span><span class="value">{{ user.email || '-' }}</span></div>
        <div class="info-row"><span class="label">{{ t('placeholder.phone') }}</span><span class="value">{{ user.phone || '-' }}</span></div>
        <div class="info-row"><span class="label">Bio</span><span class="value">{{ user.bio || '-' }}</span></div>
        <div class="info-row"><span class="label">Token</span><span class="value">{{ user.tokenBalance }}</span></div>
      </div>
      <div v-else class="loading">{{ t('profile.loading') }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { updateProfile } from '@/api/auth'
import { t, errorMessage } from '@/i18n'
import AvatarCropper from '@/components/AvatarCropper.vue'

const auth = useAuthStore()
const user = computed(() => auth.user)

const editing = ref(false)
const editNickname = ref('')
const editBio = ref('')
const editAvatar = ref('')
const saving = ref(false)
const error = ref('')
watch(error, (val) => { if (val) { ElMessage.error({ message: val, duration: 3000 }); error.value = '' } })

onMounted(() => {
  if (!auth.user) {
    auth.fetchUserInfo().catch(() => {})
  }
})

function startEdit() {
  if (!user.value) return
  editNickname.value = user.value.nickname || ''
  editBio.value = user.value.bio || ''
  editAvatar.value = ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  error.value = ''
}

function onAvatarUploaded(url: string) {
  editAvatar.value = url
}

async function saveProfile() {
  if (!editNickname.value.trim()) return
  error.value = ''
  saving.value = true
  try {
    await updateProfile(
      editNickname.value.trim(),
      editAvatar.value || undefined,
      editBio.value || undefined,
    )
    await auth.fetchUserInfo()
    editing.value = false
  } catch (e: any) {
    error.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-page { padding: 40px; display: flex; justify-content: center; }
.profile-card { width: 480px; border: 1px solid #e0e0e0; padding: 32px; }
.profile-card h2 { font-size: 18px; font-weight: 400; margin-bottom: 24px; }

.avatar-section { display: flex; flex-direction: column; align-items: center; margin-bottom: 24px; }
.avatar { width: 100px; height: 100px; border-radius: 50%; object-fit: cover; border: 1px solid #e0e0e0; }
.avatar-placeholder { width: 100px; height: 100px; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; font-size: 32px; color: #999; border: 1px solid #e0e0e0; }
.btn-edit { margin-top: 8px; padding: 4px 12px; border: 1px solid #e0e0e0; background: #fff; font-size: 13px; cursor: pointer; color: #666; transition: border-color 0.2s; }
.btn-edit:hover { border-color: #000; color: #000; }

.info-grid { display: flex; flex-direction: column; }
.info-row { display: flex; padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
.label { width: 80px; color: #999; font-size: 14px; }
.value { color: #000; font-size: 14px; }

.input { width: 100%; padding: 10px 12px; border: 1px solid #e0e0e0; font-size: 14px; outline: none; margin-bottom: 12px; box-sizing: border-box; }
.input:-webkit-autofill,
.input:-webkit-autofill:hover,
.input:-webkit-autofill:focus {
  -webkit-box-shadow: 0 0 0 1000px #fff inset;
  -webkit-text-fill-color: #333;
}
.input:focus { border-color: #000; }

.modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
.btn-primary { padding: 8px 20px; border: 1px solid #000; background: #000; color: #fff; font-size: 14px; cursor: pointer; }
.btn-primary:disabled { opacity: 0.5; }
.btn-secondary { padding: 8px 20px; border: 1px solid #e0e0e0; background: #fff; color: #000; font-size: 14px; cursor: pointer; }

.loading { text-align: center; color: #999; padding: 40px; }
</style>
