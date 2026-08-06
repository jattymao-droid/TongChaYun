import QRCode from 'qrcode'

export function toQrDataUrl(text, options = {}) {
  return QRCode.toDataURL(text, {
    width: options.width || 200,
    margin: 1,
    errorCorrectionLevel: 'M'
  })
}

/**
 * Build a shareable poster (PNG data URL) with title + QR.
 * Background: theme gradient (default) | solid color | image + overlay.
 */
export async function buildSharePoster(options = {}) {
  const title = String(options.title || '通查云 · 查询').slice(0, 28)
  const subtitle = String(options.subtitle || '扫码即可查询').slice(0, 40)
  const link = String(options.link || '')
  const theme = options.themeColor || '#1677ff'
  const brand = String(options.brand || '通查云').slice(0, 20)
  const bgType = options.bgType || 'theme'
  const bgColor = options.bgColor || '#eef2ff'
  const bgImage = options.bgImage || ''
  let bgOverlay = Number(options.bgOverlay)
  if (Number.isNaN(bgOverlay)) bgOverlay = 40
  bgOverlay = Math.min(90, Math.max(0, bgOverlay))
  const apiBase = options.apiBase || ''

  // Prefer high-res QR for poster; avoid upscaling a small preview QR
  const qrSize = 360
  let qr = null
  if (link) {
    qr = await toQrDataUrl(link, { width: qrSize * 2, margin: 2 })
  } else if (options.qrDataUrl) {
    qr = options.qrDataUrl
  }
  if (!qr) {
    throw new Error('缺少二维码')
  }

  const W = 750
  const H = 1100
  const canvas = document.createElement('canvas')
  canvas.width = W
  canvas.height = H
  const ctx = canvas.getContext('2d')

  await paintPosterBackground(ctx, W, H, {
    bgType,
    bgColor,
    bgImage,
    bgOverlay,
    theme,
    apiBase
  })

  // top brand bar
  ctx.fillStyle = theme
  fillRoundRect(ctx, 40, 40, W - 80, 10, 5)

  ctx.fillStyle = '#0f172a'
  ctx.font = '600 28px "PingFang SC","Microsoft YaHei",sans-serif'
  ctx.fillText(brand, 48, 100)

  ctx.fillStyle = '#0f172a'
  ctx.font = '700 48px "PingFang SC","Microsoft YaHei",sans-serif'
  wrapText(ctx, title, 48, 180, W - 96, 58)

  ctx.fillStyle = '#64748b'
  ctx.font = '400 28px "PingFang SC","Microsoft YaHei",sans-serif'
  wrapText(ctx, subtitle, 48, 320, W - 96, 40)

  // QR card — rounded white panel with padding and soft shadow
  const pad = 48
  const hintH = 56
  const cardW = qrSize + pad * 2
  const cardH = qrSize + pad + hintH + 28
  const cardX = (W - cardW) / 2
  const cardY = 400

  ctx.save()
  ctx.shadowColor = 'rgba(15, 23, 42, 0.14)'
  ctx.shadowBlur = 28
  ctx.shadowOffsetY = 12
  ctx.fillStyle = '#ffffff'
  fillRoundRect(ctx, cardX, cardY, cardW, cardH, 24)
  ctx.restore()

  ctx.strokeStyle = 'rgba(15, 23, 42, 0.06)'
  ctx.lineWidth = 1.5
  strokeRoundRect(ctx, cardX, cardY, cardW, cardH, 24)

  const qrImg = await loadImage(qr)
  const qrX = cardX + pad
  const qrY = cardY + pad
  // Source is 2x; high-quality downsample keeps modules sharp
  if (ctx.imageSmoothingQuality) ctx.imageSmoothingQuality = 'high'
  ctx.drawImage(qrImg, qrX, qrY, qrSize, qrSize)

  ctx.fillStyle = '#334155'
  ctx.font = '500 24px "PingFang SC","Microsoft YaHei",sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('微信 / 浏览器扫码打开', W / 2, qrY + qrSize + 40)
  ctx.textAlign = 'left'

  ctx.fillStyle = '#64748b'
  ctx.font = '400 22px "PingFang SC","Microsoft YaHei",sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('由通查云生成', W / 2, H - 48)
  ctx.textAlign = 'left'

  return canvas.toDataURL('image/png')
}

