<template>
  <div class="files-page">
    <div class="files-header">
      <h2>文件</h2>
      <div class="header-actions">
        <button class="btn-primary" @click="showCreateFolder = true">新建文件夹</button>
        <label class="btn-primary upload-btn">
          上传文件
          <input type="file" hidden @change="handleUpload" />
        </label>
      </div>
    </div>

    <div class="breadcrumb">
      <span class="crumb" @click="navigateTo(undefined)">根目录</span>
      <span v-for="(crumb, i) in breadcrumbs" :key="i" class="crumb-sep">/</span>
      <span v-for="(crumb, i) in breadcrumbs" :key="'c-' + i" class="crumb" @click="navigateTo(crumb.id)">
        {{ crumb.name }}
      </span>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else class="file-list">
      <div
        v-for="item in files"
        :key="item.id"
        class="file-item"
      >
        <div class="file-info" @click="item.isFolder ? navigateTo(item.id) : null">
          <span class="file-icon">{{ item.isFolder ? '[D]' : '[F]' }}</span>
          <span class="file-name">{{ item.name }}</span>
          <span v-if="!item.isFolder" class="file-size">{{ formatSize(item.size) }}</span>
        </div>
        <div class="file-actions">
          <button class="btn-small" @click="startRename(item)">重命名</button>
          <button class="btn-small btn-danger-small" @click="handleDelete(item)">删除</button>
        </div>
      </div>
      <p v-if="files.length === 0" class="empty">空目录</p>
    </div>

    <div v-if="showCreateFolder" class="modal-overlay" @click.self="showCreateFolder = false">
      <div class="modal">
        <h3>新建文件夹</h3>
        <input
          v-model="folderName"
          type="text"
          placeholder="文件夹名称"
          class="input"
          @keyup.enter="handleCreateFolder"
        />
        <div class="modal-actions">
          <button class="btn-secondary" @click="showCreateFolder = false">取消</button>
          <button class="btn-primary" @click="handleCreateFolder">创建</button>
        </div>
      </div>
    </div>

    <div v-if="renaming" class="modal-overlay" @click.self="renaming = null">
      <div class="modal">
        <h3>重命名</h3>
        <input
          v-model="renameName"
          type="text"
          placeholder="新名称"
          class="input"
          @keyup.enter="handleRename"
        />
        <div class="modal-actions">
          <button class="btn-secondary" @click="renaming = null">取消</button>
          <button class="btn-primary" @click="handleRename">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as fileApi from '@/api/file'
import type { FileItem } from '@/types'

const files = ref<FileItem[]>([])
const loading = ref(false)
const currentParentId = ref<number | undefined>(undefined)
const breadcrumbs = ref<{ id: number; name: string }[]>([])
const showCreateFolder = ref(false)
const folderName = ref('')
const renaming = ref<FileItem | null>(null)
const renameName = ref('')

onMounted(() => fetchFiles())

async function fetchFiles() {
  loading.value = true
  try {
    const { data } = await fileApi.listFiles(currentParentId.value)
    files.value = data.data
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

async function navigateTo(id: number | undefined) {
  currentParentId.value = id
  if (id === undefined) {
    breadcrumbs.value = []
  } else {
    try {
      const { data } = await fileApi.getFileDetail(id)
      const item = data.data
      const idx = breadcrumbs.value.findIndex((c) => c.id === id)
      if (idx >= 0) {
        breadcrumbs.value = breadcrumbs.value.slice(0, idx + 1)
      } else {
        breadcrumbs.value.push({ id: item.id, name: item.name })
      }
    } catch {
      // ignore
    }
  }
  await fetchFiles()
}

async function handleCreateFolder() {
  if (!folderName.value) return
  try {
    await fileApi.createFolder(folderName.value, currentParentId.value)
    showCreateFolder.value = false
    folderName.value = ''
    await fetchFiles()
  } catch {
    // ignore
  }
}

async function handleUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    await fileApi.uploadFile(file, currentParentId.value)
    await fetchFiles()
  } catch {
    // ignore
  }
  input.value = ''
}

function startRename(item: FileItem) {
  renaming.value = item
  renameName.value = item.name
}

async function handleRename() {
  if (!renaming.value || !renameName.value) return
  try {
    await fileApi.renameFile(renaming.value.id, renameName.value)
    renaming.value = null
    await fetchFiles()
  } catch {
    // ignore
  }
}

async function handleDelete(item: FileItem) {
  try {
    await fileApi.deleteFile(item.id)
    await fetchFiles()
  } catch {
    // ignore
  }
}

function formatSize(bytes: number) {
  if (!bytes) return ''
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(1)} ${units[i]}`
}
</script>

<style scoped>
.files-page {
  padding: 24px;
}

.files-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.files-header h2 {
  font-size: 20px;
  font-weight: 400;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.breadcrumb {
  font-size: 13px;
  color: #999;
  margin-bottom: 16px;
}

.crumb {
  cursor: pointer;
  color: #666;
}

.crumb:hover {
  color: #000;
}

.crumb-sep {
  margin: 0 4px;
}

.loading,
.empty {
  text-align: center;
  color: #999;
  padding: 40px;
  font-size: 14px;
}

.file-list {
  border: 1px solid #e0e0e0;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #e0e0e0;
}

.file-item:last-child {
  border-bottom: none;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex: 1;
}

.file-icon {
  color: #999;
  font-family: monospace;
  font-size: 12px;
}

.file-name {
  color: #000;
  font-size: 14px;
}

.file-size {
  color: #999;
  font-size: 12px;
  margin-left: auto;
}

.file-actions {
  display: flex;
  gap: 4px;
}

.btn-small {
  padding: 2px 8px;
  border: 1px solid #e0e0e0;
  background: #fff;
  color: #000;
  font-size: 12px;
  cursor: pointer;
}

.btn-danger-small {
  color: #c00;
  border-color: #c00;
}

.btn-primary {
  padding: 6px 16px;
  border: 1px solid #000;
  background: #000;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.upload-btn {
  display: inline-block;
  cursor: pointer;
}

.btn-secondary {
  padding: 6px 16px;
  border: 1px solid #e0e0e0;
  background: #fff;
  color: #000;
  font-size: 13px;
  cursor: pointer;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: #fff;
  padding: 24px;
  width: 400px;
  border: 1px solid #e0e0e0;
}

.modal h3 {
  font-size: 16px;
  font-weight: 400;
  margin-bottom: 16px;
}

.input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e0e0e0;
  font-size: 14px;
  outline: none;
  margin-bottom: 12px;
  box-sizing: border-box;
}

.input:focus {
  border-color: #000;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
