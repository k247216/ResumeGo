export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string | null
}

export interface Resume {
  id: number
  title: string
  targetJobDescriptionId?: number | null
  currentVersion?: ResumeVersion | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface ResumeVersion {
  id: number
  resumeId: number
  parentVersionId?: number | null
  versionNo: number
  content: ResumeContent
  changeSummary?: string | null
  createdByType: string
  createdAt: string
}

export interface CreateResumeVersionRequest {
  content: ResumeContent
  changeSummary?: string
}

export interface CreateResumeRequest {
  title: string
  content: ResumeContent
  changeSummary?: string
  targetJobDescriptionId?: number | null
}

export interface UpdateResumeTargetJobRequest {
  targetJobDescriptionId?: number | null
}

export interface ScoreDeduction {
  code: string
  dimension?: string
  reason: string
  points: number
  rule?: string
  suggestion?: string
  [key: string]: unknown
}


export interface ResumeContent {
  basicInfo?: {
    name?: string
    title?: string
    targetRole?: string
    phone?: string
    email?: string
    age?: string
    gender?: string
    politicalStatus?: string
    ethnicity?: string
    hometown?: string
    maritalStatus?: string
    yearsOfExperience?: string
    educationLevel?: string
    highestEducation?: string
    wechat?: string
    location?: string
    website?: string
  }
  summary?: string
  workExperience?: WorkExperienceItem[]
  education?: EducationItem[]
  projects?: ProjectItem[]
  skills?: string[]
  skillCategories?: SkillCategory[]
  certifications?: CertificationItem[]
  languages?: LanguageItem[]
  githubProjects?: GithubItem[]
  qrCodes?: QrCodeItem[]
  customSections?: CustomSectionItem[]
  hiddenSections?: string[]
  activeSections?: string[]
  [key: string]: unknown
}

export interface EducationItem {
  school?: string
  institution?: string
  major?: string
  field?: string
  degree?: string
  period?: string
  startDate?: string
  endDate?: string
  gpa?: string
  highlights?: string[]
  [key: string]: unknown
}

export interface ProjectItem {
  title?: string
  name?: string
  description?: string
  technologies?: string[]
  highlights?: string[]
  evidenceId?: number
  [key: string]: unknown
}

export interface WorkExperienceItem {
  company?: string
  position?: string
  location?: string
  startDate?: string
  endDate?: string | null
  description?: string
  technologies?: string[]
  highlights?: string[]
  [key: string]: unknown
}

export interface SkillCategory {
  name?: string
  skills?: string[]
  [key: string]: unknown
}

export interface CertificationItem {
  name?: string
  issuer?: string
  date?: string
  description?: string
  [key: string]: unknown
}

export interface LanguageItem {
  name?: string
  level?: string
  description?: string
  [key: string]: unknown
}

export interface GithubItem {
  name?: string
  url?: string
  description?: string
  technologies?: string[]
  [key: string]: unknown
}

export interface QrCodeItem {
  label?: string
  url?: string
  [key: string]: unknown
}

export interface CustomSectionItem {
  title?: string
  description?: string
  [key: string]: unknown
}
