import { computed, ref } from 'vue'
import {
  createResume,
  createResumeVersion,
  getResumeVersion,
  getResumeVersions,
} from '../api/resume'
import type { EditorModuleOption, EditorSection, EditorSectionItem, EditorSectionStatus } from '../types/editor'
import type { ResumeContent, ResumeVersion } from '../types/resume'

export interface ResumeEditorLoadContext {
  resumeId?: number | null
  versionId?: number | null
  mode?: 'blank'
}

const moduleCatalog: EditorModuleOption[] = [
  { id: 'personal-info', type: 'personal_info', title: '个人信息' },
  { id: 'summary', type: 'summary', title: '个人简介' },
  { id: 'work-experience', type: 'work_experience', title: '工作经历' },
  { id: 'education', type: 'education', title: '教育背景' },
  { id: 'skills', type: 'skills', title: '技能特长' },
  { id: 'projects', type: 'projects', title: '项目经历' },
  { id: 'certifications', type: 'certifications', title: '资格证书' },
  { id: 'languages', type: 'languages', title: '语言能力' },
  { id: 'github', type: 'github', title: 'GitHub 项目' },
  { id: 'qr-codes', type: 'qr_codes', title: '二维码' },
  { id: 'custom', type: 'custom', title: '自定义模块' },
]

const defaultSections = ['personal-info', 'summary', 'education', 'skills', 'projects']

type CollectionConfig = {
  key: keyof ResumeContent
  titleKeys: string[]
  fields: Array<[string, string]>
  lists?: Array<[string, string]>
  descriptionKey?: string
  descriptionLabel?: string
  addLabel: string
}

const collectionConfigs: Record<string, CollectionConfig> = {
  'work-experience': { key: 'workExperience', titleKeys: ['position', 'company'], fields: [['company', '公司'], ['position', '职位'], ['location', '地点'], ['startDate', '开始时间'], ['endDate', '结束时间']], lists: [['technologies', '技术栈'], ['highlights', '亮点成果']], descriptionKey: 'description', descriptionLabel: '职责与主要成就', addLabel: '添加工作经历' },
  education: { key: 'education', titleKeys: ['school', 'institution'], fields: [['school', '学校'], ['major', '专业'], ['degree', '学历'], ['gpa', 'GPA'], ['period', '时间']], lists: [['highlights', '在校亮点']], addLabel: '添加教育背景' },
  skills: { key: 'skillCategories', titleKeys: ['name'], fields: [['name', '技能类别']], lists: [['skills', '技能项']], addLabel: '添加技能类别' },
  projects: { key: 'projects', titleKeys: ['title', 'name'], fields: [['title', '项目名称']], lists: [['technologies', '技术栈'], ['highlights', '项目亮点']], descriptionKey: 'description', descriptionLabel: '项目描述', addLabel: '添加项目经历' },
  certifications: { key: 'certifications', titleKeys: ['name'], fields: [['name', '证书名称'], ['issuer', '颁发机构'], ['date', '获得时间']], descriptionKey: 'description', descriptionLabel: '说明', addLabel: '添加证书' },
  languages: { key: 'languages', titleKeys: ['name'], fields: [['name', '语言'], ['level', '熟练度']], descriptionKey: 'description', descriptionLabel: '说明', addLabel: '添加语言能力' },
  github: { key: 'githubProjects', titleKeys: ['name'], fields: [['name', '项目名称'], ['url', '仓库链接']], lists: [['technologies', '技术栈']], descriptionKey: 'description', descriptionLabel: '项目说明', addLabel: '添加 GitHub 项目' },
  'qr-codes': { key: 'qrCodes', titleKeys: ['label'], fields: [['label', '名称'], ['url', '链接']], addLabel: '添加二维码' },
  custom: { key: 'customSections', titleKeys: ['title'], fields: [['title', '标题']], descriptionKey: 'description', descriptionLabel: '内容', addLabel: '添加自定义内容' },
}

