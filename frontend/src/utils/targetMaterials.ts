import { getProject, updateProjectLinks } from '../api/project'

export async function linkResumeVersionToTarget(targetId: number, resumeVersionId: number) {
  const response = await getProject(targetId)
  return updateProjectLinks(targetId, {
    jobDescriptionId: response.data.jobDescriptionId,
    resumeVersionId,
  })
}
