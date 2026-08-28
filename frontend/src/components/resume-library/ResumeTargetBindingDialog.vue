<template>
  <div v-if="open" class="binding-backdrop" role="presentation" @click.self="emit('cancel')">
    <section class="binding-dialog" role="dialog" aria-modal="true" aria-labelledby="binding-title" data-test="resume-target-binding-dialog">
      <header class="binding-head">
        <div>
          <span>求职目标</span>
          <h2 id="binding-title">选择要关联的目标</h2>
          <p>绑定只改变目标使用的简历版本，不会复制或覆盖正文。</p>
        </div>
        <button type="button" class="binding-close" aria-label="关闭绑定选择" @click="emit('cancel')">×</button>
      </header>

      <div v-if="targets.length" class="target-options">
        <button
          v-for="target in targets"
          :key="target.targetId"
          type="button"
          class="target-option"
          :class="{ selected: selectedId === target.targetId }"
          :data-test="`target-option-${target.targetId}`"
          @click="selectedId = target.targetId"
        >
          <span class="target-option-mark" aria-hidden="true">
            <img v-if="companyMarkFor(target).icon" :src="companyMarkFor(target).icon" alt="" />
            <span v-else :style="{ background: companyMarkFor(target).color, color: companyMarkFor(target).lightText ? '#fff' : '#1b1b1b' }">{{ companyMarkFor(target).letter }}</span>
          </span>
          <span class="target-option-copy">
            <strong>{{ target.label }}</strong>
            <small v-if="target.stageLabel">{{ target.stageLabel }}<template v-if="target.targetRole"> · {{ target.targetRole }}</template></small>
            <small v-if="target.resumeVersionId" class="target-bound">已绑定简历版本</small>
            <small v-else class="target-unbound">尚未绑定简历</small>
          </span>
          <span v-if="selectedId === target.targetId" class="target-check" aria-hidden="true">✓</span>
        </button>
      </div>
      <p v-else class="binding-empty">还没有求职目标，请先创建一个目标。</p>

      <footer class="binding-actions">
        <button type="button" class="binding-cancel" @click="emit('cancel')">取消</button>
        <button type="button" class="binding-confirm" data-test="confirm-binding" :disabled="selectedId === null" @click="confirm">确认绑定</button>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { companyMark } from '../../constants/companyBrands'
import type { CompanyMark } from '../../constants/companyBrands.types'

export interface ResumeTargetBindingOption {
  targetId: number
  label: string
  companyName?: string | null
  stageLabel?: string
  targetRole?: string | null
  resumeVersionId: number | null
}

const props = defineProps<{
  open: boolean
  targets: ResumeTargetBindingOption[]
}>()

const emit = defineEmits<{
  confirm: [targetId: number]
  cancel: []
}>()

const selectedId = ref<number | null>(null)

function companyMarkFor(target: ResumeTargetBindingOption): CompanyMark {
  return companyMark(target.companyName || target.label)
}

watch(() => props.open, (open) => {
  if (open) selectedId.value = null
})

function confirm() {
  if (selectedId.value === null) return
  emit('confirm', selectedId.value)
}
</script>

<style scoped>
.binding-backdrop{position:fixed;inset:0;z-index:85;display:grid;place-items:center;padding:24px;background:rgba(15,18,18,.28);backdrop-filter:blur(5px)}
.binding-dialog{width:min(460px,calc(100vw - 40px));max-height:min(660px,calc(100vh - 48px));overflow:auto;border:1px solid var(--border-subtle);border-radius:16px;background:var(--surface-solid,#fff);box-shadow:0 24px 70px rgba(0,0,0,.18);padding:20px;color:var(--ink)}
.binding-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.binding-head span{color:var(--muted);font-size:10px;font-weight:750;letter-spacing:.1em}.binding-head h2{margin:5px 0 4px;font-size:19px;letter-spacing:-.02em}.binding-head p{margin:0;color:var(--muted);font-size:11px;line-height:1.55}.binding-close{border:0;background:transparent;color:var(--muted);font-size:24px;line-height:1;cursor:pointer}.target-options{display:grid;gap:7px;margin-top:18px}.target-option{display:grid;grid-template-columns:32px minmax(0,1fr) auto;align-items:center;gap:10px;width:100%;border:1px solid var(--border-subtle);border-radius:10px;background:var(--surface-solid,#fff);padding:10px;text-align:left;cursor:pointer;transition:border-color .14s ease,background .14s ease,box-shadow .14s ease}.target-option:hover{border-color:var(--copy);background:var(--bg-hover)}.target-option.selected{border-color:var(--copy);box-shadow:0 0 0 2px rgba(28,31,35,.08)}.target-option-mark{display:grid;place-items:center;width:30px;height:30px;border-radius:9px;background:var(--bg-subtle);color:var(--copy);font-size:13px;font-weight:750}.target-option-copy{display:grid;gap:2px;min-width:0}.target-option-copy strong{overflow:hidden;color:var(--copy);font-size:12px;text-overflow:ellipsis;white-space:nowrap}.target-option-copy small{overflow:hidden;color:var(--muted);font-size:10.5px;text-overflow:ellipsis;white-space:nowrap}.target-option-copy .target-bound{color:var(--copy)}.target-check{display:grid;place-items:center;width:20px;height:20px;border-radius:50%;background:var(--copy);color:#fff;font-size:11px;font-weight:750}.binding-empty{margin:20px 0;color:var(--muted);font-size:12px}.binding-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:18px}.binding-actions button{border:1px solid var(--border-default);border-radius:8px;padding:7px 12px;font:inherit;font-size:11.5px;cursor:pointer}.binding-cancel{background:transparent;color:var(--copy)}.binding-confirm{border-color:var(--copy)!important;background:var(--copy);color:#fff;font-weight:650}.binding-confirm:disabled{border-color:var(--border-default)!important;background:var(--bg-subtle);color:var(--muted);cursor:not-allowed}
.target-option-mark{overflow:hidden}.target-option-mark img{width:22px;height:22px;object-fit:contain}.target-option-mark>span{display:grid;place-items:center;width:100%;height:100%;border-radius:inherit}.binding-confirm{border-color:#17181a!important;background:#17181a;color:#fff}.binding-confirm:hover:not(:disabled){background:#2b2c2e}
</style>
