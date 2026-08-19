<template>
  <section class="evidence-library">
    <header>
      <div><p>能力证据</p><h1>先记录事实，再决定如何表达</h1><span>这里保存你真实做过的事情。来源不足的内容不会被当作确定事实使用。</span></div>
      <div class="header-actions"><button type="button" :disabled="library.loading.value" @click="library.load">刷新</button><button data-test="open-evidence-form" class="primary" type="button" @click="formOpen = true">新增证据</button></div>
    </header>

    <div v-if="library.loading.value && !library.evidences.value.length" class="state">正在读取本地证据…</div>
    <div v-else-if="library.errorMessage.value && !library.evidences.value.length && !formOpen" class="state error"><strong>无法读取能力证据</strong><span>{{ library.errorMessage.value }}</span><button type="button" @click="library.load">重新加载</button></div>
    <div v-else-if="!library.evidences.value.length && !formOpen" data-test="evidence-empty" class="state"><strong>还没有能力证据</strong><span>从一个真实项目、实习任务或技能使用场景开始。</span><button type="button" @click="formOpen = true">记录第一条证据</button></div>

    <div v-else class="content-layout" :class="{ 'form-visible': formOpen }">
      <main>
        <p v-if="library.errorMessage.value && !formOpen" class="inline-error">{{ library.errorMessage.value }}</p>
        <div class="evidence-list">
          <article v-for="item in library.evidences.value" :key="item.id">
            <div class="evidence-title"><span>{{ typeLabel(item.evidenceType) }}</span><h2>{{ item.title }}</h2></div>
            <p v-if="item.situation"><strong>背景</strong>{{ item.situation }}</p>
            <p><strong>实际行动</strong>{{ item.actionText }}</p>
            <p v-if="item.resultText"><strong>结果</strong>{{ item.resultText }}</p>
            <div class="tags"><span v-for="skill in item.skillTags" :key="skill">{{ skill }}</span></div>
            <small>{{ item.sourceNote || '来源说明待补充' }}</small>
          </article>
        </div>
      </main>

      <aside v-if="formOpen" class="evidence-form-panel">
        <div class="form-title"><div><small>新增能力证据</small><h2>记录可追溯的事实</h2></div><button type="button" aria-label="关闭" @click="closeForm">×</button></div>
        <form @submit.prevent="saveEvidence">
          <label>经历类型<select v-model="form.evidenceType"><option value="project">项目经历</option><option value="internship">实习经历</option><option value="competition">竞赛经历</option><option value="skill">技能证据</option><option value="other">其他</option></select></label>
          <label>标题<input v-model="form.title" data-test="evidence-title" maxlength="200" placeholder="例如：校园二手交易小程序"></label>
          <label>背景 / 任务<textarea v-model="form.situation" rows="3" placeholder="发生在什么场景，需要解决什么问题"></textarea></label>
          <label>实际行动<textarea v-model="form.actionText" data-test="evidence-action" rows="4" placeholder="只写你实际完成的行动"></textarea></label>
          <label>结果 / 指标<textarea v-model="form.resultText" rows="3" placeholder="没有可验证数字时可以留空"></textarea></label>
          <label>技能标签<input v-model="form.skillTagsText" data-test="evidence-skills" placeholder="Java, Spring Boot, MySQL"></label>
          <label>来源说明<input v-model="form.sourceNote" maxlength="500" placeholder="课程项目、仓库、证书等"></label>
          <p v-if="formError || library.errorMessage.value" class="form-error">{{ formError || library.errorMessage.value }}</p>
          <div class="form-actions"><button type="button" @click="closeForm">取消</button><button class="primary" type="submit" :disabled="library.saving.value">{{ library.saving.value ? '保存中…' : '保存证据' }}</button></div>
        </form>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useEvidenceLibrary } from '../../composables/useEvidenceLibrary'
import type { EvidenceType } from '../../types/evidence'

const library = useEvidenceLibrary()
const formOpen = ref(false)
const formError = ref('')
const form = reactive({ evidenceType: 'project' as EvidenceType, title: '', situation: '', actionText: '', resultText: '', skillTagsText: '', sourceNote: '' })
onMounted(() => { void library.load() })

