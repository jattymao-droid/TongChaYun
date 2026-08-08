/** Shared survey question helpers (keep ruoyi-ui / ruoyi-h5 in sync). */

export const TYPE_LABELS = {
  radio: '单选',
  checkbox: '多选',
  select: '下拉',
  yesno: '是非题',
  image_radio: '图片单选',
  image_checkbox: '图片多选',
  likert: '量表',
  input: '单行填空',
  textarea: '多行填空',
  number: '数字',
  email: '邮箱',
  phone: '手机号',
  url: '网址',
  idcard: '身份证',
  date: '日期',
  time: '时间',
  datetime: '日期时间',
  rate: '评分',
  nps: 'NPS',
  slider: '滑块',
  file: '附件',
  section: '说明段落',
  page_break: '分页符',
  agreement: '协议同意',
  signature: '手写签名',
  matrix_radio: '矩阵单选',
  cascade_select: '级联选择'
}

export const JUMP_TYPES = ['radio', 'select', 'yesno', 'image_radio', 'likert']
export const CHOICE_TYPES = ['radio', 'checkbox', 'select', 'yesno', 'image_radio', 'image_checkbox', 'likert', 'cascade_select']
export const MULTI_CHOICE_TYPES = ['checkbox', 'image_checkbox']
export const NUMERIC_TYPES = ['rate', 'nps', 'slider', 'number']
export const DISPLAY_ONLY_TYPES = ['section', 'page_break']

/** Structural / non-answerable question types (section, page_break). */
export function isDisplayOnly(type) {
  return DISPLAY_ONLY_TYPES.includes(type)
}

/** Server hard ceiling; design page maxSizeMb must not exceed this. */
export const SURVEY_UPLOAD_HARD_MAX_MB = 10

export const LIKERT_DEFAULT_OPTIONS = [
  { label: '非常不同意', value: '1' },
  { label: '不同意', value: '2' },
  { label: '一般', value: '3' },
  { label: '同意', value: '4' },
  { label: '非常同意', value: '5' }
]

export function typeLabel(t) {
  return TYPE_LABELS[t] || t || '未知'
}

export function parseJsonSafe(raw, fallback) {
  if (raw == null || raw === '') return fallback
  try {
    return JSON.parse(raw)
  } catch (e) {
    return fallback
  }
}

export function placeholderOf(q) {
  if (q && q._placeholder) return q._placeholder
  if (q && q.qType === 'phone') return '请输入手机号'
  if (q && q.qType === 'email') return '请输入邮箱'
  if (q && q.qType === 'url') return '请输入网址，如 https://example.com'
  if (q && q.qType === 'idcard') return '请输入18位身份证号'
  if (q && q.qType === 'time') return '请选择时间'
  return '请输入'
}

export function defaultAnswerValue(q) {
  if (!q) return ''
  if (MULTI_CHOICE_TYPES.includes(q.qType)) return []
  if (q.qType === 'rate') return 0
  if (q.qType === 'nps' || q.qType === 'slider') {
    const n = Number(q._min)
    return Number.isFinite(n) ? n : 0
  }
  if (q.qType === 'number') return undefined
  if (q.qType === 'matrix_radio') return {}
  if (q.qType === 'cascade_select') return []
  return ''
}

/**
 * Normalize a question from API for rendering / validation.
 * @param {object} q
 * @param {number} i index
 * @param {{ keyMode?: 'preview'|'open' }} [opts]
 */
