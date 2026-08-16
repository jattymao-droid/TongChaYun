export function parseDictOptions(raw) {
  if (raw == null || raw === '') return []
  if (Array.isArray(raw)) {
    return raw.map(item => {
      if (item && typeof item === 'object') {
        return { label: item.label || item.value || item.text, value: item.value != null ? item.value : item.label }
      }
      return { label: String(item), value: String(item) }
    }).filter(o => o.value !== '' && o.value != null)
  }
  const text = String(raw).trim()
  try {
    const j = JSON.parse(text)
    return parseDictOptions(j)
  } catch (e) { /* ignore */ }
  return text.split(/[\n,]/).map(s => s.trim()).filter(Boolean).map(s => {
    const i = s.indexOf('|')
    if (i > 0) return { label: s.slice(i + 1).trim() || s.slice(0, i).trim(), value: s.slice(0, i).trim() }
    return { label: s, value: s }
  })
}

export function defaultLayout() {
  return {
    showSerial: false,
    resultLayout: 'auto',
    resultTitle: '',
    resultBgType: 'gradient',
    resultBgColor: '#f5f7fb',
    resultBgGradient: 'linear-gradient(180deg, #e8f1ff 0%, #f7f7f7 280px, #f7f7f7 100%)',
    resultBgImage: '',
    resultImageOverlay: 45,
    resultPanelStyle: 'card',
    resultContentAlign: 'center',
    resultMaxWidth: 960,
    resultShowTotal: true,
    resultShowExport: true,
    resultShowPrint: true,
    resultShowConditions: true,
    resultDense: false,
    resultTextTone: 'dark',
    resultPageSize: 10,
    resultShowEyebrow: true,
    resultCardColumns: 1,
    resultShowEmptyIcon: true,
    resultAnim: true,
    resultShowChart: false,
    resultChartType: 'bar',
    resultChartDefaultField: '',
    resultTitleField: '',
    resultSummaryFields: [],
    resultEmptyGuide: '',
    resultStyle: 'default',
    // form page
    formPanelStyle: 'glass',
    formColumns: 'auto',
    formWidthMode: 'auto',
    formMaxWidth: 720,
    formShowEyebrow: true,
    formShowAmbient: true,
    formShowFillHint: true,
    formAlign: 'center',
    formCompact: false,
    formBtnBlock: false,
    formAnim: true,
    formBgStyle: 'theme',
    posterBgType: 'theme',
    posterBgColor: '#eef2ff',
    posterBgImage: '',
    posterBgOverlay: 40,
    showLogo: false,
    logoUrl: '',
    formNoticeEnabled: false,
    formNoticeTitle: '说明',
    formNoticeText: '',
    formNoticeStyle: 'info',
    formNoticeAlign: 'left',
    formNoticeAnim: true
  }
}

export function noticeBoxVisible(layout) {
  return !!(layout && layout.formNoticeEnabled && String(layout.formNoticeText || '').trim())
}

export function formatNoticeHtml(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/\n/g, '<br>')
}

