<template>
  <div v-if="open" class="dialog-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="apply-dialog-title">
      <header>
        <div>
          <small>投递工具</small>
          <h2 id="apply-dialog-title">{{ target?.name }}</h2>
        </div>
        <button type="button" aria-label="关闭" @click="$emit('close')">×</button>
      </header>

      <!-- 投递信息表单 -->
      <form @submit.prevent="save">
        <a v-if="matchedSite" class="matched-site" :href="matchedSite.url" target="_blank" rel="noopener noreferrer">
          <svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true"><path fill="currentColor" d="M8 1a7 7 0 1 0 0 14A7 7 0 0 0 8 1Zm5.5 6h-2.2a11 11 0 0 0-.9-4.2A5.5 5.5 0 0 1 13.5 7ZM8 2.6c.7.8 1.6 2.3 1.8 4.4H6.2C6.4 4.9 7.3 3.4 8 2.6ZM2.5 7a5.5 5.5 0 0 1 3.1-4.2A11 11 0 0 0 4.7 7H2.5Zm2.2 2h2.2a11 11 0 0 0 .9 4.4A5.5 5.5 0 0 1 4.7 9Zm4.4 4.4A11 11 0 0 0 11.3 9h2.2a5.5 5.5 0 0 1-4.6 4.4Z"/></svg>
          官网直达：{{ matchedSite.name }}
          <span class="matched-url">{{ matchedSite.url.replace('https://', '') }}</span>
        </a>
        <div class="field-row">
          <label>
            行业
            <input v-model="industry" data-test="apply-industry" autocomplete="off" placeholder="如：互联网">
          </label>
          <label>
            期望岗位
            <input v-model="role" data-test="apply-role" autocomplete="off" placeholder="如：后端开发">
          </label>
        </div>
        <label>
          地点
          <input v-model="location" data-test="apply-location" autocomplete="off" placeholder="如：深圳 · 南山">
        </label>
        <label>
          备注
          <textarea v-model="notes" data-test="apply-notes" rows="3" placeholder="内推人、投递批次、进度备忘…"></textarea>
        </label>

        <p v-if="errorMessage || saveError" class="error" role="alert">{{ saveError || errorMessage }}</p>
        <footer>
          <button type="button" @click="$emit('close')">取消</button>
          <button class="primary" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存投递信息' }}</button>
        </footer>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { JobProject } from '../../types/project'
import { CAREER_SITES } from '../../constants/careerSites'

const matchedSite = computed(() => {
  const target = props.target
  if (!target) return null
  const name = target.name.split(/[·•\-—|]/)[0]?.trim().split(/\s+/)[0] ?? ''
  return CAREER_SITES.find((site) => (name && name.includes(site.company))) ?? null
})

const props = withDefaults(defineProps<{
  open: boolean
  target: JobProject | null
  saving?: boolean
  errorMessage?: string
}>(), { open: false, saving: false, errorMessage: '' })

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'save', payload: { industry: string; role: string; location: string; notes: string }): void
}>()

const industry = ref('')
const role = ref('')
const location = ref('')
const notes = ref('')
const saveError = ref('')

watch(() => [props.open, props.target?.id] as const, ([open]) => {
  if (!open) return
  industry.value = props.target?.industry ?? ''
  location.value = props.target?.location ?? ''
  notes.value = props.target?.notes ?? ''
  role.value = roleHint(props.target)
  saveError.value = ''
}, { immediate: true })

function roleHint(target: JobProject | null): string {
  if (!target) return ''
  const name = target.name
  return name.includes('·') ? name.split('·').slice(1).join('·').trim() : ''
}

function save() {
  emit('save', { industry: industry.value.trim(), role: role.value.trim(), location: location.value.trim(), notes: notes.value.trim() })
}
</script>

<style scoped>
.dialog-backdrop{position:fixed;inset:0;z-index:40;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}
.dialog{width:min(600px,100%);max-height:min(80vh,760px);overflow-y:auto;border-radius:16px;background:#fff;color:#141516;box-shadow:0 22px 60px rgba(0,0,0,.35);padding:22px;animation:dialog-in .18s ease-out}
@keyframes dialog-in{from{opacity:0;transform:translateY(8px) scale(.985)}to{opacity:1;transform:none}}
@media (prefers-reduced-motion: reduce){.dialog{animation:none}}
.dialog header{display:flex;justify-content:space-between;gap:16px;margin-bottom:14px}
.dialog h2{margin:5px 0 0;font-size:20px}
.dialog small{color:var(--brand,#168866);font-weight:700}
.dialog header button{align-self:start;border:0;background:none;color:var(--muted,#5b6570);font-size:24px;cursor:pointer}
.dialog form,.dialog label{display:grid;gap:9px}
.dialog form{gap:14px;padding-bottom:16px;border-bottom:1px solid var(--line-subtle,rgba(28,31,35,.08))}
.dialog label{font-weight:650;color:#344552;font-size:13px}
.field-row{display:grid;grid-template-columns:1fr 1fr;gap:12px}
.dialog input,.dialog textarea{border:1px solid var(--line,#d6dfe4);border-radius:9px;padding:10px 12px;background:#fff;color:#22313b;font:inherit;font-weight:400;resize:vertical}
.dialog input:focus,.dialog textarea:focus{outline:none;border-color:var(--brand,#168866);box-shadow:0 0 0 3px var(--brand-soft,rgba(22,139,104,.12))}
.error{margin:0;color:var(--danger,#b53c32);font-size:13px}
.matched-site{display:inline-flex;align-items:center;gap:6px;justify-self:start;padding:7px 12px;border-radius:9px;background:#f4f8f6;color:var(--brand,#168b68);font-size:12.5px;font-weight:600;text-decoration:none}
.matched-site:hover{background:#eaf3ef}
.matched-url{font-weight:500;color:#5f8a79}
.dialog footer{display:flex;justify-content:flex-end;gap:9px}
.dialog footer button{border:1px solid var(--line,#d6dfe4);border-radius:9px;background:#fff;color:#141516;padding:9px 13px;font:inherit;cursor:pointer}
.dialog footer .primary{border-color:var(--brand,#168866);background:var(--brand,#168866);color:#fff;font-weight:600}

.state{padding:10px 2px;color:var(--muted,#989893);font-size:13px}
</style>
