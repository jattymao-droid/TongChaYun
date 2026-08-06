const BASE = import.meta.env.VITE_APP_BASE_API || ''

export async function request(url, options = {}) {
  const res = await fetch(BASE + url, {
    ...options,
    headers: {
      ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...(options.headers || {})
    }
  })
  const data = await res.json().catch(() => ({}))
  if (data.code !== undefined && data.code !== 200) {
    const err = new Error(data.msg || '请求失败')
    err.data = data
    throw err
  }
  return data
}
