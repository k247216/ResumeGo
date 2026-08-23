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
      <pre v-else-if="block.type === 'code'" class="md-code" :data-lang="block.lang">
        <span v-if="block.lang" class="md-code-head">{{ langLabel(block.lang) }}</span>
        <code v-html="block.html"></code>
      </pre>
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
  lang?: string
  html?: string
}

const NL = '\n'

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

const LANG_LABELS: Record<string, string> = {
  ts: 'TypeScript', js: 'JavaScript', python: 'Python', py: 'Python', java: 'Java',
  sql: 'SQL', bash: 'Bash', sh: 'Shell', json: 'JSON', css: 'CSS', html: 'HTML',
  xml: 'XML', yaml: 'YAML', md: 'Markdown', markdown: 'Markdown', go: 'Go', rust: 'Rust',
}

function langLabel(lang: string): string {
  return LANG_LABELS[lang] ?? lang
}

const KEYWORDS: Record<string, string[]> = {
  ts: 'const let var function return if else for while class interface type new import export async await default switch case break continue try catch throw extends implements readonly static public private protected yield void boolean number string any null undefined true false'.split(' '),
  js: 'const let var function return if else for while class new import export async await default switch case break continue try catch throw extends static yield void null undefined true false'.split(' '),
  python: 'def return if elif else for while class import from as in not and or try except finally lambda None True False pass break continue with yield global raise is del'.split(' '),
  java: 'public private protected class interface extends implements return if else for while do switch case break continue new try catch finally throw throws static final void int long double boolean char float byte String import package this super null true false'.split(' '),
  sql: 'select from where insert into values update set delete create table drop alter join left right inner on and or not null as group by order having limit distinct count sum avg min max primary key foreign references'.split(' '),
  bash: 'if then else fi for do done while case esac function export echo cd ls rm mkdir cp mv sudo apt get curl wget exit return'.split(' '),
  go: 'package import func var const type struct interface if else for range return go defer map chan select switch case break continue true false nil'.split(' '),
  rust: 'fn let mut pub struct enum impl trait use mod match if else for while loop return async await move ref true false'.split(' '),
}

/** 轻量语法高亮：先转义 HTML；注释/字符串先保护为占位（无字面字符），再标色关键字与数字（XSS 安全）。 */
function highlightCode(lang: string, code: string): string {
  const escaped = escapeHtml(code)
  const protectedTokens: string[] = []
  const MARK = String.fromCharCode(2)
  const protect = (match: string): string => {
    protectedTokens.push(match)
    return MARK.repeat(protectedTokens.length)
  }
  let s = escaped
  s = s.replace(/(\/\/[^\n]*|#[^\n]*|--[^\n]*|\/\*[\s\S]*?\*\/)/g, (m) => protect('<span class="tok-comment">' + m + '</span>'))
  s = s.replace(/(&quot;.*?&quot;|&#39;.*?&#39;|`.*?`)/g, (m) => protect('<span class="tok-string">' + m + '</span>'))
  const kws = KEYWORDS[lang] ?? []
  if (kws.length) {
    const pattern = new RegExp('\\b(' + kws.join('|') + ')\\b', 'g')
    s = s.replace(pattern, (m) => protect('<span class="tok-keyword">' + m + '</span>'))
  }
  s = s.replace(/\b(\d+(?:\.\d+)?)\b/g, (m) => protect('<span class="tok-number">' + m + '</span>'))
  return s.replace(new RegExp(MARK + '+', 'g'), (m) => protectedTokens[m.length - 1] ?? '')
}

function splitCells(row: string): string[] {
  return row.replace(/^\|/, '').replace(/\|$/, '').split('|').map((c) => c.trim())
}

function parseBlocks(src: string): MdBlock[] {
  const out: MdBlock[] = []
  const lines = (src ?? '').split(NL)
  let inCode = false
  let codeFenceLang = ''
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
        const raw = codeBuf.join(NL)
        const lang = codeFenceLang
        out.push({ type: 'code', text: raw, lang, html: lang ? highlightCode(lang, raw) : escapeHtml(raw) })
        codeBuf = []; inCode = false; codeFenceLang = ''
      } else {
        inCode = true
        codeFenceLang = trimmed.replace(/^```/, '').trim()
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
  if (inCode) {
    const raw = codeBuf.join(NL)
    out.push({ type: 'code', text: raw, lang: codeFenceLang, html: codeFenceLang ? highlightCode(codeFenceLang, raw) : escapeHtml(raw) })
  }
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
.md-inline-code{padding:1px 6px;border-radius:5px;background:var(--bg-subtle);color:var(--brand);font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:.88em}
.md-para a,.md-list a,.md-table a{color:var(--brand);text-decoration:underline;text-underline-offset:2px}
.md-list{margin:0 0 12px;padding-left:24px;display:grid;gap:4px}
.md-ol{list-style:decimal}
.md-quote{margin:0 0 12px;padding:2px 0 2px 14px;border-left:3px solid var(--brand-soft);color:var(--copy);font-size:15px}
.md-code{margin:0 0 14px;padding:12px 16px 14px;border-radius:10px;background:var(--bg-subtle);color:var(--ink);font-size:13.5px;line-height:1.6;overflow-x:auto;font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace}
.md-code code{white-space:pre-wrap;display:block}
.md-code-head{display:inline-flex;align-items:center;margin:-12px 0 8px -16px;padding:4px 12px;border-radius:10px 0 10px 0;background:var(--border-subtle);color:var(--muted);font-size:10.5px;font-weight:650;letter-spacing:.06em;text-transform:uppercase;font-family:inherit}
.tok-comment{color:var(--muted);font-style:italic}
.tok-string{color:#2f9e6e}
.tok-keyword{color:var(--brand);font-weight:600}
.tok-number{color:#c4772f}
.md-table{margin:0 0 14px;border-collapse:collapse;font-size:14px;line-height:1.6;width:100%}
.md-table th,.md-table td{padding:7px 12px;border:1px solid var(--border-default);text-align:left}
.md-table thead th{background:var(--bg-subtle);font-weight:600;color:var(--ink)}
.md-table tbody tr:nth-child(even){background:var(--surface-subtle)}
.md-hr{margin:18px 0;border:0;border-top:1px solid var(--border-default)}
</style>