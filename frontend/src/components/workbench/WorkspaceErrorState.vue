<template><section data-test="workspace-error" class="state-card"><strong>本地工作区暂时无法读取</strong><p>{{ message }}</p><button type="button" @click="$emit('retry')">重新加载</button></section></template>
<script setup lang="ts">defineProps<{ message: string }>(); defineEmits<{ (event: 'retry'): void }>()</script>
<style scoped>.state-card{margin:64px auto;max-width:520px;padding:32px;border:1px solid #f0caca;border-radius:16px;background:white;text-align:center}.state-card p{color:#8a5a5a}.state-card button{border:0;border-radius:8px;background:#172238;color:white;padding:9px 14px}</style>
