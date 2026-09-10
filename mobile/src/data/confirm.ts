import { ref } from 'vue'

export interface ConfirmState {
  open: boolean
  title: string
  message: string
  confirmLabel: string
  danger: boolean
  resolve: ((v: boolean) => void) | null
}

export const confirmState = ref<ConfirmState>({
  open: false,
  title: '',
  message: '',
  confirmLabel: '确定',
  danger: false,
  resolve: null,
})

function settle(value: boolean) {
  const r = confirmState.value.resolve
  confirmState.value.open = false
  confirmState.value.resolve = null
  r?.(value)
}

/** 原生底部确认卡替代 window.confirm；返回 Promise，确认 true / 取消 false。 */
export function confirmAction(opts: {
  title: string
  message?: string
  confirmLabel?: string
  danger?: boolean
}): Promise<boolean> {
  if (confirmState.value.open) settle(false)
  confirmState.value = {
    open: true,
    title: opts.title,
    message: opts.message ?? '',
    confirmLabel: opts.confirmLabel ?? '确定',
    danger: opts.danger ?? false,
    resolve: null,
  }
  return new Promise((resolve) => { confirmState.value.resolve = resolve })
}

export function confirmResult(value: boolean) { settle(value) }
