<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import { importResume, listResumes, currentVersionOf, versionsOf } from '../data/store'
import { humanSize } from '../data/resumeFile'
import { toast } from '../data/toast'

const router = useRouter()
const resumes = computed(() => listResumes())
const fileRef = ref<HTMLInputElement | null>(null)
const busy = ref(false)

function openPicker() { fileRef.value?.click() }
async function onFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  busy.value = true
  try {
    const resume = await importResume('', file)
    toast('简历已导入')
    router.push({ name: 'resume-detail', params: { id: String(resume.id) } })
  } catch { toast('导入失败，请重试') } finally { busy.value = false }
}

function isPdf(mime: string, name: string) { return mime === 'application/pdf' || /\.pdf$/i.test(name) }
function verCount(id: number) { return versionsOf(id).length }
</script>

<template>
  <div>
    <header class="page-head">
      <div class="grow">
        <h1 class="page-title">我的简历</h1>
      <p class="page-sub">上传你的简历文件 · {{ resumes.length }} 份</p>
      </div>
      <button class="icon-btn" aria-label="导入简历" :disabled="busy" @click="openPicker">
        <AppIcon name="upload" :size="18" />
      </button>
    </header>
    <input ref="fileRef" type="file" hidden @change="onFile">

    <!-- 首次使用：导入引导 -->
    <div v-if="!resumes.length" class="import-hero card">
      <div class="ih-art"><AppIcon name="file" :size="30" /></div>
      <h3>还没有简历</h3>
      <p>把你的简历文件传进来就能管理。之后每次更新，<br>再传一次即可，旧版本会完整保留。</p>
      <button class="btn-primary block" :disabled="busy" @click="openPicker">
        <AppIcon name="upload" :size="17" /> {{ busy ? '导入中…' : '上传简历' }}
      </button>
      <p class="ih-note">🔒 文件只存本机，不上传任何服务器</p>
    </div>

    <div v-else class="list">
      <article
        v-for="(r, i) in resumes" :key="r.id"
        class="card resume-card" :style="{ '--i': i }"
        @click="router.push({ name: 'resume-detail', params: { id: String(r.id) } })"
      >
        <span class="rc-badge" :class="isPdf(currentVersionOf(r.id)?.mime ?? '', currentVersionOf(r.id)?.fileName ?? '') ? 'pdf' : 'md'">
          {{ isPdf(currentVersionOf(r.id)?.mime ?? '', currentVersionOf(r.id)?.fileName ?? '') ? 'PDF' : 'MD' }}
        </span>
        <div class="head-copy">
          <h3>{{ r.title }}</h3>
          <small>
            <template v-if="currentVersionOf(r.id)">当前 V{{ currentVersionOf(r.id)!.versionNo }} · {{ currentVersionOf(r.id)!.fileName }} · {{ humanSize(currentVersionOf(r.id)!.size) }}</template>
            <template v-else>暂无版本</template>
          </small>
        </div>
        <div class="rc-side">
          <span class="stage-pill" :style="{ background: 'var(--brand-soft)', color: 'var(--brand)' }">{{ verCount(r.id) }} 版</span>
          <AppIcon name="chevronRight" :size="18" class="rc-chev" />
        </div>
      </article>
    </div>

    <button v-if="resumes.length" class="fab" aria-label="导入简历" @click="openPicker"><AppIcon name="plus" :size="24" /></button>
  </div>
</template>
