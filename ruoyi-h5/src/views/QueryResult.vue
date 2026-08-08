<template>
  <div
    class="page result-shell"
    :class="[{ 'anim-page': layout.resultAnim }, 'panel-' + layout.resultPanelStyle, 'tone-' + layout.resultTextTone, { dense: layout.resultDense }, 'style-' + resultStyle]"
    :style="pageStyle"
  >
    <div class="hero" :class="{ 'anim-fade-up': layout.resultAnim }" :style="{ maxWidth: layout.resultMaxWidth + 'px' }">
      <p v-if="layout.resultShowEyebrow" class="eyebrow">查询结果</p>
      <h1>{{ resultTitle }}</h1>
      <p v-if="layout.resultShowTotal" class="hero-sub" :class="{ 'anim-fade-up anim-delay-1': layout.resultAnim }">
        <span class="total-pill">{{ rangeText }}</span>
      </p>
      <div v-if="layout.resultShowConditions && conditionTags.length" class="cond-tags" :class="{ 'anim-fade-up anim-delay-1': layout.resultAnim }">
        <button
          class="cond-chip clickable"
          type="button"
          v-for="t in conditionTags"
          :key="t.key"
          @click="goBack"
          :title="'点击返回修改'"
        >{{ t.label }}：{{ t.value }}</button>
      </div>
      <transition name="toast">
        <div v-if="toast" class="toast">{{ toast }}</div>
      </transition>
    </div>

    <div class="card" :class="{ 'anim-pop anim-delay-1': layout.resultAnim }" v-if="needPwd && !unlocked" :style="{ maxWidth: layout.resultMaxWidth + 'px' }">
      <div class="panel-head">
        <h3>访问验证</h3>
        <p>请输入访问密码后查看结果</p>
      </div>
      <div class="field">
        <label>访问密码</label>
        <input v-model="accessPwd" type="password" placeholder="请输入访问密码" @keyup.enter="unlock" />
      </div>
      <div class="actions">
        <button class="btn primary anim-btn" :disabled="metaLoading" @click="unlock">进入结果</button>
      </div>
      <p class="msg" v-if="error">{{ error }}</p>
    </div>

    <template v-else>
      <div class="card result-panel" :class="{ 'anim-pop anim-delay-1': layout.resultAnim }" :style="{ maxWidth: layout.resultMaxWidth + 'px' }">
        <div class="toolbar no-print" :class="{ 'anim-fade-up': layout.resultAnim }">
          <div class="toolbar-left">
            <button class="btn anim-btn" @click="goBack">返回修改</button>
            <button
              v-if="layout.resultLayout === 'auto'"
              class="btn anim-btn ghost"
              @click="forceLayout = forceLayout === 'card' ? 'table' : (forceLayout === 'table' ? 'card' : (useCardLayout ? 'table' : 'card'))"
            >{{ useCardLayout ? '表格视图' : '卡片视图' }}</button>
          </div>
          <div class="toolbar-right no-print">
            <button v-if="layout.resultShowPrint !== false" class="btn anim-btn" :disabled="!rows.length" @click="doPrint">打印</button>
            <button v-if="layout.resultShowExport" class="btn anim-btn primary-soft" :disabled="exportingPdf || !rows.length" @click="doExportPdf">
              {{ exportingPdf ? 'PDF…' : '导出 PDF' }}
            </button>
            <button v-if="layout.resultShowExport" class="btn anim-btn primary-soft" :disabled="exporting || !rows.length" @click="doExport">
              {{ exporting ? '导出中…' : '导出 Excel' }}
            </button>
          </div>
        </div>

        <div v-if="layout.resultShowChart && chartFields.length" class="chart-box no-print" :class="{ 'anim-fade-up': layout.resultAnim }">
          <div class="chart-head">
            <span>字段分布</span>
            <select v-model="distField" @change="loadDist">
              <option v-for="f in chartFields" :key="f.fieldKey" :value="f.fieldKey">{{ f.fieldLabel || f.fieldName }}</option>
            </select>
          </div>
          <p class="tip" v-if="distLoading">加载中…</p>
          <div v-else class="bars">
            <div class="bar-row" v-for="item in distList" :key="String(item.value)">
              <span class="bar-label">{{ item.value }}</span>
              <div class="bar-track"><div class="bar-fill" :style="{ width: barWidth(item) }" /></div>
              <span class="bar-count">{{ item.count }}</span>
            </div>
            <p class="tip" v-if="!distList.length">暂无分布数据</p>
          </div>
        </div>

        <p class="msg" v-if="error">{{ error }}</p>

        <div v-if="loading" class="skeleton-wrap">
          <div class="sk-card" v-for="n in 3" :key="n">
            <div class="sk-line" style="width:36%" />
            <div class="sk-line" :style="{ width: (92 - n * 10) + '%' }" />
            <div class="sk-line" :style="{ width: (78 - n * 8) + '%' }" />
          </div>
        </div>

        <template v-else>
          <TransitionGroup
            v-if="rows.length && useCardLayout"
            name="list"
            tag="div"
            class="card-list"
            :class="['cols-' + layout.resultCardColumns, { solo: total === 1 }]"
          >
            <article class="result-card" v-for="(row, i) in rows" :key="pageNum + '-' + i">
              <header class="card-head" v-if="primaryField">
                <div class="card-head-top">
                  <span class="card-serial" v-if="layout.showSerial">#{{ serialOf(i) }}</span>
                  <button type="button" class="copy-btn" @click="copyRow(row)">复制</button>
                </div>
                <div class="card-title" :class="{ hit: isHit(primaryField.fieldKey, row[primaryField.fieldKey]) }">{{ cell(row, primaryField) }}</div>
                <div class="card-sub">{{ primaryField.fieldLabel || primaryField.fieldName }}</div>
                <div v-if="summaryFields.length" class="summary-strip">
                  <div class="sum-item" v-for="sf in summaryFields" :key="sf.fieldKey">
                    <span class="sum-k">{{ sf.fieldLabel || sf.fieldName }}</span>
                    <span class="sum-v" :class="{ hit: isHit(sf.fieldKey, row[sf.fieldKey]) }">{{ cell(row, sf) }}</span>
                  </div>
                </div>
              </header>
              <div v-for="g in bodyGroups" :key="g.group || '_'" class="field-group">
                <div v-if="g.group && bodyGroups.length > 1" class="group-label">{{ g.group }}</div>
                <div
                  class="card-row"
                  v-for="f in g.fields"
                  :key="f.fieldKey"
                  :class="{ hit: isHit(f.fieldKey, row[f.fieldKey]) }"
                >
                  <span class="k">{{ g.group && bodyGroups.length > 1 ? f.shortLabel : (f.fieldLabel || f.fieldName) }}</span>
                  <span class="v" :class="{ muted: isEmpty(row[f.fieldKey]) }">{{ cell(row, f) }}</span>
                </div>
              </div>
            </article>
          </TransitionGroup>

          <div class="table-wrap" :class="{ 'anim-fade-up': layout.resultAnim }" v-else-if="rows.length">
            <table>
              <thead>
                <tr>
                  <th v-if="layout.showSerial" class="col-serial">序号</th>
                  <th v-for="f in listFields" :key="f.fieldKey">{{ f.fieldLabel || f.fieldName }}</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, i) in rows"
                  :key="i"
                  :class="{ 'anim-fade-up': layout.resultAnim }"
                  :style="layout.resultAnim ? { animationDelay: (i * 0.04) + 's' } : undefined"
                >
                  <td v-if="layout.showSerial" class="col-serial">{{ serialOf(i) }}</td>
                  <td
                    v-for="f in listFields"
                    :key="f.fieldKey"
                    :class="{ muted: isEmpty(row[f.fieldKey]), hit: isHit(f.fieldKey, row[f.fieldKey]) }"
                  >{{ cell(row, f) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else class="empty-box" :class="{ 'anim-fade-up': layout.resultAnim }">
            <div v-if="layout.resultShowEmptyIcon" class="empty-ico">∅</div>
            <p>{{ page.resultTips || '未查询到相关数据' }}</p>
            <p v-if="emptyGuide" class="empty-guide">{{ emptyGuide }}</p>
            <div v-if="conditionTags.length" class="empty-conds">
              <span v-for="t in conditionTags" :key="t.key">{{ t.label }}={{ t.value }}</span>
            </div>
            <button class="btn primary anim-btn" @click="goBack">返回修改条件</button>
          </div>

          <div class="pager" :class="{ 'anim-fade-up anim-delay-2': layout.resultAnim }" v-if="total > pageSize">
            <button class="btn anim-btn" :disabled="pageNum <= 1" @click="prevPage">上一页</button>
            <span class="page-info">{{ pageNum }} / {{ totalPages }}</span>
            <button class="btn anim-btn" :disabled="pageNum * pageSize >= total" @click="nextPage">下一页</button>
            <label class="jump" v-if="totalPages > 3">
              跳至
              <input v-model.number="jumpPage" type="number" min="1" :max="totalPages" @keyup.enter="doJump" />
              <button type="button" class="btn ghost mini" @click="doJump">Go</button>
            </label>
          </div>
        </template>
      </div>
    </template>

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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { queryMeta, querySearch, queryExport, queryExportPdf, queryFieldDist } from '@/api/open'
import {
  parseLayout,
  buildResultPageStyle,
  routeQueryToParams,
  paramsToRouteQuery,
  hasAllQueryParams,
  missingQueryFields,
  summarizeConditions,
  pwdStorageKey,
  saveQueryParams,
  loadQueryParams,
  displayCell,
  groupListFields,
  primaryListField,
  pageRangeText,
  isConditionHit,
  formatRowPlainText,
  copyText
} from '@/utils/bizQueryField'

const route = useRoute()
const router = useRouter()
const code = computed(() => route.params.code)
const metaLoading = ref(false)
const loading = ref(false)
const exporting = ref(false)
const exportingPdf = ref(false)
const needPwd = ref(false)
const unlocked = ref(false)
const accessPwd = ref('')
const queryName = ref('')
const page = ref({})
const layout = ref(parseLayout(null))
const queryFields = ref([])
const listFields = ref([])
const distField = ref('')
const distLoading = ref(false)
const distList = ref([])
const rows = ref([])
const total = ref(0)
const pageSize = ref(10)
const error = ref('')
const forceLayout = ref('')
const toast = ref('')
const jumpPage = ref(1)
let toastTimer = null
const isMobile = ref(typeof window !== 'undefined' && window.innerWidth <= 768)
const siteName = import.meta.env.VITE_SITE_NAME || '通查云'
const siteLink = import.meta.env.VITE_SITE_URL || '/login'
const siteLinkExternal = /^https?:\/\//i.test(siteLink)
const apiBase = import.meta.env.VITE_APP_BASE_API || '/dev-api'

const themeColor = computed(() => page.value.themeColor || '#1677ff')
const pageStyle = computed(() => buildResultPageStyle(layout.value, themeColor.value, apiBase))
const resultTitle = computed(() => layout.value.resultTitle || ((page.value.title || queryName.value || '查询') + ' · 结果'))
const pageNum = computed(() => Math.max(1, parseInt(route.query.page || '1', 10) || 1))
const totalPages = computed(() => Math.max(1, Math.ceil((total.value || 0) / (pageSize.value || 10))))
const rangeText = computed(() => pageRangeText(pageNum.value, pageSize.value, total.value))
const useCardLayout = computed(() => {
  if (forceLayout.value === 'card') return true
  if (forceLayout.value === 'table') return false
  if (layout.value.resultLayout === 'card') return true
  if (layout.value.resultLayout === 'table') return false
  return isMobile.value
})
const searchParams = computed(() => {
  const stored = loadQueryParams(code.value)
  if (stored && Object.keys(stored).length) return stored
  return routeQueryToParams(route.query, queryFields.value)
})
const conditionTags = computed(() => summarizeConditions(searchParams.value, queryFields.value))
const primaryField = computed(() => {
  const key = layout.value && layout.value.resultTitleField
  if (key) {
    const hit = (listFields.value || []).find(f => f.fieldKey === key)
    if (hit) return hit
  }
  return primaryListField(listFields.value)
})
const summaryFields = computed(() => {
  const keys = (layout.value && layout.value.resultSummaryFields) || []
  if (!keys.length) return []
  const map = new Map((listFields.value || []).map(f => [f.fieldKey, f]))
  return keys.map(k => map.get(k)).filter(Boolean)
})
const resultStyle = computed(() => (layout.value && layout.value.resultStyle) || 'default')
const emptyGuide = computed(() => (layout.value && layout.value.resultEmptyGuide) || '')
const fieldGroups = computed(() => groupListFields(listFields.value))
const bodyGroups = computed(() => {
  const pf = primaryField.value
  const skip = new Set()
  if (pf) skip.add(pf.fieldKey)
  ;(summaryFields.value || []).forEach(f => skip.add(f.fieldKey))
  return fieldGroups.value
    .map((g) => ({
      group: g.group,
      fields: g.fields.filter((f) => !skip.has(f.fieldKey))
    }))
    .filter((g) => g.fields.length)
})

const chartFields = computed(() => {
  const all = []
  const seen = new Set()
  ;[...(listFields.value || []), ...(queryFields.value || [])].forEach((f) => {
    if (f && f.fieldKey && !seen.has(f.fieldKey)) {
      seen.add(f.fieldKey)
      all.push(f)
    }
  })
  return all
})

const distMax = computed(() => Math.max(1, ...distList.value.map((i) => Number(i.count) || 0)))

function cell(row, f) {
  return displayCell(row && f ? row[f.fieldKey] : '')
}
function isEmpty(v) {
  return v == null || String(v).trim() === ''
}
function isHit(fieldKey, value) {
  return isConditionHit(fieldKey, value, searchParams.value, queryFields.value)
}
function showToast(msg) {
  toast.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 1600)
}
async function copyRow(row) {
  try {
    await copyText(formatRowPlainText(row, listFields.value))
    showToast('已复制本条结果')
  } catch (e) {
    showToast('复制失败')
  }
}
function serialOf(i) {
  return (pageNum.value - 1) * pageSize.value + i + 1
}
function scrollTop() {
  if (typeof window !== 'undefined') window.scrollTo({ top: 0, behavior: 'smooth' })
}
function doJump() {
  const n = Math.max(1, Math.min(totalPages.value, Number(jumpPage.value) || 1))
  gotoPage(n)
}
function goBack() {
  const q = { ...route.query }
  delete q.page
  router.push({ name: 'query', params: { code: code.value }, query: q })
}

async function loadMeta() {
  error.value = ''
  metaLoading.value = true
  try {
    const res = await queryMeta(code.value, accessPwd.value || undefined)
    const data = res.data || {}
    needPwd.value = !!data.needPwd
    unlocked.value = !!data.unlocked || !data.needPwd
    if (!unlocked.value) {
      document.title = data.queryName || '查询结果'
      return
    }
    if (accessPwd.value) sessionStorage.setItem(pwdStorageKey(code.value), accessPwd.value)
    queryName.value = data.queryName || ''
    page.value = data.page || {}
    layout.value = parseLayout(page.value)
    pageSize.value = layout.value.resultPageSize || 10
    queryFields.value = data.queryFields || []
    listFields.value = data.listFields || []
    const def = layout.value.resultChartDefaultField
    distField.value = (def && chartFields.value.some((f) => f.fieldKey === def))
      ? def
      : (chartFields.value[0] && chartFields.value[0].fieldKey) || ''
    document.title = resultTitle.value
    await runSearch()
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    metaLoading.value = false
  }
}

async function unlock() {
  if (!accessPwd.value) {
    error.value = '请输入访问密码'
    return
  }
  await loadMeta()
}

async function runSearch() {
  error.value = ''
  const params = searchParams.value
  const missing = missingQueryFields(params, queryFields.value)
  if (missing.length || !hasAllQueryParams(params, queryFields.value)) {
    error.value = missing.length
      ? ('请填写必填查询条件：' + missing.map(f => f.fieldLabel || f.fieldName || f.fieldKey).join('、'))
      : '请至少填写一项查询条件，请返回重新填写'
    rows.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    let captchaCode
    let captchaUuid
    try {
      const raw = sessionStorage.getItem('biz_q_captcha_' + code.value)
      if (raw) {
        const c = JSON.parse(raw)
        captchaCode = c.code
        captchaUuid = c.uuid
        sessionStorage.removeItem('biz_q_captcha_' + code.value)
      }
    } catch (e) {}
    const res = await querySearch(code.value, {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      accessPwd: accessPwd.value || undefined,
      params,
      code: captchaCode,
      uuid: captchaUuid
    })
    rows.value = (res.rows || []).map((item) => {
      try {
        return typeof item.rowData === 'string' ? JSON.parse(item.rowData) : item.rowData || {}
      } catch {
        return {}
      }
    })
    total.value = res.total || 0
    jumpPage.value = pageNum.value
    await loadDist()
    scrollTop()
  } catch (e) {
    error.value = e.message || '查询失败'
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function barWidth(item) {
  const n = Number(item.count) || 0
  return Math.round((n / distMax.value) * 100) + '%'
}

async function loadDist() {
  if (!layout.value.resultShowChart || !distField.value || !hasAllQueryParams(searchParams.value, queryFields.value)) {
    distList.value = []
    return
  }
  distLoading.value = true
  try {
    const res = await queryFieldDist(code.value, {
      fieldKey: distField.value,
      accessPwd: accessPwd.value || undefined,
      params: searchParams.value
    })
    distList.value = res.data || []
  } catch {
    distList.value = []
  } finally {
    distLoading.value = false
  }
}

function doPrint() {
  window.print()
}

async function doExport() {
  error.value = ''
  const params = searchParams.value
  if (!hasAllQueryParams(params, queryFields.value)) {
    error.value = '请填写全部查询条件后再导出'
    return
  }
  exporting.value = true
  try {
    await queryExport(code.value, {
      accessPwd: accessPwd.value || undefined,
      params
    })
  } catch (e) {
    error.value = e.message || '导出失败'
  } finally {
    exporting.value = false
  }
  if (!error.value) showToast('导出已开始')
}

async function doExportPdf() {
  error.value = ''
  const params = searchParams.value
  if (!hasAllQueryParams(params, queryFields.value)) {
    error.value = '请填写全部查询条件后再导出'
    return
  }
  exportingPdf.value = true
  try {
    await queryExportPdf(code.value, {
      accessPwd: accessPwd.value || undefined,
      params
    })
  } catch (e) {
    error.value = e.message || 'PDF 导出失败'
  } finally {
    exportingPdf.value = false
  }
  if (!error.value) showToast('PDF 导出已开始')
}

function gotoPage(n) {
  router.replace({
    name: 'query-result',
    params: { code: code.value },
    query: paramsToRouteQuery(searchParams.value, n)
  })
}
function prevPage() {
  if (pageNum.value <= 1) return
  gotoPage(pageNum.value - 1)
}
function nextPage() {
  if (pageNum.value * pageSize.value >= total.value) return
  gotoPage(pageNum.value + 1)
}

watch(() => route.fullPath, () => {
  if (unlocked.value) runSearch()
})

let onResize
function onKey(e) {
  if (!unlocked.value || loading.value) return
  const tag = (e.target && e.target.tagName) || ''
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
  if (e.key === 'ArrowLeft') prevPage()
  if (e.key === 'ArrowRight') nextPage()
}
onMounted(() => {
  onResize = () => { isMobile.value = window.innerWidth <= 768 }
  window.addEventListener('resize', onResize)
  window.addEventListener('keydown', onKey)
  const saved = sessionStorage.getItem(pwdStorageKey(code.value))
  if (saved) accessPwd.value = saved
  loadMeta()
})
onUnmounted(() => {
  if (onResize) window.removeEventListener('resize', onResize)
  window.removeEventListener('keydown', onKey)
  if (toastTimer) clearTimeout(toastTimer)
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 22px 16px calc(24px + env(safe-area-inset-bottom, 0px));
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}
.site-footer {
  flex-shrink: 0;
  text-align: center;
  padding: 28px 16px 8px;
  margin-top: auto;
}
.site-link {
  display: inline-block;
  font-size: 12px;
  letter-spacing: .04em;
  color: #94a3b8;
  text-decoration: none;
  border-bottom: 1px solid transparent;
}
.site-link:active,
.site-link:hover {
  color: #64748b;
  border-bottom-color: #cbd5e1;
}
.hero {
  margin: 0 auto 16px;
  width: 100%;
}
.eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: .14em;
  text-transform: uppercase;
  color: var(--result-muted, #64748b);
  font-weight: 600;
}
.hero h1 {
  margin: 0 0 10px;
  font-size: clamp(22px, 4vw, 28px);
  letter-spacing: -.03em;
  font-weight: 760;
  color: var(--theme);
  line-height: 1.25;
}
.hero-sub { margin: 0; }
.total-pill {
  display: inline-block;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--result-muted, #64748b);
  background: rgba(255,255,255,.62);
  border: 1px solid rgba(15,23,42,.06);
}
.tone-light .total-pill {
  background: rgba(255,255,255,.12);
  border-color: rgba(255,255,255,.2);
}
.cond-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
  justify-content: inherit;
}
.cond-chip {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  background: rgba(255,255,255,.55);
  border: 1px solid rgba(15,23,42,.08);
  color: var(--result-fg, #374151);
}
.tone-light .cond-chip {
  background: rgba(255,255,255,.12);
  border-color: rgba(255,255,255,.25);
  color: #f8fafc;
}
.card {
  margin: 0 auto 14px;
  width: 100%;
  border-radius: 18px;
  padding: 16px;
  text-align: left;
  box-sizing: border-box;
}
.panel-head { margin-bottom: 10px; }
.panel-head h3 { margin: 0 0 4px; font-size: 15px; }
.panel-head p { margin: 0; font-size: 12px; opacity: .75; }
.panel-card .card {
  background: #fff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}
.panel-flat .card {
  background: #fff;
  border: 1px solid #e5e7eb;
}
.panel-glass .card {
  background: rgba(255,255,255,.78);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255,255,255,.55);
}
.tone-light .panel-glass .card {
  background: rgba(255,255,255,.14);
  border-color: rgba(255,255,255,.28);
  color: #f8fafc;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin: -4px 0 14px;
  padding: 10px 0 12px;
  border-bottom: 1px solid rgba(15,23,42,.06);
  position: sticky;
  top: 0;
  z-index: 3;
  background: color-mix(in srgb, #fff 94%, transparent);
  backdrop-filter: blur(10px);
}
.tone-light .toolbar {
  background: color-mix(in srgb, #0f172a 72%, transparent);
  border-bottom-color: rgba(255,255,255,.12);
}
.toolbar-left, .toolbar-right { display: flex; gap: 8px; flex-wrap: wrap; }
.btn.ghost {
  background: transparent;
  border: 1px solid rgba(15,23,42,.1);
}
.btn.primary-soft {
  background: color-mix(in srgb, var(--theme) 12%, #fff);
  color: var(--theme);
  font-weight: 600;
}
.card-list { display: grid; gap: 12px; }
.card-list.cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
@media (max-width: 640px) {
  .card-list.cols-2 { grid-template-columns: 1fr; }
}
.result-card {
  border: 1px solid #e8edf5;
  border-radius: 14px;
  padding: 14px 14px 10px;
  background: linear-gradient(165deg, #ffffff 0%, #f7f9fc 100%);
  overflow: hidden;
}
.card-head {
  margin: -2px 0 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eef2f7;
}
.card-serial {
  display: inline-block;
  margin-bottom: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  color: var(--theme);
  background: color-mix(in srgb, var(--theme) 12%, #fff);
  font-weight: 700;
  font-size: 12px;
}
.card-title {
  font-size: 20px;
  font-weight: 720;
  letter-spacing: -.02em;
  color: #0f172a;
  word-break: break-all;
  line-height: 1.3;
}
.card-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}
.field-group + .field-group { margin-top: 10px; }
.group-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .06em;
  color: #94a3b8;
  margin: 4px 0 6px;
  text-transform: uppercase;
}
.card-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 0;
  font-size: 13px;
  border-bottom: 1px solid #f1f5f9;
}
.card-row:last-child { border-bottom: 0; }
.card-row .k { color: #94a3b8; flex: 0 0 38%; line-height: 1.4; }
.card-row .v { text-align: right; word-break: break-all; font-weight: 560; color: #1e293b; line-height: 1.4; }
.card-row .v.muted, td.muted { color: #cbd5e1; font-weight: 400; }
.dense table th, .dense table td { padding: 6px 8px; }
.table-wrap {
  overflow-x: auto;
  border-radius: 12px;
  border: 1px solid #e8edf5;
  background: #fff;
}
table { width: 100%; border-collapse: separate; border-spacing: 0; font-size: 13px; }
th, td {
  padding: 11px 12px;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
  white-space: nowrap;
}
thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8fafc;
  color: #64748b;
  font-weight: 650;
  font-size: 12px;
}
tbody tr:nth-child(even) { background: #fafbff; }
tbody tr:hover { background: color-mix(in srgb, var(--theme) 5%, #fff); }
.col-serial { width: 56px; text-align: center; color: #94a3b8; }
.pager {
  display: flex;
  justify-content: center;
  gap: 12px;
  align-items: center;
  margin-top: 16px;
}
.page-info {
  min-width: 64px;
  text-align: center;
  font-size: 13px;
  color: #64748b;
  font-variant-numeric: tabular-nums;
}
.skeleton-wrap { display: grid; gap: 10px; padding: 4px 0; }
.sk-card {
  border: 1px solid #eef2f7;
  border-radius: 12px;
  padding: 14px;
  background: #fff;
}
.sk-line {
  height: 12px;
  border-radius: 8px;
  margin: 0 0 10px;
  background: linear-gradient(90deg, #eef2f7 0%, #f8fafc 50%, #eef2f7 100%);
  background-size: 200% 100%;
  animation: skShine 1.1s linear infinite;
}
@keyframes skShine {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}
.empty-box { text-align: center; padding: 40px 8px 24px; color: #94a3b8; }
.empty-ico {
  width: 52px;
  height: 52px;
  margin: 0 auto 12px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--theme) 12%, #fff);
  color: var(--theme);
  font-weight: 700;
}
.empty-box p { margin: 0 0 14px; font-size: 14px; }
.chart-box {
  margin: 0 0 14px;
  padding: 12px;
  border: 1px solid rgba(15,23,42,.08);
  border-radius: 12px;
  background: rgba(255,255,255,.6);
}
.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #64748b;
}
.chart-head select { max-width: 46%; }
.bars { display: grid; gap: 8px; }
.bar-row {
  display: grid;
  grid-template-columns: 72px 1fr 36px;
  gap: 8px;
  align-items: center;
  font-size: 12px;
}
.bar-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #475569; }
.bar-track { height: 8px; border-radius: 999px; background: #e2e8f0; overflow: hidden; }
.bar-fill { height: 100%; background: var(--theme, #1677ff); border-radius: 999px; }
.bar-count { text-align: right; color: #64748b; }
@media (prefers-reduced-motion: reduce) {
  .sk-line { animation: none; }
}

.cond-chip.clickable {
  cursor: pointer;
  border: 0;
  font: inherit;
}
.cond-chip.clickable:hover {
  border-color: color-mix(in srgb, var(--theme) 35%, #fff);
  color: var(--theme);
}
.toast {
  position: fixed;
  left: 50%;
  bottom: 28px;
  transform: translateX(-50%);
  z-index: 50;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(15, 23, 42, .88);
  color: #fff;
  font-size: 13px;
  box-shadow: 0 10px 30px rgba(15,23,42,.2);
}
.toast-enter-active, .toast-leave-active { transition: all .25s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, 8px); }
.card-list.solo { max-width: 560px; margin: 0 auto; }
.card-list.solo .result-card {
  padding: 18px 16px 14px;
  box-shadow: 0 14px 36px rgba(15,23,42,.08);
  border-color: color-mix(in srgb, var(--theme) 22%, #e8edf5);
}
.card-list.solo .card-title { font-size: 26px; }
.card-head-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}
.copy-btn, .btn.mini {
  border: 0;
  background: color-mix(in srgb, var(--theme) 10%, #fff);
  color: var(--theme);
  border-radius: 8px;
  padding: 8px 12px;
  min-height: 36px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.copy-btn:hover { filter: brightness(.97); }
.card-row.hit, .card-title.hit {
  background: color-mix(in srgb, var(--theme) 8%, transparent);
}
.card-row.hit {
  margin: 0 -8px;
  padding-left: 8px;
  padding-right: 8px;
  border-radius: 8px;
  border-bottom-color: transparent;
}
td.hit {
  background: color-mix(in srgb, var(--theme) 10%, #fff) !important;
  color: var(--theme);
  font-weight: 650;
}
.empty-conds {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
  margin: 0 0 14px;
  font-size: 12px;
  color: #94a3b8;
}
.empty-conds span {
  padding: 3px 8px;
  border-radius: 6px;
  background: #f1f5f9;
}
.jump {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: 8px;
  font-size: 12px;
  color: #64748b;
}
.jump input {
  width: 56px;
  min-height: 36px;
  height: 36px;
  padding: 6px 8px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  font-size: 16px;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 8px;
  margin-top: 12px;
}
.sum-item {
  background: color-mix(in srgb, var(--theme) 8%, #fff);
  border-radius: 10px;
  padding: 8px 10px;
  text-align: left;
}
.sum-k { display: block; font-size: 11px; color: #94a3b8; margin-bottom: 2px; }
.sum-v { font-size: 16px; font-weight: 700; color: #0f172a; word-break: break-all; }
.sum-v.hit { color: var(--theme); }
.style-scorecard .result-card {
  border-radius: 16px;
  border: 1px solid color-mix(in srgb, var(--theme) 18%, #e8edf5);
  background: linear-gradient(180deg, #fff 0%, color-mix(in srgb, var(--theme) 4%, #fff) 100%);
}
.style-scorecard .card-title { font-size: 24px; letter-spacing: -.03em; }
.style-scorecard .card-head { border-bottom-style: solid; }
.empty-guide { margin: 0 0 12px; font-size: 13px; color: #64748b; line-height: 1.5; }

@media print {
  .no-print, .toolbar, .chart-box, .pager, .toast, .cond-tags button, .site-footer, .ambient { display: none !important; }
  .page, .result-shell { background: #fff !important; padding: 12mm !important; min-height: auto !important; }
  .card, .result-panel, .result-card { box-shadow: none !important; break-inside: avoid; page-break-inside: avoid; }
  .hero { margin-bottom: 12px !important; }
  .result-card { page-break-inside: avoid; border: 1px solid #ddd !important; margin-bottom: 12px !important; }
  .card-list { gap: 10px !important; }
}
</style>
