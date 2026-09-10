const KEY = 'zhida-mobile-theme'

export type Theme = 'light' | 'mint' | 'dark'

export const THEME_OPTIONS: Array<{ value: Theme; label: string; description: string }> = [
  { value: 'light', label: '纸张浅色', description: '清爽的暖白工作区' },
  { value: 'mint', label: '薄荷绿', description: '柔和、低对比的专注色' },
  { value: 'dark', label: '深色', description: '夜间低亮度工作区' },
]

// 无 localStorage 的环境（SSR / 部分测试环境）降级为内存存储，保证模块可加载、读写一致。
const memoryStore = new Map<string, string>()
const storage = {
  get(key: string): string | null {
    try { return localStorage.getItem(key) } catch { return memoryStore.get(key) ?? null }
  },
  set(key: string, value: string) {
    try { localStorage.setItem(key, value) } catch { memoryStore.set(key, value) }
  },
}

export function getTheme(): Theme {
  const saved = storage.get(KEY)
  if (saved === 'light' || saved === 'mint' || saved === 'dark') return saved
  return 'light' // 未选择时固定默认「纸张浅色」，不跟随系统深色
}

export function applyTheme(theme: Theme) {
  document.documentElement.dataset.theme = theme
  const meta = document.querySelector('meta[name="theme-color"]')
  if (meta) meta.setAttribute('content', theme === 'dark' ? '#111212' : theme === 'mint' ? '#eef8f2' : '#ffffff')
}

export function setTheme(theme: Theme) {
  storage.set(KEY, theme)
  applyTheme(theme)
}

export function toggleTheme(): Theme {
  const next = getTheme() === 'dark' ? 'light' : 'dark'
  setTheme(next)
  return next
}
