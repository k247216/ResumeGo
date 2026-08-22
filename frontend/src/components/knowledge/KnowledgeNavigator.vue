<template>
  <aside class="navigator" data-test="knowledge-navigator">
      <div class="nav-section">
        <div class="nav-head">
          <strong>资料库</strong>
          <span class="nav-head-actions">
            <button type="button" class="nav-action" data-test="navigator-new-folder" title="新建文件夹" aria-label="新建文件夹" @click="$emit('new-folder')"><el-icon :size="14"><Plus /></el-icon></button>
            <button type="button" class="nav-action" data-test="navigator-close" title="收起资料库" aria-label="收起资料库" @click="$emit('close')"><el-icon :size="14"><ArrowLeft /></el-icon></button>
          </span>
        </div>
        <p v-if="treeError" class="nav-error" data-test="navigator-tree-error">
          {{ treeError }}
          <button type="button" class="text-btn" data-test="navigator-tree-retry" @click="$emit('retry-tree')">重试</button>
        </p>
        <KnowledgeFolderTree
          :nodes="nodes"
          :expanded-ids="expandedIds"
          :selected-id="selectedId"
          @toggle="$emit('toggle-folder', $event)"
          @select="$emit('select-folder', $event)"
          @new-child="$emit('new-child', $event)"
          @rename="$emit('rename-folder', $event)"
          @move="$emit('move-folder', $event)"
          @delete="$emit('delete-folder', $event)"
        />
      </div>
      <div class="nav-section">
        <div class="nav-head">
          <strong>标签</strong>
          <button type="button" class="nav-action" data-test="navigator-new-tag" title="新建标签" aria-label="新建标签" @click="$emit('new-tag')"><el-icon :size="14"><Plus /></el-icon></button>
        </div>
        <ul class="tag-list">
          <li v-for="tag in tags" :key="tag.id">
            <button
              type="button"
              class="tag-item"
              :class="{ active: tag.id === activeTagId }"
              :data-test="'navigator-tag-' + tag.id"
              @click="$emit('select-tag', tag.id === activeTagId ? null : tag.id)"
            ><el-icon class="tag-icon" :data-test="'navigator-tag-icon-' + tag.id" :size="14"><PriceTag /></el-icon><span>{{ tag.name }}</span></button>
          </li>
          <li v-if="!tags.length" class="tag-empty">暂无标签</li>
        </ul>
      </div>
  </aside>
</template>

<script setup lang="ts">
import { ArrowLeft, Plus, PriceTag } from '@element-plus/icons-vue'
import KnowledgeFolderTree from './KnowledgeFolderTree.vue'
import type { KnowledgeCategoryNode, KnowledgeTag } from '../../types/knowledge'

defineProps<{
  nodes: KnowledgeCategoryNode[]
  tags: KnowledgeTag[]
  expandedIds: Set<number>
  selectedId: number | null
  activeTagId: number | null
  treeError: string
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'new-folder'): void
  (e: 'toggle-folder', id: number): void
  (e: 'select-folder', id: number): void
  (e: 'new-child', parentId: number | null): void
  (e: 'rename-folder', id: number): void
  (e: 'move-folder', id: number): void
  (e: 'delete-folder', id: number): void
  (e: 'select-tag', id: number | null): void
  (e: 'new-tag'): void
  (e: 'retry-tree'): void
}>()
</script>

<style scoped>
.navigator{box-sizing:border-box;width:184px;min-width:0;border-right:1px solid var(--border-subtle);overflow-y:auto;padding:10px 8px 18px;background:var(--bg-subtle)}
.nav-section{display:flex;flex-direction:column;gap:4px;margin-bottom:16px}
.nav-head{display:flex;align-items:center;justify-content:space-between;height:32px;flex:none;margin-bottom:5px;padding:0 4px;border-bottom:1px solid var(--border-subtle)}
.nav-head-actions{display:inline-flex;align-items:center;gap:2px}
.nav-head strong{font-size:13px;font-weight:700;color:var(--ink);letter-spacing:.05em}
.nav-action{display:grid;place-items:center;width:24px;height:24px;border:0;border-radius:7px;background:transparent;color:var(--copy);cursor:pointer;transition:background .15s ease,color .15s ease}
.nav-action:hover{background:var(--bg-hover);color:var(--brand)}
.tag-list{list-style:none;margin:0;padding:0 2px;display:flex;flex-direction:column;gap:2px}
.tag-item{display:flex;align-items:center;gap:7px;border:0;background:transparent;color:var(--ink);font-size:13.5px;text-align:left;padding:6px 9px;border-radius:7px;cursor:pointer;width:100%}
.tag-icon{flex:none;color:var(--copy)}
.tag-item:hover{background:var(--bg-hover)}
.tag-item.active{background:var(--bg-selected);color:var(--brand)}
.tag-item.active .tag-icon{color:var(--brand)}
.tag-empty{color:var(--muted);font-size:13px;padding:6px 10px}
.nav-error{display:grid;gap:6px;padding:6px 8px;color:var(--danger);font-size:12px}
.text-btn{border:0;background:transparent;color:var(--brand);font-size:12px;cursor:pointer;padding:0;justify-self:start}
</style>
