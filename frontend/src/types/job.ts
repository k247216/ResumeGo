/** 对应 API CreateJobDescriptionRequest */
export interface CreateJobDescriptionRequest {
  jobTitle: string
  companyName?: string
  rawText: string
  sourceMetaJson?: string
  jobType?: string
}

/** 对应 API ParsedJobDescription */
export interface ParsedJobDescription {
  requiredSkills: string[]
  preferredSkills: string[]
  responsibilities: string[]
  experienceRequirements: string[]
  educationRequirements: string[]
}

/** 岗位来源元数据 */
export interface SourceMeta {
  base?: string
  salary?: string
  salaryMin?: number
  salaryMax?: number
  salaryAvg?: number
  platform?: string
  sourceUrl?: string
  logoUrl?: string
  logo?: string
  iconUrl?: string
  companyLogo?: string
  companyLogoUrl?: string
  companyDomain?: string
  companyWebsite?: string
  domain?: string
  website?: string
  industry?: string
  workType?: string
  companySize?: string
  collectedAt?: string
  education?: string
  experience?: string
  views?: number
  applications?: number
  tags?: string[]
}

/** 对应 API JobDescription */
export interface JobDescription {
  id: number
  jobTitle: string
  companyName?: string
  rawText: string
  parsed?: ParsedJobDescription | null
  parseStatus: 'pending' | 'succeeded' | 'failed'
  promptVersion?: string | null
  sourceMeta?: SourceMeta | null
  jobType?: string | null
  createdAt: string
  updatedAt: string
}

/** 公司偏好 Profile：仅用于 AI 建议和面试建议的表达参考，不参与评分、排序或状态机 */
export interface CompanyProfile {
  companyName?: string
  sourceType?: string[]
  sourceNote?: string | null
  preferenceTags?: string[]
  writingStyle?: string | null
  interviewFocus?: string[]
  resumeAdviceRules?: string[]
  confidenceLevel?: string | null
  lastVerifiedAt?: string | null
}

/** API 统一响应包装 */
export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string | null
}

/** API 错误响应 */
export interface ApiError {
  success: boolean
  data: null
  message: string
}
