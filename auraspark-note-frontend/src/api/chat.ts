import client from './client'
import type { ApiResponse, Conversation, BranchMessages, ChatProvider } from '@/types'

export function chat(
  message: string,
  apiKey: string,
  conversationId?: number,
  provider?: string,
  model?: string,
  baseUrl?: string,
  branch?: number,
) {
  return client.post<ApiResponse<{ response: string }>>('/ai/chat', {
    message,
    apiKey,
    conversationId,
    provider,
    model,
    baseUrl,
    branch,
  })
}

export function listConversations() {
  return client.get<ApiResponse<Conversation[]>>('/ai/chat/conversations')
}

export function deleteConversation(id: number) {
  return client.delete<ApiResponse<null>>(`/ai/chat/conversations/${id}`)
}

export function updateConversationTitle(id: number, title: string) {
  return client.put<ApiResponse<null>>(`/ai/chat/conversations/${id}/title`, { title })
}

export function getMessages(id: number) {
  return client.get<ApiResponse<{ branches: BranchMessages[] }>>(
    `/ai/chat/conversations/${id}/messages`,
  )
}

export function editMessage(
  conversationId: number,
  messageId: number,
  content: string,
  apiKey: string,
  provider?: string,
  model?: string,
  baseUrl?: string,
) {
  return client.put<ApiResponse<{ response: string }>>(
    `/ai/chat/conversations/${conversationId}/messages/${messageId}`,
    { content, apiKey, provider, model, baseUrl },
  )
}

export function deleteMessage(conversationId: number, messageId: number) {
  return client.delete<ApiResponse<null>>(
    `/ai/chat/conversations/${conversationId}/messages/${messageId}`,
  )
}

export function getProviders() {
  return client.get<ApiResponse<ChatProvider[]>>('/ai/chat/providers')
}
