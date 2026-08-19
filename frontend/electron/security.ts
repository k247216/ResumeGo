export function isTrustedRendererUrl(url: string, trustedOrigin: string): boolean {
  if (!trustedOrigin) return false
  try {
    return new URL(url).origin === trustedOrigin
  } catch {
    return false
  }
}
