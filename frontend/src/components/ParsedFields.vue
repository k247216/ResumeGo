<template>
  <div class="parsed-fields">
    <div class="field-group">
      <h4>工作职责</h4>
      <p v-if="!parsed.responsibilities?.length" class="empty-text">暂无</p>
      <ul v-else>
        <li v-for="(item, i) in parsed.responsibilities" :key="'resp-' + i">
          {{ item }}
        </li>
      </ul>
    </div>

    <div class="field-group">
      <h4>经验要求</h4>
      <p v-if="!parsed.experienceRequirements?.length" class="empty-text">暂无</p>
      <ul v-else>
        <li v-for="(item, i) in parsed.experienceRequirements" :key="'exp-' + i">
          {{ item }}
        </li>
      </ul>
    </div>

    <div class="field-group">
      <h4>学历要求</h4>
      <p v-if="!parsed.educationRequirements?.length" class="empty-text">不限</p>
      <ul v-else>
        <li v-for="(item, i) in parsed.educationRequirements" :key="'edu-' + i">
          {{ item }}
        </li>
      </ul>
    </div>

    <el-empty v-if="isEmpty" description="暂无结构化解析结果" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ParsedJobDescription } from '../types/job'

const props = defineProps<{
  parsed: ParsedJobDescription
}>()

const isEmpty = computed(() => {
  const p = props.parsed
  return (
    (!p.responsibilities || p.responsibilities.length === 0) &&
    (!p.experienceRequirements || p.experienceRequirements.length === 0) &&
    (!p.educationRequirements || p.educationRequirements.length === 0)
  )
})
</script>

<style scoped>
.parsed-fields {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.field-group {
  border: 1px solid var(--line, #e5eaf2);
  border-radius: 16px;
  background: var(--surface-solid, #ffffff);
  padding: 13px;
}

.field-group h4 {
  margin: 0 0 9px;
  color: var(--ink, #101a33);
  font-size: 13px;
  font-weight: 950;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-text {
  margin: 0;
  color: var(--muted, #94a3b8);
  font-size: 13px;
}

ul {
  margin: 0;
  padding-left: 18px;
}

ul li {
  color: var(--copy, #475569);
  font-size: 13px;
  line-height: 1.8;
}
</style>
