<script setup lang="ts" generic="T extends string | number">
import { computed, nextTick, onScopeDispose, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'
import { registerOverlay } from '../data/overlays'

interface Option<T> { value: T; label: string; hint?: string; meta?: string }
const props = withDefaults(defineProps<{
  modelValue: T | null
  options: Option<T>[]
  title?: string
  placeholder?: string
  label?: string
  clearable?: boolean
  clearLabel?: string
  searchable?: boolean
  icon?: string
  disabled?: boolean
}>(), {
  title: '请选择', placeholder: '请选择', label: '', clearable: false,
  clearLabel: '不关联', searchable: false, icon: '', disabled: false,
})
const emit = defineEmits<{ (e: 'update:modelValue', v: T | null): void }>()

const open = ref(false)
const keyword = ref('')
const listRef = ref<HTMLElement | null>(null)

let unregisterOverlay: (() => void) | null = null
watch(open, (v) => {
  unregisterOverlay?.()
  unregisterOverlay = v ? registerOverlay(() => { open.value = false }) : null
})
onScopeDispose(() => unregisterOverlay?.())

const current = computed(() => props.options.find((o) => o.value === props.modelValue) ?? null)
const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return props.options
  return props.options.filter((o) => o.label.toLowerCase().includes(k) || (o.hint ?? '').toLowerCase().includes(k))
})

async function show() {
  if (props.disabled) return
  open.value = true
  keyword.value = ''
  await nextTick()
  const el = listRef.value?.querySelector<HTMLElement>('[data-selected="true"]')
  if (el) el.scrollIntoView({ block: 'center' })
}
function pick(v: T | null) { emit('update:modelValue', v); open.value = false }
</script>

<template>
  <div class="picker-field">
    <span v-if="label" class="pf-label">{{ label }}</span>
    <button type="button" class="pf-trigger" :class="{ placeholder: !current }" :disabled="disabled" @click="show">
      <AppIcon v-if="current && icon" :name="icon" :size="17" class="pf-lead" />
      <span class="pf-text">{{ current ? current.label : placeholder }}</span>
      <span v-if="current?.meta" class="pf-meta">{{ current.meta }}</span>
      <AppIcon name="chevronDown" :size="18" class="pf-chev" />
    </button>
  </div>

  <Teleport to="body">
    <div v-if="open" class="sheet-backdrop" @click="open = false" />
    <section v-if="open" class="sheet picker-sheet" role="dialog" aria-modal="true" :aria-label="title">
      <div class="sheet-grab" />
      <header class="picker-head">
        <button class="picker-x" aria-label="关闭" @click="open = false"><AppIcon name="close" :size="20" /></button>
        <h2>{{ title }}</h2>
        <span class="picker-spacer" />
      </header>
      <label v-if="searchable" class="picker-search">
        <AppIcon name="search" :size="16" />
        <input v-model="keyword" placeholder="搜索…" autocomplete="off">
      </label>
      <div ref="listRef" class="picker-list">
        <button v-if="clearable" class="picker-row" :class="{ on: modelValue == null }" @click="pick(null)">
          <span class="pr-label muted">{{ clearLabel }}</span>
          <AppIcon v-if="modelValue == null" name="check" :size="20" class="pr-check" />
        </button>
        <button
          v-for="o in filtered" :key="String(o.value)"
          class="picker-row" :class="{ on: o.value === modelValue }"
          :data-selected="o.value === modelValue" @click="pick(o.value)"
        >
          <span class="pr-main">
            <span class="pr-label">{{ o.label }}</span>
            <span v-if="o.hint" class="pr-hint">{{ o.hint }}</span>
          </span>
          <span v-if="o.meta" class="pr-meta">{{ o.meta }}</span>
          <AppIcon v-if="o.value === modelValue" name="check" :size="20" class="pr-check" />
        </button>
        <p v-if="searchable && !filtered.length" class="picker-empty">没有匹配项。</p>
      </div>
    </section>
  </Teleport>
</template>
