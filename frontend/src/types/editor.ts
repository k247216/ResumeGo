export type EditorSectionType =
  | 'personal_info'
  | 'summary'
  | 'work_experience'
  | 'education'
  | 'skills'
  | 'projects'
  | 'certifications'
  | 'languages'
  | 'github'
  | 'qr_codes'
  | 'custom'

export type EditorSectionStatus = 'ready' | 'warning' | 'empty'

export interface EditorField {
  key: string
  label: string
  value: string
  control?: 'text' | 'select'
  options?: EditorFieldOption[]
}

export type EditorFieldOption = string | {
  label: string
  value: string
}

export interface EditorSection {
  id: string
  type: EditorSectionType
  title: string
  subtitle: string
  status: EditorSectionStatus
  visible: boolean
  fields: EditorField[]
  chips: string[]
  paragraphs: string[]
  paragraphLabels?: string[]
  items?: EditorSectionItem[]
  addLabel?: string
  meta?: string
}

export interface EditorSectionItem {
  id: string
  title: string
  description: string
  fields?: EditorField[]
  descriptionKey?: string
  descriptionLabel?: string
  listFields?: EditorListField[]
  evidenceLabel?: string
}

export interface EditorListField {
  key: string
  label: string
  value: string[]
  placeholder?: string
}

export interface EditorModuleOption {
  id: string
  type: EditorSectionType
  title: string
}
