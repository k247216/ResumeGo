import type { JobDescription } from '../types/job'
import type { InterviewStatusResponse } from '../types/interview'

export interface InterviewPlanContext {
  planId: string
  sessionId: number
  resumeVersionId: number
  resumeLabel: string
  jobDescriptionId: number
  jobLabel: string
  personaIds: number[]
  personaNames: string[]
  currentPersonaIndex: number
  createdAt?: string | null
}

export interface InterviewRecord {
  id: string
  title: string
  subtitle: string
  dateLabel: string
  sessions: InterviewStatusResponse[]
  latestSession: InterviewStatusResponse
  job: JobDescription | null
  resumeLabel: string
  completedCount: number
  totalCount: number
  isCompleted: boolean
  isInProgress: boolean
  jobDescriptionId: number | null
  resumeVersionId: number | null
}

interface BuildInterviewRecordsInput {
  sessions: InterviewStatusResponse[]
  plansBySessionId: Record<number, InterviewPlanContext>
  jobs: JobDescription[]
  deletedSessionIds: ReadonlySet<number>
}

export type InterviewRoundPresentationStatus = 'completed' | 'failed' | 'cancelled' | 'active'
export type InterviewRecordPresentationStatus = 'completed' | 'failed' | 'cancelled' | 'active'

export function interviewRoundStatus(
  session: InterviewStatusResponse,
): InterviewRoundPresentationStatus {
  if (session.completed === true && session.status === 'COMPLETED') return 'completed'
  if (session.status === 'FAILED') return 'failed'
  if (session.status === 'CANCELLED') return 'cancelled'
  return 'active'
}

export function interviewRecordStatus(
  record: InterviewRecord,
): InterviewRecordPresentationStatus {
  if (record.isCompleted) return 'completed'
  if (record.sessions.some((session) => interviewRoundStatus(session) === 'failed')) return 'failed'
  if (record.sessions.some((session) => interviewRoundStatus(session) === 'cancelled')) return 'cancelled'
  return 'active'
}

export function interviewRecordProgress(
  record: Pick<InterviewRecord, 'completedCount' | 'totalCount'>,
): number {
  if (record.totalCount <= 0) return 0
  return Math.min(100, Math.max(0, Math.round((record.completedCount / record.totalCount) * 100)))
}

export function buildInterviewRecords({
  sessions,
  plansBySessionId,
  jobs,
  deletedSessionIds,
}: BuildInterviewRecordsInput): InterviewRecord[] {
  const grouped = new Map<string, InterviewStatusResponse[]>()

  for (const session of sessions) {
    if (deletedSessionIds.has(session.sessionId)) continue
    const plan = plansBySessionId[session.sessionId]
    const recordId = plan?.planId || `legacy_${session.sessionId}`
    grouped.set(recordId, [...(grouped.get(recordId) ?? []), session])
  }

  return [...grouped.entries()]
    .map(([id, groupedSessions]) => {
      const sortedSessions = [...groupedSessions].sort((left, right) => {
        const leftIndex = plansBySessionId[left.sessionId]?.currentPersonaIndex ?? Number.MAX_SAFE_INTEGER
        const rightIndex = plansBySessionId[right.sessionId]?.currentPersonaIndex ?? Number.MAX_SAFE_INTEGER
        return leftIndex === rightIndex ? left.sessionId - right.sessionId : leftIndex - rightIndex
      })
      const latestSession = sortedSessions[sortedSessions.length - 1]
      const plan = sortedSessions
        .map((session) => plansBySessionId[session.sessionId])
        .find((candidate) => candidate !== undefined)
      const completedCount = sortedSessions.filter(
        (session) => interviewRoundStatus(session) === 'completed',
      ).length
      const totalCount = Math.max(plan?.personaIds.length ?? sortedSessions.length, sortedSessions.length)

      return {
        id,
        title: plan?.jobLabel ?? '本次多轮面试',
        subtitle: plan?.personaNames.join(' / ')
          ?? sortedSessions.map((session) => session.personaName || '面试官').join(' / '),
        dateLabel: formatPlanDate(plan?.createdAt),
        sessions: sortedSessions,
        latestSession,
        job: plan
          ? jobs.find((item) => item.id === plan.jobDescriptionId) ?? null
          : null,
        resumeLabel: plan?.resumeLabel ?? '未知简历版本',
        completedCount,
        totalCount,
        isCompleted: totalCount > 0 && completedCount >= totalCount,
        isInProgress: completedCount < totalCount,
        jobDescriptionId: plan?.jobDescriptionId ?? null,
        resumeVersionId: plan?.resumeVersionId ?? null,
      }
    })
    .sort((left, right) => right.latestSession.sessionId - left.latestSession.sessionId)
}

/** 记录行日期：来自面试计划的创建时间；缺失或非法时不渲染。 */
function formatPlanDate(value: string | null | undefined): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getMonth() + 1}月${date.getDate()}日`
}