const sectionCopy: Record<string, { subtitle: string }> = {
  'personal-info': { subtitle: '姓名、岗位与联系方式' },
  summary: { subtitle: '集中表达求职定位和核心优势' },
  'work-experience': { subtitle: '公司、岗位、职责和成果' },
  education: { subtitle: '学校、专业、学历和时间' },
  skills: { subtitle: '按类别维护技能与能力关键词' },
  projects: { subtitle: '只记录可说明来源的真实项目事实' },
  certifications: { subtitle: '证书、奖项和认证信息' },
  languages: { subtitle: '语言、熟练度和证明' },
  github: { subtitle: '开源项目、仓库链接和技术说明' },
  'qr-codes': { subtitle: '作品集、博客等二维码入口' },
  custom: { subtitle: '补充其他求职相关内容' },
}

export function useResumeEditor() {
  const loading = ref(false)
  const saving = ref(false)
  const errorMessage = ref('')
  const resumeId = ref<number | null>(null)
  const resumeTitle = ref('新简历草稿')
  const versions = ref<ResumeVersion[]>([])
  const selectedVersionId = ref<number | null>(null)
  const blank = ref(true)
  const draft = ref<ResumeContent>(createBlankContent())
  const baseSnapshot = ref(snapshot(draft.value))
  const undoStack = ref<string[]>([])
  const redoStack = ref<string[]>([])

  const sections = computed(() => buildSections(draft.value))
  const activeIds = computed(() => resolveActiveSections(draft.value))
  const availableModules = computed(() => moduleCatalog.filter((module) => !activeIds.value.includes(module.id)))
  const dirty = computed(() => snapshot(draft.value) !== baseSnapshot.value)
  const versionLabel = computed(() => {
    if (blank.value) return '新简历草稿'
    const version = versions.value.find((item) => item.id === selectedVersionId.value)
    return version ? `v${version.versionNo}` : '未选择版本'
  })
  const updatedAt = computed(() => versions.value.find((item) => item.id === selectedVersionId.value)?.createdAt ?? null)
  const canUndo = computed(() => undoStack.value.length > 0)
  const canRedo = computed(() => redoStack.value.length > 0)

  async function load(context: ResumeEditorLoadContext) {
    loading.value = true
    errorMessage.value = ''
    try {
      if (context.mode === 'blank' || (!context.resumeId && !context.versionId)) {
        setBlankDraft()
        return
      }
      let version: ResumeVersion | null = null
      if (context.versionId) version = (await getResumeVersion(context.versionId)).data
      const resolvedResumeId = version?.resumeId ?? context.resumeId ?? null
      if (!resolvedResumeId) throw new Error('缺少可读取的简历版本')
      const versionResponse = await getResumeVersions(resolvedResumeId)
      versions.value = versionResponse.data
      version = version ?? versions.value.find((item) => item.id === context.versionId) ?? versions.value[0] ?? null
      if (!version) throw new Error('该简历还没有可编辑版本')
      resumeId.value = resolvedResumeId
      resumeTitle.value = `简历 #${resolvedResumeId}`
      blank.value = false
      selectedVersionId.value = version.id
      replaceDraft(version.content)
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '读取简历失败'
    } finally {
      loading.value = false
    }
  }

  async function save() {
    if (!dirty.value || saving.value) return
    saving.value = true
    errorMessage.value = ''
    try {
      if (blank.value) {
        const response = await createResume({
          title: deriveTitle(draft.value),
          content: clone(draft.value),
          changeSummary: '创建本地简历初始版本',
          targetJobDescriptionId: null,
        })
        resumeId.value = response.data.id
        resumeTitle.value = response.data.title
        blank.value = false
        versions.value = (await getResumeVersions(response.data.id)).data
        selectedVersionId.value = response.data.currentVersion?.id ?? versions.value[0]?.id ?? null
      } else {
        if (!resumeId.value) throw new Error('缺少简历标识，无法保存')
        const response = await createResumeVersion(resumeId.value, {
          content: clone(draft.value),
          changeSummary: `人工编辑：基于 ${versionLabel.value} 保存`,
        })
        versions.value = (await getResumeVersions(resumeId.value)).data
        selectedVersionId.value = response.data.id
      }
      baseSnapshot.value = snapshot(draft.value)
      undoStack.value = []
      redoStack.value = []
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '保存简历失败'
      throw error
    } finally {
      saving.value = false
    }
  }

  function updateField(_sectionId: string, fieldKey: string, value: string) {
    mutate((next) => setByPath(next, fieldKey, value))
  }

  function updateParagraph(sectionId: string, index: number, value: string) {
    if (sectionId === 'summary') mutate((next) => { next.summary = value })
    else if (sectionId === 'projects') mutate((next) => {
      const items = arrayOfRecords(next.projects)
      items[index] = { ...(items[index] ?? {}), description: value }
      next.projects = items
    })
  }

  function updateChips(sectionId: string, value: string) {
    if (sectionId !== 'skills') return
    mutate((next) => { next.skills = splitList(value) })
  }

  function updateListItem(_sectionId: string, fieldKey: string, index: number, value: string) {
    mutate((next) => {
      const list = listByPath(next, fieldKey)
      list[index] = value
      setByPath(next, fieldKey, list)
      syncFlatSkills(next, fieldKey)
    })
  }

  function addListItem(_sectionId: string, fieldKey: string) {
    mutate((next) => { const list = listByPath(next, fieldKey); list.push(''); setByPath(next, fieldKey, list) })
  }

  function removeListItem(_sectionId: string, fieldKey: string, index: number) {
    mutate((next) => { const list = listByPath(next, fieldKey); list.splice(index, 1); setByPath(next, fieldKey, list); syncFlatSkills(next, fieldKey) })
  }

  function moveListItem(_sectionId: string, fieldKey: string, index: number, direction: 'up' | 'down') {
    mutate((next) => {
      const list = listByPath(next, fieldKey)
      moveEntry(list, index, direction)
      setByPath(next, fieldKey, list)
      syncFlatSkills(next, fieldKey)
    })
  }

  function addItem(sectionId: string) {
    const config = collectionConfigs[sectionId]
    if (!config) return
    mutate((next) => {
      const items = arrayOfRecords(next[config.key])
      next[config.key] = [...items, emptyItem(sectionId)] as never
      syncFlatSkills(next, sectionId === 'skills' ? 'skillCategories' : '')
    })
  }

  function removeItem(sectionId: string, index: number) {
    const config = collectionConfigs[sectionId]
    if (!config) return
    mutate((next) => {
      const items = arrayOfRecords(next[config.key]); items.splice(index, 1); next[config.key] = items as never
      syncFlatSkills(next, sectionId === 'skills' ? 'skillCategories' : '')
    })
  }

  function moveItem(sectionId: string, index: number, direction: 'up' | 'down') {
    const config = collectionConfigs[sectionId]
    if (!config) return
    mutate((next) => { const items = arrayOfRecords(next[config.key]); moveEntry(items, index, direction); next[config.key] = items as never })
  }

  function toggleVisibility(sectionId: string) {
    mutate((next) => {
      const hidden = new Set(stringArray(next.hiddenSections))
      hidden.has(sectionId) ? hidden.delete(sectionId) : hidden.add(sectionId)
      next.hiddenSections = [...hidden]
    })
  }

  function addModule(sectionId: string) {
    mutate((next) => { next.activeSections = ordered([...resolveActiveSections(next), sectionId]) })
  }

  function removeModule(sectionId: string) {
    if (sectionId === 'personal-info') return
    mutate((next) => { next.activeSections = resolveActiveSections(next).filter((id) => id !== sectionId) })
  }

  function moveModule(sectionId: string, direction: 'up' | 'down') {
    mutate((next) => {
      const ids = resolveActiveSections(next)
      moveEntry(ids, ids.indexOf(sectionId), direction)
      next.activeSections = ids
    })
  }

  function reset() { replaceDraft(JSON.parse(baseSnapshot.value) as ResumeContent, false) }
  function undo() { restoreHistory(undoStack.value, redoStack.value) }
  function redo() { restoreHistory(redoStack.value, undoStack.value) }

  function switchVersion(versionId: number) {
    if (dirty.value) return false
    const version = versions.value.find((item) => item.id === versionId)
    if (!version) return false
    selectedVersionId.value = version.id
    replaceDraft(version.content)
    return true
  }

  function setBlankDraft() {
    resumeId.value = null
    resumeTitle.value = '新简历草稿'
    versions.value = []
    selectedVersionId.value = null
    blank.value = true
    replaceDraft(createBlankContent())
  }

  function mutate(change: (next: ResumeContent) => void) {
    const before = snapshot(draft.value)
    const next = clone(draft.value)
    change(next)
    const after = snapshot(next)
    if (after === before) return
    undoStack.value.push(before)
    if (undoStack.value.length > 50) undoStack.value.shift()
    redoStack.value = []
    draft.value = next
  }

  function replaceDraft(content: ResumeContent, updateBase = true) {
    draft.value = clone(content)
    if (updateBase) baseSnapshot.value = snapshot(draft.value)
    undoStack.value = []
    redoStack.value = []
  }

  function restoreHistory(source: string[], destination: string[]) {
    const value = source.pop()
    if (!value) return
    destination.push(snapshot(draft.value))
    draft.value = JSON.parse(value) as ResumeContent
  }

  return {
    loading, saving, errorMessage, resumeId, resumeTitle, versions, selectedVersionId,
    blank, draft, sections, availableModules, dirty, versionLabel, updatedAt, canUndo, canRedo,
    load, save, reset, undo, redo, switchVersion, updateField, updateParagraph, updateChips,
    updateListItem, addListItem, removeListItem, moveListItem, addItem, removeItem, moveItem,
    toggleVisibility, addModule, removeModule, moveModule,
  }
}