export function normalizeQuestion(q, i, opts) {
  const keyMode = (opts && opts.keyMode) || 'open'
  let options = parseJsonSafe(q.optionsJson, [])
  const props = parseJsonSafe(q.propsJson, {}) || {}
  if (!Array.isArray(options)) options = []
  if ((!options || !options.length) && q.qType === 'yesno') {
    options = [{ label: '是', value: '1' }, { label: '否', value: '0' }]
  }
  if ((!options || !options.length) && q.qType === 'likert') {
    options = LIKERT_DEFAULT_OPTIONS.map(o => ({ ...o }))
  }
  const key = q.questionId != null ? ('id_' + q.questionId) : ('tmp_' + i)
  const maxSizeMb = props.maxSizeMb == null ? 5 : Number(props.maxSizeMb)
  return {
    ...q,
    _key: key,
    questionId: q.questionId != null ? q.questionId : (keyMode === 'preview' ? key : q.questionId),
    _options: options,
    _placeholder: props.placeholder || '',
    _jumps: props.jumps || [],
    _minLength: props.minLength,
    _maxLength: props.maxLength,
    _max: props.max != null ? Number(props.max) : (q.qType === 'nps' ? 10 : (q.qType === 'slider' ? 100 : 5)),
    _min: props.min != null ? Number(props.min) : (q.qType === 'nps' || q.qType === 'slider' ? 0 : undefined),
    _step: props.step != null ? Number(props.step) : 1,
    _maxSizeMb: Number.isFinite(maxSizeMb) ? Math.min(Math.max(maxSizeMb, 1), SURVEY_UPLOAD_HARD_MAX_MB) : 5,
    _leftLabel: props.leftLabel || '',
    _rightLabel: props.rightLabel || '',
    _content: props.content || '',
    _agreeLabel: props.agreeLabel || '我已阅读并同意',
    _bindAgreementSort: props.bindAgreementSort != null && props.bindAgreementSort !== ''
      ? Number(props.bindAgreementSort) : null,
    _penColor: props.penColor || '#111111',
    _padHeight: props.padHeight != null ? Number(props.padHeight) : 160,
    _visibleIf: props.visibleIf || null,
    _rows: Array.isArray(props.rows) ? props.rows : [],
    sort: q.sort == null ? i : q.sort
  }
}

/**
 * @param {Array} questions
 * @param {(q: object) => any} getAnswer
 */
export function resolveVisibleQuestions(questions, getAnswer) {
  if (!questions || !questions.length) return []
  const sortIndex = {}
  questions.forEach((q, i) => { sortIndex[q.sort == null ? i : q.sort] = i })
  const path = []
  const visited = new Set()
  let i = 0
  while (i >= 0 && i < questions.length && !visited.has(i)) {
    visited.add(i)
    const q = questions[i]
    path.push(q)
    let next = i + 1 < questions.length ? i + 1 : null
    if (JUMP_TYPES.includes(q.qType)) {
      const ans = getAnswer(q)
      const value = ans == null || ans === '' ? null : String(ans)
      if (value != null) {
        const jump = (q._jumps || []).find(j => String(j.value) === value)
        if (jump && jump.toSort !== null && jump.toSort !== undefined && jump.toSort !== '') {
          const toSort = Number(jump.toSort)
          next = toSort < 0 ? null : (sortIndex[toSort] !== undefined ? sortIndex[toSort] : next)
        }
      }
    }
    if (next === null || next === undefined) break
    i = next
  }
  return path.filter(q => matchesVisibleIf(q, questions, getAnswer))
}

function matchesVisibleIf(q, questions, getAnswer) {
  const rule = q && q._visibleIf
  if (!rule || rule.sourceSort === null || rule.sourceSort === undefined || rule.sourceSort === '') return true
  const sourceSort = Number(rule.sourceSort)
  if (!Number.isFinite(sourceSort)) return true
  const expect = rule.value == null ? null : String(rule.value)
  if (expect == null) return true
  const source = questions.find((x, idx) => Number(x.sort == null ? idx : x.sort) === sourceSort)
  if (!source) return false
  const ans = getAnswer(source)
  const scalar = Array.isArray(ans) ? (ans.length ? String(ans[0]) : null) : (ans == null || ans === '' ? null : String(ans))
  return expect === scalar
}

function isEmptyAnswer(q, v) {
  if (q.qType === 'file' || q.qType === 'signature') {
    if (!v) return true
    if (typeof v === 'object') return !v.fileName
    return String(v).trim() === ''
  }
  if (q.qType === 'agreement') {
    return v !== '1' && v !== 1 && v !== true
  }
  if (MULTI_CHOICE_TYPES.includes(q.qType) || q.qType === 'cascade_select') return !v || !v.length
  if (q.qType === 'rate') return v === undefined || v === null || v === '' || Number(v) <= 0
  if (NUMERIC_TYPES.includes(q.qType) && q.qType !== 'rate') {
    return v === undefined || v === null || String(v).trim() === ''
  }
  return v === undefined || v === null || String(v).trim() === ''
}

/**
 * Split visible questions into pages by page_break markers.
 * Consecutive breaks collapse; trailing empty page dropped.
 * @returns {{ title: string, questions: Array }[]}
 */
