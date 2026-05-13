<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1 class="app-title">Auraspark</h1>
      </div>
      <nav class="nav">
        <router-link to="/home" class="nav-item">首页</router-link>
        <router-link to="/notes" class="nav-item">笔记</router-link>
        <router-link to="/files" class="nav-item">文件</router-link>
        <router-link to="/chat" class="nav-item">AI 对话</router-link>
      </nav>
      <div class="sidebar-footer">
        <router-link to="/profile" class="nav-item profile-link">
          {{ auth.user?.nickname || '用户' }}
        </router-link>
        <button class="btn-logout" @click="handleLogout">退出</button>
      </div>
    </aside>
    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

onMounted(() => {
  if (!auth.user) {
    auth.fetchUserInfo().catch(() => {})
  }
})

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 200px;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.sidebar-header {
  padding: 20px 16px;
  border-bottom: 1px solid #e0e0e0;
}

.app-title {
  font-size: 18px;
  font-weight: 300;
  letter-spacing: 3px;
}

.nav {
  flex: 1;
  padding: 8px 0;
}

.nav-item {
  display: block;
  padding: 10px 16px;
  color: #333;
  text-decoration: none;
  font-size: 14px;
  border-left: 3px solid transparent;
}

.nav-item:hover,
.nav-item.router-link-active {
  background: #eee;
  border-left-color: #000;
  color: #000;
}

.sidebar-footer {
  border-top: 1px solid #e0e0e0;
  padding: 8px 0;
}

.profile-link {
  font-size: 13px;
}

.btn-logout {
  width: 100%;
  padding: 8px 16px;
  border: none;
  background: none;
  color: #999;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.btn-logout:hover {
  color: #c00;
}

.main {
  flex: 1;
  overflow-y: auto;
}
</style>