export function resultStylePresets() {
  return [
    {
      name: '清蓝办公',
      themeColor: '#1677ff',
      layout: {
        resultLayout: 'auto',
        resultPanelStyle: 'card',
        resultBgType: 'gradient',
        resultBgGradient: 'linear-gradient(180deg, #e8f1ff 0%, #f7f7f7 280px, #f7f7f7 100%)',
        resultTextTone: 'dark',
        resultContentAlign: 'center',
        resultMaxWidth: 960,
        resultDense: false,
        resultShowTotal: true,
        resultShowExport: true,
        resultShowPrint: true,
        resultShowConditions: true,
        resultShowEyebrow: true,
        resultCardColumns: 1,
        showSerial: false,
        formPanelStyle: 'glass',
        formColumns: 'auto',
        formWidthMode: 'auto',
        formShowAmbient: true,
        formBgStyle: 'theme'
      }
    },
    {
      name: '薄荷绿',
      themeColor: '#0f766e',
      layout: {
        resultLayout: 'auto',
        resultPanelStyle: 'flat',
        resultBgType: 'gradient',
        resultBgGradient: 'linear-gradient(180deg, #ecfdf5 0%, #f7f7f7 280px, #f7f7f7 100%)',
        resultTextTone: 'dark',
        resultContentAlign: 'center',
        resultMaxWidth: 920,
        resultDense: false,
        resultShowTotal: true,
        resultShowExport: true,
        resultShowPrint: true,
        resultShowConditions: true,
        showSerial: false,
        formPanelStyle: 'flat',
        formColumns: '2',
        formWidthMode: 'medium',
        formShowAmbient: true,
        formBgStyle: 'theme'
      }
    },
    {
      name: '暖沙卡片',
      themeColor: '#c2410c',
      layout: {
        resultLayout: 'card',
        resultPanelStyle: 'card',
        resultBgType: 'gradient',
        resultBgGradient: 'linear-gradient(180deg, #fff7ed 0%, #fafafa 280px, #fafafa 100%)',
        resultTextTone: 'dark',
        resultContentAlign: 'center',
        resultMaxWidth: 880,
        resultDense: false,
        resultShowTotal: true,
        resultShowExport: true,
        resultShowPrint: true,
        resultShowConditions: true,
        resultCardColumns: 2,
        showSerial: true,
        formPanelStyle: 'card',
        formColumns: 'auto',
        formWidthMode: 'narrow',
        formShowAmbient: false,
        formBgStyle: 'soft'
      }
    },
    {
      name: '深空毛玻璃',
      themeColor: '#93c5fd',
      layout: {
        resultLayout: 'auto',
        resultPanelStyle: 'glass',
        resultBgType: 'gradient',
        resultBgGradient: 'linear-gradient(180deg, #1f2937 0%, #111827 100%)',
        resultTextTone: 'light',
        resultContentAlign: 'center',
        resultMaxWidth: 960,
        resultDense: false,
        resultShowTotal: true,
        resultShowExport: true,
        resultShowPrint: true,
        resultShowConditions: true,
        showSerial: false,
        formPanelStyle: 'glass',
        formColumns: 'auto',
        formWidthMode: 'medium',
        formShowAmbient: true,
        formBgStyle: 'theme'
      }
    },
    {
      name: '扁平紧凑',
      themeColor: '#1f2937',
      layout: {
        resultLayout: 'table',
        resultPanelStyle: 'flat',
        resultBgType: 'color',
        resultBgColor: '#f8fafc',
        resultTextTone: 'dark',
        resultContentAlign: 'left',
        resultMaxWidth: 1100,
        resultDense: true,
        resultShowTotal: true,
        resultShowExport: true,
        resultShowPrint: true,
        resultShowConditions: false,
        resultShowEyebrow: false,
        showSerial: true,
        resultPageSize: 20,
        formPanelStyle: 'flat',
        formColumns: '2',
        formWidthMode: 'wide',
        formCompact: true,
        formShowAmbient: false,
        formShowEyebrow: false,
        formBgStyle: 'plain'
      }
    }
  ]
}

export function formStylePresets() {
  return [
    {
      name: '玻璃氛围',
      layout: {
        formPanelStyle: 'glass', formColumns: 'auto', formWidthMode: 'auto',
        formShowEyebrow: true, formShowAmbient: true, formShowFillHint: true,
        formAlign: 'center', formCompact: false, formBgStyle: 'theme', formAnim: true
      }
    },
    {
      name: '卡片居中',
      layout: {
        formPanelStyle: 'card', formColumns: '1', formWidthMode: 'narrow',
        formShowEyebrow: true, formShowAmbient: false, formShowFillHint: true,
        formAlign: 'center', formCompact: false, formBgStyle: 'soft', formAnim: true
      }
    },
    {
      name: '双列高效',
      layout: {
        formPanelStyle: 'flat', formColumns: '2', formWidthMode: 'medium',
        formShowEyebrow: false, formShowAmbient: false, formShowFillHint: true,
        formAlign: 'left', formCompact: true, formBgStyle: 'plain', formAnim: false
      }
    },
    {
      name: '宽屏三列',
      layout: {
        formPanelStyle: 'card', formColumns: '3', formWidthMode: 'wide',
        formShowEyebrow: true, formShowAmbient: true, formShowFillHint: true,
        formAlign: 'center', formCompact: false, formBgStyle: 'theme', formAnim: true, formBtnBlock: false
      }
    }
  ]
}

