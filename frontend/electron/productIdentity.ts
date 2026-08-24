import path from 'node:path'

export const V2_PREVIEW_IDENTITY = Object.freeze({
  appName: 'ZhidaCareerOSPreview',
  productName: '职达 Career OS Preview',
  appId: 'com.resumego.careeros.preview',
  userDataDirectoryName: 'ZhidaCareerOSPreview',
})

export function resolveV2PreviewUserDataPath(appDataRoot: string): string {
  return path.join(appDataRoot, V2_PREVIEW_IDENTITY.userDataDirectoryName)
}