function buildSections(content: ResumeContent): EditorSection[] {
  const active = resolveActiveSections(content)
  const hidden = new Set(stringArray(content.hiddenSections))
  const basic = content.basicInfo ?? {}
  const personal: EditorSection = {
    id: 'personal-info', type: 'personal_info', title: '个人信息', subtitle: sectionCopy['personal-info'].subtitle,
    status: status([basic.name, basic.targetRole, basic.email, basic.phone]), visible: !hidden.has('personal-info'), chips: [], paragraphs: [],
    fields: [['name', '姓名'], ['targetRole', '目标岗位'], ['phone', '电话'], ['email', '邮箱'], ['location', '所在地'], ['website', '个人主页']].map(([key, label]) => ({ key: `basicInfo.${key}`, label, value: text((basic as Record<string, unknown>)[key]) })),
  }
  const summary = text(content.summary)
  const summarySection: EditorSection = { id: 'summary', type: 'summary', title: '个人简介', subtitle: sectionCopy.summary.subtitle, status: summary ? 'ready' : 'empty', visible: !hidden.has('summary'), fields: [], chips: [], paragraphs: [summary], paragraphLabels: ['个人简介'] }
  const byId = new Map<string, EditorSection>([['personal-info', personal], ['summary', summarySection]])
  for (const module of moduleCatalog) {
    const config = collectionConfigs[module.id]
    if (!config) continue
    let rows = arrayOfRecords(content[config.key])
    if (module.id === 'skills' && !rows.length && stringArray(content.skills).length) rows = [{ name: '技术栈', skills: stringArray(content.skills) }]
    const items: EditorSectionItem[] = rows.map((row, index) => ({
      id: `${module.id}-${index}`,
      title: config.titleKeys.map((key) => text(row[key])).find(Boolean) || `${module.title} ${index + 1}`,
      description: config.descriptionKey ? text(row[config.descriptionKey]) : '',
      descriptionKey: config.descriptionKey ? `${String(config.key)}.${index}.${config.descriptionKey}` : undefined,
      descriptionLabel: config.descriptionLabel,
      fields: config.fields.map(([key, label]) => ({ key: `${String(config.key)}.${index}.${key}`, label, value: text(row[key]) })),
      listFields: (config.lists ?? []).map(([key, label]) => ({ key: `${String(config.key)}.${index}.${key}`, label, value: editableStringArray(row[key]) })),
    }))
    byId.set(module.id, {
      id: module.id, type: module.type, title: module.title, subtitle: sectionCopy[module.id].subtitle,
      status: rows.length ? 'ready' : 'empty', visible: !hidden.has(module.id), fields: [], chips: [], paragraphs: [], items,
      addLabel: config.addLabel, meta: `${rows.length} 条`,
    })
  }
  return active.map((id) => byId.get(id)).filter((section): section is EditorSection => Boolean(section))
}

