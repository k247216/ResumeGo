<template>
  <div class="kselect" :class="{ open }" ref="root">
    <button
      type="button"
      class="kselect-trigger"
      :class="{ disabled }"
      :disabled="disabled"
      :data-test="testId"
      :aria-haspopup="'listbox'"
      :aria-expanded="open"
      @click="toggle"
    >
      <span class="kselect-value" :class="{ placeholder: !displayLabel }">{{ displayLabel || placeholder }}</span>
      <el-icon class="kselect-chevron" :size="14"><CaretBottom /></el-icon>
    </button>
    <div v-if="open" class="kselect-menu" :data-test="testId + '-menu'" role="listbox">
      <button
        v-for="opt in options"
        :key="opt.value"
        type="button"
        class="kselect-option"
        :class="{ selected: opt.value === modelValue }"
        :style="opt.indent ? { paddingLeft: (12 + opt.indent * 14) + 'px' } : undefined"
        :data-test="testId + '-option-' + String(opt.value)"
        role="option"
        :aria-selected="opt.value === modelValue"
        @click="pick(opt.value)"
      >
        <span class="kselect-option-label">{{ opt.label }}</span>
        <el-icon v-if="opt.value === modelValue" class="kselect-check" :size="13"><Check /></el-icon>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { CaretBottom, Check } from '@element-plus/icons-vue'

export interface KnowledgeSelectOption {
  value: string
  label: string
  indent?: number
}

const props = defineProps<{
  modelValue: string
  options: KnowledgeSelectOption[]
  placeholder?: string
  testId: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'change', value: string): void
}>()

const open = ref(false)
const root = ref<HTMLElement | null>(null)

const displayLabel = computed(() => {
  const hit = props.options.find((o) => o.value === props.modelValue)
  return hit ? hit.label : ''
})

function toggle() {
  if (props.disabled) return
  open.value = !open.value
}

function pick(value: string) {
  emit('change', value)
  open.value = false
}

function onGlobalPointer(event: PointerEvent) {
  if (open.value && root.value && !root.value.contains(event.target as Node)) {
    open.value = false
  }
}

function onKeydown(event: KeyboardEvent) {
  if (open.value && event.key === 'Escape') open.value = false
}

onMounted(() => {
  document.addEventListener('pointerdown', onGlobalPointer)
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onGlobalPointer)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.kselect{position:relative;width:100%}
.kselect-trigger{box-sizing:border-box;display:flex;align-items:center;justify-content:space-between;gap:8px;width:100%;height:36px;padding:0 12px;border:1px solid var(--border-default);border-radius:8px;background:var(--surface-solid);color:var(--ink);font-size:13px;font-weight:500;cursor:pointer;transition:border-color .16s ease,box-shadow .16s ease}
.kselect-trigger:hover:not(.disabled){border-color:var(--copy)}
.kselect-trigger:focus-visible{outline:0;border-color:var(--brand);box-shadow:0 0 0 3px var(--brand-soft)}
.kselect-trigger.disabled{cursor:not-allowed;opacity:.58}
.kselect.open .kselect-trigger{border-color:var(--brand);box-shadow:0 0 0 3px var(--brand-soft)}
.kselect-value{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-align:left}
.kselect-value.placeholder{color:var(--muted);font-weight:450}
.kselect-chevron{flex:none;color:var(--muted);transition:transform .16s ease}
.kselect.open .kselect-chevron{transform:rotate(180deg);color:var(--brand)}
.kselect-menu{position:absolute;z-index:40;top:calc(100% + 5px);left:0;right:0;max-height:224px;overflow-y:auto;padding:5px;border:1px solid var(--border-default);border-radius:10px;background:var(--surface-solid);box-shadow:0 10px 28px rgba(0,0,0,.14)}
.kselect-option{display:flex;align-items:center;justify-content:space-between;gap:8px;width:100%;padding:8px 10px;border:0;border-radius:7px;background:transparent;color:var(--copy);font-size:13px;text-align:left;cursor:pointer}
.kselect-option:hover{background:var(--bg-hover);color:var(--ink)}
.kselect-option.selected{background:var(--brand-soft);color:var(--brand);font-weight:600}
.kselect-option-label{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.kselect-check{flex:none;color:var(--brand)}
</style>