export function parseLayout(page) {
  const layout = defaultLayout()
  if (!page) return layout
  let raw = page.layoutJson
  if (!raw && page.layout) raw = page.layout
  if (!raw) return layout
  try {
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (!obj || typeof obj !== 'object') return layout
    Object.keys(layout).forEach(k => {
      if (obj[k] !== undefined && obj[k] !== null && obj[k] !== '') layout[k] = obj[k]
    })
    // booleans (including false)
    ;['showSerial','resultShowTotal','resultShowExport','resultShowPrint','resultShowConditions','resultDense',
      'resultShowEyebrow','resultShowEmptyIcon','resultAnim','resultShowChart',
      'formShowEyebrow','formShowAmbient','formShowFillHint','formCompact','formBtnBlock','formAnim',
      'showLogo','formNoticeEnabled','formNoticeAnim'
    ].forEach(k => {
      if (obj[k] !== undefined && obj[k] !== null) layout[k] = !!obj[k]
    })
    layout.resultShowTotal = layout.resultShowTotal !== false
    layout.resultShowExport = layout.resultShowExport !== false
    layout.resultShowPrint = layout.resultShowPrint !== false
    layout.resultShowConditions = layout.resultShowConditions !== false
    layout.resultShowEyebrow = layout.resultShowEyebrow !== false
    layout.resultShowEmptyIcon = layout.resultShowEmptyIcon !== false
    layout.resultShowChart = layout.resultShowChart === true
    if (!layout.resultChartType) layout.resultChartType = 'bar'
    if (layout.resultChartDefaultField == null) layout.resultChartDefaultField = ''
    layout.resultAnim = layout.resultAnim !== false
    layout.formShowEyebrow = layout.formShowEyebrow !== false
    layout.formShowAmbient = layout.formShowAmbient !== false
    layout.formShowFillHint = layout.formShowFillHint !== false
    layout.formAnim = layout.formAnim !== false
    layout.formNoticeEnabled = layout.formNoticeEnabled === true
    layout.formNoticeAnim = layout.formNoticeAnim !== false
    if (layout.formNoticeTitle == null || layout.formNoticeTitle === '') layout.formNoticeTitle = '说明'
    if (layout.formNoticeText == null) layout.formNoticeText = ''
    if (!['info', 'tip', 'warn', 'soft', 'quote', 'plain'].includes(layout.formNoticeStyle)) layout.formNoticeStyle = 'info'
    if (!['left', 'center'].includes(layout.formNoticeAlign)) layout.formNoticeAlign = 'left'

    layout.resultMaxWidth = Number(layout.resultMaxWidth) || 960
    layout.formMaxWidth = Number(layout.formMaxWidth) || 720
    layout.resultImageOverlay = Math.min(90, Math.max(0, Number(layout.resultImageOverlay)))
    if (Number.isNaN(layout.resultImageOverlay)) layout.resultImageOverlay = 45
    const ps = Number(layout.resultPageSize)
    layout.resultPageSize = [10, 20, 50].includes(ps) ? ps : 10
    const cc = Number(layout.resultCardColumns)
    layout.resultCardColumns = [1, 2].includes(cc) ? cc : 1

    if (!['table', 'card', 'auto'].includes(layout.resultLayout)) layout.resultLayout = 'auto'
    if (!['default', 'scorecard'].includes(layout.resultStyle)) layout.resultStyle = 'default'
    if (layout.resultTitleField == null) layout.resultTitleField = ''
    if (!Array.isArray(layout.resultSummaryFields)) {
      layout.resultSummaryFields = layout.resultSummaryFields
        ? String(layout.resultSummaryFields).split(',').map(s => s.trim()).filter(Boolean)
        : []
    }
    if (layout.resultEmptyGuide == null) layout.resultEmptyGuide = ''
    if (!['color', 'gradient', 'image'].includes(layout.resultBgType)) layout.resultBgType = 'gradient'
    if (!['card', 'flat', 'glass'].includes(layout.resultPanelStyle)) layout.resultPanelStyle = 'card'
    if (!['dark', 'light'].includes(layout.resultTextTone)) layout.resultTextTone = 'dark'
    if (!['card', 'flat', 'glass'].includes(layout.formPanelStyle)) layout.formPanelStyle = 'glass'
    if (!['auto', '1', '2', '3'].includes(String(layout.formColumns))) layout.formColumns = 'auto'
    layout.formColumns = String(layout.formColumns)
    if (!['auto', 'narrow', 'medium', 'wide', 'custom'].includes(layout.formWidthMode)) layout.formWidthMode = 'auto'
    if (!['center', 'left'].includes(layout.formAlign)) layout.formAlign = 'center'
    if (!['theme', 'soft', 'plain'].includes(layout.formBgStyle)) layout.formBgStyle = 'theme'
    if (!['theme', 'color', 'image'].includes(layout.posterBgType)) layout.posterBgType = 'theme'
    let overlay = Number(layout.posterBgOverlay)
    if (Number.isNaN(overlay)) overlay = 40
    layout.posterBgOverlay = Math.min(90, Math.max(0, overlay))
  } catch (e) { /* ignore */ }
  return layout
}

