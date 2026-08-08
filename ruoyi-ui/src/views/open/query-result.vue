<template>
  <div
    class="open-query result-shell"
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
        <el-tag
          v-for="t in conditionTags"
          :key="t.key"
          size="mini"
          effect="plain"
          type="info"
          class="cond-tag clickable"
          @click="goBack"
        >{{ t.label }}：{{ t.value }}</el-tag>
      </div>
    </div>

    <div class="panel" :class="{ 'anim-pop anim-delay-1': layout.resultAnim }" v-if="needPwd && !unlocked" :style="{ maxWidth: layout.resultMaxWidth + 'px' }">
      <div class="panel-head">
        <h3>访问验证</h3>
        <p>请输入访问密码后查看结果</p>
      </div>
      <el-form @submit.native.prevent="unlock" label-position="top" size="small">
        <el-form-item label="访问密码">
          <el-input v-model="accessPwd" show-password placeholder="请输入访问密码" @keyup.enter.native="unlock" />
        </el-form-item>
        <el-button type="primary" class="theme-btn anim-btn" :loading="metaLoading" @click="unlock">进入结果</el-button>
      </el-form>
    </div>

    <div class="panel" :class="{ 'anim-pop anim-delay-1': layout.resultAnim }" v-else :style="{ maxWidth: layout.resultMaxWidth + 'px' }">
      <div class="toolbar no-print" :class="{ 'anim-fade-up': layout.resultAnim }">
        <div class="toolbar-left">
          <el-button size="small" class="anim-btn" icon="el-icon-back" @click="goBack">返回修改</el-button>
          <el-button
            v-if="layout.resultLayout === 'auto'"
            size="small"
            class="anim-btn"
            plain
            @click="toggleForceLayout"
          >{{ useCardLayout ? '表格视图' : '卡片视图' }}</el-button>
        </div>
        <div class="toolbar-right">
          <el-button
            v-if="layout.resultShowPrint !== false"
            size="small"
            class="anim-btn"
            icon="el-icon-printer"
            :disabled="!rows.length"
            @click="handlePrint"
          >打印</el-button>
          <el-button
            v-if="layout.resultShowExport"
            size="small"
            class="anim-btn theme-outline"
            :loading="exportingPdf"
            :disabled="!rows.length"
            @click="handleExportPdf"
          >导出 PDF</el-button>
          <el-button
            v-if="layout.resultShowExport"
            size="small"
            class="anim-btn theme-outline"
            :loading="exporting"
            :disabled="!rows.length"
            @click="handleExport"
          >导出 Excel</el-button>
        </div>
      </div>

      <div v-if="layout.resultShowChart && chartFields.length && unlocked" class="chart-box no-print" :class="{ 'anim-fade-up': layout.resultAnim }" v-loading="distLoading">
        <div class="chart-head">
          <span>字段分布</span>
          <el-select v-model="distField" size="mini" placeholder="选择字段" style="width:160px" @change="loadDist">
            <el-option v-for="f in chartFields" :key="f.fieldKey" :label="f.fieldLabel || f.fieldName" :value="f.fieldKey" />
          </el-select>
        </div>
        <div ref="distChart" class="dist-chart" />
      </div>

      <div v-if="loading" class="skeleton-wrap">
        <div class="sk-card" v-for="n in 3" :key="n">
          <div class="sk-line" style="width:34%" />
          <div class="sk-line" :style="{ width: (90 - n * 10) + '%' }" />
          <div class="sk-line" :style="{ width: (76 - n * 8) + '%' }" />
        </div>
      </div>

      <div v-else>
        <transition-group
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
                <el-button type="text" size="mini" class="copy-btn" @click="copyRow(row)">复制</el-button>
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
        </transition-group>

        <div class="table-wrap" :class="{ 'anim-fade-up': layout.resultAnim }" v-else-if="rows.length">
          <el-table :data="rows" :size="layout.resultDense ? 'mini' : 'small'" border stripe style="width: 100%">
            <el-table-column v-if="layout.showSerial" label="序号" width="70" align="center">
              <template slot-scope="scope">{{ serialOf(scope.$index) }}</template>
            </el-table-column>
            <el-table-column
              v-for="f in listFields"
              :key="f.fieldKey"
              :prop="f.fieldKey"
              :label="f.fieldLabel || f.fieldName"
              min-width="120"
              :show-overflow-tooltip="true"
            >
              <template slot-scope="scope">
                <span :class="{ muted: isEmpty(scope.row[f.fieldKey]), hit: isHit(f.fieldKey, scope.row[f.fieldKey]) }">{{ cell(scope.row, f) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-else class="empty-box" :class="{ 'anim-fade-up': layout.resultAnim }">
          <div v-if="layout.resultShowEmptyIcon" class="empty-ico">∅</div>
          <p>{{ page.resultTips || '未查询到相关数据' }}</p>
          <p v-if="emptyGuide" class="empty-guide">{{ emptyGuide }}</p>
          <div v-if="conditionTags.length" class="empty-conds">
            <span v-for="t in conditionTags" :key="t.key">{{ t.label }}={{ t.value }}</span>
          </div>
          <el-button size="mini" type="primary" class="anim-btn" @click="goBack">返回修改条件</el-button>
        </div>

        <div class="pager" v-if="total > 0">
          <span class="page-range">{{ rangeText }}</span>
          <el-pagination
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page.sync="pageNum"
            @current-change="onPageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { openQueryMeta, openQuerySearch, openQueryExport, openQueryExportPdf, openQueryFieldDist } from '@/api/biz/query'
import '@/assets/styles/biz-open-motion.css'
import { saveAs } from 'file-saver'
import { blobValidate } from '@/utils/ruoyi'
import {
  parseLayout,
  buildResultPageStyle,
  routeQueryToParams,
  paramsToRouteQuery,
  hasAllQueryParams,
  missingQueryFields,
  summarizeConditions,
  pwdStorageKey,
  displayCell,
  groupListFields,
  primaryListField,
  pageRangeText,
  loadQueryParams,
  isConditionHit,
  formatRowPlainText,
  copyText
} from '@/utils/bizQueryField'

export default {
  name: 'OpenQueryResult',
  data() {
    return {
      code: '',
      metaLoading: false,
      loading: false,
      exporting: false,
      exportingPdf: false,
      queryName: '',
      page: {},
      layout: parseLayout(null),
      queryFields: [],
      listFields: [],
      rows: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      needPwd: false,
      unlocked: false,
      accessPwd: '',
      isMobile: false,
      forceLayout: '',
      distField: '',
      distLoading: false,
      chart: null
    }
  },
  computed: {
    themeColor() {
      return (this.page && this.page.themeColor) || '#1677ff'
    },
    pageStyle() {
      return buildResultPageStyle(this.layout, this.themeColor, process.env.VUE_APP_BASE_API)
    },
    resultTitle() {
      return this.layout.resultTitle || ((this.page.title || this.queryName || '查询') + ' · 结果')
    },
    rangeText() {
      return pageRangeText(this.pageNum, this.pageSize, this.total)
    },
    useCardLayout() {
      if (this.forceLayout === 'card') return true
      if (this.forceLayout === 'table') return false
      if (this.layout.resultLayout === 'card') return true
      if (this.layout.resultLayout === 'table') return false
      return this.isMobile
    },
    searchParams() {
      const stored = loadQueryParams(this.code)
      if (stored && Object.keys(stored).length) return stored
      return routeQueryToParams(this.$route.query, this.queryFields)
    },
    resultStyle() {
      return (this.layout && this.layout.resultStyle) || 'default'
    },
    emptyGuide() {
      return (this.layout && this.layout.resultEmptyGuide) || ''
    },
    summaryFields() {
      const keys = (this.layout && this.layout.resultSummaryFields) || []
      if (!keys.length) return []
      const map = {}
      ;(this.listFields || []).forEach(f => { map[f.fieldKey] = f })
      return keys.map(k => map[k]).filter(Boolean)
    },
    conditionTags() {
      return summarizeConditions(this.searchParams, this.queryFields)
    },
    primaryField() {
      const key = this.layout && this.layout.resultTitleField
      if (key) {
        const hit = (this.listFields || []).find(f => f.fieldKey === key)
        if (hit) return hit
      }
      return primaryListField(this.listFields)
    },
    fieldGroups() {
      return groupListFields(this.listFields)
    },
    bodyGroups() {
      const pf = this.primaryField
      const skip = {}
      if (pf) skip[pf.fieldKey] = true
      ;(this.summaryFields || []).forEach(f => { skip[f.fieldKey] = true })
      return this.fieldGroups
        .map(g => ({
          group: g.group,
          fields: g.fields.filter(f => !skip[f.fieldKey])
        }))
        .filter(g => g.fields.length)
    },
    chartFields() {
      const all = []
      const seen = new Set()
      ;[...(this.listFields || []), ...(this.queryFields || [])].forEach(f => {
        if (f && f.fieldKey && !seen.has(f.fieldKey)) {
          seen.add(f.fieldKey)
          all.push(f)
        }
      })
      return all
    }
  },
  watch: {
    '$route.fullPath'() {
      this.syncPageFromRoute()
      if (this.unlocked) this.runSearch()
    }
  },
  created() {
    this.code = this.$route.params.code
    this.updateMobile()
    window.addEventListener('resize', this.updateMobile)
    const saved = sessionStorage.getItem(pwdStorageKey(this.code))
    if (saved) this.accessPwd = saved
    this.syncPageFromRoute()
    this.loadMeta()
  },
  beforeDestroy() {
    if (this.chart) this.chart.dispose()
    window.removeEventListener('resize', this.resizeChart)
    window.removeEventListener('resize', this.updateMobile)
  },
  methods: {
    cell(row, f) {
      return displayCell(row && f ? row[f.fieldKey] : '')
    },
    isEmpty(v) {
      return v == null || String(v).trim() === ''
    },
    isHit(fieldKey, value) {
      return isConditionHit(fieldKey, value, this.searchParams, this.queryFields)
    },
    copyRow(row) {
      copyText(formatRowPlainText(row, this.listFields)).then(() => {
        this.$message.success('已复制本条结果')
      }).catch(() => {
        this.$message.error('复制失败')
      })
    },
    toggleForceLayout() {
      this.forceLayout = this.useCardLayout ? 'table' : 'card'
    },
    updateMobile() {
      this.isMobile = window.innerWidth <= 768
    },
    syncPageFromRoute() {
      this.pageNum = Math.max(1, parseInt(this.$route.query.page || '1', 10) || 1)
    },
    serialOf(index) {
      return (this.pageNum - 1) * this.pageSize + index + 1
    },
    resizeChart() { if (this.chart) this.chart.resize() },
    loadDist() {
      if (!this.layout.resultShowChart || !this.distField || !hasAllQueryParams(this.searchParams, this.queryFields)) return
      this.distLoading = true
      openQueryFieldDist(this.code, {
        fieldKey: this.distField,
        accessPwd: this.accessPwd || undefined,
        params: this.searchParams
      }).then(res => {
        this.$nextTick(() => this.renderChart(res.data || []))
      }).catch(() => {}).finally(() => { this.distLoading = false })
    },
    renderChart(list) {
      const el = this.$refs.distChart
      if (!el) return
      if (this.chart) this.chart.dispose()
      this.chart = echarts.init(el)
      const labels = list.map(i => i.value)
      const values = list.map(i => Number(i.count) || 0)
      const isPie = this.layout.resultChartType === 'pie'
      if (isPie) {
        this.chart.setOption({
          tooltip: { trigger: 'item' },
          series: [{
            type: 'pie', radius: ['35%', '62%'], center: ['50%', '50%'],
            data: labels.map((n, i) => ({ name: n, value: values[i] })),
            label: { formatter: '{b}\n{d}%' }
          }]
        })
      } else {
        this.chart.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: 40, right: 16, top: 24, bottom: 40 },
          xAxis: { type: 'category', data: labels, axisLabel: { rotate: labels.length > 8 ? 30 : 0 } },
          yAxis: { type: 'value', minInterval: 1 },
          series: [{ type: 'bar', data: values, itemStyle: { color: this.themeColor } }]
        })
      }
      window.removeEventListener('resize', this.resizeChart)
      window.addEventListener('resize', this.resizeChart)
    },
    loadMeta() {
      this.metaLoading = true
      openQueryMeta(this.code, this.accessPwd || undefined).then(res => {
        const data = res.data || {}
        this.queryName = data.queryName
        this.needPwd = !!data.needPwd
        this.unlocked = !!data.unlocked || !data.needPwd
        if (!this.unlocked) {
          document.title = this.queryName || '查询结果'
          return
        }
        if (this.accessPwd) sessionStorage.setItem(pwdStorageKey(this.code), this.accessPwd)
        this.page = data.page || {}
        this.layout = parseLayout(this.page)
        this.pageSize = this.layout.resultPageSize || 10
        this.queryFields = data.queryFields || []
        this.listFields = data.listFields || []
        const def = this.layout.resultChartDefaultField
        this.distField = (def && this.chartFields.some(f => f.fieldKey === def))
          ? def
          : (this.chartFields[0] && this.chartFields[0].fieldKey) || ''
        document.title = this.resultTitle
        this.runSearch()
      }).catch(err => {
        this.$message.error((err && err.msg) || '查询不存在或未发布')
      }).finally(() => { this.metaLoading = false })
    },
    unlock() {
      if (!this.accessPwd) {
        this.$message.warning('请输入访问密码')
        return
      }
      this.loadMeta()
    },
    goBack() {
      const q = { ...this.$route.query }
      delete q.page
      this.$router.push({ path: '/q/' + this.code, query: q })
    },
    onPageChange(p) {
      this.$router.replace({
        path: '/q/' + this.code + '/result',
        query: paramsToRouteQuery(this.searchParams, p)
      })
    },
    runSearch() {
      const params = this.searchParams
      const missing = missingQueryFields(params, this.queryFields)
      if (missing.length || !hasAllQueryParams(params, this.queryFields)) {
        const names = missing.map(f => f.fieldLabel || f.fieldName || f.fieldKey).join('、')
        this.$message.warning(names ? ('请填写必填查询条件：' + names) : '请至少填写一项查询条件，请返回重新填写')
        this.rows = []
        this.total = 0
        return
      }
      this.loading = true
      let captchaCode, captchaUuid
      try {
        const raw = sessionStorage.getItem('biz_q_captcha_' + this.code)
        if (raw) {
          const c = JSON.parse(raw)
          captchaCode = c.code
          captchaUuid = c.uuid
          sessionStorage.removeItem('biz_q_captcha_' + this.code)
        }
      } catch (e) {}
      openQuerySearch(this.code, {
        pageNum: this.pageNum,
        pageSize: this.pageSize,
        accessPwd: this.accessPwd || undefined,
        params,
        code: captchaCode,
        uuid: captchaUuid
      }).then(res => {
        const list = res.rows || []
        this.rows = list.map(item => {
          try {
            return typeof item.rowData === 'string' ? JSON.parse(item.rowData) : (item.rowData || {})
          } catch (e) {
            return {}
          }
        })
        this.total = res.total || 0
        this.loadDist()
        if (typeof window !== 'undefined') window.scrollTo({ top: 0, behavior: 'smooth' })
      }).catch(err => {
        this.$message.error((err && err.msg) || '查询失败')
        this.rows = []
        this.total = 0
      }).finally(() => { this.loading = false })
    },
    handlePrint() {
      window.print()
    },
    handleExport() {
      const params = this.searchParams
      if (!hasAllQueryParams(params, this.queryFields)) {
        this.$message.warning('请完善查询条件后再导出')
        return
      }
      this.exporting = true
      openQueryExport(this.code, {
        accessPwd: this.accessPwd || undefined,
        params
      }).then(async data => {
        const ok = await blobValidate(data)
        if (ok) {
          saveAs(data, (this.page.title || this.queryName || 'query') + '.xlsx')
        } else {
          this.$message.error('导出失败')
        }
      }).finally(() => { this.exporting = false })
    },
    handleExportPdf() {
      const params = this.searchParams
      if (!hasAllQueryParams(params, this.queryFields)) {
        this.$message.warning('请完善查询条件后再导出')
        return
      }
      this.exportingPdf = true
      openQueryExportPdf(this.code, {
        accessPwd: this.accessPwd || undefined,
        params
      }).then(async data => {
        const ok = await blobValidate(data)
        if (ok) {
          saveAs(data, (this.page.title || this.queryName || 'query') + '.pdf')
        } else {
          this.$message.error('PDF 导出失败')
        }
      }).finally(() => { this.exportingPdf = false })
    }
  }
}
</script>

