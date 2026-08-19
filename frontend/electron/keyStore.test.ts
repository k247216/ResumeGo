// @vitest-environment node

import { mkdtemp, writeFile } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { describe, expect, it } from 'vitest'
import { DesktopKeyStore } from './keyStore.js'

const codec = {
  isAvailable: () => true,
  encrypt: (value: string) => Buffer.from(`encrypted:${value}`),
  decrypt: (value: Buffer) => value.toString().replace('encrypted:', ''),
}

describe('DesktopKeyStore', () => {
  it('saves encrypted values and only exposes plaintext to the main process method', async () => {
    const directory = await mkdtemp(path.join(os.tmpdir(), 'resumego-keys-'))
    const store = new DesktopKeyStore(directory, codec)

    await store.save(12, 'sk-secret')

    await expect(store.has(12)).resolves.toBe(true)
    await expect(store.getForMain(12)).resolves.toBe('sk-secret')
  })

  it('deletes a key without affecting other profiles', async () => {
    const directory = await mkdtemp(path.join(os.tmpdir(), 'resumego-keys-'))
    const store = new DesktopKeyStore(directory, codec)
    await store.save(1, 'key-one')
    await store.save(2, 'key-two')

    await store.delete(1)

    await expect(store.has(1)).resolves.toBe(false)
    await expect(store.getForMain(2)).resolves.toBe('key-two')
  })

  it('keeps keys in memory for this session when safe storage is unavailable', async () => {
    const directory = await mkdtemp(path.join(os.tmpdir(), 'resumego-session-keys-'))
    const store = new DesktopKeyStore(directory, { ...codec, isAvailable: () => false })

    expect(store.mode()).toBe('session')
    await store.save(3, 'session-secret')

    await expect(store.has(3)).resolves.toBe(true)
    await expect(store.getForMain(3)).resolves.toBe('session-secret')
    await expect(new DesktopKeyStore(directory, codec).has(3)).resolves.toBe(false)
  })

  it('refuses to overwrite a corrupted secure key file', async () => {
    const directory = await mkdtemp(path.join(os.tmpdir(), 'resumego-corrupt-keys-'))
    await writeFile(path.join(directory, 'provider-keys.json'), '{broken')
    const store = new DesktopKeyStore(directory, codec)

    await expect(store.save(4, 'new-secret')).rejects.toThrow('密钥文件无法读取')
  })
})
