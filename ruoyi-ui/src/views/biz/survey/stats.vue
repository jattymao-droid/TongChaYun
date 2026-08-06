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
import { getSurveyStats, getSurveyCrossStats, exportSurveyStats } from '@/api/biz/survey'
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
  idcard: '身份证'
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
      crossLoaded: false
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
    sampleRows(q) {
      return (q.samples || []).map(v => ({ value: v }))
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
</style>