export function groupVisibleIntoPages(visibleQuestions) {
  const pages = []
  let current = { title: '', questions: [] }
  for (const q of visibleQuestions || []) {
    if (q.qType === 'page_break') {
      if (current.questions.length) {
        pages.push(current)
        current = { title: (q.title || '').trim(), questions: [] }
      } else if ((q.title || '').trim()) {
        current.title = (q.title || '').trim()
      }
      continue
    }
    current.questions.push(q)
  }
  if (current.questions.length) pages.push(current)
  return pages.length ? pages : [{ title: '', questions: [] }]
}

/** 1-based display number among answerable questions in the full visible list. */
export function questionDisplayNo(visibleQuestions, q) {
  if (!q || isDisplayOnly(q.qType)) return ''
  let n = 0
  for (const item of visibleQuestions || []) {
    if (isDisplayOnly(item.qType)) continue
    n++
    if (item === q || (item.questionId != null && item.questionId === q.questionId)) return n
  }
  return n
}

function sortOf(q, fallbackIndex) {
  if (q && q.sort != null && q.sort !== '') {
    const n = Number(q.sort)
    if (Number.isFinite(n)) return n
  }
  return fallbackIndex
}

/** Signature bound to an agreement question (rendered under that agreement). */
export function isEmbeddedSignature(q, allQuestions) {
  if (!q || q.qType !== 'signature') return false
  if (q._bindAgreementSort == null || q._bindAgreementSort === '') return false
  const bind = Number(q._bindAgreementSort)
  if (!Number.isFinite(bind)) return false
  return (allQuestions || []).some((x, i) => x.qType === 'agreement' && sortOf(x, i) === bind)
}

/** Visible signatures bound to a given agreement (same sort / bindAgreementSort). */
export function getBoundSignatures(agreementQ, visibleQuestions) {
  if (!agreementQ || agreementQ.qType !== 'agreement') return []
  const target = Number(agreementQ.sort)
  if (!Number.isFinite(target)) return []
  return (visibleQuestions || []).filter(q =>
    q.qType === 'signature'
    && q._bindAgreementSort != null
    && q._bindAgreementSort !== ''
    && Number(q._bindAgreementSort) === target
  )
}

/** Drop bound signatures from top-level lists; they render inside the agreement. */
export function withoutEmbeddedSignatures(list, allQuestions) {
  const pool = allQuestions || list || []
  return (list || []).filter(q => !isEmbeddedSignature(q, pool))
}

/** Include nested bound signatures after each agreement (for validation). */
export function expandWithBoundSignatures(displayList, allVisible) {
  const out = []
  const seen = new Set()
  const mark = (q) => {
    const id = q && (q.questionId != null ? 'id:' + q.questionId : (q._key != null ? 'k:' + q._key : null))
    if (id) {
      if (seen.has(id)) return false
      seen.add(id)
    }
    return true
  }
  for (const q of displayList || []) {
    if (!mark(q)) continue
    out.push(q)
    if (q.qType === 'agreement') {
      getBoundSignatures(q, allVisible).forEach(sq => {
        if (mark(sq)) out.push(sq)
      })
    }
  }
  return out
}

/**
 * Client-side validation aligned with backend BizSurveyServiceImpl.validateAnswerProps.
 * @returns {{ ok: true } | { ok: false, message: string }}
 */
