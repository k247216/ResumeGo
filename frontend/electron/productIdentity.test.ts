import { describe, expect, it } from 'vitest'
import { V2_PREVIEW_IDENTITY, resolveV2PreviewUserDataPath } from './productIdentity'

describe('V2 preview product identity', () => {
  it('resolves a userData directory that is isolated from V1', () => {
    expect(resolveV2PreviewUserDataPath('/Users/test/Library/Application Support'))
      .toBe('/Users/test/Library/Application Support/ZhidaCareerOSPreview')
    expect(resolveV2PreviewUserDataPath('/Users/test/Library/Application Support'))
      .not.toBe('/Users/test/Library/Application Support/ResumeGo')
  })

  it('exposes the approved preview display identity', () => {
    expect(V2_PREVIEW_IDENTITY.appName).toBe('ZhidaCareerOSPreview')
    expect(V2_PREVIEW_IDENTITY.productName).toBe('职达 Career OS Preview')
    expect(V2_PREVIEW_IDENTITY.appId).toBe('com.resumego.careeros.preview')
  })
})
