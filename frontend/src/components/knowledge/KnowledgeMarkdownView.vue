<template>
  <div class="markdown-view" data-test="markdown-view">
    <template v-for="(block, i) in blocks" :key="i">
      <h1 v-if="block.type === 'h1'" class="md-h1" v-html="block.text"></h1>
      <h2 v-else-if="block.type === 'h2'" class="md-h2" v-html="block.text"></h2>
      <h3 v-else-if="block.type === 'h3'" class="md-h3" v-html="block.text"></h3>
      <h4 v-else-if="block.type === 'h4'" class="md-h4" v-html="block.text"></h4>
      <ul v-else-if="block.type === 'list'" class="md-list">
        <li v-for="(item, j) in block.items" :key="j" v-html="item"></li>
      </ul>
      <ol v-else-if="block.type === 'ol'" class="md-list md-ol">
        <li v-for="(item, j) in block.items" :key="j" v-html="item"></li>
      </ol>
      <blockquote v-else-if="block.type === 'quote'" class="md-quote" v-html="block.text"></blockquote>
      <pre v-else-if="block.type === 'code'" class="md-code"><code>{{ block.text }}</code></pre>
      <table v-else-if="block.type === 'table'" class="md-table">
        <thead><tr><th v-for="(cell, c) in block.header" :key="'h' + c" v-html="cell"></th></tr></thead>
        <tbody>
          <tr v-for="(row, r) in block.rows" :key="r"><td v-for="(cell, c) in row" :key="c" v-html="cell"></td></tr>
        </tbody>
      </table>
      <hr v-else-if="block.type === 'hr'" class="md-hr" />
      <p v-else class="md-para" v-html="block.text"></p>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ source: string }>()

interface MdBlock {
  type: 'h1' | 'h2' | 'h3' | 'h4' | 'list' | 'ol' | 'quote' | 'code' | 'table' | 'hr' | 'p'
  text?: string
  items?: string[]
  header?: string[]
  rows?: string[][]
}

const NL = '\n'

/** 先转义 HTML 再应用行内格式（加粗/斜体/行内代码/安全链接），杜绝 XSS。 */
function escapeHtml(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

function renderInline(raw: string): string {
  let s = escapeHtml(raw)
  s = s.replace(/`([^`]+)`/g, '<code class="md-inline-code">$1</code>')
  s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  s = s.replace(/\*([^*]+)\*/g, '<em>$1</em>')
  s = s.replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+|mailto:[^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
  return s
}

function splitCells(row: string): string[] {
  return row.replace(/^\|/, '').replace(/\|$/, '').split('|').map((c) => c.trim())
}

function parseBlocks(src: string): MdBlock[] {
  const out: MdBlock[] = []
  const lines = (src ?? '').split(NL)
  let inCode = false
  let codeBuf: string[] = []
  let listBuf: string[] = []
  let olBuf: string[] = []
  let quoteBuf: string[] = []
  const flushList = () => {
    if (listBuf.length) { out.push({ type: 'list', items: listBuf.map(renderInline) }); listBuf = [] }
    if (olBuf.length) { out.push({ type: 'ol', items: olBuf.map(renderInline) }); olBuf = [] }
    if (quoteBuf.length) { out.push({ type: 'quote', text: renderInline(quoteBuf.join(' ')) }); quoteBuf = [] }
  }
  for (let idx = 0; idx < lines.length; idx++) {
    const line = lines[idx]
    const trimmed = line.trim()
    if (trimmed.startsWith('```')) {
      flushList()
      if (inCode) {
        out.push({ type: 'code', text: codeBuf.join(NL) })
        codeBuf = []; inCode = false
      } else {
        inCode = true
      }
      continue
    }
    if (inCode) { codeBuf.push(line); continue }
    const h = /^(#{1,6})\s+/.exec(line)
    if (h) {
      flushList()
      const level = h[1].length
      const type = level === 1 ? 'h1' : level === 2 ? 'h2' : level === 3 ? 'h3' : 'h4'
      out.push({ type, text: renderInline(line.slice(h[0].length)) })
      continue
    }
    if (/^(-{3,}|\*{3,}|_{3,})$/.test(trimmed)) { flushList(); out.push({ type: 'hr' }); continue }
    if (trimmed.startsWith('>')) {
      flushList()
      quoteBuf.push(trimmed.replace(/^>\s?/, ''))
      continue
    }
    if (trimmed.startsWith('|') && trimmed.endsWith('|') && idx + 1 < lines.length && /^\|?[\s:|-]+\|?$/.test(lines[idx + 1].trim())) {
      flushList()
      const header = splitCells(trimmed).map(renderInline)
      const rows: string[][] = []
      idx++
      while (idx + 1 < lines.length && lines[idx + 1].trim().startsWith('|')) {
        idx++
        rows.push(splitCells(lines[idx].trim()).map(renderInline))
      }
      out.push({ type: 'table', header, rows })
      continue
    }
    if (/^[-*]\s/.test(line)) { listBuf.push(line.replace(/^[-*]\s*/, '')); continue }
    if (/^\d+\.\s/.test(line)) { olBuf.push(line.replace(/^\d+\.\s*/, '')); continue }
    if (/^\s*$/.test(line)) { flushList(); continue }
    flushList()
    out.push({ type: 'p', text: renderInline(line) })
  }
  flushList()
  if (inCode) out.push({ type: 'code', text: codeBuf.join(NL) })
  return out
}

const blocks = computed(() => parseBlocks(props.source))
</script>
<style scoped>
.markdown-view{font-size:16px;line-height:1.85;color:var(--ink);max-width:70ch}
.md-h1{margin:28px 0 12px;font-size:26px;font-weight:700;color:var(--ink);line-height:1.3;letter-spacing:-.01em}
.md-h2{margin:24px 0 10px;font-size:21px;font-weight:650;color:var(--ink);line-height:1.35}
.md-h3{margin:20px 0 8px;font-size:18px;font-weight:600;color:var(--ink);line-height:1.35}
.md-h4{margin:18px 0 6px;font-size:16px;font-weight:600;color:var(--ink);line-height:1.35}
.md-para{margin:0 0 12px}
.md-para strong,.md-list strong,.md-table strong{font-weight:700}
.md-para em,.md-list em{font-style:italic}
.md-inline-code{padding:1px 6px;border-radius:5px;background:var(--bg-subtle);color:var(--brand);font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:.88em}
.md-para a,.md-list a,.md-table a{color:var(--brand);text-decoration:underline;text-underline-offset:2px}
.md-list{margin:0 0 12px;padding-left:24px;display:grid;gap:4px}
.md-ol{list-style:decimal}
.md-quote{margin:0 0 12px;padding:2px 0 2px 14px;border-left:3px solid var(--brand-soft);color:var(--copy);font-size:15px}
.md-code{margin:0 0 14px;padding:14px 16px;border-radius:10px;background:var(--bg-subtle);color:var(--ink);font-size:13.5px;line-height:1.6;overflow-x:auto;white-space:pre-wrap;font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace}
.md-table{margin:0 0 14px;border-collapse:collapse;font-size:14px;line-height:1.6;width:100%}
.md-table th,.md-table td{padding:7px 12px;border:1px solid var(--border-default);text-align:left}
.md-table thead th{background:var(--bg-subtle);font-weight:600;color:var(--ink)}
.md-table tbody tr:nth-child(even){background:var(--surface-subtle)}
.md-hr{margin:18px 0;border:0;border-top:1px solid var(--border-default)}
</style>