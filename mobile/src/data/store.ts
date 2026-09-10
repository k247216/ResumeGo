import { reactive, watch } from 'vue'
import type { JobProject, StageEvent, TargetStage } from '../types/project'
import { isTerminalStage, normalizeTargetStage, stageFlowRank } from '../types/project'
import type { ScheduleEvent } from '../types/schedule'
import { deleteFile, putFile } from './fileStore'

// 文件型简历：版本＝用户上传的一份 .md / .pdf 原文件；二进制在 IndexedDB，这里只留元数据。
export interface ResumeFile {
  id: number
  title: string
  archivedAt: string | null
  currentVersionId: number | null
  createdAt: string
  updatedAt: string
}
export interface ResumeFileVer {
  id: number
  resumeId: number
  versionNo: number
  fileName: string
  mime: string
  size: number
  fileKey: string
  note: string | null
  createdAt: string
}

const DB_KEY = 'zhida-mobile-db-v2'

// 无 localStorage 的环境（SSR / 部分测试环境）降级为内存存储，保证模块可加载。
const memoryStore = new Map<string, string>()
const storage = {
  get(key: string): string | null {
    try { return localStorage.getItem(key) } catch { return memoryStore.get(key) ?? null }
  },
  set(key: string, value: string) {
    try { localStorage.setItem(key, value) } catch { memoryStore.set(key, value) }
  },
}

interface DbShape {
  targets: JobProject[]
  stageEvents: Array<StageEvent & { targetId: number }>
  schedules: ScheduleEvent[]
  resumes: ResumeFile[]
  versions: ResumeFileVer[]
  reminders: Record<number, number>
  seq: number
}

function iso(d: Date): string { return d.toISOString() }
function daysFromNow(n: number, hour = 10, minute = 0): string {
  const d = new Date()
  d.setDate(d.getDate() + n)
  d.setHours(hour, minute, 0, 0)
  return iso(d)
}

function seed(): DbShape {
  const now = iso(new Date())
  return {
    seq: 100,
    reminders: { 21: 30, 22: 60 },
    targets: [
      { id: 1, name: '字节跳动 · 前端开发', status: 'active', stage: 'interview', jobDescriptionId: 1, resumeVersionId: null, archivedAt: null, stageUpdatedAt: daysFromNow(-2), industry: '互联网', targetRole: '前端', location: '北京', notes: '', createdAt: daysFromNow(-20), updatedAt: now },
      { id: 2, name: '腾讯 · 后端开发', status: 'active', stage: 'applied', jobDescriptionId: null, resumeVersionId: null, archivedAt: null, stageUpdatedAt: daysFromNow(-5), industry: '互联网', targetRole: '后端', location: '深圳', notes: '', createdAt: daysFromNow(-8), updatedAt: now },
      { id: 3, name: '美团 · 算法工程师', status: 'active', stage: 'offer', jobDescriptionId: null, resumeVersionId: null, archivedAt: null, stageUpdatedAt: daysFromNow(-1), industry: '本地生活', targetRole: '算法', location: '上海', notes: '', createdAt: daysFromNow(-40), updatedAt: now },
    ],
    stageEvents: [
      { id: 11, targetId: 1, stage: 'applied', occurredAt: daysFromNow(-20) },
      { id: 12, targetId: 1, stage: 'exam', occurredAt: daysFromNow(-12) },
      { id: 13, targetId: 1, stage: 'interview', occurredAt: daysFromNow(-2) },
      { id: 14, targetId: 2, stage: 'applied', occurredAt: daysFromNow(-5) },
      { id: 15, targetId: 3, stage: 'applied', occurredAt: daysFromNow(-40) },
      { id: 16, targetId: 3, stage: 'offer', occurredAt: daysFromNow(-1) },
    ],
    schedules: [
      { id: 21, title: '字节跳动 一面', eventType: 'interview', startTime: daysFromNow(0, 14, 0), endTime: daysFromNow(0, 15, 0), notes: '腾讯会议', jobDescriptionId: 1, jobProjectId: 1, createdAt: now, updatedAt: now },
      { id: 22, title: '腾讯 笔试', eventType: 'exam', startTime: daysFromNow(1, 19, 0), endTime: daysFromNow(1, 21, 0), notes: '', jobDescriptionId: null, jobProjectId: 2, createdAt: now, updatedAt: now },
      { id: 23, title: '跟进美团 Offer 意向', eventType: 'followup', startTime: daysFromNow(3, 10, 0), endTime: null, notes: '', jobDescriptionId: null, jobProjectId: 3, createdAt: now, updatedAt: now },
    ],
    // 简历是“你自己上传的文件”，示例数据不含任何虚构简历，交给空态引导。
    resumes: [],
    versions: [],
  }
}

function load(): DbShape {
  try {
    const raw = storage.get(DB_KEY)
    if (raw) return JSON.parse(raw) as DbShape
  } catch { /* 损坏则回落到种子 */ }
  const fresh = seed()
  storage.set(DB_KEY, JSON.stringify(fresh))
  return fresh
}

