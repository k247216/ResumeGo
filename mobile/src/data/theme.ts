const KEY = 'zhida-mobile-theme'

export type Theme = 'light' | 'mint' | 'dark'

export const THEME_OPTIONS: Array<{ value: Theme; label: string; description: string }> = [
  { value: 'light', label: '纸张浅色', description: '清爽的暖白工作区' },
  { value: 'mint', label: '薄荷绿', description: '柔和、低对比的专注色' },
  { value: 'dark', label: '深色', description: '夜间低亮度工作区' },
]

export function getTheme(): Theme {
  const saved = localStorage.getItem(KEY)
  if (saved === 'light' || saved === 'mint' || saved === 'dark') return saved
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function applyTheme(theme: Theme) {
  document.documentElement.dataset.theme = theme
  const meta = document.querySelector('meta[name="theme-color"]')
  if (meta) meta.setAttribute('content', theme === 'dark' ? '#111212' : theme === 'mint' ? '#eef8f2' : '#ffffff')
}

export function setTheme(theme: Theme) {
  localStorage.setItem(KEY, theme)
  applyTheme(theme)
}

export function toggleTheme(): Theme {
  const next = getTheme() === 'dark' ? 'light' : 'dark'
  setTheme(next)
  return next
}
