interface InterviewRecordContext {
  jobDescriptionId: number | null
  resumeVersionId: number | null
}

export function filterTargetInterviewRecords<T extends InterviewRecordContext>(
  records: T[],
  jobDescriptionId: number | null,
  resumeVersionId: number | null,
): T[] {
  if (!isPositiveId(jobDescriptionId) || !isPositiveId(resumeVersionId)) return []
  return records.filter((record) => (
    record.jobDescriptionId === jobDescriptionId && record.resumeVersionId === resumeVersionId
  ))
}

function isPositiveId(value: number | null): value is number {
  return Number.isSafeInteger(value) && Number(value) > 0
}