function createBlankContent(): ResumeContent {
  return { activeSections: [...defaultSections], basicInfo: {}, summary: '', education: [], skills: [], skillCategories: [], projects: [] }
}

function resolveActiveSections(content: ResumeContent) {
  const stored = stringArray(content.activeSections)
  if (stored.length) return ['personal-info', ...stored.filter((id) => id !== 'personal-info' && moduleCatalog.some((module) => module.id === id))]
  const result = [...defaultSections]
  for (const [id, config] of Object.entries(collectionConfigs)) if (arrayOfRecords(content[config.key]).length && !result.includes(id)) result.push(id)
  return ordered(result)
}

function ordered(ids: string[]) {
  const unique = new Set(ids)
  return moduleCatalog.map((module) => module.id).filter((id) => unique.has(id))
}

function emptyItem(sectionId: string): Record<string, unknown> {
  const config = collectionConfigs[sectionId]
  const item: Record<string, unknown> = {}
  for (const [key] of config?.fields ?? []) item[key] = ''
  for (const [key] of config?.lists ?? []) item[key] = []
  if (config?.descriptionKey) item[config.descriptionKey] = ''
  return item
}

function deriveTitle(content: ResumeContent) {
  const name = text(content.basicInfo?.name)
  const role = text(content.basicInfo?.targetRole)
  return [name, role].filter(Boolean).join(' · ') || '未命名简历'
}

