<script setup lang="ts">
// 统一的公司标识：单色字形衬在品牌色底上；真实字标放在白盘上；其余用首字母。
// 一个组件替换掉日程、便签、目标卡里四处重复的「img / 首字母」写法。
import { computed } from 'vue'
import { companyMark } from '../constants/companyBrands'

const props = withDefaults(defineProps<{ name?: string | null; size?: number }>(), { size: 30 })

const mark = computed(() => companyMark(props.name))

// 有真实字标（整条 logo）时底用白盘，否则用品牌色衬字形/首字母。
const chipStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  borderRadius: `${Math.round(props.size * 0.3)}px`,
  background: mark.value.icon ? 'var(--logo-plate)' : mark.value.color,
}))

const glyphStyle = computed(() => ({
  width: `${Math.round(props.size * 0.62)}px`,
  height: `${Math.round(props.size * 0.62)}px`,
  // 注册表显式指定的前景色优先（整体填充型 App 图标需要强制白）；
  // 否则 lightText = 背景是深色 → 白色字形，与 letterStyle 同一语义。
  color: mark.value.fg ?? (mark.value.lightText ? '#ffffff' : '#171717'),
}))

const letterStyle = computed(() => ({
  color: mark.value.lightText ? '#ffffff' : '#171717',
  fontSize: `${Math.round(props.size * 0.4)}px`,
}))
</script>

<template>
  <span class="company-mark" :style="chipStyle" :aria-label="name || '公司'" role="img">
    <!-- 单色字形：currentColor 继承 glyphStyle 的色，落在品牌色底上 -->
    <span v-if="mark.glyph" class="cmp-glyph" :style="glyphStyle" v-html="mark.glyph" />
    <img v-else-if="mark.icon" class="cmp-img" :src="mark.icon" alt="" aria-hidden="true" />
    <span v-else class="cmp-letter" :style="letterStyle">{{ mark.letter }}</span>
  </span>
</template>

<style scoped>
.company-mark {
  display: inline-grid;
  place-items: center;
  flex: 0 0 auto;
  overflow: hidden;
  border: 1px solid var(--line-subtle);
  box-shadow: var(--shadow-card);
}
.cmp-glyph { display: block; line-height: 0; }
.cmp-glyph :deep(svg) { display: block; width: 100%; height: 100%; }
.cmp-img { width: 74%; height: 74%; object-fit: contain; }
.cmp-letter {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1;
}
</style>
