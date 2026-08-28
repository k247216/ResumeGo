import { beforeEach, describe, expect, it } from 'vitest'
import {
  defaultResumeTemplateKey,
  cloneResumeTemplate,
  getResumeDisplayTitle,
  isValidResumeTemplateKey,
  readResumeTemplate,
  resumeTemplateOptions,
  writeResumeTemplate,
} from './resumeTemplate'

describe('resumeTemplate', () => {
  beforeEach(() => {
    const store = new Map<string, string>()
    Object.defineProperty(globalThis, 'localStorage', {
      configurable: true,
      value: {
        clear: () => store.clear(),
        getItem: (key: string) => store.get(key) ?? null,
        setItem: (key: string, value: string) => store.set(key, value),
      },
    })
    globalThis.localStorage.clear()
  })

  it('keeps template preference scoped to one resume asset', () => {
    writeResumeTemplate(12, 'minimal')
    writeResumeTemplate(13, 'graphite')

    expect(readResumeTemplate(12)).toBe('minimal')
    expect(readResumeTemplate(13)).toBe('graphite')
  })

  it('copies a source template once, then keeps the copy independent', () => {
    writeResumeTemplate(12, 'timeline')
    cloneResumeTemplate(12, 13)
    writeResumeTemplate(12, 'mono')

    expect(readResumeTemplate(12)).toBe('mono')
    expect(readResumeTemplate(13)).toBe('timeline')
  })

  it('uses the legacy preference only as a fallback for an asset without a preference', () => {
    globalThis.localStorage.setItem('resumego:selectedResumeTemplate', 'emerald')

    expect(readResumeTemplate(12)).toBe('emerald')
    expect(readResumeTemplate(13)).toBe('emerald')
  })

  it('falls back to the product default for an invalid or missing template', () => {
    writeResumeTemplate(12, 'not-a-template')

    expect(readResumeTemplate(12)).toBe(defaultResumeTemplateKey)
    expect(isValidResumeTemplateKey('terminal')).toBe(true)
    expect(isValidResumeTemplateKey('nord')).toBe(true)
    expect(isValidResumeTemplateKey('editorial')).toBe(true)
  })

  it('keeps the full template catalog available to the editor', () => {
    expect(resumeTemplateOptions).toHaveLength(25)
    expect(new Set(resumeTemplateOptions.map((template) => template.key)).size).toBe(25)
  })

  it('uses the target role as the visible asset title without repeating the user name', () => {
    expect(getResumeDisplayTitle({
      title: '张三 · Java 后端实习简历',
      currentVersion: { content: { basicInfo: { name: '张三', targetRole: 'Java 后端实习' } } },
    })).toBe('Java 后端实习简历')
  })

  it('removes a legacy suffix name while retaining the role title', () => {
    expect(getResumeDisplayTitle({
      title: 'Java 后端实习简历 · 张三',
      currentVersion: { content: { basicInfo: { name: '张三' } } },
    })).toBe('Java 后端实习简历')
  })
})
