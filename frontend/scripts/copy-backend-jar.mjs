import { copyFileSync, readdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const backendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..', 'backend')
const targetDir = path.join(backendDir, 'target')
const jar = readdirSync(targetDir).find(
  (name) => /^resume-go-.*\.jar$/.test(name) && !name.endsWith('.original'),
)
if (!jar) {
  console.error('backend jar not found in', targetDir)
  process.exit(1)
}
copyFileSync(path.join(targetDir, jar), path.join(targetDir, 'resume-go.jar'))
console.log('copied', jar, '-> resume-go.jar')
