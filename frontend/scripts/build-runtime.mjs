import { rm } from 'node:fs/promises'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const frontendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const outputDir = path.join(frontendDir, 'runtime')
const javaProbe = spawnSync('java', ['-XshowSettings:properties', '-version'], { encoding: 'utf8' })
if (javaProbe.status !== 0) {
  throw new Error('未找到可用于构建桌面运行时的 JDK 21')
}
const settings = `${javaProbe.stdout}\n${javaProbe.stderr}`
const javaHome = settings.match(/^\s*java\.home\s*=\s*(.+)$/m)?.[1]?.trim()
if (!javaHome) {
  throw new Error('无法识别当前 JDK 目录')
}

await rm(outputDir, { recursive: true, force: true })
const jlink = path.join(javaHome, 'bin', process.platform === 'win32' ? 'jlink.exe' : 'jlink')
const modules = [
  'java.base',
  'java.desktop',
  'java.instrument',
  'java.logging',
  'java.management',
  'java.naming',
  'java.net.http',
  'java.prefs',
  'java.security.jgss',
  'java.sql',
  'java.transaction.xa',
  'java.xml',
  'jdk.crypto.ec',
  'jdk.unsupported',
].join(',')
const result = spawnSync(jlink, [
  '--add-modules', modules,
  '--strip-debug',
  '--no-header-files',
  '--no-man-pages',
  '--compress=zip-6',
  '--output', outputDir,
], { stdio: 'inherit' })
if (result.status !== 0) {
  throw new Error(`jlink 构建失败，退出码 ${result.status ?? 'unknown'}`)
}
