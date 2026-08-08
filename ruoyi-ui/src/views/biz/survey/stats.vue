<template>
  <div class="app-container stats-page" v-loading="loading">
    <div class="page-head mb16">
      <el-page-header @back="goBack" :content="'统计 - ' + (stats.surveyName || '')" />
      <div class="head-actions">
        <el-button size="small" icon="el-icon-document" @click="goAnswers">答卷列表</el-button>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button
          type="warning"
          plain
          size="small"
          icon="el-icon-download"
          :loading="exporting"
          @click="handleExportStats"
          v-hasPermi="['biz:survey:query']"
        >导出统计</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="mb16">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="metric-card"><div class="stat">有效答卷 <b>{{ stats.answerCount || 0 }}</b></div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="metric-card"><div class="stat">无效答卷 <b>{{ stats.invalidCount || 0 }}</b></div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="metric-card"><div class="stat">全部答卷 <b>{{ stats.totalCount || 0 }}</b></div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="metric-card"><div class="stat">浏览量 <b>{{ stats.viewCount || 0 }}</b></div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="metric-card"><div class="stat">转化率 <b>{{ convertRateText }}</b></div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="metric-card"><div class="stat">选择题 <b>{{ choiceCount }}</b></div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mb16" v-if="(stats.dailyTrends || []).length || (stats.channels || []).length">
      <el-col :xs="24" :md="14" v-if="(stats.dailyTrends || []).length">
        <el-card shadow="never">
          <div slot="header">每日提交趋势（有效答卷）</div>
          <div ref="dailyChart" class="chart trend-chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10" v-if="(stats.channels || []).length">
        <el-card shadow="never">
          <div slot="header">渠道分布</div>
          <div ref="channelChart" class="chart" />
          <el-table :data="stats.channels || []" size="mini" class="mt8">
            <el-table-column label="渠道" prop="channelCode" />
            <el-table-column label="答卷数" prop="count" width="90" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mb16 answer-matrix-card">
      <div slot="header" class="matrix-head">
        <span>答题列表<span class="sub">（每人一行）</span></span>
        <div class="matrix-tools">
          <el-radio-group v-model="matrixValidFlag" size="mini" @change="onMatrixFilterChange">
            <el-radio-button label="1">有效</el-radio-button>
            <el-radio-button label="0">无效</el-radio-button>
            <el-radio-button label="all">全部</el-radio-button>
          </el-radio-group>
          <el-button size="mini" icon="el-icon-refresh" :loading="matrixLoading" @click="loadMatrix">刷新</el-button>
        </div>
      </div>
      <div v-loading="matrixLoading">
        <el-table
          v-if="matrixRows.length"
          :data="matrixRows"
          size="mini"
          border
          class="matrix-table"
          max-height="520"
        >
          <el-table-column label="#" prop="label" width="64" fixed align="center" />
          <el-table-column label="状态" width="72" fixed align="center">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.validFlag === '0' ? 'info' : 'success'">
                {{ scope.row.validFlag === '0' ? '无效' : '有效' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" prop="submitTime" width="160" fixed />
          <el-table-column label="渠道" prop="channelCode" width="90" align="center">
            <template slot-scope="scope">{{ scope.row.channelCode || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-for="col in matrixColumns"
            :key="col.questionId"
            :label="col.title"
            min-width="140"
            :show-overflow-tooltip="col.qType !== 'agreement' && col.qType !== 'signature' && col.qType !== 'file'"
          >
            <template slot="header">
              <span>{{ col.title }}</span>
              <el-tag size="mini" type="info" class="ml6">{{ typeLabel(col.qType) }}</el-tag>
            </template>
            <template slot-scope="scope">
              <template v-if="col.qType === 'agreement'">
                <el-button type="text" size="mini" @click="openAgreement(scope.row, col)">查看协议</el-button>
                <span class="cell-agree">{{ cellDisplay(scope.row, col) || '-' }}</span>
              </template>
              <template v-else-if="col.qType === 'signature'">
                <img
                  v-if="cellMediaUrl(scope.row, col)"
                  :src="cellMediaUrl(scope.row, col)"
                  class="sig-thumb"
                  alt="signature"
                  @click="previewImage(cellMediaUrl(scope.row, col))"
                />
                <span v-else>-</span>
              </template>
              <template v-else-if="col.qType === 'file'">
                <a
                  v-if="cellMediaUrl(scope.row, col)"
                  :href="cellMediaUrl(scope.row, col)"
                  target="_blank"
                  rel="noopener"
                >{{ cellFileName(scope.row, col) || '附件' }}</a>
                <span v-else>-</span>
              </template>
              <span v-else>{{ cellDisplay(scope.row, col) || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right" align="center">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="goAnswerDetail(scope.row.answerId)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else-if="!matrixLoading" description="暂无答卷" :image-size="60" />
        <pagination
          v-show="matrixTotal > 0"
          :total="matrixTotal"
          :page.sync="matrixPageNum"
          :limit.sync="matrixPageSize"
          :page-sizes="[10, 20, 30, 50]"
          @pagination="loadMatrix"
        />
      </div>
    </el-card>

    <el-dialog
      :title="agreeDialog.title || '协议详情'"
      :visible.sync="agreeDialog.open"
      width="720px"
      append-to-body
      class="agree-dialog"
    >
      <div v-if="agreeDialog.open" class="agree-view">
        <div class="agree-meta">
          <span>答卷 {{ agreeDialog.label }}</span>
          <span v-if="agreeDialog.submitTime"> · {{ agreeDialog.submitTime }}</span>
          <el-tag size="mini" :type="agreeDialog.agreed ? 'success' : 'info'" class="ml6">
            {{ agreeDialog.agreed ? (agreeDialog.agreeLabel || '已同意') : '未同意' }}
          </el-tag>
        </div>
        <div class="agree-body" v-html="agreeDialog.content || '<p>暂无协议正文</p>'" />
        <div
          v-for="sig in agreeDialog.signatures"
          :key="sig.questionId"
          class="agree-sign-block"
        >
          <div class="agree-sign-title">{{ sig.title || '手写签名' }}</div>
          <img v-if="sig.url" :src="sig.url" class="agree-sign-img" alt="signature" @click="previewImage(sig.url)" />
          <div v-else class="agree-sign-empty">未签名</div>
        </div>
      </div>
    </el-dialog>

    <el-dialog :visible.sync="imgPreview.open" width="640px" append-to-body title="预览">
      <img v-if="imgPreview.url" :src="imgPreview.url" class="preview-full" alt="preview" />
    </el-dialog>

    <el-card shadow="never" class="mb16" v-if="choiceQuestions.length >= 2">
      <div slot="header">交叉分析</div>
      <el-form :inline="true" size="small" class="mb12">
        <el-form-item label="行题目">
          <el-select v-model="crossQ1" placeholder="选择单选题" style="width:220px" @change="onCrossChange">
            <el-option v-for="q in choiceQuestions" :key="'r'+q.questionId" :label="q.title" :value="q.questionId" :disabled="q.questionId===crossQ2" />
          </el-select>
        </el-form-item>
        <el-form-item label="列题目">
          <el-select v-model="crossQ2" placeholder="选择单选题" style="width:220px" @change="onCrossChange">
            <el-option v-for="q in choiceQuestions" :key="'c'+q.questionId" :label="q.title" :value="q.questionId" :disabled="q.questionId===crossQ1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="crossLoading" :disabled="!crossQ1 || !crossQ2" @click="loadCross">分析</el-button>
        </el-form-item>
      </el-form>
      <p v-if="cross && cross.pairedCount != null" class="sub mb12">有效配对答卷 {{ cross.pairedCount }}</p>
      <el-table v-if="crossTableRows.length" :data="crossTableRows" size="small" border>
        <el-table-column prop="rowLabel" :label="(cross.q1 && cross.q1.title) || '行'" min-width="140" fixed />
        <el-table-column v-for="col in crossColLabels" :key="col.value" :label="col.label" :prop="'c_' + col.value" min-width="90" align="center" />
      </el-table>
      <el-empty v-else-if="crossLoaded" description="暂无交叉数据" :image-size="60" />
    </el-card>

    <el-card v-for="q in (stats.questions || [])" :key="'c-' + q.questionId" shadow="never" class="mb16">
      <div slot="header">
        {{ q.title }}
        <el-tag size="mini">{{ typeLabel(q.qType) }}</el-tag>
        <span v-if="q.answeredCount != null" class="sub">作答 {{ q.answeredCount }}</span>
      </div>
      <template v-if="q.qType === 'matrix_radio'">
        <div v-for="row in (q.rows || [])" :key="row.rowValue" class="mb12">
          <div class="row-title">{{ row.rowLabel }}</div>
          <el-table :data="row.options || []" size="mini">
            <el-table-column label="选项" prop="label" />
            <el-table-column label="票数" prop="count" width="90" />
            <el-table-column label="占比" width="200">
              <template slot-scope="scope">
                <el-progress :percentage="Number(scope.row.percent) || 0" :stroke-width="12" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
      <el-row v-else :gutter="12">
        <el-col :xs="24" :md="14">
          <el-table :data="q.options || []" size="small">
            <el-table-column label="选项" prop="label" />
            <el-table-column label="票数" prop="count" width="100" />
            <el-table-column label="占比" width="220">
              <template slot-scope="scope">
                <el-progress :percentage="Number(scope.row.percent) || 0" :stroke-width="14" />
              </template>
            </el-table-column>
          </el-table>
        </el-col>
        <el-col :xs="24" :md="10">
          <div :ref="'chart-' + q.questionId" class="chart" />
        </el-col>
      </el-row>
    </el-card>

    <el-card v-for="q in (stats.textQuestions || [])" :key="'t-' + q.questionId" shadow="never" class="mb16">
      <div slot="header">
        {{ q.title }}
        <el-tag size="mini" type="info">{{ typeLabel(q.qType) }}</el-tag>
        <span class="sub">共 {{ q.count || 0 }} 条</span>
        <span v-if="q.avg != null" class="sub"> · 均值 {{ q.avg }}</span>
        <span v-if="q.qType === 'nps' && q.npsScore != null" class="sub nps"> · NPS {{ q.npsScore }}</span>
      </div>
      <el-row v-if="q.qType === 'nps'" :gutter="12" class="mb12">
        <el-col :span="6"><div class="nps-metric">推荐者<br><b>{{ q.promoters || 0 }}</b></div></el-col>
        <el-col :span="6"><div class="nps-metric">被动者<br><b>{{ q.passives || 0 }}</b></div></el-col>
        <el-col :span="6"><div class="nps-metric">贬损者<br><b>{{ q.detractors || 0 }}</b></div></el-col>
        <el-col :span="6"><div class="nps-metric">NPS<br><b>{{ q.npsScore != null ? q.npsScore : '-' }}</b></div></el-col>
      </el-row>
      <el-row v-if="(q.distribution || []).length" :gutter="12" class="mb12">
        <el-col :xs="24" :md="14">
          <el-table :data="q.distribution" size="mini" max-height="240">
            <el-table-column label="分值" prop="label" width="100" />
            <el-table-column label="票数" prop="count" width="90" />
            <el-table-column label="占比">
              <template slot-scope="scope">
                <el-progress :percentage="Number(scope.row.percent) || 0" :stroke-width="12" />
              </template>
            </el-table-column>
          </el-table>
        </el-col>
        <el-col :xs="24" :md="10">
          <div :ref="'dist-' + q.questionId" class="chart" />
        </el-col>
      </el-row>
      <div v-if="(q.trends || []).length" :ref="'trend-' + q.questionId" class="chart trend-chart mb12" />
      <el-table :data="sampleRows(q)" size="small" max-height="320">
        <el-table-column type="index" width="50" label="#" />
        <el-table-column label="答案样本（最多 50 条）" prop="value" :show-overflow-tooltip="true" />
      </el-table>
      <el-empty v-if="!(q.samples || []).length" description="暂无填空答案" :image-size="60" />
    </el-card>

    <el-empty v-if="!loading && !(stats.questions || []).length && !(stats.textQuestions || []).length" description="暂无统计数据" />
  </div>
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons')
import { getSurveyStats, getSurveyCrossStats, getSurveyAnswerMatrix, exportSurveyStats } from '@/api/biz/survey'
import { blobValidate } from '@/utils/ruoyi'
import { saveAs } from 'file-saver'

const TYPE_LABELS = {
  radio: '单选',
  checkbox: '多选',
  select: '下拉',
  yesno: '是非',
  image_radio: '图片单选',
  image_checkbox: '图片多选',
  likert: '量表',
  cascade_select: '级联',
  matrix_radio: '矩阵单选',
  input: '填空',
  textarea: '多行填空',
  phone: '手机',
  date: '日期',
  datetime: '日期时间',
  time: '时间',
  rate: '评分',
  nps: 'NPS',
  number: '数字',
  slider: '滑块',
  file: '附件',
  email: '邮箱',
  url: '网址',
  idcard: '身份证',
  agreement: '协议同意',
  signature: '手写签名'
}

export default {
  name: 'BizSurveyStats',
  data() {
    return {
      loading: false,
      exporting: false,
      surveyId: null,
      stats: {},
      charts: {},
      crossQ1: null,
      crossQ2: null,
      cross: null,
      crossLoading: false,
      crossLoaded: false,
      matrixLoading: false,
      matrixValidFlag: '1',
      matrixPageNum: 1,
      matrixPageSize: 20,
      matrixTotal: 0,
      matrixColumns: [],
      matrixRows: [],
      agreeDialog: {
        open: false,
        title: '',
        label: '',
        submitTime: '',
        content: '',
        agreeLabel: '',
        agreed: false,
        signatures: []
      },
      imgPreview: { open: false, url: '' }
    }
  },
  computed: {
    choiceQuestions() {
      const types = ['radio', 'select', 'yesno', 'image_radio', 'likert']
      return (this.stats.questions || []).filter(q => types.includes(q.qType))
    },
    choiceCount() {
      return (this.stats.questions || []).filter(q => q.qType !== 'matrix_radio').length
    },
    convertRateText() {
      const v = this.stats.convertRate
      if (v == null || v === '') return '0%'
      return v + '%'
    },
    crossColLabels() {
      return ((this.cross && this.cross.q2 && this.cross.q2.options) || []).map(o => ({
        value: String(o.value),
        label: o.label || o.value
      }))
    },
    crossTableRows() {
      if (!this.cross || !this.cross.q1) return []
      const rows = (this.cross.q1.options || []).map(o => {
        const r = { rowLabel: o.label || o.value, rowValue: String(o.value) }
        this.crossColLabels.forEach(c => { r['c_' + c.value] = 0 })
        return r
      })
      const map = {}
      rows.forEach(r => { map[r.rowValue] = r })
      ;(this.cross.cells || []).forEach(cell => {
        const rv = String(cell.rowValue)
        const cv = String(cell.colValue)
        if (!map[rv]) {
          const r = { rowLabel: cell.rowLabel || rv, rowValue: rv }
          this.crossColLabels.forEach(c => { r['c_' + c.value] = 0 })
          map[rv] = r
          rows.push(r)
        }
        map[rv]['c_' + cv] = Number(cell.count) || 0
      })
      return rows
    }
  },
  created() {
    this.surveyId = this.$route.params.surveyId
    this.load()
  },
  beforeDestroy() {
    this.disposeCharts()
    window.removeEventListener('resize', this.resizeCharts)
  },
  methods: {
    typeLabel(t) {
      return TYPE_LABELS[t] || t
    },
    goBack() { this.$router.push('/biz/survey') },
    goAnswers() {
      this.$router.push('/biz/survey-answers/index/' + this.surveyId)
    },
    goAnswerDetail(answerId) {
      this.$router.push({
        path: '/biz/survey-answers/index/' + this.surveyId,
        query: { answerId }
      })
    },
    cellOf(row, col) {
      if (!row || !col || !row.cells) return null
      return row.cells[String(col.questionId)] || null
    },
    cellDisplay(row, col) {
      const c = this.cellOf(row, col)
      return c && c.display != null ? c.display : ''
    },
    cellMediaUrl(row, col) {
      const c = this.cellOf(row, col)
      if (!c || !c.url) return ''
      const path = String(c.url)
      if (path.startsWith('http') || path.startsWith('data:')) return path
      return process.env.VUE_APP_BASE_API + path
    },
    cellFileName(row, col) {
      const c = this.cellOf(row, col)
      return (c && (c.fileName || c.display)) || '附件'
    },
    mediaUrlFromCell(cell) {
      if (!cell || !cell.url) return ''
      const path = String(cell.url)
      if (path.startsWith('http') || path.startsWith('data:')) return path
      return process.env.VUE_APP_BASE_API + path
    },
    openAgreement(row, col) {
      const cell = this.cellOf(row, col)
      const agreed = cell && (cell.raw === '1' || cell.display === '已同意')
      const signatures = (col.boundSignatures || []).map(sig => {
        const sc = row.cells && row.cells[String(sig.questionId)]
        return {
          questionId: sig.questionId,
          title: sig.title,
          url: this.mediaUrlFromCell(sc)
        }
      })
      this.agreeDialog = {
        open: true,
        title: col.title || '协议详情',
        label: row.label || ('#' + row.answerId),
        submitTime: row.submitTime || '',
        content: col.content || '',
        agreeLabel: col.agreeLabel || '我已阅读并同意',
        agreed: !!agreed,
        signatures
      }
    },
    previewImage(url) {
      if (!url) return
      this.imgPreview = { open: true, url }
    },
    onMatrixFilterChange() {
      this.matrixPageNum = 1
      this.loadMatrix()
    },
    sampleRows(q) {
      return (q.samples || []).map(v => ({ value: v }))
    },
    loadMatrix() {
      if (!this.surveyId) return
      this.matrixLoading = true
      getSurveyAnswerMatrix(this.surveyId, {
        pageNum: this.matrixPageNum,
        pageSize: this.matrixPageSize,
        validFlag: this.matrixValidFlag
      }).then(res => {
        const data = res.data || {}
        this.matrixTotal = Number(data.total) || 0
        this.matrixColumns = data.columns || []
        this.matrixRows = data.rows || []
      }).finally(() => { this.matrixLoading = false })
    },
    disposeCharts() {
      Object.keys(this.charts).forEach(k => {
        if (this.charts[k]) this.charts[k].dispose()
      })
      this.charts = {}
    },
    resizeCharts() {
      Object.keys(this.charts).forEach(k => this.charts[k] && this.charts[k].resize())
    },
    initChart(key, el) {
      const node = Array.isArray(el) ? el[0] : el
      if (!node) return null
      if (this.charts[key]) this.charts[key].dispose()
      const chart = echarts.init(node, 'macarons')
      this.charts[key] = chart
      return chart
    },
    renderCharts() {
      this.$nextTick(() => {
        this.disposeCharts()
        if ((this.stats.dailyTrends || []).length) {
          const chart = this.initChart('daily', this.$refs.dailyChart)
          if (chart) {
            const dates = this.stats.dailyTrends.map(t => t.date)
            const counts = this.stats.dailyTrends.map(t => Number(t.count) || 0)
            chart.setOption({
              tooltip: { trigger: 'axis' },
              grid: { left: 40, right: 20, top: 30, bottom: 30 },
              xAxis: { type: 'category', data: dates },
              yAxis: { type: 'value', minInterval: 1 },
              series: [{ name: '答卷数', type: 'bar', data: counts, barMaxWidth: 36, itemStyle: { color: '#2b6de5' } }]
            })
          }
        }
        if ((this.stats.channels || []).length) {
          const chart = this.initChart('channel', this.$refs.channelChart)
          if (chart) {
            chart.setOption({
              tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
              series: [{
                type: 'pie',
                radius: ['40%', '68%'],
                center: ['50%', '50%'],
                data: this.stats.channels.map(c => ({ name: c.channelCode, value: Number(c.count) || 0 })),
                label: { formatter: '{b}\n{d}%' }
              }]
            })
          }
        }
        ;(this.stats.questions || []).filter(q => q.qType !== 'matrix_radio').forEach(q => {
          const chart = this.initChart('chart-' + q.questionId, this.$refs['chart-' + q.questionId])
          if (!chart) return
          const data = (q.options || []).map(o => ({ name: o.label || o.value, value: Number(o.count) || 0 }))
          chart.setOption({
            tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
            legend: { type: 'scroll', bottom: 0 },
            series: [{
              type: 'pie',
              radius: ['35%', '62%'],
              center: ['50%', '45%'],
              data,
              label: { formatter: '{b}\n{d}%' }
            }]
          })
        })
        ;(this.stats.textQuestions || []).forEach(q => {
          if ((q.distribution || []).length) {
            const chart = this.initChart('dist-' + q.questionId, this.$refs['dist-' + q.questionId])
            if (chart) {
              chart.setOption({
                tooltip: { trigger: 'axis' },
                grid: { left: 40, right: 16, top: 24, bottom: 28 },
                xAxis: { type: 'category', data: q.distribution.map(d => d.label || d.value) },
                yAxis: { type: 'value', minInterval: 1 },
                series: [{
                  type: 'bar',
                  data: q.distribution.map(d => Number(d.count) || 0),
                  barMaxWidth: 28,
                  itemStyle: { color: '#36cfc9' }
                }]
              })
            }
          }
          if ((q.qType === 'rate' || q.qType === 'nps' || q.qType === 'slider') && (q.trends || []).length) {
            const chart = this.initChart('trend-' + q.questionId, this.$refs['trend-' + q.questionId])
            if (!chart) return
            chart.setOption({
              tooltip: { trigger: 'axis' },
              grid: { left: 40, right: 20, top: 30, bottom: 30 },
              xAxis: { type: 'category', data: q.trends.map(t => t.date) },
              yAxis: { type: 'value', scale: true },
              series: [{
                name: '日均',
                type: 'line',
                smooth: true,
                data: q.trends.map(t => Number(t.avg) || 0),
                areaStyle: { opacity: 0.08 }
              }]
            })
          }
        })
        window.removeEventListener('resize', this.resizeCharts)
        window.addEventListener('resize', this.resizeCharts)
      })
    },
    load() {
      this.loading = true
      getSurveyStats(this.surveyId).then(res => {
        this.stats = res.data || {}
        this.renderCharts()
        if (this.choiceQuestions.length >= 2 && !this.crossQ1) {
          this.crossQ1 = this.choiceQuestions[0].questionId
          this.crossQ2 = this.choiceQuestions[1].questionId
          this.loadCross()
        }
      }).finally(() => { this.loading = false })
      this.loadMatrix()
    },
    onCrossChange() {
      this.cross = null
      this.crossLoaded = false
    },
    loadCross() {
      if (!this.crossQ1 || !this.crossQ2 || this.crossQ1 === this.crossQ2) return
      this.crossLoading = true
      getSurveyCrossStats(this.surveyId, this.crossQ1, this.crossQ2).then(res => {
        this.cross = res.data || {}
        this.crossLoaded = true
      }).finally(() => { this.crossLoading = false })
    },
    handleExportStats() {
      this.$modal.confirm('确认导出当前问卷的统计汇总 Excel？若已做交叉分析将一并导出。').then(() => {
        this.exporting = true
        const q = {}
        if (this.crossQ1 && this.crossQ2 && this.crossQ1 !== this.crossQ2) {
          q.q1 = this.crossQ1
          q.q2 = this.crossQ2
        }
        return exportSurveyStats(this.surveyId, q)
      }).then(async data => {
        if (blobValidate(data)) {
          saveAs(new Blob([data]), (this.stats.surveyName || 'survey') + '-统计汇总.xlsx')
        } else {
          this.$modal.msgError('导出失败')
        }
      }).catch(() => {}).finally(() => { this.exporting = false })
    }
  }
}
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.head-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.mb16 { margin-bottom: 16px; }
.mb12 { margin-bottom: 12px; }
.mt8 { margin-top: 8px; }
.metric-card { margin-bottom: 0; }
.stat { font-size: 13px; color: #64748b; }
.stat b { font-size: 22px; color: #0f172a; margin-left: 6px; font-weight: 700; }
.sub { margin-left: 8px; color: #909399; font-size: 12px; font-weight: normal; }
.row-title { font-weight: 600; margin: 0 0 8px; color: #303133; }
.chart { height: 260px; width: 100%; min-height: 220px; }
.trend-chart { height: 220px; min-height: 180px; }
.nps { color: #1677ff; font-weight: 600; }
.nps-metric {
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px;
  text-align: center;
  color: #64748b;
  font-size: 12px;
}
.nps-metric b { display: block; margin-top: 4px; font-size: 20px; color: #0f172a; }
.matrix-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.matrix-tools { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.matrix-table { width: 100%; }
.ml6 { margin-left: 6px; }
.cell-agree { margin-left: 6px; color: #64748b; font-size: 12px; }
.sig-thumb {
  max-width: 88px;
  max-height: 40px;
  object-fit: contain;
  cursor: pointer;
  vertical-align: middle;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
}
.agree-view { max-height: 70vh; overflow: auto; }
.agree-meta { font-size: 13px; color: #64748b; margin-bottom: 12px; }
.agree-body {
  padding: 12px 14px;
  background: #fafafa;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
}
.agree-body >>> p { margin: 0 0 8px; }
.agree-sign-block {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #e5e7eb;
}
.agree-sign-title { font-weight: 650; font-size: 13px; margin-bottom: 8px; color: #0f172a; }
.agree-sign-img {
  display: block;
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: zoom-in;
}
.agree-sign-empty {
  padding: 20px;
  text-align: center;
  color: #94a3b8;
  background: #f8fafc;
  border: 1px dashed #e5e7eb;
  border-radius: 8px;
}
.preview-full { display: block; max-width: 100%; margin: 0 auto; }
</style>
