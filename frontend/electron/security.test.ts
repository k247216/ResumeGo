// @vitest-environment node

import { describe, expect, it } from 'vitest'
import { isTrustedRendererUrl } from './security.js'

describe('isTrustedRendererUrl', () => {
  it('accepts only pages served from the generated application origin', () => {
    expect(isTrustedRendererUrl('http://127.0.0.1:43123/settings', 'http://127.0.0.1:43123')).toBe(true)
    expect(isTrustedRendererUrl('https://example.com/', 'http://127.0.0.1:43123')).toBe(false)
    expect(isTrustedRendererUrl('http://127.0.0.1:43124/', 'http://127.0.0.1:43123')).toBe(false)
    expect(isTrustedRendererUrl('not a url', 'http://127.0.0.1:43123')).toBe(false)
  })
})
