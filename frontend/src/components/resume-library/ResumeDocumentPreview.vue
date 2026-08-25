<template>
  <div class="doc-preview" data-test="resume-document-preview">
    <div v-if="empty" class="doc-empty" data-test="doc-preview-empty">
      <strong>该版本还没有正文内容</strong>
      <span>进入编辑台补充基本信息、经历与技能后，这里会生成纸张预览。</span>
      <button type="button" class="doc-empty-btn" @click="emit('edit')">进入编辑台补充内容</button>
    </div>

    <article v-else-if="content" class="doc-paper" aria-label="简历正文只读预览">
      <header class="doc-head">
        <div class="doc-head-row">
          <strong class="doc-name">{{ content.basicInfo?.name?.trim() || '姓名待补充' }}</strong>
          <span v-if="content.basicInfo?.targetRole" class="doc-role">{{ content.basicInfo.targetRole }}</span>
        </div>
        <div v-if="contactParts.length" class="doc-contact">
          <span v-for="part in contactParts" :key="part" class="doc-contact-item">{{ part }}</span>
        </div>
      </header>

      <section v-if="content.summary" class="doc-section" :class="sectionClass('summary')">
        <h3>个人简介<span v-if="added('summary')" class="sec-badge">新增</span></h3>
        <p>{{ content.summary }}</p>
      </section>

      <section v-if="content.workExperience?.length" class="doc-section" :class="sectionClass('workExperience')">
        <h3>工作经验<span v-if="added('workExperience')" class="sec-badge">新增</span></h3>
        <div v-for="(item, index) in content.workExperience" :key="index" class="doc-item">
          <div class="doc-item-head">
            <strong>{{ item.company || item.position || '经历' }}</strong>
            <span v-if="item.period || item.startDate">{{ item.period || `${item.startDate ?? ''} ~ ${item.endDate ?? '至今'}` }}</span>
          </div>
          <p v-if="item.description">{{ item.description }}</p>
          <ul v-if="item.highlights?.length"><li v-for="line in item.highlights" :key="line">{{ line }}</li></ul>
        </div>
      </section>

      <section v-if="content.projects?.length" class="doc-section" :class="sectionClass('projects')">
        <h3>项目经历<span v-if="added('projects')" class="sec-badge">新增</span></h3>
        <div v-for="(item, index) in content.projects" :key="index" class="doc-item">
          <div class="doc-item-head">
            <strong>{{ item.title || item.name || '项目' }}</strong>
            <span v-if="item.technologies?.length">{{ item.technologies.join(' / ') }}</span>
          </div>
          <p v-if="item.description">{{ item.description }}</p>
          <ul v-if="item.highlights?.length"><li v-for="line in item.highlights" :key="line">{{ line }}</li></ul>
        </div>
      </section>

      <section v-if="content.education?.length" class="doc-section" :class="sectionClass('education')">
        <h3>教育经历<span v-if="added('education')" class="sec-badge">新增</span></h3>
        <div v-for="(item, index) in content.education" :key="index" class="doc-item">
          <div class="doc-item-head">
            <strong>{{ item.school || item.institution || '学校' }}</strong>
            <span v-if="item.period">{{ item.period }}</span>
          </div>
          <p v-if="item.major">{{ item.major }}<template v-if="item.degree"> · {{ item.degree }}</template></p>
        </div>
      </section>

      <section v-if="hasSkills" class="doc-section" :class="sectionClass(hasSkillsKey)">
        <h3>技能清单<span v-if="added(hasSkillsKey)" class="sec-badge">新增</span></h3>
        <div class="skill-chips">
          <span v-for="skill in flatSkills" :key="skill" class="skill-chip">{{ skill }}</span>
        </div>
      </section>

      <section v-if="content.certifications?.length" class="doc-section" :class="sectionClass('certifications')">
        <h3>证书<span v-if="added('certifications')" class="sec-badge">新增</span></h3>
        <ul><li v-for="item in content.certifications" :key="item.name">{{ item.name }}<template v-if="item.date">（{{ item.date }}）</template></li></ul>
      </section>
    </article>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ResumeContent } from '../../types/resume'
import type { ResumeChapterChange } from '../../utils/resumeVersionDiff'

const props = defineProps<{
  content: ResumeContent | null
  /** 比较模式下选中版本相对父版本的章节级变化 */
  changes?: ResumeChapterChange[]
  compareMode?: boolean
}>()

const emit = defineEmits<{ edit: [] }>()

