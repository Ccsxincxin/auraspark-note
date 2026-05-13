import client from './client'
import type { ApiResponse, FileItem } from '@/types'

export function listFiles(parentId?: number) {
  return client.get<ApiResponse<FileItem[]>>('/files', {
    params: { parentId },
  })
}

export function createFolder(name: string, parentId?: number) {
  return client.post<ApiResponse<FileItem>>('/files/folder', { name, parentId })
}

export function uploadFile(file: File, parentId?: number) {
  const form = new FormData()
  form.append('file', file)
  if (parentId != null) form.append('parentId', String(parentId))
  return client.post<ApiResponse<FileItem>>('/files/upload', form)
}

export function renameFile(id: number, name: string) {
  return client.put<ApiResponse<FileItem>>(`/files/${id}`, { name })
}

export function deleteFile(id: number) {
  return client.delete<ApiResponse<null>>(`/files/${id}`)
}

export function getFileTree() {
  return client.get<ApiResponse<FileItem[]>>('/files/tree')
}

export function getFileDetail(id: number) {
  return client.get<ApiResponse<FileItem>>(`/files/${id}`)
}
