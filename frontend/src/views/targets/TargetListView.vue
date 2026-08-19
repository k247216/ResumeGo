<template>
  <section class="target-list">
    <header>
      <div><p>求职目标</p><h1>管理正在进行的求职准备</h1></div>
      <router-link :to="{ name: 'workbench' }">返回工作台</router-link>
    </header>
    <p v-if="store.loading">正在读取本地目标…</p>
    <p v-else-if="store.errorMessage" class="error">{{ store.errorMessage }} <button type="button" @click="store.retry">重试</button></p>
    <div v-else-if="store.targets.length" class="targets">
      <article v-for="target in store.targets" :key="target.id" :class="{ active: target.id === store.activeTargetId }">
        <div><strong>{{ target.name }}</strong><small>{{ target.status === 'active' ? '进行中' : '已归档' }}</small></div>
        <button type="button" @click="selectTarget(target.id)">{{ target.id === store.activeTargetId ? '当前目标' : '设为当前目标' }}</button>
      </article>
    </div>
    <div v-else class="empty">还没有求职目标。可从工作台创建第一个目标。</div>
  </section>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTargetsStore } from '../../stores/targets'

const store = useTargetsStore()
const router = useRouter()
onMounted(() => { if (!store.targets.length) void store.load() })
function selectTarget(id: number) { store.select(id); void router.push({ name: 'workbench' }) }
</script>

<style scoped>
.target-list{padding:42px}.target-list header{display:flex;align-items:center;justify-content:space-between}.target-list p{color:#168866;font-weight:700}.target-list h1{margin:6px 0 24px}.target-list a{color:#168866}.targets{display:grid;gap:10px}.targets article{display:flex;justify-content:space-between;align-items:center;border:1px solid #dfe5e9;border-radius:12px;background:#fff;padding:17px}.targets article.active{border-color:#5bb697}.targets article div{display:grid;gap:5px}.targets small{color:#78858f}.targets button,.error button{border:1px solid #d6dfe4;border-radius:8px;background:#fff;padding:8px 11px}.empty{border:1px dashed #ccd7dd;border-radius:12px;padding:28px;color:#74828c}.error{color:#b53c32}
</style>