/** 章节全部为空视为无正文 */
const empty = computed(() => {
  const content = props.content
  if (!content) return true
  const chapters = [
    content.basicInfo && Object.keys(content.basicInfo).length ? 'x' : '',
    content.summary,
    content.workExperience?.length ? 'x' : '',
    content.projects?.length ? 'x' : '',
    content.education?.length ? 'x' : '',
    content.skills?.length ? 'x' : '',
    content.skillCategories?.length ? 'x' : '',
    content.certifications?.length ? 'x' : '',
  ]
  return chapters.every((chapter) => !chapter)
})

/** 技能清单：skills 与 skillCategories 合并展示 */
const hasSkills = computed(() => !!(props.content?.skills?.length || props.content?.skillCategories?.length))
const hasSkillsKey = computed(() => (props.content?.skills?.length ? 'skills' : 'skillCategories'))
const flatSkills = computed(() => {
  const content = props.content
  const flat: string[] = []
  for (const skill of content?.skills ?? []) flat.push(skill)
  for (const category of content?.skillCategories ?? []) {
    for (const skill of category.skills ?? []) flat.push(skill)
  }
  return flat
})

const contactParts = computed(() => {
  const basic = props.content?.basicInfo
  const parts: string[] = []
  if (basic?.phone) parts.push(basic.phone)
  if (basic?.email) parts.push(basic.email)
  if (basic?.location) parts.push(basic.location)
  return parts
})

function changeFor(key: string) {
  if (!props.compareMode) return undefined
  return (props.changes ?? []).find((change) => change.chapterKey === key)
}
function added(key: string) {
  return changeFor(key)?.changeType === 'added'
}
function sectionClass(key: string) {
  const change = changeFor(key)
  if (change?.changeType === 'added') return { 'sec-added': true }
  if (change?.changeType === 'modified') return { 'sec-modified': true }
  return {}
}
</script>

<style scoped>
.doc-preview{min-height:0}
.doc-paper{background:var(--surface-solid,#fff);border:1px solid var(--border-subtle);border-radius:12px;padding:26px 30px;box-shadow:0 1px 3px rgba(16,24,40,.04)}
.doc-head{padding-bottom:14px;border-bottom:1px solid var(--border-subtle);margin-bottom:6px}
.doc-head-row{display:flex;align-items:baseline;gap:12px}
.doc-name{font-size:20px;font-weight:750;color:var(--ink);letter-spacing:.02em}
.doc-role{font-size:12.5px;color:var(--muted)}
.doc-contact{display:flex;flex-wrap:wrap;gap:14px;margin-top:8px}
.doc-contact-item{font-size:11px;color:var(--muted)}
.doc-section{padding:12px 10px;border-bottom:1px solid var(--border-subtle);border-radius:8px;margin:0 -10px}
.doc-section:last-child{border-bottom:0}
.doc-section h3{margin:0 0 8px;font-size:11px;font-weight:650;letter-spacing:.07em;color:var(--brand);display:flex;align-items:center;gap:8px}
/* 比较高亮：修改=琥珀底纹；新增=绿色底纹 */
.doc-section.sec-modified{background:rgba(217,119,6,.06);outline:1px solid rgba(217,119,6,.18)}
.doc-section.sec-added{background:rgba(22,139,104,.05);outline:1px solid rgba(22,139,104,.2)}
.sec-badge{padding:1px 7px;border-radius:999px;font-size:9.5px;font-weight:700;background:var(--brand-soft);color:var(--brand)}
.doc-item{display:grid;gap:4px;padding:6px 0}
.doc-item-head{display:flex;align-items:baseline;justify-content:space-between;gap:10px}
.doc-item-head strong{font-size:13px;font-weight:650;color:var(--ink)}
.doc-item-head span{font-size:11px;color:var(--muted);white-space:nowrap}
.doc-item p{margin:0;color:var(--copy);font-size:12px;line-height:1.7}
.doc-item ul{margin:0;padding-left:16px;color:var(--copy);font-size:12px;line-height:1.8}
.doc-skills{margin:0}
.skill-chips{display:flex;flex-wrap:wrap;gap:7px}
.skill-chip{padding:3px 10px;border-radius:6px;background:var(--bg-subtle);color:var(--copy);font-size:11.5px}
.doc-empty{display:grid;justify-items:center;gap:10px;border:1px dashed var(--border-default);border-radius:12px;padding:44px 24px;text-align:center;color:var(--muted)}
.doc-empty strong{color:var(--ink);font-size:14px}
.doc-empty span{font-size:12px;line-height:1.7;max-width:320px}
.doc-empty-btn{border:1px solid var(--brand);border-radius:9px;background:var(--brand);color:#fff;padding:8px 15px;font-size:12.5px;font-weight:600;cursor:pointer}
</style>