function hydrate(db: DbShape) {
  for (const resume of db.resumes) {
    const versions = db.versions.filter((v) => v.resumeId === resume.id).sort((a, b) => a.versionNo - b.versionNo)
    if (resume.currentVersionId == null || !versions.some((v) => v.id === resume.currentVersionId)) {
      resume.currentVersionId = versions[versions.length - 1]?.id ?? null
    }
  }
}

const db = reactive(load()) as DbShape
hydrate(db)

watch(db, () => {
  storage.set(DB_KEY, JSON.stringify(db))
}, { deep: true })

function nextId(): number { db.seq += 1; return db.seq }

// ── 求职目标 ──
export const targets = () => db.targets
export function listTargets(): JobProject[] { return db.targets }
export function createTarget(name: string, opts: { jobDescriptionId?: number | null; resumeVersionId?: number | null } = {}): JobProject {
  const now = iso(new Date())
  const target: JobProject = {
    id: nextId(), name, status: 'active', stage: 'applied',
    jobDescriptionId: opts.jobDescriptionId ?? null,
    resumeVersionId: opts.resumeVersionId ?? null,
    archivedAt: null, stageUpdatedAt: now,
    industry: null, targetRole: null, location: null, notes: null,
    createdAt: now, updatedAt: now,
  }
  db.targets.unshift(target)
  db.stageEvents.push({ id: nextId(), targetId: target.id, stage: 'applied', occurredAt: now })
  return target
}
export function renameTarget(id: number, name: string) {
  const t = db.targets.find((x) => x.id === id); if (t) { t.name = name; t.updatedAt = iso(new Date()) }
}
export function setStage(id: number, stage: TargetStage): { ok: boolean; message?: string } {
  const t = db.targets.find((x) => x.id === id); if (!t) return { ok: false, message: '目标不存在' }
  const cur = normalizeTargetStage(t.stage)
  if (cur === stage) return { ok: true }
  if (isTerminalStage(cur)) return { ok: false, message: '该计划已有最终结果，状态已锁定' }
  if (stageFlowRank(cur) > 0 && stageFlowRank(stage) > 0 && stageFlowRank(stage) < stageFlowRank(cur)) {
    return { ok: false, message: '阶段只能向前推进，不能回退' }
  }
  t.stage = stage; t.stageUpdatedAt = iso(new Date()); t.updatedAt = iso(new Date())
  db.stageEvents.push({ id: nextId(), targetId: id, stage, occurredAt: iso(new Date()) })
  return { ok: true }
}
export function setTargetStatus(id: number, status: 'active' | 'archived') {
  const t = db.targets.find((x) => x.id === id); if (!t) return
  t.status = status
  t.archivedAt = status === 'archived' ? iso(new Date()) : null
  t.updatedAt = iso(new Date())
}
export function deleteTarget(id: number) {
  db.targets = db.targets.filter((x) => x.id !== id)
  db.stageEvents = db.stageEvents.filter((x) => x.targetId !== id)
}
export function updateApplication(id: number, payload: { industry?: string | null; role?: string | null; location?: string | null; notes?: string | null }) {
  const t = db.targets.find((x) => x.id === id); if (!t) return
  if (payload.industry !== undefined) t.industry = payload.industry
  if (payload.role !== undefined) t.targetRole = payload.role
  if (payload.location !== undefined) t.location = payload.location
  if (payload.notes !== undefined) t.notes = payload.notes
  t.updatedAt = iso(new Date())
}
export function stageEventsOf(id: number): StageEvent[] {
  return db.stageEvents.filter((e) => e.targetId === id).sort((a, b) => a.occurredAt.localeCompare(b.occurredAt))
}
export function linkResume(id: number, resumeVersionId: number | null) {
  const t = db.targets.find((x) => x.id === id); if (t) { t.resumeVersionId = resumeVersionId; t.updatedAt = iso(new Date()) }
}

// ── 日程 ──
export function listSchedules(): ScheduleEvent[] {
  return [...db.schedules].sort((a, b) => a.startTime.localeCompare(b.startTime))
}
export function createSchedule(req: Omit<ScheduleEvent, 'id' | 'createdAt' | 'updatedAt'>): ScheduleEvent {
  const now = iso(new Date())
  const event: ScheduleEvent = { ...req, id: nextId(), createdAt: now, updatedAt: now }
  db.schedules.push(event)
  return event
}
export function updateSchedule(id: number, req: Partial<Omit<ScheduleEvent, 'id' | 'createdAt' | 'updatedAt'>>) {
  const e = db.schedules.find((x) => x.id === id); if (!e) return
  Object.assign(e, req, { updatedAt: iso(new Date()) })
}
export function deleteSchedule(id: number) {
  db.schedules = db.schedules.filter((x) => x.id !== id)
  delete db.reminders[id]
}
export function getReminder(id: number): number { return db.reminders[id] ?? 0 }
export function setReminder(id: number, minutes: number) {
  if (minutes > 0) db.reminders[id] = minutes
  else delete db.reminders[id]
}

