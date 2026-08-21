<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const visible = ref(false)
const show = () => { visible.value = true }

onMounted(() => window.addEventListener('resumego:ai-not-configured', show))
onBeforeUnmount(() => window.removeEventListener('resumego:ai-not-configured', show))
</script>

<template>
  <div v-if="visible" class="guide-backdrop" role="dialog" aria-modal="true" aria-label="配置 AI 模型">
    <section class="guide-card">
      <span>可选能力</span>
      <h2>使用 AI 前需要配置模型服务</h2>
      <p>ResumeGo 不提供公共密钥。你可以使用自己的 OpenAI、Anthropic、Gemini、DeepSeek、GLM 等账号；不配置也不会影响本地简历编辑。</p>
      <div>
        <button type="button" @click="visible = false">暂不使用</button>
        <router-link to="/settings" @click="visible = false">去配置</router-link>
      </div>
    </section>
  </div>
</template>

<style scoped>
.guide-backdrop{position:fixed;z-index:3000;inset:0;display:grid;place-items:center;padding:24px;background:rgba(7,8,8,.5);backdrop-filter:blur(4px)}.guide-card{width:min(460px,100%);padding:30px;border:1px solid var(--line,#dce4e2);border-radius:20px;background:var(--surface-solid,#fff);box-shadow:0 24px 70px rgba(0,0,0,.35);color:var(--ink,#26343d)}.guide-card>span{color:var(--brand,#168866);font-size:12px;font-weight:800;letter-spacing:.12em}.guide-card h2{margin:10px 0 12px;font-size:24px}.guide-card p{margin:0;color:var(--muted,#687586);line-height:1.7}.guide-card div{display:flex;justify-content:flex-end;gap:10px;margin-top:24px}.guide-card button,.guide-card a{padding:10px 16px;border:1px solid var(--line,#d8e0e1);border-radius:10px;background:var(--surface-solid,#fff);color:var(--copy,#42515c);font-weight:750;text-decoration:none}.guide-card a{border-color:var(--brand,#168866);background:var(--brand,#168866);color:#fff}
</style>
