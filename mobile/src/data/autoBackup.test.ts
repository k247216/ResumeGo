import { describe, expect, it } from 'vitest'
import { autoBackupStatus, backupFileNameFor, staleBackupNames } from './autoBackup'

describe('自动备份文件名与轮转', () => {
  it('文件名即日期：补零、固定前缀', () => {
    expect(backupFileNameFor(new Date(2026, 8, 17))).toBe('zhida-auto-2026-09-17.json')
    expect(backupFileNameFor(new Date(2026, 0, 3))).toBe('zhida-auto-2026-01-03.json')
  })

  it('轮转：保留最近 7 份（含今天），超出部分按日期从旧到新删除', () => {
    const names = [
      'zhida-auto-2026-09-01.json',
      'zhida-auto-2026-09-02.json',
      'zhida-auto-2026-09-05.json',
      'zhida-auto-2026-09-09.json',
      'zhida-auto-2026-09-10.json',
      'zhida-auto-2026-09-11.json',
      'zhida-auto-2026-09-12.json',
      'zhida-auto-2026-09-16.json',
    ]
    const stale = staleBackupNames(names, 'zhida-auto-2026-09-17.json')
    // 共 9 份（含今天），留 7 → 删最旧的 2 份
    expect(stale).toEqual(['zhida-auto-2026-09-01.json', 'zhida-auto-2026-09-02.json'])
  })

  it('不足 7 份时什么都不删', () => {
    const names = ['zhida-auto-2026-09-15.json', 'zhida-auto-2026-09-16.json']
    expect(staleBackupNames(names, 'zhida-auto-2026-09-17.json')).toEqual([])
  })

  it('今天重写覆盖：同名文件不会被算成待删，也不会重复入队', () => {
    const names = ['zhida-auto-2026-09-17.json']
    expect(staleBackupNames(names, 'zhida-auto-2026-09-17.json')).toEqual([])
  })

  it('不碰非自动备份命名：用户手动放进目录的文件一律不动', () => {
    const names = ['zhida-backup-2026-01-01.json', 'notes.txt', 'zhida-auto-20260901.json']
    expect(staleBackupNames(names, 'zhida-auto-2026-09-17.json')).toEqual([])
  })
})

describe('autoBackupStatus', () => {
  it('Web 预览环境返回 null（UI 整行隐藏，不编造状态）', () => {
    // vitest 跑在 happy-dom，Capacitor.isNativePlatform() 恒为 false
    expect(autoBackupStatus()).toBeNull()
  })
})