export function resolveFormWidth(layout, fieldCount) {
  const L = layout || defaultLayout()
  const mode = L.formWidthMode || 'auto'
  if (mode === 'custom') return Number(L.formMaxWidth) || 720
  if (mode === 'narrow') return 480
  if (mode === 'medium') return 720
  if (mode === 'wide') return 960
  const n = fieldCount || 0
  if (n <= 2) return 480
  if (n <= 5) return 720
  return 960
}

export function resolveFormColumns(layout, fieldCount) {
  const L = layout || defaultLayout()
  const c = String(L.formColumns || 'auto')
  if (c === '1' || c === '2' || c === '3') return c
  const n = fieldCount || 0
  if (n <= 1) return '1'
  if (n <= 5) return '2'
  return '3'
}

export function buildFormPageStyle(layout, themeColor) {
  const L = layout || defaultLayout()
  const theme = themeColor || '#1677ff'
  const style = { '--theme': theme }
  if (L.formBgStyle === 'plain') {
    style.background = '#f5f7fb'
  } else if (L.formBgStyle === 'soft') {
    style.background = 'linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%)'
  } else {
    style.background =
      'radial-gradient(900px 320px at 50% -8%, color-mix(in srgb, ' + theme + ' 28%, transparent), transparent 70%),' +
      'linear-gradient(180deg, #f7f9fc 0%, #eef1f6 100%)'
  }
  return style
}

