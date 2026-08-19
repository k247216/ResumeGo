<template>
  <div v-if="open && target" class="dialog-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="delete-target-title">
      <header><div><small>删除求职目标</small><h2 id="delete-target-title">确认删除“{{ target.name }}”</h2></div><button type="button" aria-label="关闭" @click="$emit('close')">×</button></header>
      <form @submit.prevent="submit">
        <p>删除后，该目标不会再出现在工作台中，但不会删除关联的简历版本和岗位材料。</p>
        <label>输入目标名称以确认<input v-model="confirmation" data-test="delete-confirm-name" autocomplete="off"></label>
        <p v-if="validationMessage || errorMessage" class="error" role="alert">{{ validationMessage || errorMessage }}</p>
        <footer><button type="button" @click="$emit('close')">取消</button><button class="danger" type="submit" :disabled="submitting">{{ submitting ? '删除中…' : '删除目标' }}</button></footer>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { JobProject } from '../../types/project'
const props = withDefaults(defineProps<{ open: boolean; target: JobProject | null; submitting?: boolean; errorMessage?: string }>(), { submitting: false, errorMessage: '' })
const emit = defineEmits<{ (event: 'close'): void; (event: 'confirm'): void }>()
const confirmation = ref('')
const validationMessage = ref('')
watch(() => props.open, (open) => { if (!open) { confirmation.value = ''; validationMessage.value = '' } })
function submit() { if (confirmation.value.trim() !== props.target?.name) { validationMessage.value = '请输入完整的求职目标名称'; return }; validationMessage.value = ''; emit('confirm') }
</script>

<style scoped>
.dialog-backdrop{position:fixed;inset:0;z-index:50;display:grid;place-items:center;background:rgba(19,31,43,.42);padding:24px}.dialog{width:min(520px,100%);border-radius:16px;background:#fff;box-shadow:0 22px 60px rgba(15,35,47,.24);padding:22px}.dialog header{display:flex;justify-content:space-between;gap:16px}.dialog h2{margin:5px 0 18px;font-size:21px}.dialog small{color:#b43e35;font-weight:700}.dialog header button{align-self:start;border:0;background:none;font-size:24px}.dialog form,.dialog label{display:grid;gap:9px}.dialog form{gap:16px}.dialog p{margin:0;color:#64737c;line-height:1.6}.dialog label{font-weight:700;color:#344552}.dialog input{border:1px solid #d6dfe4;border-radius:9px;padding:11px 12px}.dialog .error{color:#b53c32}.dialog footer{display:flex;justify-content:flex-end;gap:9px}.dialog footer button{border:1px solid #d6dfe4;border-radius:9px;background:#fff;padding:9px 13px}.dialog footer .danger{border-color:#b43e35;background:#b43e35;color:#fff}
</style>