export function downloadDataUrl(dataUrl, filename = 'share-poster.png') {
  const a = document.createElement('a')
  a.href = dataUrl
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

/** Normalize poster bg fields from layoutJson or themeJson. */
export function resolvePosterBg(source = {}, themeColor) {
  const type = source.posterBgType || 'theme'
  return {
    bgType: ['theme', 'color', 'image'].includes(type) ? type : 'theme',
    bgColor: source.posterBgColor || '#eef2ff',
    bgImage: source.posterBgImage || '',
    bgOverlay: source.posterBgOverlay == null ? 40 : Number(source.posterBgOverlay),
    themeColor: themeColor || source.color || source.themeColor || '#1677ff'
  }
}

async function paintPosterBackground(ctx, W, H, opts) {
  const { bgType, bgColor, bgImage, bgOverlay, theme, apiBase } = opts
  if (bgType === 'color') {
    ctx.fillStyle = bgColor || '#eef2ff'
    ctx.fillRect(0, 0, W, H)
    return
  }
  if (bgType === 'image' && bgImage) {
    try {
      const url = resolvePosterAssetUrl(bgImage, apiBase)
      const img = await loadImage(url)
      const scale = Math.max(W / img.width, H / img.height)
      const dw = img.width * scale
      const dh = img.height * scale
      const dx = (W - dw) / 2
      const dy = (H - dh) / 2
      ctx.drawImage(img, dx, dy, dw, dh)
      const a = bgOverlay / 100
      ctx.fillStyle = `rgba(255,255,255,${Math.min(0.92, a * 0.85 + 0.15).toFixed(2)})`
      ctx.fillRect(0, 0, W, H)
      return
    } catch (e) {
      /* fall through to theme */
    }
  }
  const g = ctx.createLinearGradient(0, 0, W, H)
  g.addColorStop(0, mixHex(theme, '#ffffff', 0.82))
  g.addColorStop(0.45, '#f8fafc')
  g.addColorStop(1, '#eef2ff')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, W, H)
}

function resolvePosterAssetUrl(path, apiBase) {
  if (!path) return ''
  let url = String(path).split(',')[0].trim()
  if (!url) return ''
  if (/^https?:\/\//i.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url
  url = url.replace(/^\/?(dev-api|prod-api)/, '')
  if (!url.startsWith('/')) url = '/' + url
  const base = (apiBase || process.env.VUE_APP_BASE_API || '').replace(/\/$/, '')
  return base ? base + url : url
}

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => resolve(img)
    img.onerror = reject
    img.src = src
  })
}

function wrapText(ctx, text, x, y, maxWidth, lineHeight) {
  const chars = String(text || '').split('')
  let line = ''
  let cy = y
  for (let i = 0; i < chars.length; i++) {
    const test = line + chars[i]
    if (ctx.measureText(test).width > maxWidth && line) {
      ctx.fillText(line, x, cy)
      line = chars[i]
      cy += lineHeight
    } else {
      line = test
    }
  }
  if (line) ctx.fillText(line, x, cy)
}

function pathRoundRect(ctx, x, y, w, h, r) {
  const radius = Math.min(r, w / 2, h / 2)
  ctx.beginPath()
  if (typeof ctx.roundRect === 'function') {
    ctx.roundRect(x, y, w, h, radius)
    return
  }
  ctx.moveTo(x + radius, y)
  ctx.arcTo(x + w, y, x + w, y + h, radius)
  ctx.arcTo(x + w, y + h, x, y + h, radius)
  ctx.arcTo(x, y + h, x, y, radius)
  ctx.arcTo(x, y, x + w, y, radius)
  ctx.closePath()
}

function fillRoundRect(ctx, x, y, w, h, r) {
  pathRoundRect(ctx, x, y, w, h, r)
  ctx.fill()
}

function strokeRoundRect(ctx, x, y, w, h, r) {
  pathRoundRect(ctx, x, y, w, h, r)
  ctx.stroke()
}

function mixHex(a, b, t) {
  const pa = hexToRgb(a) || { r: 22, g: 119, b: 255 }
  const pb = hexToRgb(b) || { r: 255, g: 255, b: 255 }
  const r = Math.round(pa.r + (pb.r - pa.r) * t)
  const g = Math.round(pa.g + (pb.g - pa.g) * t)
  const bl = Math.round(pa.b + (pb.b - pa.b) * t)
  return `rgb(${r},${g},${bl})`
}

function hexToRgb(hex) {
  if (!hex) return null
  let h = String(hex).replace('#', '')
  if (h.length === 3) h = h.split('').map(c => c + c).join('')
  if (h.length !== 6) return null
  return {
    r: parseInt(h.slice(0, 2), 16),
    g: parseInt(h.slice(2, 4), 16),
    b: parseInt(h.slice(4, 6), 16)
  }
}
