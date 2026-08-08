<template>
  <div class="wb" v-loading="loading">
    <header class="wb-toolbar">
      <h1>{{ currentTitle }}</h1>
      <div class="toolbar-right" v-if="activeNav === 'recent'">
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
      <div class="toolbar-right" v-else-if="activeNav !== 'notify'">
        <el-select v-model="statusFilter" size="small" clearable placeholder="筛选状态" class="status-filter">
          <el-option label="全部状态" value="" />
          <el-option label="已发布" value="1" />
          <el-option label="未发布" value="0" />
        </el-select>
        <div class="view-toggle">
          <button type="button" :class="{ on: viewMode === 'grid' }" @click="viewMode = 'grid'" title="卡片">
            <i class="el-icon-menu"></i>
          </button>
          <button type="button" :class="{ on: viewMode === 'list' }" @click="viewMode = 'list'" title="列表">
            <i class="el-icon-s-operation"></i>
          </button>
        </div>
      </div>
      <div class="toolbar-right" v-else>
        <div class="notify-filters">
          <button type="button" :class="{ on: notifyFilter === '' }" @click="setNotifyFilter('')">全部</button>
          <button type="button" :class="{ on: notifyFilter === '0' }" @click="setNotifyFilter('0')">
            未读<span v-if="notifyUnread" class="n">{{ notifyUnread > 99 ? '99+' : notifyUnread }}</span>
          </button>
          <button type="button" :class="{ on: notifyFilter === '1' }" @click="setNotifyFilter('1')">已读</button>
        </div>
        <el-button size="small" plain icon="el-icon-check" :disabled="!notifyUnread" @click="markAll">全部已读</el-button>
      </div>
    </header>

    <!-- 仪表盘 -->
    <section v-if="activeNav === 'recent'" class="dash">
      <div class="stat-grid">
        <div
          v-for="card in statCards"
          :key="card.key"
          class="stat-card"
          :class="'tone-' + card.tone"
          @click="card.route && $router.push(card.route)"
        >
          <div class="stat-icon"><i :class="card.icon"></i></div>
          <div class="stat-body">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value">
              <count-to :start-val="0" :end-val="card.value" :duration="1400" :decimals="0" />
            </div>
            <div class="stat-sub" v-if="card.sub">{{ card.sub }}</div>
          </div>
        </div>
      </div>

      <div class="chart-row">
        <div class="panel">
          <div class="panel-head">
            <h2>项目状态分布</h2>
            <span class="panel-hint">查询 + 问卷</span>
          </div>
          <div ref="statusChart" class="chart-box"></div>
        </div>
        <div class="panel">
          <div class="panel-head">
            <h2>业务数据概览</h2>
            <span class="panel-hint">累计</span>
          </div>
          <div ref="trafficChart" class="chart-box"></div>
        </div>
      </div>

      <div class="bottom-row">
        <div class="panel recent-panel">
          <div class="panel-head">
            <h2>最近修改</h2>
            <el-button type="text" size="mini" @click="$router.push('/biz/query')">查看全部</el-button>
          </div>
          <div v-if="!filteredItems.length && !loading" class="mini-empty">暂无项目，可从左侧新建</div>
          <div v-else class="recent-list">
            <article
              v-for="item in filteredItems.slice(0, 8)"
              :key="item.key"
              class="recent-row"
              @click="openItem(item)"
            >
              <span class="kind-pill" :class="'k-' + item.kind">{{ item.kindLabel }}</span>
              <div class="recent-main">
                <div class="recent-title">{{ item.title }}</div>
                <div class="recent-meta">{{ item.metaLeft }}</div>
              </div>
              <span class="status" :class="'st-' + item.status">
                <i class="status-dot"></i>{{ item.statusLabel }}
              </span>
            </article>
          </div>
        </div>

        <div class="panel side-panel">
          <div class="panel-head">
            <h2>答卷通知</h2>
            <el-button type="text" size="mini" @click="$router.push('/biz/notify')">
              {{ overview.unreadNotify > 0 ? ('未读 ' + overview.unreadNotify) : '全部' }}
            </el-button>
          </div>
          <div v-if="!(overview.recentNotifies || []).length" class="mini-empty">暂无通知</div>
          <div v-else class="notify-mini">
            <div
              v-for="row in (overview.recentNotifies || []).slice(0, 6)"
              :key="row.notifyId"
              class="notify-mini-row"
              :class="{ unread: row.readFlag !== '1' }"
              @click="goAnswers(row)"
            >
              <span v-if="row.readFlag !== '1'" class="dot-new"></span>
              <div class="nm-body">
                <div class="nm-title">{{ row.title || '收到新答卷' }}</div>
                <div class="nm-meta">{{ row.surveyName || '问卷' }} · {{ relTime(row.createTime) }}</div>
              </div>
            </div>
          </div>

          <div class="quick-actions">
            <button type="button" v-hasPermi="['biz:query:add']" @click="goCreateQuery">
              <i class="el-icon-plus"></i>新建查询
            </button>
            <button type="button" v-hasPermi="['biz:survey:add']" @click="goTemplateSurvey">
              <i class="el-icon-document-copy"></i>问卷模板
            </button>
            <button type="button" @click="$router.push('/biz/notify')">
              <i class="el-icon-bell"></i>答卷通知
            </button>
          </div>
        </div>
      </div>
    </section>

    <section v-else-if="activeNav === 'notify'" class="notify-panel" v-loading="notifyLoading">
      <div class="notify-summary" v-if="notifyTotal > 0 || notifyUnread > 0">
        <span>共 <b>{{ notifyTotal }}</b> 条通知</span>
        <span class="sep">·</span>
        <span>未读 <b class="hot">{{ notifyUnread }}</b></span>
      </div>

      <div v-if="!notifyLoading && !notifyList.length" class="empty-state notify-empty">
        <div class="empty-icon"><i class="el-icon-bell"></i></div>
        <p>{{ notifyFilter === '0' ? '没有未读通知' : (notifyFilter === '1' ? '暂无已读通知' : '暂无答卷通知') }}</p>
        <div class="empty-sub">问卷有新答卷时，会在这里提醒你及时查看</div>
      </div>

      <div v-else class="notify-list">
        <article
          v-for="row in notifyList"
          :key="row.notifyId"
          class="notify-card"
          :class="{ unread: row.readFlag !== '1' }"
          @click="goAnswers(row)"
        >
          <div class="notify-mark"><i class="el-icon-document"></i></div>
          <div class="notify-main">
            <div class="notify-top">
              <h3 class="notify-title">
                <span v-if="row.readFlag !== '1'" class="dot-new"></span>
                {{ row.title || '收到新答卷' }}
              </h3>
              <el-tag size="mini" :type="row.readFlag === '1' ? 'info' : ''" effect="plain">
                {{ row.readFlag === '1' ? '已读' : '未读' }}
              </el-tag>
            </div>
            <p class="notify-content" v-if="row.content">{{ row.content }}</p>
            <div class="notify-meta">
              <span class="chip"><i class="el-icon-tickets"></i>{{ row.surveyName || '问卷' }}</span>
              <span class="chip" v-if="row.answerId"><i class="el-icon-s-order"></i>答卷 #{{ row.answerId }}</span>
              <span class="chip muted"><i class="el-icon-time"></i>{{ relTime(row.createTime) }} · {{ row.createTime || '' }}</span>
            </div>
          </div>
          <div class="notify-actions" @click.stop>
            <el-button type="primary" size="mini" plain @click="goAnswers(row)">查看答卷</el-button>
            <el-button v-if="row.readFlag !== '1'" size="mini" @click="markOne(row)">标为已读</el-button>
          </div>
        </article>
      </div>

      <pagination
        v-show="notifyTotal > 0"
        :total="notifyTotal"
        :page.sync="notifyQuery.pageNum"
        :limit.sync="notifyQuery.pageSize"
        @pagination="loadNotifies"
      />
    </section>

    <section v-else>
      <div v-if="!filteredItems.length && !loading" class="empty-state">
        <i class="el-icon-folder-opened"></i>
        <p>还没有项目</p>
        <div class="empty-actions">
          <el-button type="primary" size="small" icon="el-icon-plus" v-hasPermi="['biz:query:add']" @click="goCreateQuery">新建查询</el-button>
          <el-button size="small" icon="el-icon-document" v-hasPermi="['biz:query:add']" @click="goTemplateQuery">查询模板</el-button>
          <el-button size="small" icon="el-icon-document-copy" v-hasPermi="['biz:survey:add']" @click="goTemplateSurvey">问卷模板</el-button>
        </div>
      </div>

      <div v-else-if="viewMode === 'grid'" class="card-grid">
        <article
          v-for="item in filteredItems"
          :key="item.key"
          class="proj-card"
          :class="'kind-' + item.kind"
          @click="openItem(item)"
        >
          <div class="card-cover">
            <span class="kind-tag">{{ item.kindLabel }}</span>
            <span v-if="item.countBadge" class="count-badge">{{ item.countBadge }}</span>
            <div class="cover-art">
              <i :class="item.kind === 'query' ? 'el-icon-search' : 'el-icon-edit-outline'"></i>
            </div>
          </div>
          <div class="card-body">
            <h3 :title="item.title">{{ item.title }}</h3>
            <div class="card-meta">
              <span class="meta-left">{{ item.metaLeft }}</span>
              <span class="status" :class="'st-' + item.status">
                <i class="status-dot"></i>{{ item.statusLabel }}
              </span>
            </div>
          </div>
          <div class="card-actions" @click.stop>
            <el-button type="text" size="mini" v-if="item.kind === 'survey'" @click="openDesign(item)">设计</el-button>
            <el-button type="text" size="mini" @click="openItem(item)">设置</el-button>
            <el-button type="text" size="mini" v-if="item.publicCode" @click="copyItemLink(item)">链接</el-button>
          </div>
        </article>
      </div>

      <el-table v-else :data="filteredItems" size="small" class="list-table" @row-click="openItem">
        <el-table-column label="类型" width="80">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.kind === 'query' ? '' : 'success'" effect="plain">{{ scope.row.kindLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="名称" prop="title" min-width="180" :show-overflow-tooltip="true" />
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <span class="status" :class="'st-' + scope.row.status"><i class="status-dot"></i>{{ scope.row.statusLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数据" prop="metaLeft" width="140" />
        <el-table-column label="操作" width="140" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" v-if="scope.row.kind === 'survey'" @click.stop="openDesign(scope.row)">设计</el-button>
            <el-button type="text" size="mini" @click.stop="openItem(scope.row)">设置</el-button>
            <el-button type="text" size="mini" v-if="scope.row.publicCode" @click.stop="copyItemLink(scope.row)">链接</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import CountTo from 'vue-count-to'
import { getBizOverview } from '@/api/biz/dashboard'
import { listBizNotify, readBizNotify, readAllBizNotify, unreadBizNotifyCount } from '@/api/biz/notify'

export default {
  name: 'BizWorkbench',
  components: { CountTo },
  data() {
    return {
      loading: false,
      overview: {},
      statusFilter: '',
      viewMode: 'grid',
      notifyLoading: false,
      notifyList: [],
      notifyTotal: 0,
      notifyUnread: 0,
      notifyFilter: '',
      notifyQuery: {
        pageNum: 1,
        pageSize: 10,
        readFlag: undefined
      },
      charts: {}
    }
  },
  computed: {
    activeNav() {
      const p = this.$route.path || ''
      if (p.indexOf('/biz/notify') === 0) return 'notify'
      if (p.indexOf('/biz/query') === 0) return 'query'
      if (p.indexOf('/biz/survey') === 0) return 'survey'
      return 'recent'
    },
    currentTitle() {
      const map = { recent: '仪表盘', query: '我的查询', survey: '我的问卷', notify: '答卷通知' }
      return map[this.activeNav] || (this.$route.meta && this.$route.meta.title) || '工作台'
    },
    allItems() {
      const queries = (this.overview.recentQueries || []).map(q => ({
        key: 'q-' + q.queryId,
        kind: 'query',
        kindLabel: '查询',
        id: q.queryId,
        title: q.queryName || '未命名查询',
        status: q.status || '0',
        statusLabel: this.statusText(q.status),
        publicCode: q.publicCode,
        countBadge: q.searchCount > 0 ? q.searchCount : null,
        metaLeft: (q.rowCount || 0) + ' 行 · ' + this.relTime(q.updateTime || q.createTime),
        sortTime: q.updateTime || q.createTime || ''
      }))
      const surveys = (this.overview.recentSurveys || []).map(s => ({
        key: 's-' + s.surveyId,
        kind: 'survey',
        kindLabel: '问卷',
        id: s.surveyId,
        title: s.surveyName || '未命名问卷',
        status: s.status || '0',
        statusLabel: this.statusText(s.status),
        publicCode: s.publicCode,
        countBadge: s.answerCount > 0 ? s.answerCount : null,
        metaLeft: (s.answerCount || 0) + ' 份 · ' + this.relTime(s.updateTime || s.createTime),
        sortTime: s.updateTime || s.createTime || ''
      }))
      return queries.concat(surveys).sort((a, b) => String(b.sortTime).localeCompare(String(a.sortTime)))
    },
    filteredItems() {
      let list = this.allItems
      if (this.activeNav === 'query') list = list.filter(i => i.kind === 'query')
      if (this.activeNav === 'survey') list = list.filter(i => i.kind === 'survey')
      if (this.statusFilter !== '' && this.statusFilter != null) {
        list = list.filter(i => String(i.status) === String(this.statusFilter))
      }
      // 仪表盘最近修改只展示最近 30 条；「我的查询/问卷」展示全部
      if (this.activeNav === 'recent') list = list.slice(0, 30)
      return list
    },
    statCards() {
      const o = this.overview || {}
      const todayUv = (Number(o.queryUvToday) || 0) + (Number(o.surveyUvToday) || 0)
      return [
        {
          key: 'query',
          label: '查询项目',
          value: Number(o.queryTotal) || 0,
          sub: (Number(o.queryPublished) || 0) + ' 已发布',
          icon: 'el-icon-search',
          tone: 'blue',
          route: '/biz/query'
        },
        {
          key: 'survey',
          label: '问卷项目',
          value: Number(o.surveyTotal) || 0,
          sub: (Number(o.surveyPublished) || 0) + ' 已发布',
          icon: 'el-icon-edit-outline',
          tone: 'teal',
          route: '/biz/survey'
        },
        {
          key: 'answer',
          label: '答卷总数',
          value: Number(o.answerCount) || 0,
          sub: (Number(o.rowCount) || 0) + ' 查询数据行',
          icon: 'el-icon-document',
          tone: 'amber',
          route: '/biz/survey'
        },
        {
          key: 'uv',
          label: '今日访问 UV',
          value: todayUv,
          sub: '浏览 ' + ((Number(o.queryViews) || 0) + (Number(o.surveyViews) || 0)),
          icon: 'el-icon-view',
          tone: 'violet',
          route: null
        },
        {
          key: 'notify',
          label: '未读通知',
          value: Number(o.unreadNotify) || 0,
          sub: '检索 ' + (Number(o.querySearches) || 0) + ' 次',
          icon: 'el-icon-bell',
          tone: 'rose',
          route: '/biz/notify'
        }
      ]
    }
  },
  watch: {
    activeNav: {
      immediate: true,
      handler(nav) {
        if (nav === 'notify') this.refreshNotifies()
        else this.load()
      }
    }
  },
  beforeDestroy() {
    this.disposeCharts()
    window.removeEventListener('resize', this.onResize)
  },
  mounted() {
    window.addEventListener('resize', this.onResize)
  },
  methods: {
    statusText(status) {
      if (status === '1') return '进行中'
      if (status === '2') return '已停用'
      return '未发布'
    },
    relTime(t) {
      if (!t) return '刚刚'
      const d = new Date(String(t).replace(/-/g, '/'))
      if (isNaN(d.getTime())) return String(t).slice(0, 10)
      const diff = Date.now() - d.getTime()
      const day = 86400000
      if (diff < day) return '今天'
      if (diff < 7 * day) return Math.floor(diff / day) + '天前'
      if (diff < 30 * day) return Math.floor(diff / (7 * day)) + '周前'
      if (diff < 365 * day) return Math.floor(diff / (30 * day)) + '月前'
      return Math.floor(diff / (365 * day)) + '年前'
    },
    load() {
      this.loading = true
      getBizOverview().then(res => {
        this.overview = res.data || {}
        this.$nextTick(() => {
          if (this.activeNav === 'recent') this.renderCharts()
        })
      }).catch(() => {
        this.overview = {}
      }).finally(() => { this.loading = false })
    },
    disposeCharts() {
      Object.keys(this.charts).forEach(k => {
        if (this.charts[k]) this.charts[k].dispose()
      })
      this.charts = {}
    },
    onResize() {
      Object.keys(this.charts).forEach(k => this.charts[k] && this.charts[k].resize())
    },
    initChart(key, el) {
      if (!el) return null
      if (this.charts[key]) this.charts[key].dispose()
      const chart = echarts.init(el)
      this.charts[key] = chart
      return chart
    },
    renderCharts() {
      if (this.activeNav !== 'recent') return
      const items = this.allItems
      const statusMap = { '1': 0, '0': 0, '2': 0 }
      items.forEach(i => {
        const s = String(i.status)
        if (statusMap[s] != null) statusMap[s] += 1
        else statusMap['0'] += 1
      })
      const statusChart = this.initChart('status', this.$refs.statusChart)
      if (statusChart) {
        const hasData = statusMap['1'] + statusMap['0'] + statusMap['2'] > 0
        statusChart.setOption({
          color: ['#00b42a', '#ff7d00', '#86909c'],
          tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
          legend: { bottom: 0, left: 'center', itemWidth: 10, itemHeight: 10, textStyle: { color: '#64748b', fontSize: 12 } },
          series: [{
            type: 'pie',
            radius: ['42%', '68%'],
            center: ['50%', '46%'],
            avoidLabelOverlap: true,
            itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
            label: { show: false },
            data: hasData ? [
              { name: '进行中', value: statusMap['1'] },
              { name: '未发布', value: statusMap['0'] },
              { name: '已停用', value: statusMap['2'] }
            ] : [{ name: '暂无数据', value: 1, itemStyle: { color: '#e2e8f0' } }]
          }]
        })
      }

      const o = this.overview || {}
      const trafficChart = this.initChart('traffic', this.$refs.trafficChart)
      if (trafficChart) {
        const cats = ['查询浏览', '查询检索', '问卷浏览', '答卷数']
        const vals = [
          Number(o.queryViews) || 0,
          Number(o.querySearches) || 0,
          Number(o.surveyViews) || 0,
          Number(o.answerCount) || 0
        ]
        trafficChart.setOption({
          color: ['#1d4ed8'],
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          grid: { left: 12, right: 16, top: 24, bottom: 8, containLabel: true },
          xAxis: {
            type: 'category',
            data: cats,
            axisTick: { show: false },
            axisLine: { lineStyle: { color: '#e2e8f0' } },
            axisLabel: { color: '#64748b', fontSize: 12 }
          },
          yAxis: {
            type: 'value',
            minInterval: 1,
            splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
            axisLabel: { color: '#94a3b8' }
          },
          series: [{
            type: 'bar',
            barWidth: 28,
            data: vals,
            itemStyle: {
              borderRadius: [6, 6, 0, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#3b82f6' },
                { offset: 1, color: '#1d4ed8' }
              ])
            }
          }]
        })
      }
    },
    goCreateQuery() { this.$router.push({ path: '/biz/query', query: { action: 'create' } }) },
    goTemplateQuery() { this.$router.push({ path: '/biz/query', query: { action: 'template' } }) },
    goTemplateSurvey() { this.$router.push({ path: '/biz/survey', query: { action: 'template' } }) },
    openItem(item) {
      if (item.kind === 'query') this.$router.push('/biz/query-setup/index/' + item.id)
      else this.$router.push('/biz/survey-setup/index/' + item.id)
    },
    openDesign(item) {
      if (!item || item.kind !== 'survey') return
      this.$router.push('/biz/survey-design/index/' + item.id)
    },
    copyItemLink(item) {
      const path = item.kind === 'query' ? ('/q/' + item.publicCode) : ('/s/' + item.publicCode)
      const text = window.location.origin + path
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => this.$modal.msgSuccess('链接已复制'))
      } else {
        this.$modal.msgSuccess(text)
      }
    },
    setNotifyFilter(val) {
      this.notifyFilter = val
      this.notifyQuery.pageNum = 1
      this.notifyQuery.readFlag = val === '' ? undefined : val
      this.loadNotifies()
    },
    refreshNotifies() {
      this.loadUnreadCount()
      this.loadNotifies()
    },
    loadUnreadCount() {
      unreadBizNotifyCount().then(res => {
        this.notifyUnread = Number(res.data) || 0
      }).catch(() => { this.notifyUnread = 0 })
    },
    loadNotifies() {
      this.notifyLoading = true
      const params = {
        pageNum: this.notifyQuery.pageNum,
        pageSize: this.notifyQuery.pageSize
      }
      if (this.notifyQuery.readFlag !== undefined && this.notifyQuery.readFlag !== '') {
        params.readFlag = this.notifyQuery.readFlag
      }
      listBizNotify(params).then(res => {
        this.notifyList = res.rows || []
        this.notifyTotal = res.total || 0
      }).catch(() => {
        this.notifyList = []
        this.notifyTotal = 0
      }).finally(() => { this.notifyLoading = false })
    },
    goAnswers(row) {
      const go = () => {
        const q = row.answerId ? ('?answerId=' + row.answerId) : ''
        this.$router.push('/biz/survey-answers/index/' + row.surveyId + q)
      }
      if (row.readFlag !== '1') {
        readBizNotify(row.notifyId).then(() => {
          row.readFlag = '1'
          this.notifyUnread = Math.max(0, this.notifyUnread - 1)
          if (this.overview && this.overview.unreadNotify != null) {
            this.overview.unreadNotify = Math.max(0, (Number(this.overview.unreadNotify) || 0) - 1)
          }
        }).finally(go)
      } else {
        go()
      }
    },
    markOne(row) {
      readBizNotify(row.notifyId).then(() => {
        row.readFlag = '1'
        this.notifyUnread = Math.max(0, this.notifyUnread - 1)
        this.$modal.msgSuccess('已标为已读')
        if (this.notifyFilter === '0') this.loadNotifies()
      })
    },
    markAll() {
      readAllBizNotify().then(() => {
        this.$modal.msgSuccess('已全部标为已读')
        this.notifyUnread = 0
        this.refreshNotifies()
      })
    }
  }
}
</script>