<style scoped>
.open-query {
  min-height: 100vh;
  padding: 28px 16px 56px;
  box-sizing: border-box;
}
.hero { margin: 0 auto 18px; width: 100%; }
.eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: .12em;
  text-transform: uppercase;
  color: var(--result-muted, #64748b);
  font-weight: 600;
}
.hero h1 {
  margin: 0 0 10px;
  font-size: 28px;
  letter-spacing: -.02em;
  font-weight: 700;
  color: var(--theme);
  line-height: 1.25;
}
.hero-sub { margin: 0; }
.total-pill {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--result-muted, #64748b);
  background: rgba(255,255,255,.55);
  border: 1px solid rgba(15,23,42,.06);
}
.tone-light .total-pill {
  background: rgba(255,255,255,.12);
  border-color: rgba(255,255,255,.2);
  color: var(--result-muted);
}
.cond-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; justify-content: inherit; }
.cond-tag { max-width: 100%; border-radius: 8px !important; }
.panel {
  margin: 0 auto 16px;
  width: 100%;
  border-radius: 18px;
  padding: 18px 16px;
  text-align: left;
  box-sizing: border-box;
}
.panel-head { margin-bottom: 12px; }
.panel-head h3 { margin: 0 0 4px; font-size: 16px; }
.panel-head p { margin: 0; font-size: 13px; opacity: .75; }
.panel-card .panel { background: #fff; box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06); }
.panel-flat .panel { background: #fff; border: 1px solid #e5e7eb; }
.panel-glass .panel {
  background: rgba(255,255,255,.76);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255,255,255,.55);
  box-shadow: 0 10px 36px rgba(15, 23, 42, 0.08);
}
.tone-light .panel-glass .panel {
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
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(15,23,42,.06);
}
.toolbar-left { display: flex; gap: 8px; flex-wrap: wrap; }
.theme-btn { background: var(--theme); border-color: var(--theme); }
.theme-outline { color: var(--theme); border-color: color-mix(in srgb, var(--theme) 45%, #fff); }
.table-wrap { overflow-x: auto; border-radius: 12px; }
.pager {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}
.page-range { font-size: 12px; color: #94a3b8; margin-right: auto; }
.card-list { display: grid; gap: 12px; }
.card-list.cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
@media (max-width: 720px) {
  .card-list.cols-2 { grid-template-columns: 1fr; }
}
.result-card {
  border: 1px solid #e8edf5;
  border-radius: 14px;
  padding: 14px 14px 10px;
  background: linear-gradient(165deg, #ffffff 0%, #f7f9fc 100%);
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
  font-weight: 700;
  letter-spacing: -.02em;
  color: #0f172a;
  word-break: break-all;
  line-height: 1.3;
}
.card-sub { margin-top: 4px; font-size: 12px; color: #94a3b8; }
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
.card-row .k { color: #94a3b8; flex: 0 0 36%; }
.card-row .v { color: #0f172a; text-align: right; word-break: break-all; font-weight: 500; }
.card-row .v.muted, .muted { color: #cbd5e1 !important; font-weight: 400; }
.dense ::v-deep .el-table td, .dense ::v-deep .el-table th { padding: 4px 0; }
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
.empty-box { text-align: center; padding: 40px 12px 28px; color: #94a3b8; }
.empty-ico {
  width: 52px; height: 52px; margin: 0 auto 12px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--theme) 12%, #fff);
  color: var(--theme); font-size: 20px; font-weight: 700;
}
.empty-box p { margin: 0 0 14px; }
@media (max-width: 768px) {
  .hero h1 { font-size: 22px; }
  .open-query { padding: 18px 12px 40px; }
  .toolbar { position: sticky; top: 0; z-index: 2; background: inherit; }
}
@media (prefers-reduced-motion: reduce) {
  .sk-line { animation: none; }
}
.chart-box { margin: 0 0 16px; padding: 12px; border: 1px solid rgba(15,23,42,.06); border-radius: 12px; background: rgba(255,255,255,.55); }
.chart-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; color: #64748b; font-size: 13px; }
.dist-chart { height: 260px; width: 100%; }

.cond-tag.clickable { cursor: pointer; }
.cond-tag.clickable:hover { color: var(--theme); border-color: color-mix(in srgb, var(--theme) 40%, #dcdfe6); }
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
  min-height: 24px;
}
.copy-btn { padding: 0 !important; font-weight: 600; }
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
.hit { color: var(--theme) !important; font-weight: 650; }
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

.summary-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
  gap: 8px;
  margin-top: 12px;
}
.sum-item {
  background: color-mix(in srgb, var(--theme) 8%, #fff);
  border-radius: 10px;
  padding: 8px 10px;
}
.sum-k { display: block; font-size: 11px; color: #94a3b8; margin-bottom: 2px; }
.sum-v { font-size: 16px; font-weight: 700; color: #0f172a; word-break: break-all; }
.style-scorecard .result-card {
  border-radius: 16px;
  border: 1px solid color-mix(in srgb, var(--theme) 18%, #e8edf5);
  background: linear-gradient(180deg, #fff 0%, color-mix(in srgb, var(--theme) 4%, #fff) 100%);
}
.style-scorecard .card-title { font-size: 24px; }
.empty-guide { margin: 0 0 12px; font-size: 13px; color: #64748b; line-height: 1.5; }

.toolbar-right { display: inline-flex; gap: 8px; flex-wrap: wrap; }
@media print {
  .no-print, .toolbar, .chart-box, .pager { display: none !important; }
  .open-query, .result-shell { background: #fff !important; padding: 12mm !important; }
  .panel, .result-card { box-shadow: none !important; page-break-inside: avoid; border: 1px solid #ddd !important; }
  .result-card { margin-bottom: 12px !important; }
}
</style>
