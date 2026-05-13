<template>
  <div class="notes-page">
    <div class="notes-header">
      <h2>笔记</h2>
      <div class="header-actions">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索笔记..."
          class="search-input"
        />
        <button class="btn-primary" @click="showCreate = true">新建</button>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else class="notes-list">
      <div
        v-for="note in filteredNotes"
        :key="note.id"
        class="note-item"
        @click="selectedNote = note; showDetail = true"
      >
        <div class="note-title">{{ note.title || '无标题' }}</div>
        <div class="note-meta">{{ note.format }} / {{ formatTime(note.updatedAt) }}</div>
      </div>
      <p v-if="filteredNotes.length === 0" class="empty">暂无笔记</p>
    </div>

    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal">
        <h3>新建笔记</h3>
        <input v-model="createForm.title" type="text" placeholder="标题" class="input" />
        <textarea v-model="createForm.content" placeholder="内容" class="textarea" rows="8"></textarea>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showCreate = false">取消</button>
          <button class="btn-primary" @click="handleCreate">创建</button>
        </div>
      </div>
    </div>

    <div v-if="showDetail && selectedNote" class="modal-overlay" @click.self="showDetail = false">
      <div class="modal modal-wide">
        <h3>
          <input v-model="editForm.title" type="text" class="input-inline" />
        </h3>
        <textarea v-model="editForm.content" class="textarea" rows="12"></textarea>
        <div class="modal-actions">
          <button class="btn-danger" @click="handleDelete(selectedNote!.id)">删除</button>
          <button class="btn-secondary" @click="showDetail = false">取消</button>
          <button class="btn-primary" @click="handleUpdate">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import * as noteApi from '@/api/note'
import type { Note } from '@/types'

const notes = ref<Note[]>([])
const loading = ref(false)
const selectedNote = ref<Note | null>(null)
const showCreate = ref(false)
const showDetail = ref(false)
const searchQuery = ref('')

const createForm = ref({ title: '', content: '' })
const editForm = ref({ title: '', content: '' })

const filteredNotes = computed(() => {
  if (!searchQuery.value) return notes.value
  const q = searchQuery.value.toLowerCase()
  return notes.value.filter(
    (n) => n.title?.toLowerCase().includes(q) || n.content?.toLowerCase().includes(q),
  )
})

onMounted(() => {
  fetchNotes()
})

async function fetchNotes() {
  loading.value = true
  try {
    const { data } = await noteApi.listNotes(0, 50)
    notes.value = data.data
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  try {
    await noteApi.createNote(createForm.value.title, createForm.value.content)
    showCreate.value = false
    createForm.value = { title: '', content: '' }
    await fetchNotes()
  } catch {
    // ignore
  }
}

function handleUpdate() {
  if (!selectedNote.value) return
  noteApi
    .updateNote(selectedNote.value.id, editForm.value.title, editForm.value.content)
    .then(() => {
      showDetail.value = false
      fetchNotes()
    })
}

function handleDelete(id: number) {
  noteApi.deleteNote(id).then(() => {
    showDetail.value = false
    fetchNotes()
  })
}

function formatTime(t: string) {
  return t?.slice(0, 16).replace('T', ' ')
}
</script>

<style scoped>
.notes-page {
  padding: 24px;
}

.notes-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.notes-header h2 {
  font-size: 20px;
  font-weight: 400;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.search-input {
  width: 200px;
  padding: 6px 10px;
  border: 1px solid #e0e0e0;
  font-size: 13px;
  outline: none;
  background: #fff;
  color: #000;
}

.search-input:focus {
  border-color: #000;
}

.loading,
.empty {
  text-align: center;
  color: #999;
  padding: 40px;
  font-size: 14px;
}

.notes-list {
  display: flex;
  flex-direction: column;
}

.note-item {
  padding: 14px 16px;
  border: 1px solid #e0e0e0;
  margin-bottom: -1px;
  cursor: pointer;
}

.note-item:hover {
  background: #f5f5f5;
}

.note-title {
  font-size: 15px;
  margin-bottom: 4px;
  color: #000;
}

.note-meta {
  font-size: 12px;
  color: #999;
}

.btn-primary {
  padding: 6px 16px;
  border: 1px solid #000;
  background: #000;
  color: #fff;
  font-size: 13px;
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

.btn-danger {
  padding: 6px 16px;
  border: 1px solid #c00;
  background: #fff;
  color: #c00;
  font-size: 13px;
  cursor: pointer;
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

.input-inline {
  width: 100%;
  padding: 4px 0;
  border: none;
  font-size: 18px;
  outline: none;
  border-bottom: 1px solid #e0e0e0;
}

.textarea {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e0e0e0;
  font-size: 14px;
  outline: none;
  margin-bottom: 12px;
  box-sizing: border-box;
  resize: vertical;
  font-family: inherit;
}

.textarea:focus {
  border-color: #000;
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
  width: 480px;
  border: 1px solid #e0e0e0;
}

.modal-wide {
  width: 640px;
}

.modal h3 {
  font-size: 16px;
  font-weight: 400;
  margin-bottom: 16px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
