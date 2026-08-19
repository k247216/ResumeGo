<template>
  <section data-test="resume-library" class="resume-library-view">
    <header class="page-header">
      <div><p>本地简历</p><h1>维护可以长期使用的简历版本</h1><span>每次保存都会生成新版本，历史内容不会被静默覆盖。</span></div>
      <div class="header-actions"><button type="button" :disabled="library.loading.value" @click="library.load">刷新</button><router-link :to="buildResumeEditorLocation({ mode: 'blank' })">创建空白简历</router-link></div>
    </header>

    <div v-if="library.loading.value && !library.resumes.value.length" class="state-card">正在读取本地简历…</div>
    <div v-else-if="library.errorMessage.value && !library.resumes.value.length" data-test="resume-library-error" class="state-card error"><strong>无法读取本地简历</strong><span>{{ library.errorMessage.value }}</span><button type="button" @click="library.load">重新加载</button></div>
    <div v-else-if="!library.resumes.value.length" data-test="resume-library-empty" class="state-card empty"><strong>创建第一份本地简历</strong><span>可以先整理通用简历，之后再把具体版本用于不同求职目标。</span><router-link :to="buildResumeEditorLocation({ mode: 'blank' })">从空白开始</router-link></div>

    <template v-else>
      <p v-if="library.errorMessage.value" class="inline-error">{{ library.errorMessage.value }} <button type="button" @click="library.load">重试</button></p>
      <div class="library-layout">
        <aside>
          <div class="aside-head"><strong>全部简历</strong><span>{{ library.resumes.value.length }} 份</span></div>
          <button v-for="resume in library.resumes.value" :key="resume.id" class="resume-card" :class="{ active: resume.id === library.selectedResumeId.value }" type="button" @click="library.selectResume(resume.id)">
            <span class="document-mark">R</span>
            <span><strong>{{ resume.title }}</strong><small>{{ resume.currentVersion ? `当前 v${resume.currentVersion.versionNo}` : '尚无版本' }}</small></span>
          </button>
        </aside>

        <main v-if="library.selectedResume.value" class="resume-detail">
          <section class="resume-summary">
            <div><small>当前选择</small><h2>{{ library.selectedResume.value.title }}</h2><p>{{ identityLine }}</p></div>
            <router-link :to="buildResumeEditorLocation({ resumeId: library.selectedResume.value.id, versionId: library.selectedVersion.value?.id })">继续编辑</router-link>
          </section>
          <div class="summary-stats">
            <article><small>查看版本</small><strong>{{ library.selectedVersion.value ? `v${library.selectedVersion.value.versionNo}` : '-' }}</strong></article>
            <article><small>项目经历</small><strong>{{ library.selectedVersion.value?.content.projects?.length ?? 0 }}</strong></article>
            <article><small>技能项</small><strong>{{ skillCount }}</strong></article>
          </div>
          <section class="version-section">
            <div class="section-title"><div><h3>版本历史</h3><p>选择一个历史版本查看摘要或继续编辑。</p></div><span>{{ library.versions.value.length }} 个版本</span></div>
            <p v-if="library.versionLoading.value">正在读取版本…</p>
            <p v-else-if="library.versionError.value" class="version-error">{{ library.versionError.value }}</p>
            <div v-else class="version-list">
              <button v-for="version in library.versions.value" :key="version.id" type="button" :class="{ active: version.id === library.selectedVersionId.value }" @click="library.selectVersion(version.id)">
                <strong>v{{ version.versionNo }}</strong><span>{{ createdByLabel(version.createdByType) }}</span><small>{{ formatTime(version.createdAt) }}</small><em>{{ version.changeSummary || '手工保存的简历版本' }}</em>
              </button>
            </div>
          </section>
        </main>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useResumeLibrary } from '../../composables/useResumeLibrary'
import { buildResumeEditorLocation } from '../../utils/editorRoute'

