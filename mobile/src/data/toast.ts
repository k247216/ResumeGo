import { ref } from 'vue'

export const toastMessage = ref('')
let timer: ReturnType<typeof setTimeout> | null = null

export function toast(msg: string) {
  toastMessage.value = msg
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { toastMessage.value = '' }, 2200)
}
