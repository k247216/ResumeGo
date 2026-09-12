import { ref } from 'vue'

export interface ToastAction { label: string; run: () => void }

export const toastMessage = ref('')
export const toastAction = ref<ToastAction | null>(null)
let timer: ReturnType<typeof setTimeout> | null = null

const PLAIN_MS = 2200
const ACTION_MS = 5200

export function toast(msg: string, action?: ToastAction) {
  toastMessage.value = msg
  toastAction.value = action ?? null
  if (timer) clearTimeout(timer)
  timer = setTimeout(dismissToast, action ? ACTION_MS : PLAIN_MS)
}

export function dismissToast() {
  if (timer) { clearTimeout(timer); timer = null }
  toastMessage.value = ''
  toastAction.value = null
}

/** 先收起再执行：撤销按钮点下去之后不该还挂在那儿等人二次点击。 */
export function runToastAction() {
  const action = toastAction.value
  dismissToast()
  action?.run()
}
