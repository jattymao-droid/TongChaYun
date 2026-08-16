<template>
  <div
    class="page form-shell"
    :class="[{ 'anim-page': layout.formAnim }, 'align-' + layout.formAlign, { compact: layout.formCompact }]"
    :style="shellStyle"
  >
    <div v-if="layout.formShowAmbient" class="ambient" aria-hidden="true">
      <span class="orb orb-a" />
      <span class="orb orb-b" />
    </div>

    <div class="layout" :style="{ maxWidth: contentWidth + 'px' }">
      <header class="hero" :class="{ 'anim-fade-up': layout.formAnim }">
        <div class="hero-text">
          <p v-if="layout.formShowEyebrow" class="eyebrow">通查云 · 查询</p>
          <div class="title-row">
            <img v-if="logoSrc" :src="logoSrc" class="hero-logo" alt="" />
            <h1>{{ page.title || queryName || '数据查询' }}</h1>
          </div>
        </div>
        <div v-if="bannerSrc" class="banner-wrap" :class="{ 'anim-banner': layout.formAnim }">
          <img :src="bannerSrc" class="banner" alt="" />
        </div>
      </header>

      <main
        class="card"
        :class="['style-' + layout.formPanelStyle, { 'anim-pop anim-delay-1': layout.formAnim }]"
        v-if="needPwd && !unlocked"
      >
        <div class="panel-head">
          <h3>访问验证</h3>
          <p>请输入访问密码后继续</p>
        </div>
        <div class="field">
          <label>访问密码</label>
          <input v-model="accessPwd" type="password" placeholder="请输入访问密码" @keyup.enter="loadMeta" />
        </div>
        <div class="actions-bar single">
          <button class="btn primary anim-btn block" :disabled="metaLoading" @click="loadMeta">进入查询</button>
        </div>
        <p class="msg" v-if="error">{{ error }}</p>
      </main>

      <main
        class="card"
        :class="['style-' + layout.formPanelStyle, { 'anim-pop anim-delay-1': layout.formAnim }]"
        v-else
      >
        <p class="empty" v-if="metaLoading">加载中…</p>
        <template v-else-if="queryFields.length">
          <div class="panel-head row">
            <div>
              <h3>
                查询条件
                <span v-if="page.subtitle" class="head-sub">{{ page.subtitle }}</span>
              </h3>
              <p>{{ page.subtitle ? ('共 ' + queryFields.length + ' 项') : ('必填项需填写，选填可组合；至少填写一项（共 ' + queryFields.length + ' 项）') }}</p>
            </div>
            <span v-if="layout.formShowFillHint" class="fill-hint" :class="{ ready: requiredReady }">
              {{ filledCount ? ('已填 ' + filledCount + ' / ' + queryFields.length) : '请填写条件' }}
            </span>
          </div>

          <div class="fields" :class="[{ 'anim-stagger': layout.formAnim }, 'cols-' + fieldsCols]">
            <div
              class="field field-cell"
              v-for="f in queryFields"
              :key="f.fieldKey"
              :class="{ filled: isFilled(f), 'span-all': isWideField(f), invalid: invalidKeys.includes(f.fieldKey) }"
              :data-field-key="f.fieldKey"
              @input="clearFieldInvalid(f.fieldKey)"
              @change="clearFieldInvalid(f.fieldKey)"
            >
              <label>{{ f.fieldLabel || f.fieldName }} <span class="req" v-if="f.isRequired !== '0'">*</span><span class="opt" v-else>选填</span></label>
              <select
                v-if="f.htmlType === 'select' && String(f.queryType || '').toUpperCase() === 'IN'"
                v-model="form[f.fieldKey]"
                multiple
                class="multi"
              >
                <option v-for="opt in dictOf(f)" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
              <select v-else-if="f.htmlType === 'select'" v-model="form[f.fieldKey]">
                <option value="">请选择</option>
                <option v-for="opt in dictOf(f)" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
              <template v-else-if="f.htmlType === 'date' && String(f.queryType || '').toUpperCase() === 'BETWEEN'">
                <div class="range">
                  <input v-model="form[f.fieldKey][0]" type="date" />
                  <span>至</span>
                  <input v-model="form[f.fieldKey][1]" type="date" />
                </div>
              </template>
              <input v-else-if="f.htmlType === 'date'" v-model="form[f.fieldKey]" type="date" />
              <input
                v-else-if="String(f.queryType || '').toUpperCase() === 'IN'"
                v-model="form[f.fieldKey]"
                type="text"
                placeholder="多个值用逗号分隔"
                @keyup.enter="goResult"
              />
              <input
                v-else
                v-model="form[f.fieldKey]"
                type="text"
                :placeholder="'请输入' + (f.fieldLabel || f.fieldName)"
                @keyup.enter="goResult"
              />
            </div>
          </div>

          <div v-if="needCaptcha" class="captcha-field">
            <div class="captcha-head">
              <label>验证码 <span class="req">*</span></label>
              <button type="button" class="captcha-refresh-btn" @click="refreshCaptcha">↻ 换一张</button>
            </div>
            <div class="captcha-row">
              <input
                v-model="captchaCode"
                class="captcha-input"
                type="text"
                maxlength="6"
                inputmode="numeric"
                placeholder="请输入计算结果"
                @keyup.enter="goResult"
              />
              <button type="button" class="captcha-media" title="点击刷新" @click="refreshCaptcha">
                <img v-if="captchaUrl" :src="captchaUrl" alt="验证码" draggable="false" />
                <span v-else class="captcha-loading">加载中</span>
              </button>
            </div>
            <p class="captcha-hint">看不清可点击图片或「换一张」刷新</p>
          </div>
          <div class="actions-bar" :class="[{ 'anim-fade-up anim-delay-2': layout.formAnim }, { block: layout.formBtnBlock }, { shake: actionsShake }]">
            <button class="btn anim-btn" @click="reset">重置</button>
            <button class="btn primary anim-btn primary-btn" :class="{ 'is-leaving': leaving }" @click="goResult">查询</button>
          </div>
        </template>
        <div v-else class="empty-box">
          <div class="empty-ico">?</div>
          <p>{{ error || '查询配置不完整或未发布' }}</p>
        </div>
        <p class="msg" v-if="error && unlocked && queryFields.length">{{ error }}</p>
      </main>

      <aside
        v-if="showNotice"
        class="notice-box"
        :class="[
          'style-' + layout.formNoticeStyle,
          'align-' + layout.formNoticeAlign,
          { 'anim-fade-up anim-delay-2': layout.formAnim && layout.formNoticeAnim }
        ]"
      >
        <div v-if="layout.formNoticeTitle" class="notice-title">{{ layout.formNoticeTitle }}</div>
        <div class="notice-body" v-html="noticeHtml" />
      </aside>
    </div>

    <footer class="site-footer">
      <a
        class="site-link"
        :href="siteLink"
        :target="siteLinkExternal ? '_blank' : '_self'"
        :rel="siteLinkExternal ? 'noopener noreferrer' : null"
      >{{ siteName }}</a>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { queryMeta } from '@/api/open'
