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
        <h2 class="asset-title">{{ displayTitle }}</h2>
        <span class="asset-kind" data-test="asset-kind-label">{{ kindLabel }}</span>
        <button type="button" class="head-btn icon-btn" data-test="start-rename" aria-label="改名" title="改名" @click="startRename">
          <el-icon :size="13"><EditPen /></el-icon>
        </button>
      </template>
    </div>
    <div v-if="!renaming" class="header-actions">
      <button type="button" class="head-btn icon-btn" :class="{ favorite: favorite }" data-test="toggle-favorite" :aria-pressed="favorite" aria-label="收藏简历" title="收藏简历" @click="toggleFavorite">
        <el-icon :size="14"><component :is="favorite ? StarFilled : Star" /></el-icon>
      </button>
      <button type="button" class="head-btn icon-btn" data-test="asset-more" aria-label="更多简历操作" title="更多简历操作" @click="moreOpen = !moreOpen">
        <el-icon :size="15"><MoreFilled /></el-icon>
      </button>
      <div v-if="moreOpen" class="asset-more-menu" data-test="asset-more-menu" role="menu">
        <button type="button" data-test="more-rename" @click="startRenameFromMenu">重命名</button>
        <button type="button" data-test="more-create-job-version" @click="emitMore('fork')">创建岗位版本</button>
        <button type="button" data-test="more-delete" @click="emitMore('delete')">{{ resume.archivedAt ? '从回收站恢复' : '归档到回收站' }}</button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { EditPen, MoreFilled, Star, StarFilled } from '@element-plus/icons-vue'
import type { Resume } from '../../types/resume'
import { getResumeDisplayTitle } from '../../utils/resumeTemplate'
import { isResumeFavorite, setResumeFavorite } from '../../utils/resumeFavorite'

const props = defineProps<{ resume: Resume }>()
const emit = defineEmits<{ rename: [title: string]; fork: []; archive: [] }>()

const kindLabel = computed(() => (props.resume.kind === 'JOB_EXPRESSION' ? '岗位版本' : '基础简历'))
const displayTitle = computed(() => getResumeDisplayTitle(props.resume))

const renaming = ref(false)
const draftTitle = ref('')
const favorite = ref(false)
const moreOpen = ref(false)

watch(() => props.resume.id, (id) => {
  renaming.value = false
  moreOpen.value = false
  favorite.value = isResumeFavorite(id)
}, { immediate: true })

function toggleFavorite() {
  favorite.value = !favorite.value
  setResumeFavorite(props.resume.id, favorite.value)
}

function startRename() {
  draftTitle.value = props.resume.title
  renaming.value = true
}
function startRenameFromMenu() {
  moreOpen.value = false
  startRename()
}
function emitMore(action: 'fork' | 'delete') {
  moreOpen.value = false
  if (action === 'fork') emit('fork')
  else emit('archive')
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
.asset-header{position:relative;display:flex;align-items:center;gap:10px;flex:0 0 auto}
.header-actions{display:flex;align-items:center;gap:2px;margin-left:auto}
.header-copy{display:flex;align-items:baseline;gap:10px;min-width:0;flex-wrap:wrap}
.asset-title{margin:0;font-size:17px;font-weight:700;color:var(--ink);letter-spacing:-.01em}
.asset-kind{padding:2px 9px;border-radius:999px;background:var(--bg-subtle);color:var(--copy);font-size:10.5px;font-weight:650}
.head-btn{border:0;background:none;padding:5px;border-radius:6px;color:var(--muted);font-size:11.5px;cursor:pointer}
.head-btn:hover{background:var(--bg-hover);color:var(--ink)}
.asset-more-menu{position:absolute;z-index:20;top:32px;right:0;display:grid;min-width:132px;border:1px solid var(--border-subtle);border-radius:9px;background:var(--surface-solid,#fff);padding:4px;box-shadow:0 12px 32px rgba(16,24,40,.14)}.asset-more-menu button{border:0;border-radius:6px;background:none;color:var(--copy);padding:7px 9px;text-align:left;font-size:11px;cursor:pointer}.asset-more-menu button:hover{background:var(--bg-hover);color:var(--ink)}
.head-btn.favorite{color:#d18a19}
.icon-btn{display:grid;place-items:center}
.rename-input{border:1px solid var(--brand);border-radius:8px;padding:6px 10px;font:inherit;font-size:14px;color:var(--ink);min-width:220px}
</style>
