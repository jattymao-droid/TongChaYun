<template>
  <div :class="embedded ? 'preview-embed' : 'app-container'" v-loading="metaLoading">
    <el-page-header v-if="!embedded" @back="goBack" :content="'预览 - ' + (queryName || '')" class="mb16" />
    <el-alert v-if="!embedded" title="预览模式：使用真实数据检索，不会增加公开访问量。条件区与结果区会应用「页面设计」布局。" type="info" :closable="false" class="mb16" />
    <el-alert v-else title="使用真实数据检索验证效果，不会增加公开访问量。" type="info" :closable="false" class="mb16" />

    <div
      class="form-shell"
      :class="['style-' + layout.formPanelStyle, { compact: layout.formCompact }]"
      :style="formShellStyle"
    >
      <div class="form-inner" :style="{ maxWidth: formWidth + 'px' }">
        <p v-if="layout.formShowEyebrow" class="form-eyebrow">通查云 · 查询</p>
        <div class="title-row">
          <img v-if="logoSrc" :src="logoSrc" class="hero-logo" alt="" />
          <h2 class="title" :style="{ color: themeColor }">{{ (page && page.title) || queryName || '数据查询' }}</h2>
        </div>
        <div v-if="queryFields.length" class="cond-head">
          <strong>查询条件</strong>
          <span v-if="page && page.subtitle" class="head-sub">{{ page.subtitle }}</span>
          <span v-else class="muted">共 {{ queryFields.length }} 项</span>
        </div>
        <el-form v-if="queryFields.length" :model="form" label-position="top" size="small" class="query-form">
          <div class="fields" :class="'cols-' + fieldsCols">
            <div
              v-for="f in queryFields"
              :key="f.fieldKey"
              class="field-cell"
              :class="{ 'span-all': isWideField(f) }"
            >
              <el-form-item :label="f.fieldLabel || f.fieldName">
                <el-select
                  v-if="f.htmlType === 'select' && String(f.queryType || '').toUpperCase() === 'IN'"
                  v-model="form[f.fieldKey]"
                  multiple
                  clearable
                  filterable
                  collapse-tags
                  style="width:100%"
                >
                  <el-option v-for="opt in dictOf(f)" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
                <el-select v-else-if="f.htmlType === 'select'" v-model="form[f.fieldKey]" clearable filterable style="width:100%">
                  <el-option v-for="opt in dictOf(f)" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
                <el-date-picker
                  v-else-if="f.htmlType === 'date' && String(f.queryType || '').toUpperCase() === 'BETWEEN'"
                  v-model="form[f.fieldKey]" type="daterange" value-format="yyyy-MM-dd"
                  range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width:100%"
                />
                <el-input
                  v-else-if="String(f.queryType || '').toUpperCase() === 'IN'"
                  v-model="form[f.fieldKey]"
                  clearable
                  placeholder="多个值用逗号分隔"
                  @keyup.enter.native="handleSearch"
                />
                <el-date-picker
                  v-else-if="f.htmlType === 'date'"
                  v-model="form[f.fieldKey]" type="date" value-format="yyyy-MM-dd" style="width:100%"
                />
                <el-input v-else v-model="form[f.fieldKey]" clearable @keyup.enter.native="handleSearch" />
              </el-form-item>
            </div>
          </div>
          <div class="actions-bar" :class="{ block: layout.formBtnBlock }">
            <el-button @click="resetForm">重置</el-button>
            <el-button type="primary" :loading="searching" :style="{ background: themeColor, borderColor: themeColor }" @click="handleSearch">查询</el-button>
          </div>
        </el-form>
        <aside
          v-if="queryFields.length && showNotice"
          class="notice-box"
          :class="['style-' + layout.formNoticeStyle, 'align-' + layout.formNoticeAlign]"
        >
          <div v-if="layout.formNoticeTitle" class="notice-title">{{ layout.formNoticeTitle }}</div>
          <div class="notice-body" v-html="noticeHtml" />
        </aside>
        <el-empty v-else-if="!metaLoading && !queryFields.length" description="请先上传 Excel 并配置字段" />
      </div>
    </div>

    <div class="panel" v-if="chartFields.length">
      <div class="result-head">
        字段分布
        <el-select v-model="distField" size="mini" placeholder="选择字段" style="width: 180px; margin-left: 8px" @change="loadDist">
          <el-option v-for="f in chartFields" :key="f.fieldKey" :label="f.fieldLabel || f.fieldName" :value="f.fieldKey" />
        </el-select>
      </div>
      <div ref="distChart" class="chart" v-loading="distLoading" />
    </div>

    <div
      v-if="searched"
      class="result-shell"
      :class="['panel-' + layout.resultPanelStyle, 'tone-' + layout.resultTextTone, { dense: layout.resultDense }]"
      :style="resultShellStyle"
    >
      <div class="result-inner" :style="{ maxWidth: layout.resultMaxWidth + 'px' }">
        <p v-if="layout.resultShowEyebrow" class="eyebrow">查询结果</p>
        <h2 class="result-title">{{ resultTitle }}</h2>
        <p v-if="layout.resultShowTotal" class="result-total">共 {{ total }} 条结果</p>
        <div v-if="layout.resultShowConditions && conditionTags.length" class="cond-tags">
          <el-tag v-for="t in conditionTags" :key="t.key" size="mini" effect="plain" type="info">{{ t.label }}：{{ t.value }}</el-tag>
        </div>
        <div class="result-panel">
          <div v-loading="searching">
            <div v-if="rows.length && useCardLayout" class="card-list" :class="'cols-' + layout.resultCardColumns">
              <div class="result-card" v-for="(row, i) in rows" :key="pageNum + '-' + i">
                <div class="card-serial" v-if="layout.showSerial">#{{ (pageNum - 1) * pageSize + i + 1 }}</div>
                <div class="card-row" v-for="f in listFields" :key="f.fieldKey">
                  <span class="k">{{ f.fieldLabel || f.fieldName }}</span>
                  <span class="v">{{ row[f.fieldKey] }}</span>
                </div>
              </div>
            </div>
            <el-table v-else-if="rows.length" :data="rows" :size="layout.resultDense ? 'mini' : 'small'" border stripe>
              <el-table-column v-if="layout.showSerial" label="序号" width="70" align="center">
                <template slot-scope="scope">{{ (pageNum - 1) * pageSize + scope.$index + 1 }}</template>
              </el-table-column>
              <el-table-column v-for="f in listFields" :key="f.fieldKey" :prop="f.fieldKey" :label="f.fieldLabel || f.fieldName" min-width="120" show-overflow-tooltip />
            </el-table>
            <div v-else class="empty-box">
              <div v-if="layout.resultShowEmptyIcon" class="empty-ico">∅</div>
              <p>{{ (page && page.resultTips) || '未查询到相关数据' }}</p>
            </div>
            <el-pagination
              v-if="total > 0"
              class="pager"
              layout="prev, pager, next"
              :total="total"
              :page-size="pageSize"
              :current-page.sync="pageNum"
              @current-change="handleSearch"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons')