import {
  parseDictOptions,
  normalizeQueryParams,
  hasAllQueryParams,
  hasAnyQueryParam,
  missingQueryFields,
  paramsToRouteQuery,
  routeQueryToParams,
  saveQueryParams,
  loadQueryParams,
  pwdStorageKey,
  bannerUrl,
  parseLayout,
  buildFormPageStyle,
  resolveFormWidth,
  resolveFormColumns,
  resolveAssetUrl,
  noticeBoxVisible,
  formatNoticeHtml
} from '@/utils/bizQueryField'
import { getCaptchaImage } from '@/api/open'

const route = useRoute()
const router = useRouter()
const code = computed(() => route.params.code)
const metaLoading = ref(false)
const needPwd = ref(false)
const unlocked = ref(false)
const accessPwd = ref('')
const queryName = ref('')
const page = ref({})
const layout = ref(parseLayout(null))
const queryFields = ref([])
const form = reactive({})
const error = ref('')
const leaving = ref(false)
const invalidKeys = ref([])
const actionsShake = ref(false)
const needCaptcha = ref(false)
const captchaCode = ref('')
const captchaUuid = ref('')
const captchaUrl = ref('')
const apiBase = import.meta.env.VITE_APP_BASE_API || '/dev-api'
const siteName = import.meta.env.VITE_SITE_NAME || '通查云'
const siteLink = import.meta.env.VITE_SITE_URL || '/login'
const siteLinkExternal = /^https?:\/\//i.test(siteLink)

