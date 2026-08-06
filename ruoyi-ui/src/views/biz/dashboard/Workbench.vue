<template>
  <div class="wb" v-loading="loading">
    <header class="wb-toolbar">
      <h1>{{ currentTitle }}</h1>
      <div class="toolbar-right" v-if="activeNav !== 'notify'">
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

    <section v-if="activeNav === 'notify'" class="notify-panel" v-loading="notifyLoading">
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
import { getBizOverview } from '@/api/biz/dashboard'
import { listBizNotify, readBizNotify, readAllBizNotify, unreadBizNotifyCount } from '@/api/biz/notify'

export default {
  name: 'BizWorkbench',
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
      }
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
      const map = { recent: '最近修改', query: '我的查询', survey: '我的问卷', notify: '答卷通知' }
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
      // 「最近修改」只展示最近 30 条；「我的查询/问卷」展示全部
      if (this.activeNav === 'recent') list = list.slice(0, 30)
      return list
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
  created() {},
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
      }).catch(() => {
        this.overview = {}
      }).finally(() => { this.loading = false })
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
  --wb-blue: #1d4ed8;
  --wb-blue-soft: #eff6ff;
  --wb-bg: #f5f7fb;
  --wb-line: #e8ecf2;
  --wb-text: #1f2329;
  --wb-muted: #8a9199;
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
.status { display: inline-flex; align-items: center; gap: 4px; white-space: nowrap; }
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
@media (max-width: 720px) {
  .notify-card { flex-wrap: wrap; }
  .notify-actions { width: 100%; flex-direction: row; justify-content: flex-end; }
}
@media (max-width: 900px) {
  .wb { padding: 16px; }
  .wb-toolbar { flex-wrap: wrap; }
  .wb-toolbar h1 { font-size: 22px; }
}
</style>
