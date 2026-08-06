import { request } from './http'

export function getCaptchaImage() {
  return request('/captchaImage')
}

export function queryMeta(code, accessPwd) {
  const q = accessPwd ? `?accessPwd=${encodeURIComponent(accessPwd)}` : ''
  return request(`/open/query/${code}/meta${q}`)
}

export function querySearch(code, body) {
  return request(`/open/query/${code}/search`, { method: 'POST', body: JSON.stringify(body || {}) })
}

export function surveyMeta(code, accessPwd) {
  const q = accessPwd ? `?accessPwd=${encodeURIComponent(accessPwd)}` : ''
  return request(`/open/survey/${code}/meta${q}`)
}

export function surveySubmit(code, body) {
  return request(`/open/survey/${code}/submit`, { method: 'POST', body: JSON.stringify(body || {}) })
}

export function surveyDraft(code, params) {
  const q = new URLSearchParams()
  if (params && params.clientToken) q.set('clientToken', params.clientToken)
  if (params && params.accessPwd) q.set('accessPwd', params.accessPwd)
  const qs = q.toString()
  return request(`/open/survey/${code}/draft${qs ? ('?' + qs) : ''}`)
}

export function saveSurveyDraft(code, body) {
  return request(`/open/survey/${code}/draft`, { method: 'PUT', body: JSON.stringify(body || {}) })
}

export function surveyUploadUrl(code) {
  const BASE = import.meta.env.VITE_APP_BASE_API || ''
  return `${BASE}/open/survey/${code}/upload`
}

export async function queryExport(code, body) {
  const BASE = import.meta.env.VITE_APP_BASE_API || ''
  const res = await fetch(BASE + `/open/query/${code}/export`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body || {})
  })
  if (!res.ok) {
    throw new Error('导出失败')
  }
  const blob = await res.blob()
  const cd = res.headers.get('Content-Disposition') || ''
  let name = 'query.xlsx'
  const m = /filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i.exec(cd)
  if (m) name = decodeURIComponent(m[1] || m[2])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name
  a.click()
  URL.revokeObjectURL(url)
}

export function queryFieldDist(code, data) {
  return request(`/open/query/${code}/dist`, { method: 'POST', body: JSON.stringify(data || {}) })
}
