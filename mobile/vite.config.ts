import { readFileSync } from 'node:fs'
// 用 vitest/config 的 defineConfig，才能在同一份配置里声明 test 段；
// 它本身兼容 vite 的配置项，构建行为不变。
import { defineConfig } from 'vitest/config'
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
  test: {
    // 环境写在这里而不是只靠 package.json 的 --environment 参数：
    // theme/overlays 两个用例需要 document，任何绕过 npm 脚本的调用
    // （IDE 的 vitest 插件、单跑某个文件）都会因为缺这行而集体报 document is not defined。
    environment: 'happy-dom',
  },
})