/** Normalize upload path: strip API prefix, take first of comma-separated */
export function resolveAssetUrl(path, apiBase) {
  if (!path) return ''
  let url = String(path).split(',')[0].trim()
  if (!url) return ''
  if (/^https?:\/\//i.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url
  url = url.replace(/\\/g, '/')
  const marker = '/uploadPath/'
  const mi = url.indexOf(marker)
  if (mi >= 0) {
    url = '/profile/' + url.substring(mi + marker.length).replace(/^\/+/, '')
  } else {
    const uploadIdx = url.indexOf('/upload/')
    if (url.includes('wwwroot') && uploadIdx >= 0) {
      url = '/profile' + url.substring(uploadIdx)
    }
  }
  url = url.replace(/^\/?(dev-api|prod-api)/, '')
  if (!url.startsWith('/')) url = '/' + url
  const base = (apiBase || '').replace(/\/$/, '')
  return base ? base + url : url
}

/** CSS vars / inline style for result page shell */
export function buildResultPageStyle(layout, themeColor, apiBase) {
  const L = layout || defaultLayout()
  const theme = themeColor || '#1677ff'
  const light = L.resultTextTone === 'light'
  const style = {
    '--theme': theme,
    '--result-max': (L.resultMaxWidth || 960) + 'px',
    '--result-muted': light ? 'rgba(248,250,252,.72)' : '#6b7280',
    '--result-fg': light ? '#f8fafc' : '#1f2937',
    color: light ? '#f8fafc' : undefined,
    textAlign: L.resultContentAlign === 'left' ? 'left' : 'center'
  }
  if (L.resultBgType === 'color') {
    style.background = L.resultBgColor || '#f5f7fb'
  } else if (L.resultBgType === 'image' && L.resultBgImage) {
    const url = resolveAssetUrl(L.resultBgImage, apiBase)
    let overlay = Number(L.resultImageOverlay)
    if (Number.isNaN(overlay)) overlay = 45
    overlay = Math.min(90, Math.max(0, overlay)) / 100
    const a1 = (overlay * 0.55).toFixed(2)
    const a2 = Math.min(0.92, overlay + 0.2).toFixed(2)
    style.backgroundColor = '#f5f7fb'
    style.backgroundImage = `linear-gradient(180deg, rgba(255,255,255,${a1}), rgba(247,247,247,${a2})), url("${url}")`
    style.backgroundSize = 'cover'
    style.backgroundPosition = 'center'
    style.backgroundRepeat = 'no-repeat'
  } else {
    style.background = L.resultBgGradient || 'linear-gradient(180deg, #e8f1ff 0%, #f7f7f7 280px, #f7f7f7 100%)'
  }
  return style
}

/** Build human-readable condition chips for result page */
export function summarizeConditions(params, queryFields) {
  const list = []
  ;(queryFields || []).forEach(f => {
    const key = f.fieldKey
    const raw = params && params[key]
    if (raw == null || raw === '') return
    const label = f.fieldLabel || f.fieldName || key
    let value = raw
    if (Array.isArray(raw)) {
      if (!raw[0] && !raw[1]) return
      value = (raw[0] || '') + ' ~ ' + (raw[1] || '')
    }
    list.push({ key, label, value: String(value) })
  })
  return list
}

export function normalizeQueryParams(form, queryFields) {
  const params = {}
  ;(queryFields || []).forEach(f => {
    const key = f.fieldKey
    let val = form[key]
    if (val == null || val === '') return
    if (Array.isArray(val)) {
      if (val.length === 0 || (val[0] == null && val[1] == null)) return
      if (String(f.queryType || '').toUpperCase() === 'BETWEEN') {
        const from = val[0] == null ? '' : String(val[0]).trim()
        const to = val[1] == null ? '' : String(val[1]).trim()
        if (!from || !to) return
        params[key] = [from, to]
        return
      }
      const parts = val.map(v => (v == null ? '' : String(v).trim())).filter(Boolean)
      if (!parts.length) return
      params[key] = parts.join(',')
      return
    }
    const s = String(val).trim()
    if (!s) return
    params[key] = s
  })
  return params
}

/** Whether a single query field has a complete value (BETWEEN needs both ends). */
export function isQueryFieldFilled(f, formOrParams) {
  if (!f || !formOrParams) return false
  const val = formOrParams[f.fieldKey]
  const op = String(f.queryType || '').toUpperCase()
  if (val == null || val === '') return false
  if (Array.isArray(val)) {
    if (op === 'BETWEEN') {
      return String(val[0] == null ? '' : val[0]).trim() !== '' && String(val[1] == null ? '' : val[1]).trim() !== ''
    }
    return val.map(v => (v == null ? '' : String(v).trim())).filter(Boolean).length > 0
  }
  return String(val).trim() !== ''
}

/** Required query-condition fields that are still empty. */
export function missingQueryFields(formOrParams, queryFields) {
  return (queryFields || []).filter(f => {
    if (!f) return false
    const required = f.isRequired !== '0' && f.isRequired !== 0 && f.isRequired !== false
    return required && !isQueryFieldFilled(f, formOrParams || {})
  })
}

/** Required fields filled, and at least one condition present. */
export function hasAllQueryParams(formOrParams, queryFields) {
  const list = queryFields || []
  if (!list.length) return false
  if (missingQueryFields(formOrParams, list).length) return false
  return hasAnyQueryParam(normalizeQueryParams(formOrParams || {}, list))
}

export function hasAnyQueryParam(params) {
  return Object.keys(params || {}).length > 0
}

export function paramsStorageKey(code) {
  return 'biz_q_params_' + code
}

export function saveQueryParams(code, params) {
  try {
    sessionStorage.setItem(paramsStorageKey(code), JSON.stringify(params || {}))
  } catch (e) { /* ignore */ }
}

export function loadQueryParams(code) {
  try {
    const raw = sessionStorage.getItem(paramsStorageKey(code))
    if (!raw) return {}
    const obj = JSON.parse(raw)
    return obj && typeof obj === 'object' ? obj : {}
  } catch (e) {
    return {}
  }
}

export function paramsToRouteQuery(_params, pageNum) {
  return { page: String(pageNum || 1) }
}

export function routeQueryToParams(query, queryFields) {
  const params = {}
  ;(queryFields || []).forEach(f => {
    const key = f.fieldKey
    const raw = query && query[key]
    if (raw == null || raw === '') return
    const op = String(f.queryType || '').toUpperCase()
    if (op === 'BETWEEN') {
      const parts = String(raw).split(',')
      params[key] = [parts[0] || '', parts[1] || '']
    } else if (op === 'IN' && f.htmlType === 'select') {
      params[key] = String(raw).split(',').map(s => s.trim()).filter(Boolean)
    } else {
      params[key] = String(raw)
    }
  })
  return params
}

export function pwdStorageKey(code) {
  return 'biz_q_pwd_' + code
}

export function bannerUrl(page, apiBase) {
  const u = page && page.bannerUrl
  if (!u) return ''
  if (/^https?:\/\//i.test(u) || u.startsWith('data:')) return u
  return (apiBase || '') + u
}

export function displayCell(v) {
  if (v == null) return '\u2014'
  const s = String(v).trim()
  return s === '' ? '\u2014' : s
}

export function splitFieldLabel(label) {
  const raw = String(label || '')
  const i = raw.indexOf('.')
  if (i > 0 && i < raw.length - 1) {
    return { group: raw.slice(0, i), name: raw.slice(i + 1) }
  }
  return { group: '', name: raw }
}

/** Group list fields by "Dataset.Column" label prefix (multi-table join). */
export function groupListFields(fields) {
  const list = fields || []
  const map = new Map()
  const order = []
  list.forEach((f) => {
    const label = f.fieldLabel || f.fieldName || f.fieldKey
    const parts = splitFieldLabel(label)
    const g = parts.group || ''
    if (!map.has(g)) {
      map.set(g, [])
      order.push(g)
    }
    map.get(g).push(Object.assign({}, f, { shortLabel: parts.name || label }))
  })
  return order.map((g) => ({ group: g, fields: map.get(g) }))
}

export function primaryListField(fields) {
  return (fields && fields.length) ? fields[0] : null
}

export function pageRangeText(pageNum, pageSize, total) {
  const n = Number(total) || 0
  if (n <= 0) return '\u5171 0 \u6761'
  const from = (pageNum - 1) * pageSize + 1
  const to = Math.min(pageNum * pageSize, n)
  return '\u7b2c ' + from + '\u2013' + to + ' \u6761\uff0c\u5171 ' + n + ' \u6761'
}

export function isConditionHit(fieldKey, cellValue, params, queryFields) {
  if (!params || fieldKey == null || params[fieldKey] == null || params[fieldKey] === '') return false
  const cell = String(cellValue == null ? '' : cellValue).trim()
  if (!cell) return false
  const f = (queryFields || []).find((x) => x && x.fieldKey === fieldKey)
  const op = String((f && f.queryType) || 'EQ').toUpperCase()
  const raw = params[fieldKey]
  if (op === 'LIKE') return cell.toLowerCase().includes(String(raw).trim().toLowerCase())
  if (op === 'IN') {
    const parts = Array.isArray(raw)
      ? raw.map((s) => String(s).trim()).filter(Boolean)
      : String(raw).split(/[,，]/).map((s) => s.trim()).filter(Boolean)
    return parts.includes(cell)
  }
  if (op === 'BETWEEN') {
    let a = raw
    let b = null
    if (Array.isArray(raw)) {
      a = raw[0]
      b = raw[1]
    } else {
      const parts = String(raw).split(',')
      a = parts[0]
      b = parts[1]
    }
    if (a == null || b == null || a === '' || b === '') return false
    return cell >= String(a).trim() && cell <= String(b).trim()
  }
  return cell === String(raw).trim()
}

export function formatRowPlainText(row, fields) {
  return (fields || []).map((f) => {
    const label = f.fieldLabel || f.fieldName || f.fieldKey
    return label + ': ' + displayCell(row ? row[f.fieldKey] : '')
  }).join('\n')
}

export function copyText(text) {
  const value = String(text == null ? '' : text)
  if (!value) return Promise.reject(new Error('empty'))
  if (typeof navigator !== 'undefined' && navigator.clipboard && navigator.clipboard.writeText) {
    return navigator.clipboard.writeText(value)
  }
  return new Promise((resolve, reject) => {
    try {
      const ta = document.createElement('textarea')
      ta.value = value
      ta.setAttribute('readonly', '')
      ta.style.position = 'fixed'
      ta.style.left = '-9999px'
      document.body.appendChild(ta)
      ta.select()
      const ok = document.execCommand('copy')
      document.body.removeChild(ta)
      ok ? resolve() : reject(new Error('copy failed'))
    } catch (e) {
      reject(e)
    }
  })
}

