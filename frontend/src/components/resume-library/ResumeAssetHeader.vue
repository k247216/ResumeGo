<template>
  <header v-if="resume" class="asset-header" data-test="asset-header">
    <div class="header-copy">
      <template v-if="renaming">
        <input
          v-model="draftTitle"
          class="rename-input"
          data-test="rename-input"
          maxlength="120"
          @keydown.enter.prevent="submitRename"
          @keydown.esc="cancelRename"
        />
        <button type="button" class="head-btn" data-test="rename-submit" @click="submitRename">保存</button>
        <button type="button" class="head-btn" @click="cancelRename">取消</button>
      </template>
      <template v-else>
        <h2 class="asset-title">{{ resume.title }}</h2>
        <span class="asset-kind" data-test="asset-kind-label">{{ kindLabel }}</span>
        <button type="button" class="head-btn" data-test="start-rename" @click="startRename">改名</button>
      </template>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Resume } from '../../types/resume'

const props = defineProps<{ resume: Resume }>()
const emit = defineEmits<{ rename: [title: string] }>()

const kindLabel = computed(() => (props.resume.kind === 'JOB_EXPRESSION' ? '岗位表达' : '通用简历'))

const renaming = ref(false)
const draftTitle = ref('')

watch(() => props.resume.id, () => { renaming.value = false })

function startRename() {
  draftTitle.value = props.resume.title
  renaming.value = true
}
function cancelRename() {
  renaming.value = false
  draftTitle.value = ''
}
function submitRename() {
  const title = draftTitle.value.trim()
  if (!title || title === props.resume.title) {
    cancelRename()
    return
  }
  emit('rename', title)
  cancelRename()
}
</script>

<style scoped>
.asset-header{display:flex;align-items:center;gap:10px;flex:0 0 auto}
.header-copy{display:flex;align-items:baseline;gap:10px;min-width:0;flex-wrap:wrap}
.asset-title{margin:0;font-size:17px;font-weight:700;color:var(--ink);letter-spacing:-.01em}
.asset-kind{padding:2px 9px;border-radius:999px;background:var(--bg-subtle);color:var(--copy);font-size:10.5px;font-weight:650}
.head-btn{border:0;background:none;padding:2px 6px;border-radius:6px;color:var(--muted);font-size:11.5px;cursor:pointer}
.head-btn:hover{background:var(--bg-hover);color:var(--ink)}
.rename-input{border:1px solid var(--brand);border-radius:8px;padding:6px 10px;font:inherit;font-size:14px;color:var(--ink);min-width:220px}
</style>
