<template>
  <div class="doc-preview" data-test="resume-document-preview">
    <div v-if="empty" class="doc-empty" data-test="doc-preview-empty">
      <strong>该版本还没有正文内容</strong>
      <span>进入编辑台补充基本信息、经历与技能后，这里会生成纸张预览。</span>
      <button type="button" class="doc-empty-btn" @click="emit('edit')">进入编辑台补充内容</button>
    </div>

    <article v-else-if="content" class="doc-paper" aria-label="简历正文只读预览">
      <header class="doc-head">
        <strong class="doc-name">{{ content.basicInfo?.name?.trim() || '姓名待补充' }}</strong>
        <span v-if="content.basicInfo?.targetRole" class="doc-role">{{ content.basicInfo.targetRole }}</span>
      </header>

      <section v-if="content.summary" class="doc-section">
        <h3>个人简介</h3>
        <p>{{ content.summary }}</p>
      </section>

      <section v-if="content.workExperience?.length" class="doc-section">
        <h3>工作经历</h3>
        <div v-for="(item, index) in content.workExperience" :key="index" class="doc-item">
          <div class="doc-item-head">
            <strong>{{ item.company || item.position || '经历' }}</strong>
            <span v-if="item.period || item.startDate">{{ item.period || `${item.startDate ?? ''} ~ ${item.endDate ?? '至今'}` }}</span>
          </div>
          <p v-if="item.description">{{ item.description }}</p>
        </div>
      </section>

      <section v-if="content.projects?.length" class="doc-section">
        <h3>项目经历</h3>
        <div v-for="(item, index) in content.projects" :key="index" class="doc-item">
          <div class="doc-item-head">
            <strong>{{ item.title || item.name || '项目' }}</strong>
            <span v-if="item.technologies?.length">{{ item.technologies.join(' / ') }}</span>
          </div>
          <p v-if="item.description">{{ item.description }}</p>
          <ul v-if="item.highlights?.length"><li v-for="line in item.highlights" :key="line">{{ line }}</li></ul>
        </div>
      </section>

      <section v-if="content.education?.length" class="doc-section">
        <h3>教育经历</h3>
        <div v-for="(item, index) in content.education" :key="index" class="doc-item">
          <div class="doc-item-head">
            <strong>{{ item.school || item.institution || '学校' }}</strong>
            <span v-if="item.period">{{ item.period }}</span>
          </div>
          <p v-if="item.major">{{ item.major }}<template v-if="item.degree"> · {{ item.degree }}</template></p>
        </div>
      </section>

      <section v-if="content.skills?.length || content.skillCategories?.length" class="doc-section">
        <h3>技能</h3>
        <p class="doc-skills">
          <template v-if="content.skills?.length">{{ content.skills.join(' · ') }}</template>
          <template v-for="category in content.skillCategories ?? []" :key="category.name">
            <template v-if="category.skills?.length">{{ (content.skills?.length ? ' · ' : '') + category.name + '：' + category.skills.join('、') }}</template>
          </template>
        </p>
      </section>

      <section v-if="content.certifications?.length" class="doc-section">
        <h3>证书</h3>
        <ul><li v-for="item in content.certifications" :key="item.name">{{ item.name }}<template v-if="item.date">（{{ item.date }}）</template></li></ul>
      </section>
    </article>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ResumeContent } from '../../types/resume'

const props = defineProps<{ content: ResumeContent | null }>()
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
</script>

<style scoped>
.doc-preview{min-height:0}
.doc-paper{background:var(--surface-solid,#fff);border:1px solid var(--border-subtle);border-radius:12px;padding:26px 28px;box-shadow:0 1px 3px rgba(16,24,40,.04)}
.doc-head{display:grid;gap:4px;padding-bottom:14px;border-bottom:1px solid var(--border-subtle);margin-bottom:14px}
.doc-name{font-size:19px;font-weight:750;color:var(--ink);letter-spacing:-.01em}
.doc-role{font-size:12px;color:var(--muted)}
.doc-section{padding:12px 0;border-bottom:1px solid var(--border-subtle)}
.doc-section:last-child{border-bottom:0}
.doc-section h3{margin:0 0 8px;font-size:11px;font-weight:650;letter-spacing:.07em;color:var(--brand)}
.doc-section p{margin:0;color:var(--copy);font-size:12.5px;line-height:1.75;white-space:pre-wrap}
.doc-item{display:grid;gap:4px;padding:6px 0}
.doc-item-head{display:flex;align-items:baseline;justify-content:space-between;gap:10px}
.doc-item-head strong{font-size:13px;font-weight:650;color:var(--ink)}
.doc-item-head span{font-size:11px;color:var(--muted);white-space:nowrap}
.doc-item p{margin:0;color:var(--copy);font-size:12px;line-height:1.7}
.doc-item ul{margin:0;padding-left:16px;color:var(--copy);font-size:12px;line-height:1.7}
.doc-skills{margin:0}
.doc-empty{display:grid;justify-items:center;gap:10px;border:1px dashed var(--border-default);border-radius:12px;padding:44px 24px;text-align:center;color:var(--muted)}
.doc-empty strong{color:var(--ink);font-size:14px}
.doc-empty span{font-size:12px;line-height:1.7;max-width:320px}
.doc-empty-btn{border:1px solid var(--brand);border-radius:9px;background:var(--brand);color:#fff;padding:8px 15px;font-size:12.5px;font-weight:600;cursor:pointer}
</style>