function syncFlatSkills(content: ResumeContent, fieldKey: string) {
  if (!fieldKey.startsWith('skillCategories')) return
  content.skills = arrayOfRecords(content.skillCategories).flatMap((item) => stringArray(item.skills))
}

function status(values: unknown[]): EditorSectionStatus {
  const count = values.filter((value) => text(value)).length
  return count === 0 ? 'empty' : count === values.length ? 'ready' : 'warning'
}

function clone(content: ResumeContent): ResumeContent { return JSON.parse(JSON.stringify(content ?? {})) as ResumeContent }
function snapshot(content: ResumeContent) { return JSON.stringify(content ?? {}) }
function text(value: unknown) { return typeof value === 'string' ? value : value == null ? '' : String(value) }
function stringArray(value: unknown) { return Array.isArray(value) ? value.map(text).map((item) => item.trim()).filter(Boolean) : [] }
function editableStringArray(value: unknown) { return Array.isArray(value) ? value.map(text) : [] }
function arrayOfRecords(value: unknown): Array<Record<string, unknown>> { return Array.isArray(value) ? value.map((item) => ({ ...(item as Record<string, unknown>) })) : [] }
function splitList(value: string) { return value.split(/[\n,，、]/).map((item) => item.trim()).filter(Boolean) }

function setByPath(target: ResumeContent, path: string, value: unknown) {
  const parts = path.split('.')
  let current: Record<string, unknown> = target
  for (let index = 0; index < parts.length - 1; index += 1) {
    const key = parts[index]
    const nextKey = parts[index + 1]
    if (!current[key] || typeof current[key] !== 'object') current[key] = /^\d+$/.test(nextKey) ? [] : {}
    current = current[key] as Record<string, unknown>
  }
  current[parts.at(-1)!] = value
}

function listByPath(target: ResumeContent, path: string) {
  let current: unknown = target
  for (const part of path.split('.')) current = current && typeof current === 'object' ? (current as Record<string, unknown>)[part] : undefined
  return editableStringArray(current)
}

function moveEntry<T>(items: T[], index: number, direction: 'up' | 'down') {
  const nextIndex = direction === 'up' ? index - 1 : index + 1
  if (index < 0 || nextIndex < 0 || nextIndex >= items.length) return
  const [item] = items.splice(index, 1)
  items.splice(nextIndex, 0, item)
}
