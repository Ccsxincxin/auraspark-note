<template>
  <div class="chat-page">
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <h3>对话</h3>
        <button class="btn-new" @click="handleNewChat">新建</button>
      </div>
      <div class="conv-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          :class="['conv-item', { active: currentConvId === conv.id }]"
        >
          <div class="conv-title" @click="selectConversation(conv.id)">
            {{ conv.title || `对话 ${conv.id}` }}
          </div>
          <button class="btn-del" @click="handleDeleteConv(conv.id)">x</button>
        </div>
      </div>
      <div class="provider-config">
        <select v-model="provider" class="select">
          <option value="DEEPSEEK">DeepSeek</option>
          <option value="OPENAI">OpenAI</option>
        </select>
        <input v-model="apiKey" type="password" placeholder="API Key" class="input-sm" />
        <input v-model="model" type="text" placeholder="模型 (可选)" class="input-sm" />
      </div>
    </div>
    <div class="chat-main">
      <div v-if="!currentConvId" class="chat-empty">选择一个对话或新建对话</div>
      <template v-else>
        <div class="messages" ref="messagesRef">
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['msg', msg.role === 'user' ? 'msg-user' : 'msg-assistant']"
          >
            <div class="msg-role">{{ msg.role === 'user' ? '你' : 'AI' }}</div>
            <div class="msg-content">{{ msg.content }}</div>
          </div>
          <div v-if="chatLoading" class="msg msg-assistant">
            <div class="msg-role">AI</div>
            <div class="msg-content typing">思考中...</div>
          </div>
        </div>
        <div class="input-area">
          <textarea
            v-model="inputMessage"
            class="msg-input"
            placeholder="输入消息..."
            rows="3"
            @keydown.enter.prevent="handleSend"
          ></textarea>
          <button class="btn-send" :disabled="!inputMessage.trim() || !apiKey" @click="handleSend">
            发送
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as chatApi from '@/api/chat'
import type { Conversation, Message } from '@/types'

const conversations = ref<Conversation[]>([])
const messages = ref<Message[]>([])
const currentConvId = ref<number | undefined>(undefined)
const inputMessage = ref('')
const chatLoading = ref(false)
const provider = ref('DEEPSEEK')
const apiKey = ref(localStorage.getItem('chatApiKey') || '')
const model = ref('')
const messagesRef = ref<HTMLDivElement | null>(null)

onMounted(() => {
  fetchConversations()
})

async function scrollToBottom() {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

async function fetchConversations() {
  try {
    const { data } = await chatApi.listConversations()
    conversations.value = data.data
  } catch {
    // ignore
  }
}

async function selectConversation(id: number) {
  currentConvId.value = id
  await fetchMessages(id)
}

async function fetchMessages(id: number) {
  try {
    const { data } = await chatApi.getMessages(id)
    const all: Message[] = []
    for (const b of data.data.branches) {
      if (b.active) {
        all.push(...b.messages.filter((m) => !m.deleted))
      }
    }
    messages.value = all
    await scrollToBottom()
  } catch {
    // ignore
  }
}

async function handleNewChat() {
  currentConvId.value = undefined
  messages.value = []
  inputMessage.value = ''
}

async function handleSend() {
  if (!inputMessage.value.trim() || !apiKey.value) return
  const msg = inputMessage.value
  inputMessage.value = ''
  chatLoading.value = true

  messages.value.push({
    id: Date.now(),
    conversationId: currentConvId.value || 0,
    role: 'user',
    content: msg,
    tokens: 0,
    compressed: false,
    branch: 1,
    versionOf: 0,
    deleted: false,
    createdAt: new Date().toISOString(),
  })
  await scrollToBottom()

  localStorage.setItem('chatApiKey', apiKey.value)

  try {
    const { data } = await chatApi.chat(
      msg,
      apiKey.value,
      currentConvId.value,
      provider.value,
      model.value || undefined,
    )
    if (!currentConvId.value) {
      await fetchConversations()
      if (conversations.value.length > 0) {
        currentConvId.value = conversations.value[0].id
      }
    }
    await fetchMessages(currentConvId.value!)
  } catch {
    messages.value.push({
      id: Date.now() + 1,
      conversationId: currentConvId.value || 0,
      role: 'assistant',
      content: '请求失败，请检查 API Key 和网络连接',
      tokens: 0,
      compressed: false,
      branch: 1,
      versionOf: 0,
      deleted: false,
      createdAt: new Date().toISOString(),
    })
  } finally {
    chatLoading.value = false
    await scrollToBottom()
  }
}

async function handleDeleteConv(id: number) {
  try {
    await chatApi.deleteConversation(id)
    if (currentConvId.value === id) {
      currentConvId.value = undefined
      messages.value = []
    }
    await fetchConversations()
  } catch {
    // ignore
  }
}
</script>

<style scoped>
.chat-page {
  display: flex;
  height: calc(100vh - 60px);
}

.chat-sidebar {
  width: 260px;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.sidebar-header h3 {
  font-size: 14px;
  font-weight: 400;
}

.btn-new {
  padding: 4px 12px;
  border: 1px solid #000;
  background: #000;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}

.conv-list {
  flex: 1;
  overflow-y: auto;
}

.conv-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #e0e0e0;
  cursor: pointer;
}

.conv-item:hover,
.conv-item.active {
  background: #eee;
}

.conv-title {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.btn-del {
  border: none;
  background: none;
  color: #999;
  cursor: pointer;
  font-size: 12px;
  padding: 0 4px;
}

.provider-config {
  padding: 12px 16px;
  border-top: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.select {
  padding: 6px 8px;
  border: 1px solid #e0e0e0;
  background: #fff;
  font-size: 13px;
  outline: none;
}

.input-sm {
  padding: 6px 8px;
  border: 1px solid #e0e0e0;
  font-size: 13px;
  outline: none;
}

.input-sm:focus {
  border-color: #000;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: #999;
  font-size: 14px;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.msg {
  margin-bottom: 16px;
  max-width: 70%;
}

.msg-user {
  margin-left: auto;
}

.msg-assistant {
  margin-right: auto;
}

.msg-role {
  font-size: 11px;
  color: #999;
  margin-bottom: 4px;
}

.msg-content {
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-user .msg-content {
  background: #000;
  color: #fff;
  border-color: #000;
}

.msg-assistant .msg-content {
  background: #fff;
  color: #000;
}

.typing {
  color: #999;
}

.input-area {
  display: flex;
  padding: 16px;
  border-top: 1px solid #e0e0e0;
  gap: 8px;
}

.msg-input {
  flex: 1;
  padding: 10px;
  border: 1px solid #e0e0e0;
  font-size: 14px;
  outline: none;
  resize: none;
  font-family: inherit;
}

.msg-input:focus {
  border-color: #000;
}

.btn-send {
  padding: 10px 20px;
  border: 1px solid #000;
  background: #000;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  align-self: flex-end;
}

.btn-send:disabled {
  opacity: 0.5;
}
</style>