export function validateSurveyAnswers(visibleQuestions, getValue) {
  for (const q of visibleQuestions || []) {
    if (isDisplayOnly(q.qType)) continue
    let v = getValue(q)
    if (q.qType === 'agreement') {
      if (q.required === '1' && isEmptyAnswer(q, v)) {
        return { ok: false, message: '请阅读并同意：' + (q.title || '协议') }
      }
      continue
    }
    if (q.qType === 'signature') {
      if (q.required === '1' && isEmptyAnswer(q, v)) {
        return { ok: false, message: '请完成签名：' + (q.title || '手写签名') }
      }
      continue
    }
    if (q.qType === 'cascade_select') {
      const path = Array.isArray(v) ? v : (typeof v === 'string' && v.trim().startsWith('[') ? (() => { try { return JSON.parse(v) } catch (e) { return [] } })() : (v ? [v] : []))
      if (q.required === '1' && (!path || !path.length)) return { ok: false, message: '请完成必填题：' + q.title }
      if (path && path.length && !cascadePathValid(q._options || [], path)) {
        return { ok: false, message: '级联选项无效：' + q.title }
      }
      continue
    }
    if (q.qType === 'matrix_radio') {
      const rows = q._rows || []
      const ans = v && typeof v === 'object' && !Array.isArray(v) ? v : {}
      if (q.required === '1') {
        for (const row of rows) {
          const rk = row.value || row.label
          if (!ans[rk]) return { ok: false, message: '请完成必填题：' + q.title }
        }
      }
      const allowed = new Set((q._options || []).map(o => String(o.value)))
      for (const rk of Object.keys(ans)) {
        if (ans[rk] && allowed.size && !allowed.has(String(ans[rk]))) {
          return { ok: false, message: '矩阵题选项无效：' + q.title }
        }
      }
      continue
    }
    if (NUMERIC_TYPES.includes(q.qType) && q.qType !== 'file') {
      if (v === 0 || v) v = v
      else v = ''
    }
    if (q.required === '1' && isEmptyAnswer(q, v)) {
      return { ok: false, message: '请完成必填题：' + q.title }
    }
    if (q.qType === 'file' || isEmptyAnswer(q, v)) continue

    const s = Array.isArray(v) ? '' : String(v)

    if (q.qType === 'phone' && !/^1\d{10}$/.test(s)) {
      return { ok: false, message: '请输入正确手机号：' + q.title }
    }
    if (q.qType === 'email' && !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(s)) {
      return { ok: false, message: '请输入正确邮箱：' + q.title }
    }
    if (q.qType === 'url' && !/^(https?:\/\/).+/.test(s)) {
      return { ok: false, message: '请输入正确网址（需以 http:// 或 https:// 开头）：' + q.title }
    }
    if (q.qType === 'idcard' && !/^[1-9]\d{5}(19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[0-9Xx]$/.test(s)) {
      return { ok: false, message: '请输入正确身份证号：' + q.title }
    }
    if (q.qType === 'time' && !/^([01]\d|2[0-3]):[0-5]\d$/.test(s)) {
      return { ok: false, message: '请输入正确时间（HH:mm）：' + q.title }
    }
    if (q._minLength && s.length < Number(q._minLength)) {
      return { ok: false, message: q.title + ' 至少 ' + q._minLength + ' 个字符' }
    }
    if (q._maxLength && s.length > Number(q._maxLength)) {
      return { ok: false, message: q.title + ' 最多 ' + q._maxLength + ' 个字符' }
    }
    if (q.qType === 'number') {
      const num = Number(s)
      if (Number.isNaN(num)) return { ok: false, message: '请输入有效数字：' + q.title }
      if (q._min != null && num < Number(q._min)) {
        return { ok: false, message: q.title + ' 不能小于 ' + q._min }
      }
      if (q._max != null && num > Number(q._max)) {
        return { ok: false, message: q.title + ' 不能大于 ' + q._max }
      }
    }
    if (q.qType === 'rate' || q.qType === 'nps' || q.qType === 'slider') {
      const rate = Number(s)
      if (Number.isNaN(rate)) return { ok: false, message: '分值格式错误：' + q.title }
      let min = q.qType === 'nps' ? 0 : 1
      let max = q.qType === 'nps' ? 10 : 5
      if (q.qType === 'slider') {
        min = q._min != null ? Number(q._min) : 0
        max = q._max != null ? Number(q._max) : 100
      } else if (q._max != null) {
        max = Number(q._max)
      }
      if (q.qType === 'rate' && q._min != null) min = Number(q._min)
      if (rate < min || rate > max) {
        return { ok: false, message: '分值超出范围：' + q.title }
      }
    }
  }
  return { ok: true }
}

function cascadePathValid(options, path) {
  let nodes = options || []
  for (let i = 0; i < path.length; i++) {
    const want = String(path[i])
    const hit = (nodes || []).find(n => String(n.value) === want)
    if (!hit) return false
    nodes = hit.children || []
  }
  return true
}

export function effectiveUploadMaxMb(q) {
  const n = Number(q && q._maxSizeMb)
  if (!Number.isFinite(n) || n <= 0) return Math.min(5, SURVEY_UPLOAD_HARD_MAX_MB)
  return Math.min(n, SURVEY_UPLOAD_HARD_MAX_MB)
}
