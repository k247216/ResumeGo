<template>
  <div v-if="open" class="sources-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="sources-dialog" role="dialog" aria-modal="true" aria-labelledby="external-sources-title">
      <header>
        <div><small>外部日历</small><h2 id="external-sources-title">导入其他日历</h2></div>
        <button type="button" aria-label="关闭" @click="$emit('close')">×</button>
      </header>

      <p class="sources-intro">
        导入 .ics / iCal 文件，把飞书、Google 日历、Apple 日历里的安排叠加到本页面上。
        文件只在本机解析和保存，不会上传；外部日程为只读，不能编辑。
      </p>
      <p class="sources-hint">
        飞书：日历设置 → 公开分享 → 复制订阅链接，在浏览器打开并保存为 .ics 后导入。
      </p>

      <form @submit.prevent="submit">
        <label>来源名称<input v-model="form.name" data-test="source-name" maxlength="40" placeholder="例如：飞书日历"></label>
        <label>选择文件
          <span class="file-row">
            <button type="button" class="file-button" data-test="source-file-button" @click="fileInput?.click()">选择 .ics 文件</button>
            <em v-if="fileName" data-test="source-file-name">{{ fileName }}</em>
          </span>
          <input ref="fileInput" class="file-input" type="file" accept=".ics,.ical,.txt,text/calendar" data-test="source-file" @change="onFileChange">
        </label>

        <p v-if="preview" class="preview" data-test="source-preview">{{ preview }}</p>
        <p v-if="error" class="error" role="alert">{{ error }}</p>

        <footer>
          <span v-if="sources.length" class="count">{{ sources.length }} / 5 个来源</span>
          <span class="spacer"></span>
          <button type="button" :disabled="importing" @click="$emit('close')">取消</button>
          <button class="primary" type="submit" data-test="source-import" :disabled="!parsed || !parsed.events.length || importing">
            {{ importing ? '导入中…' : '导入' }}
          </button>
        </footer>
      </form>

      <div v-if="sources.length" class="source-list" data-test="source-list">
        <small>已接入来源</small>
        <div v-for="source in sources" :key="source.id" class="source-item">
          <span class="source-dot"></span>
          <strong>{{ source.name }}</strong>
          <em>{{ importedLabel(source.importedAt) }}</em>
          <button type="button" class="remove" data-test="source-remove" @click="$emit('remove', source.id)">移除</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { parseIcs } from '../../utils/ics'
import type { ExternalCalendarSource } from '../../types/schedule'

const props = defineProps<{ open: boolean; sources: ExternalCalendarSource[]; importing?: boolean }>()
const emit = defineEmits<{
  (event: 'close'): void
  (event: 'import', payload: { name: string; raw: string }): void
  (event: 'remove', id: number): void
}>()

const form = ref({ name: '' })
const fileName = ref('')
const raw = ref('')
const parsed = ref<ReturnType<typeof parseIcs> | null>(null)
const error = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

watch(() => props.open, (open) => {
  if (!open) return
  form.value.name = ''
  fileName.value = ''
  raw.value = ''
  parsed.value = null
  error.value = ''
})

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  fileName.value = file.name
  if (!form.value.name.trim()) form.value.name = file.name.replace(/\.(ics|ical|txt)$/i, '')
  error.value = ''
  file.text().then((text) => {
    raw.value = text
    try {
      const result = parseIcs(text)
      if (!result.events.length && !result.skippedRecurring) {
        error.value = '未解析到任何日程，请确认这是 .ics / iCal 格式'
        parsed.value = null
        return
      }
      parsed.value = result
    } catch {
      error.value = '文件读取失败'
      parsed.value = null
    }
  })
}

const preview = computed(() => {
  if (!parsed.value) return ''
  const parts = [`解析到 ${parsed.value.events.length} 条日程`]
  if (parsed.value.skippedRecurring) parts.push(`${parsed.value.skippedRecurring} 条重复规则事件暂不支持`)
  return parts.join('，')
})

function submit() {
  if (!raw.value) return
  emit('import', { name: form.value.name, raw: raw.value })
}

function importedLabel(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getMonth() + 1} 月 ${date.getDate()} 日导入`
}
</script>

<style scoped>
.sources-backdrop{position:fixed;inset:0;z-index:46;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}
.sources-dialog{width:min(460px,100%);max-height:86vh;overflow-y:auto;border-radius:16px;background:var(--surface-solid);color:var(--ink);box-shadow:0 22px 60px rgba(0,0,0,.35);padding:22px}
.sources-dialog header{display:flex;justify-content:space-between;gap:16px}
.sources-dialog h2{margin:5px 0 14px;font-size:20px}
.sources-dialog small{color:var(--brand);font-weight:700}
.sources-dialog header button{align-self:start;border:0;background:none;color:var(--muted);font-size:24px;cursor:pointer}
.sources-intro{margin:0 0 8px;color:var(--copy);font-size:13px;line-height:1.65}
.sources-hint{margin:0 0 16px;padding:9px 12px;border-radius:9px;background:var(--surface-subtle);color:var(--muted);font-size:12px;line-height:1.6}
.sources-dialog form{display:grid;gap:13px}
.sources-dialog label{display:grid;gap:7px;font-weight:600;color:var(--ink);font-size:13px}
.sources-dialog input:not(.file-input){box-sizing:border-box;width:100%;border:1px solid var(--border-default);border-radius:9px;padding:10px 12px;background:var(--surface-solid);color:var(--ink);font:inherit}
.file-row{display:flex;align-items:center;gap:10px;min-height:34px}
.file-button{padding:9px 14px;border:1px dashed var(--border-strong);border-radius:9px;background:var(--surface-solid);color:var(--ink);font-size:13px;font-weight:600;cursor:pointer}
.file-button:hover{border-color:var(--action-bg)}
.file-input{display:none}
.file-row em{overflow:hidden;color:var(--muted);font-size:12px;font-style:normal;text-overflow:ellipsis;white-space:nowrap}
.preview{margin:0;color:var(--brand);font-size:13px;font-weight:600}
.error{margin:0;color:var(--danger);font-size:13px}
.sources-dialog footer{display:flex;align-items:center;gap:9px;margin-top:2px}
.sources-dialog footer .spacer{flex:1}
.count{color:var(--muted);font-size:12px}
.sources-dialog footer button{border:1px solid var(--border-default);border-radius:9px;background:var(--surface-solid);color:var(--ink);padding:9px 13px;font-size:13px;cursor:pointer}
.sources-dialog footer button:disabled{opacity:.55;cursor:not-allowed}
.sources-dialog footer .primary{background:var(--action-bg);color:var(--action-fg);border-color:var(--action-bg);font-weight:600}
.source-list{margin-top:18px;padding-top:14px;border-top:1px solid var(--border-subtle)}
.source-list>small{display:block;margin-bottom:4px;color:var(--muted);font-size:11px;font-weight:700;letter-spacing:.08em}
.source-item{display:flex;align-items:center;gap:9px;padding:9px 2px}
.source-item+.source-item{border-top:1px solid var(--border-subtle)}
.source-dot{width:7px;height:7px;border-radius:50%;background:var(--external-accent)}
.source-item strong{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:13px}
.source-item em{color:var(--muted);font-size:12px;font-style:normal}
.source-item .remove{border:0;padding:4px 6px;background:none;color:var(--danger);font-size:12px;cursor:pointer}
.source-item .remove:hover{text-decoration:underline}
</style>