const library = useResumeLibrary()
const identityLine = computed(() => {
  const basic = library.selectedVersion.value?.content.basicInfo
  return [basic?.name || '姓名待补充', basic?.targetRole || '目标方向待补充'].join(' · ')
})
const skillCount = computed(() => {
  const content = library.selectedVersion.value?.content
  return content?.skillCategories?.reduce((count, category) => count + (category.skills?.length ?? 0), 0) || content?.skills?.length || 0
})
onMounted(() => { void library.load() })
function createdByLabel(type: string) { if (type === 'user') return '手工维护'; if (type === 'system') return '系统创建'; if (type === 'ai_suggestion') return '建议生成'; return '版本记录' }
function formatTime(value: string) { if (!value) return '时间未知'; return new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) }
</script>

<style scoped>
.resume-library-view{padding:38px 42px;color:#203039}.page-header{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-bottom:28px}.page-header p{margin:0;color:#168866;font-weight:800}.page-header h1{margin:6px 0 8px;font-size:30px}.page-header span{color:#71808a}.header-actions{display:flex;gap:9px}.header-actions button,.header-actions a,.state-card a,.state-card button,.resume-summary a{border:1px solid #ced9de;border-radius:9px;background:#fff;color:#31505b;padding:9px 13px;text-decoration:none}.header-actions a,.state-card a,.resume-summary a{border-color:#168866;background:#168866;color:#fff}.state-card{min-height:260px;display:grid;place-content:center;justify-items:center;gap:12px;border:1px solid #dfe6e9;border-radius:16px;background:#fff;color:#71808a}.state-card strong{color:#203039;font-size:20px}.state-card.error{color:#a23d35}.library-layout{display:grid;grid-template-columns:270px minmax(0,1fr);gap:18px;min-height:540px}.library-layout>aside,.resume-detail>section,.summary-stats article{border:1px solid #dfe6e9;border-radius:14px;background:#fff}.library-layout>aside{padding:14px}.aside-head{display:flex;justify-content:space-between;padding:5px 4px 14px}.aside-head span{color:#71808a}.resume-card{box-sizing:border-box;width:100%;display:flex;align-items:center;gap:10px;border:1px solid transparent;border-radius:10px;background:transparent;padding:11px;text-align:left}.resume-card.active{border-color:#86cbb5;background:#eff9f5}.resume-card>span:last-child{display:grid;gap:4px}.resume-card small{color:#71808a}.document-mark{display:grid;place-items:center;width:34px;height:42px;border-radius:6px;background:#168866;color:#fff;font-weight:800}.resume-detail{display:grid;align-content:start;gap:14px}.resume-summary{display:flex;justify-content:space-between;align-items:center;padding:22px}.resume-summary small{color:#168866;font-weight:700}.resume-summary h2{margin:5px 0}.resume-summary p{margin:0;color:#71808a}.summary-stats{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.summary-stats article{display:grid;gap:6px;padding:16px}.summary-stats small{color:#71808a}.summary-stats strong{font-size:20px}.version-section{padding:20px}.section-title{display:flex;justify-content:space-between}.section-title h3{margin:0 0 5px}.section-title p{margin:0;color:#71808a}.section-title>span{color:#168866}.version-list{display:grid;gap:8px;margin-top:18px}.version-list button{display:grid;grid-template-columns:52px 90px 130px minmax(0,1fr);align-items:center;gap:10px;border:1px solid #e1e7e9;border-radius:9px;background:#fff;padding:11px;text-align:left}.version-list button.active{border-color:#70bea5;background:#f2faf7}.version-list span,.version-list small,.version-list em{color:#71808a;font-style:normal}.inline-error,.version-error{border:1px solid #efc7c2;border-radius:9px;background:#fff2f0;color:#a23d35;padding:9px 12px}.inline-error button{border:0;background:none;color:#a23d35;text-decoration:underline}
</style>
