import { readFileSync } from 'node:fs'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// tsconfig 未开 resolveJsonModule，这里直接读文件取版本号，避免「我的」页硬编码版本漂移。
const appVersion = (JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf-8')) as { version: string }).version

export default defineConfig({
  plugins: [vue()],
  define: {
    __APP_VERSION__: JSON.stringify(appVersion),
  },
  server: {
    port: 5174,
    host: true,
  },
  build: {
    outDir: 'dist',
  },
})
