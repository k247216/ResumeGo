import { apiFetch } from './http'
import type {
  ApiResponse,
  CreateJobProjectRequest,
  JobProject,
  UpdateJobProjectLinksRequest,
} from '../types/project'

const BASE = '/api/v1/projects'

async function request<T>(path = '', init: RequestInit = {}, fallback = '求职项目操作失败') {
  const response = await apiFetch(`${BASE}${path}`, init)
  const body = await response.json().catch(() => null)
  if (!response.ok || !body?.success) {
    throw new Error(body?.message || fallback)
  }
  return body as ApiResponse<T>
}

const jsonRequest = (method: string, body: unknown): RequestInit => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
})

export const listProjects = () => request<JobProject[]>('', {}, '获取求职项目失败')
export const getProject = (id: number) => request<JobProject>(`/${id}`, {}, '获取求职项目失败')
export const createProject = (payload: CreateJobProjectRequest) =>
  request<JobProject>('', jsonRequest('POST', payload), '创建求职项目失败')
export const renameProject = (id: number, name: string) =>
  request<JobProject>(`/${id}/name`, jsonRequest('PATCH', { name }), '重命名求职项目失败')
export const updateProjectLinks = (id: number, payload: UpdateJobProjectLinksRequest) =>
  request<JobProject>(`/${id}/links`, jsonRequest('PATCH', payload), '更新求职项目关联失败')
export const archiveProject = (id: number) =>
  request<JobProject>(`/${id}/archive`, { method: 'POST' }, '归档求职项目失败')
export const restoreProject = (id: number) =>
  request<JobProject>(`/${id}/restore`, { method: 'POST' }, '恢复求职项目失败')
export const deleteProject = (id: number) =>
  request<null>(`/${id}`, { method: 'DELETE' }, '删除求职项目失败')
