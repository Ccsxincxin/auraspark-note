export interface ApiResponse<T> {
  code: number
  message: string
  messageCode: string | null
  data: T
  timestamp: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  userId: number
  nickname: string
  avatar: string
  expiresIn: number
}

export interface UserInfo {
  userId: number
  email: string
  phone: string
  nickname: string
  avatar: string
  bio: string
  tokenBalance: number
  totalUsed: number
  totalGranted: number
}

export interface PasswordPolicy {
  minLength: number
  requireUppercase: boolean
  requireLowercase: boolean
  requireDigit: boolean
  requireSpecial: boolean
  pattern: string
  message: string
}

export interface Note {
  id: number
  userId: number
  title: string
  content: string
  format: string
  createdAt: string
  updatedAt: string
}

export interface FileItem {
  id: number
  userId: number
  name: string
  isFolder: boolean
  parentId: number | null
  format: string
  size: number
  url: string
  createdAt: string
  updatedAt: string
  children?: FileItem[]
}

export interface Conversation {
  id: number
  userId: number
  title: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface Message {
  id: number
  conversationId: number
  role: string
  content: string
  tokens: number
  compressed: boolean
  branch: number
  versionOf: number
  deleted: boolean
  createdAt: string
}

export interface BranchMessages {
  branch: number
  active: boolean
  messages: Message[]
}

export interface ChatProvider {
  name: string
  baseUrl: string
  defaultModel: string
}
