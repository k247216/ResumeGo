import { mkdir, readFile, rename, writeFile } from 'node:fs/promises'
import path from 'node:path'

export interface SafeStorageCodec {
  isAvailable: () => boolean
  encrypt: (value: string) => Buffer
  decrypt: (value: Buffer) => string
}

export class DesktopKeyStore {
  private readonly filePath: string
  private readonly sessionValues: Record<string, string> = {}

  constructor(private readonly directory: string, private readonly codec: SafeStorageCodec) {
    this.filePath = path.join(directory, 'provider-keys.json')
  }

  async save(profileId: number, apiKey: string): Promise<void> {
    if (!apiKey.trim()) throw new Error('API Key 不能为空')
    if (!this.codec.isAvailable()) {
      this.sessionValues[String(profileId)] = apiKey.trim()
      return
    }
    const values = await this.readValues()
    values[String(profileId)] = this.codec.encrypt(apiKey.trim()).toString('base64')
    await this.writeValues(values)
  }

  async delete(profileId: number): Promise<void> {
    delete this.sessionValues[String(profileId)]
    if (!this.codec.isAvailable()) return
    const values = await this.readValues()
    delete values[String(profileId)]
    await this.writeValues(values)
  }

  async has(profileId: number): Promise<boolean> {
    if (!this.codec.isAvailable()) return Boolean(this.sessionValues[String(profileId)])
    const values = await this.readValues()
    return Boolean(values[String(profileId)])
  }

  async getForMain(profileId: number): Promise<string | null> {
    if (!this.codec.isAvailable()) return this.sessionValues[String(profileId)] ?? null
    const encoded = (await this.readValues())[String(profileId)]
    return encoded ? this.codec.decrypt(Buffer.from(encoded, 'base64')) : null
  }

  mode(): 'secure' | 'session' {
    return this.codec.isAvailable() ? 'secure' : 'session'
  }

  private async readValues(): Promise<Record<string, string>> {
    try {
      const parsed = JSON.parse(await readFile(this.filePath, 'utf8')) as unknown
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {}
      return Object.fromEntries(Object.entries(parsed).filter((entry): entry is [string, string] =>
        typeof entry[1] === 'string'))
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') return {}
      throw new Error('密钥文件无法读取；为避免覆盖已有密钥，未执行写入', { cause: error })
    }
  }

  private async writeValues(values: Record<string, string>): Promise<void> {
    await mkdir(this.directory, { recursive: true })
    const temporary = `${this.filePath}.tmp`
    await writeFile(temporary, JSON.stringify(values), { encoding: 'utf8', mode: 0o600 })
    await rename(temporary, this.filePath)
  }
}
