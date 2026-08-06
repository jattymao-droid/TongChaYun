/**
 * Survey open-page draft + client token helpers (keep in sync with ruoyi-h5 / ruoyi-ui).
 */

export function draftKey(code) {
  return 'ss_survey_draft_' + code
}

export function submittedKey(code) {
  return 'ss_survey_submitted_' + code
}

export function clientTokenKey(code) {
  return 'ss_survey_token_' + code
}

function deviceFingerprintSeed() {
  try {
    const parts = [
      typeof navigator !== 'undefined' ? (navigator.userAgent || '') : '',
      typeof navigator !== 'undefined' ? (navigator.language || '') : '',
      typeof screen !== 'undefined' ? (screen.width + 'x' + screen.height + 'x' + (screen.colorDepth || '')) : '',
      String(new Date().getTimezoneOffset())
    ]
    return parts.join('|')
  } catch (e) {
    return 'na'
  }
}

function hashSeed(str) {
  // FNV-1a 32-bit → hex (lightweight, no crypto dependency)
  let h = 0x811c9dc5
  const s = String(str || '')
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i)
    h = Math.imul(h, 0x01000193)
  }
  return ('00000000' + (h >>> 0).toString(16)).slice(-8)
}

export function getOrCreateClientToken(code) {
  const key = clientTokenKey(code)
  let token = ''
  try { token = localStorage.getItem(key) || '' } catch (e) { token = '' }
  if (!token) {
    const uuid = (typeof crypto !== 'undefined' && crypto.randomUUID)
      ? crypto.randomUUID().replace(/-/g, '')
      : ('t' + Date.now().toString(16) + Math.random().toString(16).slice(2))
    token = (uuid + hashSeed(deviceFingerprintSeed())).slice(0, 64)
    try { localStorage.setItem(key, token) } catch (e) { /* ignore */ }
  }
  return token
}

export function loadDraft(code) {
  try {
    const raw = localStorage.getItem(draftKey(code))
    if (!raw) return null
    const data = JSON.parse(raw)
    return data && typeof data === 'object' ? data : null
  } catch (e) {
    return null
  }
}

export function saveDraft(code, form) {
  try {
    localStorage.setItem(draftKey(code), JSON.stringify({
      savedAt: Date.now(),
      form: form || {}
    }))
  } catch (e) { /* ignore quota */ }
}

export function clearDraft(code) {
  try { localStorage.removeItem(draftKey(code)) } catch (e) { /* ignore */ }
}

export function markSubmitted(code) {
  try { localStorage.setItem(submittedKey(code), '1') } catch (e) { /* ignore */ }
  clearDraft(code)
}

export function isSubmittedLocally(code) {
  try { return localStorage.getItem(submittedKey(code)) === '1' } catch (e) { return false }
}
