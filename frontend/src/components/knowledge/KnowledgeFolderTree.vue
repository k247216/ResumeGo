<template>
  <div class="folder-tree" data-test="knowledge-folder-tree">
    <ul class="tree-root">
      <KnowledgeFolderNode
        v-for="root in roots"
        :key="root.id"
        :node="root"
        :all-nodes="nodes"
        :expanded-ids="expandedIds"
        :selected-id="selectedId"
        @toggle="$emit('toggle', $event)"
        @select="$emit('select', $event)"
        @new-child="$emit('new-child', $event)"
        @rename="$emit('rename', $event)"
        @move="$emit('move', $event)"
        @delete="$emit('delete', $event)"
      />
    </ul>
    <p v-if="!nodes.length" class="tree-empty" data-test="folder-tree-empty">还没有资料文件夹，点击「新建文件夹」开始整理。</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import KnowledgeFolderNode from './KnowledgeFolderNode.vue'
import type { KnowledgeCategoryNode } from '../../types/knowledge'

const props = defineProps<{
  nodes: KnowledgeCategoryNode[]
  expandedIds: Set<number>
  selectedId: number | null
}>()

defineEmits<{
  (e: 'toggle', id: number): void
  (e: 'select', id: number): void
  (e: 'new-child', parentId: number | null): void
  (e: 'rename', id: number): void
  (e: 'move', id: number): void
  (e: 'delete', id: number): void
}>()

const roots = computed(() => props.nodes.filter((n) => n.parentId == null))
</script>

<style scoped>
.folder-tree{font-size:14px}
.tree-root{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:1px}
.tree-empty{padding:10px 6px;color:var(--muted);font-size:13px}
</style>