async function saveEvidence() {
  const tags = form.skillTagsText.split(/[,，、]/).map((item) => item.trim()).filter(Boolean)
  if (!form.title.trim() || !form.actionText.trim() || !tags.length) { formError.value = '请补充标题、实际行动和至少一个技能标签'; return }
  formError.value = ''
  try {
    await library.create({ evidenceType: form.evidenceType, title: form.title.trim(), actionText: form.actionText.trim(), skillTags: tags, ...(form.situation.trim() ? { situation: form.situation.trim() } : {}), ...(form.resultText.trim() ? { resultText: form.resultText.trim() } : {}), ...(form.sourceNote.trim() ? { sourceNote: form.sourceNote.trim() } : {}) })
    resetForm(); formOpen.value = false
  } catch { /* library error remains visible while preserving the form */ }
}
function closeForm() { if (library.saving.value) return; formOpen.value = false; formError.value = ''; library.errorMessage.value = '' }
function resetForm() { form.evidenceType = 'project'; form.title = ''; form.situation = ''; form.actionText = ''; form.resultText = ''; form.skillTagsText = ''; form.sourceNote = '' }
function typeLabel(type: EvidenceType) { return ({ project: '项目', internship: '实习', competition: '竞赛', skill: '技能', other: '其他' })[type] }
</script>

<style scoped>
.evidence-library{padding:38px 42px;color:#203039}.evidence-library>header{display:flex;justify-content:space-between;align-items:flex-end;gap:24px;margin-bottom:26px}.evidence-library header p{margin:0;color:#168866;font-weight:800}.evidence-library header h1{margin:6px 0 8px;font-size:30px}.evidence-library header span{color:#71808a}.header-actions{display:flex;gap:9px}.header-actions button,.state button,.form-actions button{border:1px solid #d1dcdf;border-radius:9px;background:#fff;padding:9px 13px}.primary{border-color:#168866!important;background:#168866!important;color:#fff}.state{min-height:260px;display:grid;place-content:center;justify-items:center;gap:12px;border:1px solid #dfe6e9;border-radius:15px;background:#fff;color:#71808a}.state strong{color:#203039;font-size:20px}.state.error{color:#a23d35}.content-layout{display:grid;grid-template-columns:1fr;gap:18px}.content-layout.form-visible{grid-template-columns:minmax(0,1fr) 390px}.evidence-list{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:12px}.evidence-list article{display:grid;align-content:start;gap:10px;border:1px solid #dfe6e9;border-radius:13px;background:#fff;padding:18px}.evidence-title span{color:#168866;font-size:12px;font-weight:800}.evidence-title h2{margin:4px 0 0;font-size:18px}.evidence-list p{display:grid;gap:4px;margin:0;color:#566770;line-height:1.55}.evidence-list p strong{color:#263941;font-size:12px}.evidence-list small{color:#829098}.tags{display:flex;flex-wrap:wrap;gap:6px}.tags span{border-radius:999px;background:#edf7f3;color:#14765c;padding:5px 8px;font-size:12px}.evidence-form-panel{border:1px solid #dfe6e9;border-radius:14px;background:#fff;padding:19px}.form-title{display:flex;justify-content:space-between}.form-title small{color:#168866;font-weight:800}.form-title h2{margin:5px 0 16px}.form-title>button{border:0;background:none;font-size:22px}.evidence-form-panel form,.evidence-form-panel label{display:grid;gap:8px}.evidence-form-panel form{gap:13px}.evidence-form-panel label{color:#344751;font-weight:700}.evidence-form-panel input,.evidence-form-panel select,.evidence-form-panel textarea{box-sizing:border-box;width:100%;border:1px solid #d4dee2;border-radius:8px;padding:9px 10px;background:#fff;font:inherit}.evidence-form-panel textarea{resize:vertical}.form-actions{display:flex;justify-content:flex-end;gap:8px}.form-error,.inline-error{border:1px solid #efc7c2;border-radius:8px;background:#fff2f0;color:#a23d35;padding:9px 11px}
</style>