<style scoped>
.wb {
  --wb-blue: var(--biz-accent);
  --wb-blue-soft: var(--biz-accent-soft);
  --wb-bg: var(--biz-bg);
  --wb-line: var(--biz-line);
  --wb-text: var(--biz-text);
  --wb-muted: var(--biz-muted-soft);
  min-height: calc(100vh - 90px);
  padding: 22px 28px 32px;
  background: var(--wb-bg);
  color: var(--wb-text);
}
.wb-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.wb-toolbar h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.toolbar-right { display: flex; align-items: center; gap: 10px; }
.status-filter { width: 120px; }
.view-toggle {
  display: inline-flex;
  border: 1px solid var(--wb-line);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.view-toggle button {
  width: 34px;
  height: 32px;
  border: 0;
  background: transparent;
  color: #86909c;
  cursor: pointer;
}
.view-toggle button.on {
  background: var(--wb-blue-soft);
  color: var(--wb-blue);
}

/* Dashboard */
.dash { display: flex; flex-direction: column; gap: 16px; }
.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}
.stat-card {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 18px 16px;
  background: #fff;
  border: 1px solid var(--wb-line);
  border-radius: 14px;
  cursor: pointer;
  transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(31, 35, 41, 0.07);
  border-color: #c7d7fe;
}
.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.tone-blue .stat-icon { background: #eff6ff; color: #1d4ed8; }
.tone-teal .stat-icon { background: #ecfdf5; color: #0f766e; }
.tone-amber .stat-icon { background: #fff7ed; color: #c2410c; }
.tone-violet .stat-icon { background: #f5f3ff; color: #6d28d9; }
.tone-rose .stat-icon { background: #fff1f2; color: #e11d48; }
.stat-label { font-size: 13px; color: #64748b; margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.15; color: #0f172a; letter-spacing: -0.03em; }
.stat-sub { margin-top: 6px; font-size: 12px; color: #94a3b8; }
.chart-row, .bottom-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.bottom-row { grid-template-columns: 1.4fr 1fr; }
.panel {
  background: #fff;
  border: 1px solid var(--wb-line);
  border-radius: 14px;
  padding: 16px 18px 14px;
  min-width: 0;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.panel-head h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}
.panel-hint { font-size: 12px; color: #94a3b8; }
.chart-box { height: 260px; width: 100%; }
.mini-empty {
  padding: 36px 12px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}
.recent-list { display: flex; flex-direction: column; gap: 6px; }
.recent-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background .15s ease;
}
.recent-row:hover { background: #f8fafc; }
.kind-pill {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  background: #eff6ff;
  color: #1d4ed8;
}
.kind-pill.k-survey { background: #ecfdf5; color: #0f766e; }
.recent-main { flex: 1; min-width: 0; }
.recent-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recent-meta { margin-top: 2px; font-size: 12px; color: #94a3b8; }
.notify-mini { display: flex; flex-direction: column; gap: 4px; }
.notify-mini-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px;
  border-radius: 10px;
  cursor: pointer;
}
.notify-mini-row:hover { background: #f8fafc; }
.notify-mini-row.unread { background: linear-gradient(90deg, #f8fbff, #fff); }
.nm-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nm-meta { margin-top: 2px; font-size: 12px; color: #94a3b8; }
.nm-body { flex: 1; min-width: 0; }
.quick-actions {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--wb-line);
}
.quick-actions button {
  appearance: none;
  border: 1px solid var(--wb-line);
  background: #f8fafc;
  border-radius: 10px;
  padding: 10px 6px;
  font-size: 12px;
  color: #334155;
  cursor: pointer;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  transition: background .15s ease, border-color .15s ease, color .15s ease;
}
.quick-actions button i { font-size: 16px; color: #1d4ed8; }
.quick-actions button:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.proj-card {
  background: #fff;
  border: 1px solid var(--wb-line);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow .18s ease, transform .18s ease;
}
.proj-card:hover {
  box-shadow: 0 8px 24px rgba(31, 35, 41, 0.08);
  transform: translateY(-1px);
}
.card-cover {
  position: relative;
  height: 118px;
  background: linear-gradient(160deg, #dbe8ff 0%, #eef4ff 55%, #f7faff 100%);
}
.proj-card.kind-survey .card-cover {
  background: linear-gradient(160deg, #d8f3ec 0%, #eefaf6 55%, #f7fffc 100%);
}
.kind-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(255,255,255,.85);
  color: #4e5969;
  font-size: 12px;
}
.count-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: #f53f3f;
  color: #fff;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
}
.cover-art {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cover-art i { font-size: 42px; color: rgba(43, 109, 229, 0.35); }
.proj-card.kind-survey .cover-art i { color: rgba(15, 118, 110, 0.35); }
.card-body { padding: 12px 14px 6px; }
.card-body h3 {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--wb-muted);
  font-size: 12px;
}
.card-actions {
  display: flex;
  justify-content: flex-end;
  padding: 0 8px 8px;
}
.status { display: inline-flex; align-items: center; gap: 4px; white-space: nowrap; font-size: 12px; }
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: #c0c4cc; }
.status.st-1 .status-dot { background: #00b42a; }
.status.st-1 { color: #00b42a; }
.status.st-2 .status-dot { background: #86909c; }
.status.st-0 .status-dot { background: #ff7d00; }
.status.st-0 { color: #ff7d00; }
.empty-state { text-align: center; padding: 72px 16px; color: var(--wb-muted); }
.empty-state i { font-size: 40px; margin-bottom: 10px; }
.empty-state p { margin: 0 0 16px; font-size: 15px; }
.empty-actions { display: flex; justify-content: center; gap: 10px; flex-wrap: wrap; }
.notify-filters {
  display: inline-flex;
  padding: 3px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid var(--wb-line);
}
.notify-filters button {
  appearance: none;
  border: 0;
  background: transparent;
  height: 30px;
  padding: 0 12px;
  border-radius: 8px;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.notify-filters button.on {
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
}
.notify-filters .n {
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
}
.notify-panel {
  background: transparent;
  border: 0;
  padding: 0;
}
.notify-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  color: #64748b;
  font-size: 13px;
}
.notify-summary b { color: #0f172a; font-weight: 700; }
.notify-summary .hot { color: #1d4ed8; }
.notify-summary .sep { color: #cbd5e1; }
.notify-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.notify-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid var(--wb-line);
  border-radius: 14px;
  cursor: pointer;
  transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease;
}
.notify-card:hover {
  transform: translateY(-2px);
  border-color: #bfdbfe;
  box-shadow: 0 12px 28px rgba(29, 78, 216, 0.08);
}
.notify-card.unread {
  background: linear-gradient(180deg, #f8fbff 0%, #fff 70%);
  border-color: #bfdbfe;
  box-shadow: inset 3px 0 0 #1d4ed8;
}
.notify-mark {
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #eff6ff, #dbeafe);
  color: #1d4ed8;
  font-size: 18px;
}
.notify-main { flex: 1; min-width: 0; }
.notify-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 6px;
}
.notify-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.4;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.dot-new {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  flex-shrink: 0;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.18);
  margin-top: 5px;
}
.notify-content {
  margin: 0 0 10px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.notify-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.notify-meta .chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  font-size: 12px;
  color: #334155;
}
.notify-meta .chip.muted { color: #94a3b8; }
.notify-meta .chip i { font-size: 12px; }
.notify-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}
.notify-empty .empty-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 14px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #eff6ff, #dbeafe);
  color: #1d4ed8;
  font-size: 28px;
}
.notify-empty .empty-sub {
  margin: -8px 0 0;
  font-size: 13px;
  color: #94a3b8;
}
.list-table { background: #fff; border-radius: 12px; overflow: hidden; }
@media (max-width: 1200px) {
  .stat-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .bottom-row { grid-template-columns: 1fr; }
}
@media (max-width: 900px) {
  .wb { padding: 16px; }
  .wb-toolbar { flex-wrap: wrap; }
  .wb-toolbar h1 { font-size: 22px; }
  .stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .chart-row { grid-template-columns: 1fr; }
  .quick-actions { grid-template-columns: 1fr 1fr 1fr; }
}
@media (max-width: 720px) {
  .notify-card { flex-wrap: wrap; }
  .notify-actions { width: 100%; flex-direction: row; justify-content: flex-end; }
  .stat-grid { grid-template-columns: 1fr; }
}
</style>
