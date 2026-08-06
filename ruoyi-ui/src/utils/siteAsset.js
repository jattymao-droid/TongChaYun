/**
 * Resolve site logo / uploaded asset URL for <img src>.
 */
export function resolveSiteAsset(path) {
  const version = '20260807'
  const fallback = (process.env.BASE_URL || '/') + 'logo.svg?v=' + version
  if (!path) return fallback
  const p = String(path).trim()
  if (!p) return fallback
  if (/^(https?:)?\/\//i.test(p) || p.startsWith('data:') || p.startsWith('blob:')) {
    return p
  }
  // public static (frontend)
  if (p === '/logo.svg' || p === 'logo.svg' || p.startsWith('/logo.')) {
    const name = p.replace(/^\//, '')
    return (process.env.BASE_URL || '/') + name + (name.includes('?') ? '&' : '?') + 'v=' + version
  }
  // uploaded via /common/upload → /profile/...
  const api = process.env.VUE_APP_BASE_API || ''
  if (p.startsWith('/profile') || p.startsWith('profile/')) {
    return api + (p.startsWith('/') ? p : '/' + p)
  }
  if (p.startsWith('/')) {
    // prefer API for unknown absolute paths (RuoYi uploads)
    if (p.startsWith('/profile') || p.includes('/upload')) {
      return api + p
    }
    return (process.env.BASE_URL || '/') + p.replace(/^\//, '')
  }
  return api + '/' + p
}