// ── 简历（导入文件版）──
export function listResumes(): ResumeFile[] { return db.resumes.filter((r) => !r.archivedAt) }
export function getResume(resumeId: number): ResumeFile | undefined { return db.resumes.find((r) => r.id === resumeId) }
export function versionsOf(resumeId: number): ResumeFileVer[] {
  return db.versions.filter((v) => v.resumeId === resumeId).sort((a, b) => a.versionNo - b.versionNo)
}
export function versionById(versionId: number): ResumeFileVer | undefined {
  return db.versions.find((v) => v.id === versionId)
}
export function currentVersionOf(resumeId: number): ResumeFileVer | null {
  const r = getResume(resumeId)
  return r ? versionById(r.currentVersionId ?? -1) ?? null : null
}
export function resumeLabel(versionId: number | null): string | null {
  if (versionId == null) return null
  const v = versionById(versionId); if (!v) return null
  const r = getResume(v.resumeId)
  return r ? `${r.title} · V${v.versionNo}` : `V${v.versionNo}`
}

function detectMime(file: File): string {
  if (file.type) return file.type
  const lower = file.name.toLowerCase()
  if (lower.endsWith('.md') || lower.endsWith('.markdown')) return 'text/markdown'
  if (lower.endsWith('.pdf')) return 'application/pdf'
  if (lower.endsWith('.txt')) return 'text/plain'
  return 'application/octet-stream'
}

async function addVersion(resume: ResumeFile, file: File, note: string | null): Promise<ResumeFileVer> {
  const versionNo = versionsOf(resume.id).length + 1
  const fileKey = `resume-${resume.id}-v${versionNo}-${Date.now()}`
  await putFile(fileKey, file)
  const ver: ResumeFileVer = {
    id: nextId(), resumeId: resume.id, versionNo,
    fileName: file.name, mime: detectMime(file), size: file.size,
    fileKey, note, createdAt: iso(new Date()),
  }
  db.versions.push(ver)
  resume.currentVersionId = ver.id
  resume.updatedAt = iso(new Date())
  return ver
}

/** 导入一份文件新建一份简历（title 为空时取文件名去掉扩展名）。 */
export async function importResume(title: string, file: File): Promise<ResumeFile> {
  const now = iso(new Date())
  const name = title.trim() || file.name.replace(/\.[^.]+$/, '') || '未命名简历'
  const resume: ResumeFile = { id: nextId(), title: name, archivedAt: null, currentVersionId: null, createdAt: now, updatedAt: now }
  db.resumes.push(resume)
  await addVersion(resume, file, '初始版本')
  return resume
}

/** 给已有简历追加“最新一版”＝再传一份新文件。 */
export async function addResumeVersion(resumeId: number, file: File, note?: string): Promise<ResumeFileVer | null> {
  const resume = getResume(resumeId)
  return resume ? addVersion(resume, file, note?.trim() || null) : null
}

export function setCurrentVersion(resumeId: number, versionId: number) {
  const resume = getResume(resumeId)
  const version = db.versions.find((v) => v.id === versionId && v.resumeId === resumeId)
  if (resume && version) { resume.currentVersionId = versionId; resume.updatedAt = iso(new Date()) }
}
export function renameResume(resumeId: number, title: string) {
  const r = getResume(resumeId); if (r) { r.title = title; r.updatedAt = iso(new Date()) }
}
export async function deleteResume(resumeId: number) {
  const removed = db.versions.filter((v) => v.resumeId === resumeId)
  await Promise.all(removed.map((v) => deleteFile(v.fileKey)))
  db.resumes = db.resumes.filter((r) => r.id !== resumeId)
  db.versions = db.versions.filter((v) => v.resumeId !== resumeId)
  for (const t of db.targets) { if (t.resumeVersionId != null && removed.some((v) => v.id === t.resumeVersionId)) t.resumeVersionId = null }
}
export async function deleteResumeVersion(resumeId: number, versionId: number) {
  const ver = db.versions.find((v) => v.id === versionId && v.resumeId === resumeId)
  if (!ver) return
  await deleteFile(ver.fileKey)
  db.versions = db.versions.filter((v) => v.id !== versionId)
  const resume = getResume(resumeId)
  if (resume) {
    if (resume.currentVersionId === versionId) resume.currentVersionId = versionsOf(resumeId).slice(-1)[0]?.id ?? null
    for (const t of db.targets) { if (t.resumeVersionId === versionId) t.resumeVersionId = null }
    resume.updatedAt = iso(new Date())
  }
}

// ── 备份 / 恢复 ──
export function exportBackup(): string { return JSON.stringify(db, null, 2) }
export function importBackup(json: string): { ok: boolean; message?: string } {
  try {
    const parsed = JSON.parse(json) as DbShape
    if (!Array.isArray(parsed.targets) || !Array.isArray(parsed.schedules)) return { ok: false, message: '备份格式不正确' }
    Object.assign(db, parsed)
    hydrate(db)
    return { ok: true }
  } catch { return { ok: false, message: '解析备份失败' } }
}
export function resetToSeed() {
  Object.assign(db, seed())
  hydrate(db)
}
