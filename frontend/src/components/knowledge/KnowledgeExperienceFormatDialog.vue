<template>
  <div class="format-mask" data-test="knowledge-experience-format-dialog" @click.self="$emit('close')">
    <section class="format-dialog" role="dialog" aria-modal="true" aria-labelledby="experience-format-title">
      <header class="format-head">
        <div>
          <span class="format-kicker">REAL INTERVIEW SOURCE</span>
          <h2 id="experience-format-title">真实面经格式</h2>
          <p>在知识库的“真实面经”文件夹中录入或导入，系统会按同一规则识别。</p>
        </div>
        <button type="button" class="close-btn" aria-label="关闭格式说明" @click="$emit('close')">×</button>
      </header>
      <div class="format-body">
        <div class="format-section">
          <div class="section-title"><span>01</span><strong>复制模板</strong></div>
          <pre data-test="knowledge-experience-format-template"><code>{{ template }}</code></pre>
          <button type="button" class="copy-btn" data-test="knowledge-experience-format-copy" @click="copyTemplate">{{ copied ? '已复制模板' : '复制模板' }}</button>
          <p v-if="copyError" class="copy-error" data-test="knowledge-experience-format-copy-error">{{ copyError }}</p>
        </div>
        <div class="format-section rules">
          <div class="section-title"><span>02</span><strong>识别规则</strong></div>
          <ul>
            <li><b>company / role / icon</b> 是可选的显式元数据，用于公司、岗位和本地 Logo。</li>
            <li>正文使用 <b>1.</b>、<b>1)</b>、<b>1、</b> 或 <b>-</b> 开头，每行一题。</li>
            <li>导入的 PDF/TXT/Markdown 会先完成文本提取，再按同一规则识别，不改写原文件。</li>
            <li>识别成功后，知识库会显示题目数量；不符合格式时显示具体原因。</li>
          </ul>
        </div>
      </div>
      <footer class="format-foot"><span>题集只在真题演练中选择资料时创建，原始资料始终保留在知识库。</span><button type="button" class="done-btn" @click="$emit('close')">知道了</button></footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineEmits<{ (e: 'close'): void }>()

const template = `---
company: 腾讯
role: Java 后端
icon: tencent
---

1. 讲讲 Redis 缓存一致性
2. 如何排查慢查询？
3. 介绍一个你处理高并发的项目`
const copied = ref(false)
const copyError = ref('')

async function copyTemplate() {
  copyError.value = ''
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(template)
    } else {
      const area = document.createElement('textarea')
      area.value = template
      area.setAttribute('readonly', '')
      area.style.position = 'fixed'
      area.style.opacity = '0'
      document.body.appendChild(area)
      area.select()
      const copiedByFallback = document.execCommand('copy')
      area.remove()
      if (!copiedByFallback) throw new Error('系统未允许访问剪贴板')
    }
    copied.value = true
    window.setTimeout(() => { copied.value = false }, 1800)
  } catch (error) {
    copyError.value = error instanceof Error ? error.message : '复制失败，请手动选中模板'
  }
}
</script>

<style scoped>
.format-mask{position:fixed;inset:0;z-index:80;display:grid;place-items:center;padding:24px;background:rgba(16,18,20,.38);backdrop-filter:blur(3px)}
.format-dialog{width:min(720px,calc(100vw - 48px));max-height:min(720px,calc(100vh - 48px));overflow:auto;border:1px solid var(--border-default);border-radius:18px;background:var(--surface-solid);color:var(--ink);box-shadow:0 24px 70px rgba(0,0,0,.22)}
.format-head{display:flex;justify-content:space-between;gap:20px;padding:26px 28px 18px;border-bottom:1px solid var(--border-subtle)}
.format-kicker{font-size:10px;letter-spacing:.16em;color:var(--brand);font-weight:700}.format-head h2{margin:7px 0 6px;font-size:22px;letter-spacing:-.02em}.format-head p{margin:0;color:var(--muted);font-size:13px}.close-btn{width:28px;height:28px;border:0;border-radius:8px;background:transparent;color:var(--muted);font-size:22px;cursor:pointer}.close-btn:hover{background:var(--bg-hover);color:var(--ink)}
.format-body{display:grid;grid-template-columns:1.12fr .88fr;gap:26px;padding:24px 28px}.format-section{min-width:0}.section-title{display:flex;align-items:center;gap:10px;margin-bottom:12px;font-size:14px}.section-title span{font-variant-numeric:tabular-nums;color:var(--brand);font-size:11px;letter-spacing:.08em}.format-section pre{margin:0;min-height:178px;padding:16px;border:1px solid var(--border-subtle);border-radius:12px;background:var(--bg-subtle);overflow:auto;color:var(--ink);font:12px/1.7 ui-monospace,SFMono-Regular,Menlo,monospace}.copy-btn,.done-btn{margin-top:12px;padding:8px 13px;border:1px solid var(--border-default);border-radius:9px;background:var(--surface-solid);color:var(--copy);font-size:12px;cursor:pointer}.copy-btn:hover,.done-btn:hover{border-color:var(--brand);color:var(--brand)}.copy-error{margin:8px 0 0;color:var(--danger);font-size:11px}.rules ul{display:grid;gap:13px;margin:0;padding:4px 0 0 18px;color:var(--copy);font-size:13px;line-height:1.65}.rules b{color:var(--ink);font-weight:650}.format-foot{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:14px 28px 20px;border-top:1px solid var(--border-subtle);color:var(--muted);font-size:11px}.done-btn{margin:0;background:var(--action-bg);border-color:var(--action-bg);color:var(--action-fg);font-weight:600}.done-btn:hover{color:var(--action-fg);opacity:.9}
@media (max-width:640px){.format-body{grid-template-columns:1fr}.format-foot{align-items:flex-start;flex-direction:column}.format-head{padding:22px}.format-body{padding:20px 22px}.format-foot{padding:14px 22px 18px}}
</style>
