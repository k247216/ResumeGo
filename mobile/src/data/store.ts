import { reactive, watch } from 'vue'
import type { JobProject, StageEvent, TargetOutcome, TargetStage } from '../types/project'
import { isTerminalStage, normalizeTargetStage, stageFlowRank, TARGET_OUTCOME_LABELS, TARGET_STAGE_LABELS } from '../types/project'
import type { ScheduleEvent } from '../types/schedule'
import { deleteFile, putFile } from './fileStore'
import { resumeMarkOf, type ResumeMarkName } from './resumeMark'

export const DEFAULT_INTERVIEW_ROUNDS = 2

export function normalizeInterviewRounds(value: unknown): number {
  const rounds = Number(value)
  return Number.isFinite(rounds) ? Math.min(8, Math.max(1, Math.round(rounds))) : DEFAULT_INTERVIEW_ROUNDS
}
function normalizeInterviewRound(value: unknown, rounds: number): number {
  const current = Number(value)
  return Number.isFinite(current) ? Math.min(rounds, Math.max(1, Math.round(current))) : 1
}

// 文件型简历：版本＝用户上传的一份 .md / .pdf 原文件；二进制在 IndexedDB，这里只留元数据。
export interface ResumeFile {
  id: number
  title: string
  mark?: ResumeMarkName
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
// 解析失败时把原始字节挪到这里，坏数据仍可人工找回。
const QUARANTINE_KEY = `${DB_KEY}-corrupt`

// 无 localStorage 的环境（SSR / 部分测试环境）降级为内存存储，保证模块可加载。
// 注意：写入抛错时（配额用尽 / WebView 关闭了 DOM 存储）也会落到内存，这份数据重启即丢，
// 所以必须把故障留痕给 UI 提示用户，而不是假装一切正常。
const memoryStore = new Map<string, string>()
const storageFault = { message: null as string | null }
export function storageFaultMessage(): string | null { return storageFault.message }
const storage = {
  get(key: string): string | null {
    try { return localStorage.getItem(key) } catch { return memoryStore.get(key) ?? null }
  },
  set(key: string, value: string) {
    try {
      localStorage.setItem(key, value)
      storageFault.message = null
    } catch {
      memoryStore.set(key, value)
      storageFault.message = '本机存储空间不足或已被禁用，最近的改动只存在于当前会话，退出后会丢失。请尽快导出备份。'
    }
  },
  remove(key: string) {
    try { localStorage.removeItem(key) } catch { /* 内存回退分支由下一行负责 */ }
    memoryStore.delete(key)
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

function emptyDb(): DbShape {
  return { seq: 0, reminders: {}, targets: [], stageEvents: [], schedules: [], resumes: [], versions: [] }
}

/** 任意来源（本地存储 / 用户备份文件）都必须先归一，避免某个集合是 undefined 就让整页崩。 */
function normalizeDb(input: unknown): DbShape {
  const base = emptyDb()
  if (!input || typeof input !== 'object') return base
  const raw = input as Partial<DbShape>
  return {
    targets: Array.isArray(raw.targets) ? raw.targets : [],
    stageEvents: Array.isArray(raw.stageEvents) ? raw.stageEvents : [],
    schedules: Array.isArray(raw.schedules) ? raw.schedules : [],
    resumes: Array.isArray(raw.resumes) ? raw.resumes : [],
    versions: Array.isArray(raw.versions) ? raw.versions : [],
    reminders: raw.reminders && typeof raw.reminders === 'object' ? raw.reminders : {},
    seq: Number.isFinite(Number(raw.seq)) && Number(raw.seq) > 0 ? Number(raw.seq) : 0,
  }
}

function persist() { storage.set(DB_KEY, JSON.stringify(db)) }

function load(): DbShape {
  const raw = storage.get(DB_KEY)
  if (!raw) {
    const fresh = emptyDb()
    storage.set(DB_KEY, JSON.stringify(fresh))
    return fresh
  }
  try {
    return normalizeDb(JSON.parse(raw))
  } catch {
    // 解析失败绝不覆盖真实记录：原始字节隔离留存，本次以空工作区启动。
    storage.set(QUARANTINE_KEY, raw)
    const fresh = emptyDb()
    storage.set(DB_KEY, JSON.stringify(fresh))
    return fresh
  }
}

function hydrate(db: DbShape) {
  for (const target of db.targets) {
    target.interviewRounds = normalizeInterviewRounds(target.interviewRounds)
    target.interviewRound = normalizeInterviewRound(target.interviewRound, target.interviewRounds)
    target.outcome = target.outcome ?? null
    target.outcomeRound = target.outcomeRound == null ? null : normalizeInterviewRound(target.outcomeRound, target.interviewRounds)
  }
  // 心得是后加字段，旧工作区里的日程没有这个键；补成 null 后 UI 层就不必到处判 undefined。
  for (const schedule of db.schedules) {
    schedule.notes = schedule.notes ?? null
    schedule.review = schedule.review ?? null
  }
  for (const resume of db.resumes) {
    resume.mark = resume.mark ?? resumeMarkOf(resume.id)
    const versions = db.versions.filter((v) => v.resumeId === resume.id).sort((a, b) => a.versionNo - b.versionNo)
    if (resume.currentVersionId == null || !versions.some((v) => v.id === resume.currentVersionId)) {
      resume.currentVersionId = versions[versions.length - 1]?.id ?? null
    }
  }
}

const db = reactive(load()) as DbShape
hydrate(db)

watch(db, () => {
  persist()
}, { deep: true })

function nextId(): number { db.seq += 1; return db.seq }

type TargetMutationResult = { ok: boolean; message?: string }
function targetMutationLock(target: JobProject): TargetMutationResult | null {
  if (target.status === 'archived' || isTerminalStage(normalizeTargetStage(target.stage))) {
    return { ok: false, message: '该计划已有最终结果，状态已锁定' }
  }
  return null
}

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
    interviewRounds: DEFAULT_INTERVIEW_ROUNDS, interviewRound: 1, outcome: null, outcomeRound: null,
    createdAt: now, updatedAt: now,
  }
  db.targets.unshift(target)
  db.stageEvents.push({ id: nextId(), targetId: target.id, stage: 'applied', occurredAt: now })
  return target
}
export function renameTarget(id: number, name: string) {
  const t = db.targets.find((x) => x.id === id); if (t) { t.name = name; t.updatedAt = iso(new Date()) }
}
export function setStage(id: number, stage: TargetStage, opts: { allowBackward?: boolean } = {}): { ok: boolean; message?: string } {
  const t = db.targets.find((x) => x.id === id); if (!t) return { ok: false, message: '目标不存在' }
  const cur = normalizeTargetStage(t.stage)
  if (cur === stage) return { ok: true }
  if (isTerminalStage(cur)) return { ok: false, message: '该计划已有最终结果，状态已锁定' }
  // 倒退只在详情里、经用户确认后开放（allowBackward）；卡片正面的轻点仍然是单向的，避免顺手改乱进度。
  if (!opts.allowBackward && stageFlowRank(cur) > 0 && stageFlowRank(stage) > 0 && stageFlowRank(stage) < stageFlowRank(cur)) {
    return { ok: false, message: '阶段只能向前推进，不能回退' }
  }
  t.stage = stage; t.stageUpdatedAt = iso(new Date()); t.updatedAt = iso(new Date())
  db.stageEvents.push({ id: nextId(), targetId: id, stage, occurredAt: iso(new Date()) })
  return { ok: true }
}

/**
 * 一次改动的完整快照。阶段推进、结果标记、归档这类操作一旦落库就很难靠"反着点一遍"还原，
 * 所以统一在动手前拍快照、由提示条的「撤销」整份还原，比给每种操作各写一个逆操作更可靠。
 */
export interface TargetSnapshot { target: JobProject; stageEvents: Array<StageEvent & { targetId: number }> }
export function snapshotTarget(id: number): TargetSnapshot | null {
  const target = db.targets.find((x) => x.id === id)
  if (!target) return null
  return clone({
    target,
    stageEvents: db.stageEvents.filter((e) => e.targetId === id),
  })
}
/** 目标已被删除时无法还原，返回 false 让 UI 如实说明，而不是假装撤销成功。 */
export function restoreTarget(snap: TargetSnapshot): boolean {
  const target = db.targets.find((x) => x.id === snap.target.id)
  if (!target) return false
  const { target: next, stageEvents } = clone(snap)
  Object.assign(target, next)
  db.stageEvents = [
    ...db.stageEvents.filter((e) => e.targetId !== next.id),
    ...stageEvents.map((e) => ({ ...e, targetId: next.id })),
  ]
  return true
}
function clone<T>(value: T): T { return JSON.parse(JSON.stringify(value)) as T }
/** 解除终态锁定：回到该计划时间轴上最后一个非终态阶段，并清空结果标记。 */
export function reopenTarget(id: number): { ok: boolean; message?: string; stage?: TargetStage } {
  const t = db.targets.find((x) => x.id === id); if (!t) return { ok: false, message: '目标不存在' }
  if (t.status === 'archived') return { ok: false, message: '该计划已归档，请先恢复后再解锁' }
  if (!isTerminalStage(normalizeTargetStage(t.stage))) return { ok: false, message: '该计划未被锁定' }
  const events = stageEventsOf(id)
  let prev: TargetStage = 'applied'
  for (let i = events.length - 1; i >= 0; i -= 1) {
    const stage = normalizeTargetStage(events[i].stage)
    if (!isTerminalStage(stage)) { prev = stage; break }
  }
  const now = iso(new Date())
  t.stage = prev; t.outcome = null; t.outcomeRound = null
  t.stageUpdatedAt = now; t.updatedAt = now
  db.stageEvents.push({ id: nextId(), targetId: id, stage: prev, occurredAt: now })
  return { ok: true, stage: prev }
}
export function setTargetStatus(id: number, status: 'active' | 'archived') {
  const t = db.targets.find((x) => x.id === id); if (!t) return
  t.status = status
  t.archivedAt = status === 'archived' ? iso(new Date()) : null
  t.updatedAt = iso(new Date())
}
export function schedulesOfTarget(id: number): ScheduleEvent[] {
  return db.schedules.filter((e) => e.jobProjectId === id).sort((a, b) => a.startTime.localeCompare(b.startTime))
}
export function deleteTarget(id: number): { removedScheduleIds: number[] } {
  const removedScheduleIds = db.schedules.filter((e) => e.jobProjectId === id).map((e) => e.id)
  db.targets = db.targets.filter((x) => x.id !== id)
  db.stageEvents = db.stageEvents.filter((x) => x.targetId !== id)
  db.schedules = db.schedules.filter((e) => e.jobProjectId !== id)
  for (const scheduleId of removedScheduleIds) delete db.reminders[scheduleId]
  return { removedScheduleIds }
}
export function updateApplication(id: number, payload: { industry?: string | null; role?: string | null; location?: string | null; notes?: string | null }) {
  const t = db.targets.find((x) => x.id === id); if (!t) return
  if (payload.industry !== undefined) t.industry = payload.industry
  if (payload.role !== undefined) t.targetRole = payload.role
  if (payload.location !== undefined) t.location = payload.location
  if (payload.notes !== undefined) t.notes = payload.notes
  t.updatedAt = iso(new Date())
}
export function interviewRoundsOf(target: JobProject): number {
  return normalizeInterviewRounds(target.interviewRounds)
}
export function interviewRoundOf(target: JobProject): number {
  return normalizeInterviewRound(target.interviewRound, interviewRoundsOf(target))
}
export function setInterviewRounds(id: number, rounds: number): TargetMutationResult {
  const t = db.targets.find((x) => x.id === id); if (!t) return { ok: false, message: '目标不存在' }
  const locked = targetMutationLock(t); if (locked) return locked
  t.interviewRounds = normalizeInterviewRounds(rounds)
  t.interviewRound = normalizeInterviewRound(t.interviewRound, t.interviewRounds)
  t.updatedAt = iso(new Date())
  return { ok: true }
}
export function setInterviewRound(id: number, round: number): TargetMutationResult {
  const t = db.targets.find((x) => x.id === id); if (!t) return { ok: false, message: '目标不存在' }
  const locked = targetMutationLock(t); if (locked) return locked
  t.interviewRound = normalizeInterviewRound(round, interviewRoundsOf(t))
  t.updatedAt = iso(new Date())
  return { ok: true }
}
export function outcomeLabelOf(target: JobProject): string {
  if (!target.outcome) return TARGET_STAGE_LABELS[normalizeTargetStage(target.stage)]
  if (target.outcome === 'interview_failed') return `面试第 ${normalizeInterviewRound(target.outcomeRound, interviewRoundsOf(target))} 面未通过`
  return TARGET_OUTCOME_LABELS[target.outcome]
}
export function setTargetOutcome(id: number, outcome: TargetOutcome | null, round?: number): TargetMutationResult {
  const t = db.targets.find((x) => x.id === id); if (!t) return { ok: false, message: '目标不存在' }
  const locked = targetMutationLock(t); if (locked) return locked
  t.outcome = outcome
  t.outcomeRound = outcome === 'interview_failed' ? normalizeInterviewRound(round, interviewRoundsOf(t)) : null
  if (outcome) {
    const stage: TargetStage = outcome === 'pool' ? 'pool' : outcome === 'rejected' ? 'rejected' : outcome === 'closed' ? 'closed' : 'screened_out'
    t.stage = stage
    t.stageUpdatedAt = iso(new Date())
    db.stageEvents.push({ id: nextId(), targetId: id, stage, occurredAt: iso(new Date()) })
  }
  t.updatedAt = iso(new Date())
  return { ok: true }
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
/** 所有仍然有效的提醒意图（日程 id + 提前分钟数），启动时据此向系统重新排期。 */
export function eventsWithReminders(): Array<{ event: ScheduleEvent; minutes: number }> {
  return db.schedules
    .filter((e) => (db.reminders[e.id] ?? 0) > 0)
    .map((e) => ({ event: e, minutes: db.reminders[e.id] }))
}
export function setReminder(id: number, minutes: number) {
  if (minutes > 0) db.reminders[id] = minutes
  else delete db.reminders[id]
}
/** 复盘心得独立于 notes：notes 是赛前要看的（会议链接、注意事项），心得是赛后写的，两者生命周期完全不同。 */
export function setScheduleReview(id: number, review: string) {
  const e = db.schedules.find((x) => x.id === id); if (!e) return
  e.review = review.trim() || null
  e.updatedAt = iso(new Date())
}
/** 写过心得的日程，最近一场在前——复盘视图只看这一份，不必把没结束的安排也拉进来。 */
export function listReviews(): ScheduleEvent[] {
  return db.schedules
    .filter((e) => !!e.review?.trim())
    .sort((a, b) => b.startTime.localeCompare(a.startTime))
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
  const id = nextId()
  const resume: ResumeFile = { id, title: name, mark: resumeMarkOf(id), archivedAt: null, currentVersionId: null, createdAt: now, updatedAt: now }
  // 先确认文件真的落盘再挂元数据行，否则失败后会留下一份永远打不开的空简历。
  await addVersion(resume, file, null)
  db.resumes.push(resume)
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
/** 版本备注：这一版投了什么岗、改了什么，只有用户自己知道，所以给足一次改写的机会。 */
export function setVersionNote(resumeId: number, versionId: number, note: string) {
  const ver = db.versions.find((v) => v.id === versionId && v.resumeId === resumeId)
  const resume = getResume(resumeId)
  if (!ver || !resume) return
  ver.note = note.trim() || null
  resume.updatedAt = iso(new Date())
}
export async function deleteResume(resumeId: number) {
  const removed = db.versions.filter((v) => v.resumeId === resumeId)
  // 文件本体清理失败不应阻止元数据删除，否则用户会看到“删除无效”。
  await Promise.all(removed.map((v) => deleteFile(v.fileKey).catch(() => undefined)))
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
export interface RestoreSummary {
  targets: number
  schedules: number
  resumes: number
  reminders: number
}
export function exportBackup(): string { return JSON.stringify(db, null, 2) }
export function importBackup(json: string): { ok: boolean; message?: string; restored?: RestoreSummary } {
  let parsed: unknown
  try { parsed = JSON.parse(json) } catch { return { ok: false, message: '解析备份失败' } }
  const raw = parsed as Partial<DbShape>
  if (!Array.isArray(raw?.targets) || !Array.isArray(raw?.schedules)) {
    return { ok: false, message: '备份格式不正确，未做任何改动' }
  }
  const next = normalizeDb(parsed)
  // 备份里的 seq 可能落后于数据本身，必须抬高到最大 id 之上，否则新建记录会撞已有 id。
  // 用循环而非 Math.max(...spread)：备份文件是外部输入，集合过大时展开参数会抛栈溢出。
  let maxId = next.seq
  for (const list of [next.targets, next.stageEvents, next.schedules, next.resumes, next.versions]) {
    for (const item of list) {
      const id = Number((item as { id?: unknown }).id)
      if (Number.isFinite(id) && id > maxId) maxId = id
    }
  }
  next.seq = maxId
  Object.assign(db, next)
  hydrate(db)
  persist()
  clearQuarantinedData()
  return {
    ok: true,
    restored: {
      targets: next.targets.length,
      schedules: next.schedules.length,
      resumes: next.resumes.length,
      reminders: Object.keys(next.reminders).length,
    },
  }
}

/** 清空本机全部记录（含简历文件本体），用于换机前交付或彻底重来。 */
export async function resetWorkspace(): Promise<{ ok: boolean; message?: string }> {
  try {
    await Promise.all(db.versions.map((v) => deleteFile(v.fileKey).catch(() => undefined)))
  } catch { /* 文件清理失败不阻塞元数据清空 */ }
  Object.assign(db, emptyDb())
  persist()
  clearQuarantinedData()
  return { ok: true }
}

export function hasQuarantinedData(): boolean { return !!storage.get(QUARANTINE_KEY) }
/** 工作区已被整份替换，隔离的坏数据不再有机会被找回，继续留着只会让告警条常亮。 */
export function clearQuarantinedData() { storage.remove(QUARANTINE_KEY) }