const themeColor = computed(() => page.value.themeColor || '#1677ff')
const shellStyle = computed(() => buildFormPageStyle(layout.value, themeColor.value))
const bannerSrc = computed(() => bannerUrl(page.value, apiBase))
const logoSrc = computed(() => {
  if (!layout.value.showLogo) return ''
  return resolveAssetUrl(layout.value.logoUrl, apiBase)
})
const filledCount = computed(() => Object.keys(normalizeQueryParams(form, queryFields.value)).length)
const requiredReady = computed(() => hasAllQueryParams(form, queryFields.value))
const contentWidth = computed(() => {
  if (needPwd.value && !unlocked.value) return 480
  return resolveFormWidth(layout.value, queryFields.value.length)
})
const fieldsCols = computed(() => resolveFormColumns(layout.value, queryFields.value.length))
const showNotice = computed(() => noticeBoxVisible(layout.value))
const noticeHtml = computed(() => formatNoticeHtml(layout.value.formNoticeText))

function emptyFormValue(f) {
  const op = String((f && f.queryType) || '').toUpperCase()
  if (op === 'BETWEEN') return ['', '']
  if (op === 'IN' && f && f.htmlType === 'select') return []
  return ''
}

function dictOf(f) {
  return parseDictOptions(f && f.dictOptions)
}
function isWideField(f) {
  return f && f.htmlType === 'date' && String(f.queryType || '').toUpperCase() === 'BETWEEN'
}
function isFilled(f) {
  return !!normalizeQueryParams(form, [f])[f.fieldKey]
}

function clearFieldInvalid(key) {
  if (!invalidKeys.value.length) return
  invalidKeys.value = invalidKeys.value.filter(k => k !== key)
}

async function loadMeta() {
  error.value = ''
  metaLoading.value = true
  try {
    const res = await queryMeta(code.value, accessPwd.value || undefined)
    const data = res.data || {}
    needPwd.value = !!data.needPwd
    unlocked.value = !!data.unlocked || !data.needPwd
    document.title = data.queryName || '数据查询'
    if (!unlocked.value) return
    if (accessPwd.value) {
      sessionStorage.setItem(pwdStorageKey(code.value), accessPwd.value)
    }
    queryName.value = data.queryName || ''
    page.value = data.page || {}
    layout.value = parseLayout(page.value)
    queryFields.value = data.queryFields || []
    Object.keys(form).forEach((k) => delete form[k])
    const savedParams = loadQueryParams(code.value)
    const fromRoute = routeQueryToParams(route.query, queryFields.value)
    queryFields.value.forEach((f) => {
      if (savedParams[f.fieldKey] != null && savedParams[f.fieldKey] !== '') {
        form[f.fieldKey] = savedParams[f.fieldKey]
      } else if (fromRoute[f.fieldKey] != null) {
        form[f.fieldKey] = fromRoute[f.fieldKey]
      } else {
        form[f.fieldKey] = emptyFormValue(f)
      }
    })
    needCaptcha.value = !!data.needCaptcha
    if (needCaptcha.value) refreshCaptcha()
    document.title = page.value.title || queryName.value || '数据查询'
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    metaLoading.value = false
  }
}

