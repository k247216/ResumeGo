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
      <button v-if="!renaming" type="button" class="tree-name" :data-test="'folder-select-' + node.id" @click="$emit('select', node.id)">
        <el-icon
          class="folder-icon"
          :data-test="'folder-icon-' + node.id"
          :data-state="expanded ? 'open' : 'closed'"
          :size="16"
        ><component :is="expanded ? FolderOpened : Folder" /></el-icon>
        <span>{{ node.name }}</span>
      </button>
      <input
        v-else
        ref="nameInput"
        v-model="nameDraft"
        class="tree-name-input"
        :data-test="'folder-rename-input-' + node.id"
        maxlength="60"
        aria-label="文件夹名称"
        @keydown.enter.prevent="commitRename"
        @keydown.esc.prevent="cancelRename"
        @blur="commitRename"
      />
      <span class="tree-count">{{ node.descendantDocumentCount }}</span>
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
import { computed, nextTick, ref, watch } from 'vue'
import { CaretBottom, CaretRight, Folder, FolderOpened } from '@element-plus/icons-vue'
import KnowledgeFolderNode from './KnowledgeFolderNode.vue'
import type { KnowledgeCategoryNode } from '../../types/knowledge'

const props = defineProps<{
  node: KnowledgeCategoryNode
  allNodes: KnowledgeCategoryNode[]
  expandedIds: Set<number>
  selectedId: number | null
  renaming?: boolean
}>()

const emit = defineEmits<{
  (e: 'toggle', id: number): void
  (e: 'select', id: number): void
  (e: 'new-child', parentId: number | null): void
  (e: 'rename', id: number): void
  (e: 'move', id: number): void
  (e: 'delete', id: number): void
  (e: 'rename-submit', id: number, name: string): void
  (e: 'rename-cancel'): void
}>()

const children = computed(() => props.allNodes.filter((n) => n.parentId === props.node.id))
const expanded = computed(() => props.expandedIds.has(props.node.id))

function toggle() {
  if (children.value.length) {
    emit('toggle', props.node.id)
  }
}

const nameDraft = ref('')
const nameInput = ref<HTMLInputElement | null>(null)

watch(() => props.renaming, (renaming) => {
  if (renaming) {
    nameDraft.value = props.node.name
    void nextTick(() => {
      nameInput.value?.focus()
      nameInput.value?.select()
    })
  }
}, { immediate: true })

function commitRename() {
  if (!props.renaming) return
  const name = nameDraft.value.trim()
  if (name && name !== props.node.name) {
    emit('rename-submit', props.node.id, name)
  } else {
    emit('rename-cancel')
  }
}

function cancelRename() {
  emit('rename-cancel')
}
</script>

<style scoped>
.tree-node{list-style:none}
.tree-row{display:flex;align-items:center;gap:3px;height:30px;padding:0 6px;border-radius:7px;font-size:14px}
.tree-row:hover{background:var(--bg-hover)}
.tree-row.selected{background:var(--brand-soft)}
.tree-toggle{display:grid;place-items:center;border:0;background:transparent;color:var(--muted);width:16px;height:18px;padding:0;cursor:pointer;flex:none;border-radius:5px}
.tree-toggle:hover{background:var(--bg-hover);color:var(--ink)}
.tree-toggle-placeholder{cursor:default}
.tree-name{display:flex;align-items:center;gap:7px;border:0;background:transparent;color:var(--ink);flex:1;min-width:0;text-align:left;cursor:pointer;overflow:hidden;white-space:nowrap}
.tree-name span{overflow:hidden;text-overflow:ellipsis}
.tree-name-input{flex:1;min-width:0;height:22px;padding:0 6px;border:1px solid var(--brand);border-radius:6px;background:var(--surface-solid);color:var(--ink);font:inherit;font-size:13.5px;outline:0;box-shadow:0 0 0 2px var(--brand-soft)}
.folder-icon{flex:none;color:var(--muted)}
.tree-row.selected .folder-icon,.tree-name:hover .folder-icon{color:var(--brand)}
.tree-name:hover{color:var(--brand)}
.tree-count{color:var(--muted);font-size:10.5px;flex:none;min-width:18px;text-align:right}
.tree-children{margin:1px 0 0 11px;padding:1px 0 1px 8px;border-left:1px solid var(--border-subtle)}
</style>
