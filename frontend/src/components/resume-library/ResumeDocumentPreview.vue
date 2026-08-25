<template>
  <div class="doc-preview" data-test="resume-document-preview">
    <div v-if="empty" class="doc-empty" data-test="doc-preview-empty">
      <strong>该版本还没有正文内容</strong>
      <span>进入编辑台补充基本信息、经历与技能后，这里会生成纸张预览。</span>
      <button type="button" class="doc-empty-btn" @click="emit('edit')">进入编辑台补充内容</button>
    </div>

    <article v-else-if="content" class="doc-paper" aria-label="简历正文只读预览">
      <!-- 头部：姓名 + 意向 + 联系方式 -->
      <header class="doc-head" :class="sectionClass('basicInfo')">
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
            <span class="doc-item-meta">{{ [item.position, item.period || (item.startDate ? `${item.startDate ?? ''} ~ ${item.endDate ?? '至今'}` : ''), item.location].filter(Boolean).join('　') }}</span>
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
            <span v-if="item.technologies?.length" class="doc-item-meta">{{ item.technologies.join(' / ') }}</span>
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
            <span class="doc-item-meta">{{ [item.major, item.degree, item.period].filter(Boolean).join('　') }}</span>
          </div>
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

      <section v-if="content.languages?.length" class="doc-section" :class="sectionClass('languages')">
        <h3>语言<span v-if="added('languages')" class="sec-badge">新增</span></h3>
        <ul><li v-for="item in content.languages" :key="item.name">{{ item.name }}<template v-if="item.level"> · {{ item.level }}</template></li></ul>
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
    content.languages?.length ? 'x' : '',
  ]
  return chapters.every((chapter) => !chapter)
})

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
  if (basic?.phone) parts.push(`电话 ${basic.phone}`)
  if (basic?.email) parts.push(`邮箱 ${basic.email}`)
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
/* ═══════ 真实纸张简历：与编辑台同源的阅读视图 ═══════ */
.doc-preview{min-height:0;display:flex;justify-content:center}
.doc-paper{width:100%;max-width:760px;background:var(--surface-solid,#fff);border:1px solid var(--border-subtle);border-radius:6px;padding:44px 52px 52px;box-shadow:0 2px 10px rgba(16,24,40,.07),0 1px 3px rgba(16,24,40,.04)}

.doc-head{padding-bottom:18px;border-bottom:2px solid var(--ink);margin-bottom:4px}
.doc-head-row{display:flex;align-items:baseline;gap:14px}
.doc-name{font-size:23px;font-weight:800;color:var(--ink);letter-spacing:.04em}
.doc-role{font-size:13px;font-weight:600;color:var(--copy)}
.doc-contact{display:flex;flex-wrap:wrap;gap:6px 18px;margin-top:10px}
.doc-contact-item{font-size:11.5px;color:var(--muted);font-variant-numeric:tabular-nums}

.doc-section{padding:16px 8px 14px;border-radius:6px;margin:0 -8px;border-bottom:1px solid var(--border-subtle)}
.doc-section:last-child{border-bottom:0}
.doc-section h3{margin:0 0 10px;font-size:13px;font-weight:750;color:var(--ink);display:flex;align-items:center;gap:8px}
.doc-section h3::after{content:'';flex:1;height:1px;background:var(--border-subtle)}

/* 比较高亮：修改=琥珀、新增=绿 */
.doc-section.sec-modified{background:rgba(217,119,6,.055);outline:1px solid rgba(217,119,6,.22)}
.doc-section.sec-added{background:rgba(22,139,104,.05);outline:1px solid rgba(22,139,104,.22)}
.sec-badge{padding:1px 8px;border-radius:999px;font-size:9.5px;font-weight:700;background:var(--brand-soft);color:var(--brand)}

.doc-item{display:grid;gap:5px;padding:8px 0}
.doc-item + .doc-item{border-top:1px dashed var(--border-subtle)}
.doc-item-head{display:flex;align-items:baseline;justify-content:space-between;gap:12px}
.doc-item-head strong{font-size:13.5px;font-weight:700;color:var(--ink)}
.doc-item-meta{font-size:11px;color:var(--muted);white-space:nowrap;font-variant-numeric:tabular-nums}
.doc-item p{margin:0;color:var(--copy);font-size:12.5px;line-height:1.8}
.doc-item ul{margin:2px 0 0;padding-left:16px;color:var(--copy);font-size:12.5px;line-height:1.85}
.skill-chips{display:flex;flex-wrap:wrap;gap:8px}
.skill-chip{padding:4px 12px;border-radius:6px;background:var(--bg-subtle);color:var(--copy);font-size:12px}
.doc-empty{display:grid;justify-items:center;gap:10px;border:1px dashed var(--border-default);border-radius:12px;padding:44px 24px;text-align:center;color:var(--muted)}
.doc-empty strong{color:var(--ink);font-size:14px}
.doc-empty span{font-size:12px;line-height:1.7;max-width:320px}
.doc-empty-btn{border:1px solid #17181a;border-radius:9px;background:#17181a;color:#fff;padding:8px 15px;font-size:12.5px;font-weight:600;cursor:pointer}
</style>
