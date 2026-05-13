import client from './client'
import type { ApiResponse, Note } from '@/types'

export function listNotes(page = 0, size = 20) {
  return client.get<ApiResponse<Note[]>>('/notes', { params: { page, size } })
}

export function getNote(id: number) {
  return client.get<ApiResponse<Note>>(`/notes/${id}`)
}

export function createNote(title: string, content: string, format = 'markdown') {
  return client.post<ApiResponse<Note>>('/notes', { title, content, format })
}

export function updateNote(id: number, title: string, content: string, format = 'markdown') {
  return client.put<ApiResponse<Note>>(`/notes/${id}`, { title, content, format })
}

export function deleteNote(id: number) {
  return client.delete<ApiResponse<null>>(`/notes/${id}`)
}

export function uploadNote(file: File) {
  const form = new FormData()
  form.append('file', file)
  return client.post<ApiResponse<Note>>('/notes/upload', form)
}