function reset() {
  queryFields.value.forEach((f) => {
    form[f.fieldKey] = emptyFormValue(f)
  })
  error.value = ''
  invalidKeys.value = []
}

function markInvalid(keys) {
  invalidKeys.value = keys || []
  actionsShake.value = true
  setTimeout(() => { actionsShake.value = false }, 450)
  const first = (keys || [])[0]
  if (first) {
    const el = document.querySelector('[data-field-key="' + first + '"]')
    if (el && el.scrollIntoView) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

async function refreshCaptcha() {
  try {
    const res = await getCaptchaImage()
    const data = res.data || res
    captchaUuid.value = data.uuid || ''
    captchaUrl.value = data.img ? ('data:image/png;base64,' + data.img) : ''
    captchaCode.value = ''
  } catch (e) {
    captchaUrl.value = ''
  }
}

function goResult() {
  error.value = ''
  invalidKeys.value = []
  const params = normalizeQueryParams(form, queryFields.value)
  const missing = missingQueryFields(form, queryFields.value)
  if (missing.length) {
    error.value = '请填写必填查询条件：' + missing.map(f => f.fieldLabel || f.fieldName || f.fieldKey).join('、')
    markInvalid(missing.map(f => f.fieldKey))
    return
  }
  if (!hasAnyQueryParam(params)) {
    error.value = '请至少填写一项查询条件'
    markInvalid(queryFields.value.map(f => f.fieldKey))
    return
  }
  if (needCaptcha.value && !captchaCode.value) {
    error.value = '请输入验证码'
    return
  }
  if (accessPwd.value) {
    sessionStorage.setItem(pwdStorageKey(code.value), accessPwd.value)
  }
  saveQueryParams(code.value, params)
  try {
    sessionStorage.setItem('biz_q_captcha_' + code.value, JSON.stringify({
      code: captchaCode.value || '',
      uuid: captchaUuid.value || ''
    }))
  } catch (e) {}
  leaving.value = true
  setTimeout(() => {
    router.push({
      name: 'query-result',
      params: { code: code.value },
      query: paramsToRouteQuery(params, 1)
    })
  }, 220)
}

onMounted(() => {
  const saved = sessionStorage.getItem(pwdStorageKey(code.value))
  if (saved) accessPwd.value = saved
  loadMeta()
})
</script>

<style scoped>
.form-shell {
  position: relative;
  overflow: visible;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.site-footer {
  flex-shrink: 0;
  text-align: center;
  padding: 28px 16px 24px;
}
.site-link {
  display: inline-block;
  font-size: 12px;
  letter-spacing: .04em;
  color: #94a3b8;
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: color .15s, border-color .15s;
}
.site-link:active,
.site-link:hover {
  color: #64748b;
  border-bottom-color: #cbd5e1;
}
.align-left .hero { text-align: left; }
.align-center .hero { text-align: center; }
.ambient { pointer-events: none; position: absolute; inset: 0; overflow: hidden; }
.orb {
  position: absolute; border-radius: 50%; filter: blur(36px); opacity: .38;
  animation: orbFloat 12s ease-in-out infinite alternate;
}
.orb-a { width: 200px; height: 200px; left: -40px; top: 90px; background: color-mix(in srgb, var(--theme) 35%, #fff); }
.orb-b { width: 160px; height: 160px; right: -30px; top: 240px; background: color-mix(in srgb, var(--theme) 20%, #cbd5e1); animation-delay: -4s; }
@keyframes orbFloat {
  from { transform: translateY(0); }
  to { transform: translateY(16px) scale(1.05); }
}

.layout {
  position: relative;
  flex: 1;
  margin: 0 auto;
  width: 100%;
}

.hero { margin: 0 0 14px; }
.banner-wrap {
  overflow: hidden; border-radius: 14px; margin-top: 12px; margin-bottom: 0;
  box-shadow: 0 10px 28px rgba(15,23,42,.1);
}
.banner { display: block; width: 100%; max-height: 140px; object-fit: cover; margin: 0; border-radius: 0; }
.eyebrow {
  margin: 0 0 6px; font-size: 11px; letter-spacing: .1em; text-transform: uppercase;
  color: color-mix(in srgb, var(--theme) 70%, #64748b); font-weight: 600;
}
.hero h1 { margin: 0; letter-spacing: -.02em; font-weight: 700; line-height: 1.25; }
.title-row {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  max-width: 100%;
}
.align-left .title-row { justify-content: flex-start; }
.hero-logo {
  width: 42px;
  height: 42px;
  object-fit: contain;
  border-radius: 9px;
  flex-shrink: 0;
  background: #fff;
  box-shadow: 0 4px 12px rgba(15,23,42,.08);
}
.panel-head h3 {
  margin: 0 0 4px; font-size: 15px; color: #0f172a;
  display: flex; flex-wrap: wrap; align-items: baseline; gap: 6px;
}
.panel-head .head-sub { font-size: 12px; font-weight: 400; color: #64748b; }

.card {
  max-width: none !important;
  margin: 0 auto 14px !important;
  width: 100%;
  border-radius: 16px;
  padding: 16px;
}
.card.style-card { background: #fff; box-shadow: 0 10px 32px rgba(15,23,42,.06); border: 1px solid transparent; }
.card.style-flat { background: #fff; border: 1px solid #e5e7eb; }
.card.style-glass {
  position: relative;
  background: rgba(255,255,255,.88);
  border: 1px solid rgba(255,255,255,.8);
  backdrop-filter: blur(14px);
  box-shadow: 0 10px 32px rgba(15,23,42,.06);
}

.panel-head { margin-bottom: 14px; }
.panel-head.row {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 10px;
}
.panel-head h3 { margin: 0 0 4px; font-size: 15px; color: #0f172a; }
.panel-head p { margin: 0; font-size: 12px; color: #64748b; }
.fill-hint {
  flex-shrink: 0; font-size: 12px; color: #94a3b8;
  padding: 4px 10px; border-radius: 999px; background: #f1f5f9;
}
.fill-hint.ready {
  color: var(--theme); background: color-mix(in srgb, var(--theme) 12%, #fff); font-weight: 600;
}

.fields {
  display: grid;
  gap: 2px 14px;
}
.fields.cols-1 { grid-template-columns: 1fr; }
.fields.cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.fields.cols-3 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.field-cell.span-all { grid-column: 1 / -1; }
.field { margin-bottom: 12px; }
.compact .field { margin-bottom: 8px; }
.field.filled label { color: var(--theme); }
.field label { display: block; font-weight: 600; margin-bottom: 6px; font-size: 13px; color: #334155; }
.field .req { color: #ef4444; margin-left: 2px; }
.field .opt { color: #94a3b8; font-weight: 500; font-size: 12px; margin-left: 4px; }
.range { display: grid; grid-template-columns: 1fr auto 1fr; gap: 8px; align-items: center; }

.actions-bar {
  display: flex; align-items: center; justify-content: flex-end; gap: 10px;
  margin-top: 6px; padding-top: 14px; border-top: 1px solid #eef2f7;
}
.actions-bar.block { flex-wrap: wrap; }
.actions-bar.block .primary-btn { flex: 1; min-width: 120px; }
.actions-bar.single { justify-content: stretch; border-top: 0; padding-top: 4px; }
.btn.block { width: 100%; }
.primary-btn { min-width: 108px; }
.btn.primary.is-leaving {
  background: linear-gradient(90deg, var(--theme), color-mix(in srgb, var(--theme) 70%, #fff), var(--theme));
  background-size: 200% 100%;
}

.empty-box { text-align: center; padding: 28px 8px; color: #94a3b8; }
.empty-ico {
  width: 44px; height: 44px; margin: 0 auto 10px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--theme) 12%, #fff); color: var(--theme); font-weight: 700;
}
.notice-box {
  margin: 2px 0 4px;
  padding: 12px 14px;
  border-radius: 12px;
  line-height: 1.55;
  font-size: 13px;
  color: #334155;
}
.notice-box.align-center { text-align: center; }
.notice-box.align-left { text-align: left; }
.notice-title {
  font-size: 13px;
  font-weight: 700;
  margin: 0 0 6px;
  color: #0f172a;
}
.notice-body { word-break: break-word; }
.notice-box.style-info {
  background: color-mix(in srgb, var(--theme, #1677ff) 8%, #fff);
  border: 1px solid color-mix(in srgb, var(--theme, #1677ff) 22%, #e2e8f0);
  border-left: 3px solid var(--theme, #1677ff);
}
.notice-box.style-tip {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-left: 3px solid #16a34a;
}
.notice-box.style-warn {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-left: 3px solid #d97706;
}
.notice-box.style-soft {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.notice-box.style-quote {
  background: transparent;
  border: 0;
  border-left: 3px solid #94a3b8;
  border-radius: 0;
  padding-left: 12px;
  color: #475569;
  font-style: italic;
}
.notice-box.style-plain {
  background: transparent;
  border: 0;
  padding: 4px 2px;
  color: #64748b;
}

@media (max-width: 640px) {
  .fields.cols-2,
  .fields.cols-3 { grid-template-columns: 1fr; }
  .panel-head.row { flex-direction: column; }
  .fill-hint { align-self: flex-start; }
  .range { grid-template-columns: 1fr; }
  .range > span { text-align: center; color: #94a3b8; font-size: 12px; }
  .actions-bar {
    position: sticky;
    bottom: calc(8px + env(safe-area-inset-bottom, 0px));
    z-index: 2;
    margin-top: 12px; padding: 10px;
    border: 1px solid #eef2f7; border-radius: 12px;
    background: rgba(255,255,255,.96); backdrop-filter: blur(8px);
  }
  .primary-btn { flex: 1; }
}
@media (min-width: 641px) and (max-width: 900px) {
  .fields.cols-3 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (prefers-reduced-motion: reduce) {
  .orb { animation: none; }
}
select.multi { min-height: 88px; }

.field.invalid label { color: #ef4444; }
.field.invalid input,
.field.invalid select,
.field.invalid .range input {
  border-color: #ef4444 !important;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, .15);
}
.actions-bar.shake { animation: queryShake .45s ease; }
@keyframes queryShake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}

.captcha-field {
  margin: 4px 0 14px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid color-mix(in srgb, var(--theme, #1677ff) 18%, #e5e7eb);
  background: linear-gradient(180deg, color-mix(in srgb, var(--theme, #1677ff) 6%, #fff) 0%, #fff 100%);
}
.captcha-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.captcha-head > label {
  margin: 0;
  font-weight: 650;
  font-size: 14px;
}
.captcha-refresh-btn {
  border: 0;
  background: transparent;
  color: var(--theme, #1677ff);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 2px;
}
.captcha-row {
  display: flex;
  align-items: stretch;
  gap: 10px;
}
.captcha-media {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 132px;
  height: 48px;
  padding: 4px 8px;
  border: 1px dashed color-mix(in srgb, var(--theme, #1677ff) 28%, #e5e7eb);
  border-radius: 12px;
  background: repeating-linear-gradient(-12deg, #f8fafc, #f8fafc 8px, #f1f5f9 8px, #f1f5f9 16px);
  cursor: pointer;
  overflow: hidden;
}
.captcha-media img {
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
  background: #fff;
  border-radius: 4px;
}
.captcha-loading { font-size: 12px; color: #94a3b8; }
.captcha-input {
  flex: 1;
  min-width: 0;
  height: 48px;
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 12px;
  padding: 0 14px;
  font-size: 16px;
  letter-spacing: 0.06em;
}
.captcha-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #94a3b8;
}
.btn.ghost { background: transparent; border: 1px solid rgba(15,23,42,.1); }
</style>
