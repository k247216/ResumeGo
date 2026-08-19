<template>
  <section class="target-list">
    <header><div><p>求职目标</p><h1>管理正在进行的求职准备</h1></div><router-link :to="{ name: 'workbench' }">返回工作台</router-link></header>
    <p v-if="store.loading">正在读取本地目标…</p>
    <p v-else-if="store.errorMessage && !store.targets.length" class="error">{{ store.errorMessage }} <button type="button" @click="store.retry">重试</button></p>
    <p v-if="operationError" class="error">{{ operationError }}</p>
    <div v-if="store.targets.length" class="targets">
      <article v-for="target in store.targets" :key="target.id" :class="{ active: target.id === store.activeTargetId }">
        <div class="target-copy">
          <template v-if="editingId === target.id">
            <input v-model="editingName" :data-test="`target-name-${target.id}`" aria-label="求职目标名称" @keyup.enter="saveName(target.id)">
            <span class="inline-actions"><button :data-test="`save-target-name-${target.id}`" type="button" @click="saveName(target.id)">保存</button><button type="button" @click="cancelRename">取消</button></span>
          </template>
          <template v-else><strong>{{ target.name }}</strong><small>{{ target.status === 'active' ? '进行中' : '已归档' }}</small></template>
        </div>
        <div class="target-actions">
          <button v-if="target.status === 'active'" type="button" @click="selectTarget(target.id)">{{ target.id === store.activeTargetId ? '打开当前目标' : '设为当前目标' }}</button>
          <button :data-test="`rename-target-${target.id}`" type="button" @click="beginRename(target.id, target.name)">重命名</button>
          <button v-if="target.status === 'active'" :data-test="`archive-target-${target.id}`" type="button" :disabled="busyId === target.id" @click="archiveTarget(target.id)">归档</button>
          <button v-else :data-test="`restore-target-${target.id}`" type="button" :disabled="busyId === target.id" @click="restoreTarget(target.id)">恢复</button>
        </div>
      </article>
    </div>
    <div v-else-if="!store.loading" class="empty">还没有求职目标。可从工作台创建第一个目标。</div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTargetsStore } from '../../stores/targets'

const store = useTargetsStore()
const router = useRouter()
const editingId = ref<number | null>(null)
const editingName = ref('')
const busyId = ref<number | null>(null)
const operationError = ref('')
onMounted(() => { if (!store.targets.length) void store.load() })

function selectTarget(id: number) { store.select(id); void router.push({ name: 'workbench' }) }
function beginRename(id: number, name: string) { editingId.value = id; editingName.value = name; operationError.value = '' }
function cancelRename() { editingId.value = null; editingName.value = '' }
async function saveName(id: number) {
  const name = editingName.value.trim()
  if (!name) { operationError.value = '求职目标名称不能为空'; return }
  busyId.value = id; operationError.value = ''
  try { await store.rename(id, name); cancelRename() }
  catch (error) { operationError.value = error instanceof Error ? error.message : '重命名失败' }
  finally { busyId.value = null }
}
async function archiveTarget(id: number) { await runTargetAction(id, () => store.archive(id), '归档失败') }
async function restoreTarget(id: number) { await runTargetAction(id, () => store.restore(id), '恢复失败') }
async function runTargetAction(id: number, action: () => Promise<unknown>, fallback: string) {
  busyId.value = id; operationError.value = ''
  try { await action() } catch (error) { operationError.value = error instanceof Error ? error.message : fallback }
  finally { busyId.value = null }
}
</script>

<style scoped>
.target-list{padding:42px}.target-list header{display:flex;align-items:center;justify-content:space-between}.target-list p{color:#168866;font-weight:700}.target-list h1{margin:6px 0 24px}.target-list a{color:#168866}.targets{display:grid;gap:10px}.targets article{display:flex;justify-content:space-between;align-items:center;gap:18px;border:1px solid #dfe5e9;border-radius:12px;background:#fff;padding:17px}.targets article.active{border-color:#5bb697}.target-copy{display:grid;gap:6px;min-width:220px}.target-copy input{border:1px solid #b9c9ce;border-radius:8px;padding:8px 10px}.targets small{color:#78858f}.target-actions,.inline-actions{display:flex;flex-wrap:wrap;gap:7px}.targets button,.error button{border:1px solid #d6dfe4;border-radius:8px;background:#fff;padding:8px 11px}.targets button:disabled{opacity:.55}.empty{border:1px dashed #ccd7dd;border-radius:12px;padding:28px;color:#74828c}.error{border:1px solid #efc7c2;border-radius:9px;background:#fff2f0!important;color:#b53c32!important;padding:9px 12px}
</style>
