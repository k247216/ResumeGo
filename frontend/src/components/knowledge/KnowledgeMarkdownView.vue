<template>
  <div class="markdown-view" data-test="markdown-view">
    <template v-for="(block, i) in blocks" :key="i">
      <h2 v-if="block.type === 'h2'" class="md-h2">{{ block.text }}</h2>
      <h3 v-else-if="block.type === 'h3'" class="md-h3">{{ block.text }}</h3>
      <ul v-else-if="block.type === 'list'" class="md-list">
        <li v-for="(item, j) in block.items" :key="j">{{ item }}</li>
      </ul>
      <pre v-else-if="block.type === 'code'" class="md-code"><code>{{ block.text }}</code></pre>
      <p v-else class="md-para">{{ block.text }}</p>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ source: string }>()

interface MdBlock {
  type: 'h2' | 'h3' | 'list' | 'code' | 'p'
  text?: string
  items?: string[]
}

const NL = '\n'

/** 明确安全的轻量 Markdown 解析：仅标题/列表/代码块/段落，纯文本插值，不解释 raw HTML（无 XSS）。 */
function parseBlocks(src: string): MdBlock[] {
  const out: MdBlock[] = []
  const lines = (src ?? '').split(NL)
  let inCode = false
  let codeBuf: string[] = []
  let listBuf: string[] = []
  const flushList = () => {
    if (listBuf.length) {
      out.push({ type: 'list', items: listBuf })
      listBuf = []
    }
  }
  for (const line of lines) {
    const trimmed = line.trim()
    if (trimmed.startsWith('```')) {
      flushList()
      if (inCode) {
        out.push({ type: 'code', text: codeBuf.join(NL) })
        codeBuf = []
        inCode = false
      } else {
        inCode = true
      }
      continue
    }
    if (inCode) {
      codeBuf.push(line)
      continue
    }
    if (/^#{1,2}\s/.test(line)) {
      flushList()
      out.push({ type: 'h2', text: line.replace(/^#+\s*/, '') })
      continue
    }
    if (/^#{3,}\s/.test(line)) {
      flushList()
      out.push({ type: 'h3', text: line.replace(/^#+\s*/, '') })
      continue
    }
    if (/^[-*]\s/.test(line)) {
      listBuf.push(line.replace(/^[-*]\s*/, ''))
      continue
    }
    if (/^\s*$/.test(line)) {
      flushList()
      continue
    }
    flushList()
    out.push({ type: 'p', text: line })
  }
  flushList()
  if (inCode) {
    out.push({ type: 'code', text: codeBuf.join(NL) })
  }
  return out
}

const blocks = computed(() => parseBlocks(props.source))
</script>

<style scoped>
.markdown-view{font-size:14px;line-height:1.78;color:var(--ink);max-width:72ch}
.md-h2{margin:22px 0 10px;font-size:19px;font-weight:650;color:var(--ink);line-height:1.35}
.md-h3{margin:18px 0 8px;font-size:16px;font-weight:600;color:var(--ink);line-height:1.35}
.md-para{margin:0 0 12px}
.md-list{margin:0 0 12px;padding-left:22px;display:grid;gap:4px}
.md-code{margin:0 0 12px;padding:12px 14px;border-radius:10px;background:var(--bg-subtle);color:var(--ink);font-size:13px;overflow-x:auto;white-space:pre-wrap}
</style>
