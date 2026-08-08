/** Survey themeJson helpers for H5 / open fill pages. */

export function defaultSurveyTheme() {
  return {
    color: '#1677ff',
    bg: 'linear-gradient(180deg, #f5f8ff 0%, #f7f7f7 280px, #f7f7f7 100%)',
    fillMode: 'all',
    pageBgType: 'theme',
    pageBgColor: '#f5f7fb',
    pageBgImage: '',
    pageBgOverlay: 40,
    titleAlign: 'center',
    titleSize: 'md',
    titleColor: '',
    showTitle: true,
    showDesc: true,
    posterBgType: 'theme',
    posterBgColor: '#eef2ff',
    posterBgImage: '',
    posterBgOverlay: 40,
    successTitle: '�ύ�ɹ�',
    successMsg: '��л���Ĳ���',
    successRedirectUrl: '',
    successRedirectSec: 0,
    showFillAgain: true,
    channels: []
  }
}

export function normalizeSurveyTheme(raw) {
  const t = Object.assign({}, defaultSurveyTheme(), raw || {})
  if (!['theme', 'color', 'image'].includes(t.pageBgType)) t.pageBgType = 'theme'
  if (!['center', 'left'].includes(t.titleAlign)) t.titleAlign = 'center'
  if (!['sm', 'md', 'lg'].includes(t.titleSize)) t.titleSize = 'md'
  if (t.showTitle === false || t.showTitle === '0') t.showTitle = false
  else t.showTitle = true
  if (t.showDesc === false || t.showDesc === '0') t.showDesc = false
  else t.showDesc = true
  if (t.showFillAgain === false || t.showFillAgain === '0') t.showFillAgain = false
  else t.showFillAgain = true
  t.successTitle = String(t.successTitle || '�ύ�ɹ�').slice(0, 40)
  t.successMsg = String(t.successMsg || '��л���Ĳ���').slice(0, 200)
  t.successRedirectUrl = String(t.successRedirectUrl || '').trim().slice(0, 500)
  const sec = Number(t.successRedirectSec)
  t.successRedirectSec = Number.isFinite(sec) ? Math.min(60, Math.max(0, Math.floor(sec))) : 0
  if (!Array.isArray(t.channels)) t.channels = []
  t.channels = t.channels
    .map(c => ({
      code: String((c && (c.code || c.name)) || '').trim().slice(0, 32),
      label: String((c && (c.label || c.name || c.code)) || '').trim().slice(0, 40)
    }))
    .filter(c => c.code)
  const ov = Number(t.pageBgOverlay)
  t.pageBgOverlay = Number.isNaN(ov) ? 40 : Math.min(90, Math.max(0, ov))
  return t
}

/** Append ?channel= / &channel= to a survey fill URL. */
export function withChannelParam(url, channelCode) {
  const base = String(url || '')
  const code = String(channelCode || '').trim()
  if (!base || !code) return base
  const sep = base.includes('?') ? '&' : '?'
  return base + sep + 'channel=' + encodeURIComponent(code)
}

export function resolveAssetUrl(path, apiBase) {
  if (!path) return ''
  let url = String(path).split(',')[0].trim()
  if (!url) return ''
  if (/^https?:\/\//i.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url
  url = url.replace(/^\/?(dev-api|prod-api)/, '')
  if (!url.startsWith('/')) url = '/' + url
  const base = (apiBase || '').replace(/\/$/, '')
  return base ? base + url : url
}

function themeGradient(color, fallbackBg) {
  if (fallbackBg) return fallbackBg
  const c = color || '#1677ff'
  return `linear-gradient(180deg, ${c}14 0%, #f7f7f7 280px, #f7f7f7 100%)`
}

/** Page shell inline style for survey fill. */
export function buildSurveyPageStyle(theme, apiBase) {
  const t = normalizeSurveyTheme(theme)
  const style = { '--theme': t.color || '#1677ff' }
  if (t.pageBgType === 'color') {
    style.background = t.pageBgColor || '#f5f7fb'
  } else if (t.pageBgType === 'image' && t.pageBgImage) {
    const url = resolveAssetUrl(t.pageBgImage, apiBase)
    const overlay = (Number(t.pageBgOverlay) || 40) / 100
    const a1 = (overlay * 0.45).toFixed(2)
    const a2 = Math.min(0.88, overlay + 0.18).toFixed(2)
    style.backgroundColor = '#f5f7fb'
    style.backgroundImage = `linear-gradient(180deg, rgba(255,255,255,${a1}), rgba(247,247,247,${a2})), url("${url}")`
    style.backgroundSize = 'cover'
    style.backgroundPosition = 'center top'
    style.backgroundRepeat = 'no-repeat'
    style.backgroundAttachment = 'fixed'
  } else {
    style.background = themeGradient(t.color, t.bg)
  }
  return style
}

export function buildSurveyHeroStyle(theme) {
  const t = normalizeSurveyTheme(theme)
  return {
    textAlign: t.titleAlign === 'left' ? 'left' : 'center'
  }
}

const TITLE_SIZES = { sm: '20px', md: '24px', lg: '30px' }

export function buildSurveyTitleStyle(theme) {
  const t = normalizeSurveyTheme(theme)
  return {
    fontSize: TITLE_SIZES[t.titleSize] || TITLE_SIZES.md,
    color: t.titleColor || 'var(--theme)',
    fontWeight: 700,
    letterSpacing: t.titleSize === 'lg' ? '0.04em' : '0.02em'
  }
}
