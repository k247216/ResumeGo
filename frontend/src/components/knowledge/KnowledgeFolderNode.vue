<template>
  <li class="tree-node">
    <div class="tree-row" :class="{ selected: node.id === selectedId }" :data-test="'folder-node-' + node.id">
      <button
        v-if="children.length"
        type="button"
        class="tree-toggle"
        :aria-label="expanded ? '收起' : '展开'"
        :data-test="'folder-toggle-' + node.id"
        @click="toggle"
      ><el-icon :size="12"><component :is="expanded ? CaretBottom : CaretRight" /></el-icon></button>
      <span v-else class="tree-toggle tree-toggle-placeholder" aria-hidden="true"></span>
      <button type="button" class="tree-name" :data-test="'folder-select-' + node.id" @click="$emit('select', node.id)">
        <el-icon
          class="folder-icon"
          :data-test="'folder-icon-' + node.id"
          :data-state="expanded ? 'open' : 'closed'"
          :size="15"
        ><component :is="expanded ? FolderOpened : Folder" /></el-icon>
        <span>{{ node.name }}</span>
      </button>
      <span class="tree-count">{{ node.descendantDocumentCount }}</span>
      <span class="tree-actions">
        <button type="button" class="tree-action" data-test="folder-new-child" title="新建子文件夹" @click.stop="$emit('new-child', node.id)">＋</button>
        <button type="button" class="tree-action" data-test="folder-rename" title="重命名/移动" @click.stop="$emit('rename', node.id)">✎</button>
        <button type="button" class="tree-action danger" data-test="folder-delete" title="删除" aria-label="删除文件夹" @click.stop="$emit('delete', node.id)"><el-icon :size="12"><Close /></el-icon></button>
      </span>
    </div>
    <ul v-if="expanded && children.length" class="tree-children">
      <KnowledgeFolderNode
        v-for="child in children"
        :key="child.id"
        :node="child"
        :all-nodes="allNodes"
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
  </li>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CaretBottom, CaretRight, Close, Folder, FolderOpened } from '@element-plus/icons-vue'
import KnowledgeFolderNode from './KnowledgeFolderNode.vue'
import type { KnowledgeCategoryNode } from '../../types/knowledge'

const props = defineProps<{
  node: KnowledgeCategoryNode
  allNodes: KnowledgeCategoryNode[]
  expandedIds: Set<number>
  selectedId: number | null
}>()

const emit = defineEmits<{
  (e: 'toggle', id: number): void
  (e: 'select', id: number): void
  (e: 'new-child', parentId: number | null): void
  (e: 'rename', id: number): void
  (e: 'move', id: number): void
  (e: 'delete', id: number): void
}>()

const children = computed(() => props.allNodes.filter((n) => n.parentId === props.node.id))
const expanded = computed(() => props.expandedIds.has(props.node.id))

function toggle() {
  if (children.value.length) {
    emit('toggle', props.node.id)
  }
}
</script>

<style scoped>
.tree-node{list-style:none}
.tree-row{display:flex;align-items:center;gap:4px;padding:4px 6px;border-radius:8px;font-size:13px}
.tree-row:hover{background:var(--bg-hover)}
.tree-row.selected{background:var(--bg-selected)}
.tree-toggle{display:grid;place-items:center;border:0;background:transparent;color:var(--copy);width:16px;padding:0;cursor:pointer;flex:none}
.tree-toggle-placeholder{cursor:default}
.tree-name{display:flex;align-items:center;gap:7px;border:0;background:transparent;color:var(--ink);flex:1;min-width:0;text-align:left;cursor:pointer;overflow:hidden;white-space:nowrap}
.tree-name span{overflow:hidden;text-overflow:ellipsis}
.folder-icon{flex:none;color:var(--copy)}
.tree-row.selected .folder-icon,.tree-name:hover .folder-icon{color:var(--brand)}
.tree-name:hover{color:var(--brand)}
.tree-count{color:var(--muted);font-size:11px;flex:none}
.tree-actions{display:none;gap:2px}
.tree-row:hover .tree-actions{display:inline-flex}
.tree-action{border:0;background:transparent;color:var(--muted);font-size:12px;cursor:pointer;padding:0 3px}
.tree-action:hover{color:var(--brand)}
.tree-action.danger:hover{color:var(--danger)}
.tree-children{margin:2px 0 0 14px;padding:0}
</style>