import { previewQueryMeta, previewQuerySearch, queryFieldDist } from '@/api/biz/query'
import {
  parseDictOptions,
  parseLayout,
  normalizeQueryParams,
  buildResultPageStyle,
  buildFormPageStyle,
  resolveFormWidth,
  resolveFormColumns,
  resolveAssetUrl,
  defaultLayout,
  summarizeConditions,
  noticeBoxVisible,
  formatNoticeHtml
} from '@/utils/bizQueryField'

export default {
  name: 'BizQueryPreview',
  props: {
    embedded: { type: Boolean, default: false },
    queryIdProp: { type: [String, Number], default: null }
  },
  data() {
    return {
      queryId: null,
      metaLoading: false,
      searching: false,
      searched: false,
      queryName: '',
      page: {},
      layout: defaultLayout(),
      queryFields: [],
      listFields: [],
      form: {},
      rows: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      distField: '',
      distLoading: false,
      chart: null,
      isMobile: false
    }
  },
  computed: {
    themeColor() {
      return (this.page && this.page.themeColor) || '#1677ff'
    },
    formShellStyle() {
      return {
        ...buildFormPageStyle(this.layout, this.themeColor),
        marginBottom: '16px',
        borderRadius: '12px',
        padding: '20px 16px'
      }
    },
    formWidth() {
      return resolveFormWidth(this.layout, this.queryFields.length)
    },
    fieldsCols() {
      return resolveFormColumns(this.layout, this.queryFields.length)
    },
    showNotice() {
      return noticeBoxVisible(this.layout)
    },
    noticeHtml() {
      return formatNoticeHtml(this.layout.formNoticeText)
    },
    logoSrc() {
      if (!this.layout.showLogo) return ''
      return resolveAssetUrl(this.layout.logoUrl, process.env.VUE_APP_BASE_API)
    },
    resultTitle() {
      return this.layout.resultTitle || ((this.page.title || this.queryName || '查询') + ' · 结果')
    },
    resultShellStyle() {
      return {
        ...buildResultPageStyle(this.layout, this.themeColor, process.env.VUE_APP_BASE_API),
        borderRadius: '8px',
        padding: '20px 16px',
        marginBottom: '16px'
      }
    },
    useCardLayout() {
      if (this.layout.resultLayout === 'card') return true
      if (this.layout.resultLayout === 'table') return false
      return this.isMobile
    },
    conditionTags() {
      return summarizeConditions(normalizeQueryParams(this.form, this.queryFields), this.queryFields)
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
  created() {
    this.queryId = this.queryIdProp != null ? this.queryIdProp : this.$route.params.queryId
    this.updateMobile()
    window.addEventListener('resize', this.updateMobile)
    this.loadMeta()
  },
  beforeDestroy() {
    if (this.chart) this.chart.dispose()
    window.removeEventListener('resize', this.resizeChart)
    window.removeEventListener('resize', this.updateMobile)
  },
  methods: {
    goBack() { this.$router.push('/biz/query') },
    updateMobile() { this.isMobile = window.innerWidth <= 768 },
    resizeChart() { if (this.chart) this.chart.resize() },
    dictOf(f) { return parseDictOptions(f.dictOptions) },
    isWideField(f) {
      return f && f.htmlType === 'date' && String(f.queryType || '').toUpperCase() === 'BETWEEN'
    },
    loadMeta() {
      this.metaLoading = true
      previewQueryMeta(this.queryId).then(res => {
        const data = res.data || {}
        this.queryName = data.queryName
        this.page = data.page || {}
        this.layout = parseLayout(this.page)
        this.pageSize = this.layout.resultPageSize || 10
        this.queryFields = data.queryFields || []
        this.listFields = data.listFields || []
        const form = {}
        this.queryFields.forEach(f => {
          form[f.fieldKey] = (String(f.queryType || '').toUpperCase() === 'BETWEEN' || (String(f.queryType || '').toUpperCase() === 'IN' && f.htmlType === 'select')) ? [] : ''
        })
        this.form = form
        this.$nextTick(() => {
          if (this.chartFields.length) {
            this.distField = this.chartFields[0].fieldKey
            this.loadDist()
          }
        })
      }).finally(() => { this.metaLoading = false })
    },
    loadDist() {
      if (!this.distField) return
      this.distLoading = true
      queryFieldDist(this.queryId, this.distField).then(res => {
        const list = res.data || []
        this.$nextTick(() => this.renderChart(list))
      }).finally(() => { this.distLoading = false })
    },
    renderChart(list) {
      const el = this.$refs.distChart
      if (!el) return
      if (this.chart) this.chart.dispose()
      this.chart = echarts.init(el, 'macarons')
      const labels = list.map(i => i.value)
      const values = list.map(i => Number(i.count) || 0)
      this.chart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 20, top: 30, bottom: 40 },
        xAxis: { type: 'category', data: labels, axisLabel: { rotate: labels.length > 8 ? 30 : 0 } },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{ type: 'bar', data: values, itemStyle: { color: this.themeColor } }]
      })
      window.addEventListener('resize', this.resizeChart)
    },
    resetForm() {
      Object.keys(this.form).forEach(k => {
        const f = this.queryFields.find(x => x.fieldKey === k)
        const op = String((f && f.queryType) || '').toUpperCase()
        this.form[k] = (op === 'BETWEEN' || (op === 'IN' && f && f.htmlType === 'select')) ? [] : ''
      })
      this.rows = []
      this.total = 0
      this.searched = false
      this.pageNum = 1
    },
    handleSearch() {
      this.searching = true
      this.searched = true
      previewQuerySearch(this.queryId, {
        pageNum: this.pageNum,
        pageSize: this.pageSize,
        params: normalizeQueryParams(this.form, this.queryFields)
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
      }).finally(() => { this.searching = false })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.form-shell { background: #fff; border: 1px solid #ebeef5; }
.form-shell.style-glass {
  background: rgba(255,255,255,.72);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,.55);
}
.form-shell.style-flat { box-shadow: none; }
.form-shell.style-card { box-shadow: 0 6px 24px rgba(22, 119, 255, 0.06); }
.form-shell.compact .query-form >>> .el-form-item { margin-bottom: 10px; }
.form-inner { margin: 0 auto; width: 100%; }
.form-eyebrow {
  margin: 0 0 6px; font-size: 12px; letter-spacing: .1em; text-transform: uppercase;
  color: #64748b; font-weight: 600;
}
.title-row {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 12px;
  max-width: 100%;
}
.hero-logo {
  width: 40px;
  height: 40px;
  object-fit: contain;
  border-radius: 8px;
  flex-shrink: 0;
  background: #fff;
  box-shadow: 0 4px 12px rgba(15, 23, 42, .08);
}
.title { margin: 0; }
.cond-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  margin: 0 0 10px;
  font-size: 15px;
  color: #0f172a;
}
.cond-head .head-sub,
.cond-head .muted {
  font-size: 13px;
  font-weight: 400;
  color: #64748b;
}
.fields { display: grid; gap: 0 12px; }
.fields.cols-1 { grid-template-columns: 1fr; }
.fields.cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.fields.cols-3 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.field-cell.span-all { grid-column: 1 / -1; }
.actions-bar { margin-top: 4px; display: flex; gap: 10px; }
.actions-bar.block { flex-direction: column; }
.actions-bar.block .el-button { width: 100%; margin: 0; }
.notice-box {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  line-height: 1.55;
  font-size: 13px;
  color: #334155;
}
.notice-box.align-center { text-align: center; }
.notice-title { font-size: 13px; font-weight: 700; margin: 0 0 6px; color: #0f172a; }
.notice-body { word-break: break-word; }
.notice-box.style-info { background: #eff6ff; border-left: 3px solid #1677ff; }
.notice-box.style-tip { background: #f0fdf4; border-left: 3px solid #16a34a; }
.notice-box.style-warn { background: #fffbeb; border-left: 3px solid #d97706; }
.notice-box.style-soft { background: #f8fafc; border: 1px solid #e2e8f0; }
.notice-box.style-quote { background: transparent; border: 0; border-left: 3px solid #94a3b8; border-radius: 0; font-style: italic; color: #475569; }
.notice-box.style-plain { background: transparent; border: 0; padding: 4px 0; color: #64748b; }
@media (max-width: 720px) {
  .fields.cols-2, .fields.cols-3 { grid-template-columns: 1fr; }
}
.panel { background: #fff; border: 1px solid #ebeef5; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
.result-head { margin-bottom: 12px; color: #666; font-size: 13px; }
.pager { margin-top: 16px; text-align: right; }
.chart { height: 280px; width: 100%; }
.result-inner { margin: 0 auto; width: 100%; }
.result-title { margin: 0 0 8px; font-size: 24px; color: var(--theme); }
.result-total { margin: 0 0 14px; color: var(--result-muted, #666); font-size: 13px; }
.cond-tags { display: flex; flex-wrap: wrap; gap: 6px; margin: 0 0 12px; justify-content: inherit; }
.result-panel { border-radius: 12px; padding: 16px; text-align: left; }
.panel-card .result-panel { background: #fff; box-shadow: 0 6px 24px rgba(22, 119, 255, 0.06); }
.panel-flat .result-panel { background: #fff; border: 1px solid #ebeef5; }
.panel-glass .result-panel {
  background: rgba(255,255,255,.72);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,.55);
}
.tone-light .panel-glass .result-panel {
  background: rgba(255,255,255,.14);
  border-color: rgba(255,255,255,.28);
  color: #f8fafc;
}
.card-list { display: grid; gap: 10px; }
.card-list.cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
@media (max-width: 720px) {
  .card-list.cols-2 { grid-template-columns: 1fr; }
}
.result-card { border: 1px solid #eef2f7; border-radius: 10px; padding: 12px; background: #fafbff; }
.card-serial { color: var(--theme); font-weight: 600; margin-bottom: 8px; }
.card-row { display: flex; justify-content: space-between; gap: 12px; padding: 4px 0; font-size: 13px; }
.card-row .k { color: #909399; flex: 0 0 36%; }
.card-row .v { text-align: right; word-break: break-all; }
.dense ::v-deep .el-table td, .dense ::v-deep .el-table th { padding: 4px 0; }
.eyebrow {
  margin: 0 0 6px; font-size: 12px; letter-spacing: .1em; text-transform: uppercase;
  color: var(--result-muted, #64748b); font-weight: 600;
}
.empty-box { text-align: center; padding: 28px 8px; color: #94a3b8; }
.empty-ico {
  width: 44px; height: 44px; margin: 0 auto 10px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--theme) 12%, #fff); color: var(--theme); font-weight: 700;
}
.empty-box p { margin: 0; }
</style>
