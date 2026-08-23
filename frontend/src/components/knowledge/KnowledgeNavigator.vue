<template>
  <aside class="navigator" data-test="knowledge-navigator">
      <div class="nav-section">
        <div class="nav-head">
          <strong>资料库</strong>
          <span class="nav-head-actions">
            <button type="button" class="nav-action" data-test="navigator-new-folder" title="新建文件夹" aria-label="新建文件夹" @click="$emit('new-folder')"><el-icon :size="14"><FolderAdd /></el-icon></button>
            <button type="button" class="nav-action" data-test="navigator-new-child" title="在选中文件夹下新建子文件夹" aria-label="新建子文件夹" :disabled="selectedId == null" @click="selectedId != null && $emit('new-child', selectedId)"><el-icon :size="14"><FolderOpened /></el-icon></button>
            <button type="button" class="nav-action" data-test="navigator-rename-folder" title="重命名/移动选中文件夹" aria-label="重命名文件夹" :disabled="selectedId == null" @click="selectedId != null && $emit('rename-folder', selectedId)"><el-icon :size="14"><EditPen /></el-icon></button>
            <button type="button" class="nav-action danger" data-test="navigator-delete-folder" title="删除选中文件夹" aria-label="删除文件夹" :disabled="selectedId == null" @click="selectedId != null && $emit('delete-folder', selectedId)"><el-icon :size="14"><Delete /></el-icon></button>
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
          :renaming-id="renamingId"
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
import { ArrowLeft, Delete, EditPen, FolderAdd, FolderOpened, PriceTag } from '@element-plus/icons-vue'
import KnowledgeFolderTree from './KnowledgeFolderTree.vue'
import type { KnowledgeCategoryNode, KnowledgeTag } from '../../types/knowledge'

defineProps<{
  nodes: KnowledgeCategoryNode[]
  tags: KnowledgeTag[]
  expandedIds: Set<number>
  selectedId: number | null
  activeTagId: number | null
  renamingId?: number | null
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
  (e: 'rename-submit', id: number, name: string): void
  (e: 'rename-cancel'): void
  (e: 'select-tag', id: number | null): void
  (e: 'new-tag'): void
  (e: 'retry-tree'): void
}>()
</script>

<style scoped>
.navigator{box-sizing:border-box;width:184px;min-width:0;border-right:1px solid var(--border-subtle);overflow-y:auto;padding:12px 8px 18px;background:var(--bg-subtle)}
.nav-section{display:flex;flex-direction:column;gap:2px;margin-bottom:22px}
.nav-section:last-child{margin-bottom:0}
.nav-head{display:flex;align-items:center;justify-content:space-between;height:26px;flex:none;margin-bottom:6px;padding:0 6px}
.nav-head-actions{display:inline-flex;align-items:center;gap:1px;opacity:.55;transition:opacity .15s ease}
.nav-head:hover .nav-head-actions{opacity:1}
.nav-head strong{font-size:12px;font-weight:650;color:var(--muted);letter-spacing:.04em;user-select:none}
.nav-action{display:grid;place-items:center;width:22px;height:22px;border:0;border-radius:6px;background:transparent;color:var(--muted);cursor:pointer;transition:background .15s ease,color .15s ease}
.nav-action:hover{background:var(--bg-hover);color:var(--ink)}
.nav-action:disabled{opacity:.35;cursor:not-allowed}
.nav-action:disabled:hover{background:transparent;color:var(--muted)}
.nav-action.danger:hover{color:var(--danger);background:var(--danger-soft)}
.tag-list{list-style:none;margin:0;padding:0 4px;display:flex;flex-direction:column;gap:1px}
.tag-item{display:flex;align-items:center;gap:8px;height:30px;border:0;background:transparent;color:var(--ink);font-size:14px;text-align:left;padding:0 10px;border-radius:7px;cursor:pointer;width:100%;transition:background .12s ease,color .12s ease}
.tag-icon{flex:none;color:var(--muted)}
.tag-item:hover{background:var(--bg-hover)}
.tag-item.active{background:var(--brand-soft);color:var(--brand);font-weight:600}
.tag-item.active .tag-icon{color:var(--brand)}
.tag-empty{color:var(--muted);font-size:13px;padding:8px 10px}
.nav-error{display:grid;gap:6px;padding:6px 8px;color:var(--danger);font-size:12px}
.text-btn{border:0;background:transparent;color:var(--brand);font-size:12px;cursor:pointer;padding:0;justify-self:start}
</style>
