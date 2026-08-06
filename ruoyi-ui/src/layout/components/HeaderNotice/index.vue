<template>
  <div>
    <el-popover ref="noticePopover" placement="bottom-end" width="360" trigger="manual" :value="noticeVisible" popper-class="notice-popover">
      <div class="notice-header">
        <div class="notice-tabs">
          <span :class="{ on: tab === 'sys' }" @click="tab = 'sys'">系统公告</span>
          <span :class="{ on: tab === 'biz' }" @click="tab = 'biz'">答卷通知<span v-if="bizUnread" class="tab-n">{{ bizUnread }}</span></span>
        </div>
        <span class="notice-mark-all" @click="markAllRead">全部已读</span>
      </div>
      <div v-if="noticeLoading" class="notice-loading"><i class="el-icon-loading"></i> 加载中...</div>
      <template v-else-if="tab === 'sys'">
        <div v-if="noticeList.length === 0" class="notice-empty"><i class="el-icon-inbox"></i><br>暂无公告</div>
        <div v-else>
          <div v-for="item in noticeList" :key="'s'+item.noticeId" class="notice-item" :class="{ 'is-read': item.isRead }" @click="previewNotice(item)">
            <el-tag size="mini" :type="item.noticeType === '1' ? 'warning' : 'success'" class="notice-tag">
              {{ item.noticeType === '1' ? '通知' : '公告' }}
            </el-tag>
            <span class="notice-item-title">{{ item.noticeTitle }}</span>
            <span class="notice-item-date">{{ item.createTime }}</span>
          </div>
        </div>
      </template>
      <template v-else>
        <div v-if="bizList.length === 0" class="notice-empty"><i class="el-icon-inbox"></i><br>暂无答卷通知</div>
        <div v-else>
          <div v-for="item in bizList" :key="'b'+item.notifyId" class="notice-item" :class="{ 'is-read': item.readFlag === '1' }" @click="openBiz(item)">
            <el-tag size="mini" type="primary" class="notice-tag">答卷</el-tag>
            <span class="notice-item-title">{{ item.title || item.surveyName || '新答卷' }}</span>
            <span class="notice-item-date">{{ item.createTime }}</span>
          </div>
        </div>
        <div class="notice-footer" @click="goBizDashboard">查看全部答卷通知</div>
      </template>
    </el-popover>

    <div v-popover:noticePopover class="notice-trigger" @mouseenter="onNoticeEnter" @mouseleave="onNoticeLeave">
      <svg-icon icon-class="bell" />
      <span v-if="totalUnread > 0" class="notice-badge">{{ totalUnread > 99 ? '99+' : totalUnread }}</span>
    </div>

    <notice-detail-view ref="noticeViewRef" />
  </div>
</template>

<script>
import NoticeDetailView from './DetailView'
import { listNoticeTop, markNoticeRead, markNoticeReadAll } from '@/api/system/notice'
import { listBizNotifyTop, readBizNotify, readAllBizNotify } from '@/api/biz/notify'

export default {
  name: 'HeaderNotice',
  components: { NoticeDetailView },
  data() {
    return {
      tab: 'sys',
      noticeList: [],
      unreadCount: 0,
      bizList: [],
      bizUnread: 0,
      noticeLoading: false,
      noticeVisible: false,
      noticeLeaveTimer: null,
      pollTimer: null
    }
  },
  computed: {
    totalUnread() {
      return (Number(this.unreadCount) || 0) + (Number(this.bizUnread) || 0)
    }
  },
  mounted() {
    this.loadAll()
    this.pollTimer = setInterval(this.loadAll, 60000)
  },
  beforeDestroy() {
    if (this.pollTimer) clearInterval(this.pollTimer)
  },
  methods: {
    onNoticeEnter() {
      clearTimeout(this.noticeLeaveTimer)
      this.noticeVisible = true
      this.loadAll()
      this.$nextTick(() => {
        const popper = this.$refs.noticePopover.$refs.popper
        if (popper && !popper._noticeBound) {
          popper._noticeBound = true
          popper.addEventListener('mouseenter', () => clearTimeout(this.noticeLeaveTimer))
          popper.addEventListener('mouseleave', () => {
            this.noticeLeaveTimer = setTimeout(() => { this.noticeVisible = false }, 100)
          })
        }
      })
    },
    onNoticeLeave() {
      this.noticeLeaveTimer = setTimeout(() => { this.noticeVisible = false }, 150)
    },
    loadAll() {
      this.loadNoticeTop()
      this.loadBizTop()
    },
    loadNoticeTop() {
      this.noticeLoading = true
      listNoticeTop().then(res => {
        this.noticeList = res.data || []
        this.unreadCount = res.unreadCount !== undefined ? res.unreadCount : this.noticeList.filter(n => !n.isRead).length
      }).finally(() => {
        this.noticeLoading = false
      })
    },
    loadBizTop() {
      listBizNotifyTop().then(res => {
        this.bizList = res.data || []
        this.bizUnread = res.unreadCount !== undefined ? Number(res.unreadCount) : this.bizList.filter(n => n.readFlag !== '1').length
      }).catch(() => {})
    },
    previewNotice(item) {
      if (!item.isRead) {
        markNoticeRead(item.noticeId).catch(() => {})
        item.isRead = true
        const idx = this.noticeList.indexOf(item)
        if (idx !== -1) this.$set(this.noticeList, idx, { ...item, isRead: true })
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      }
      this.$refs.noticeViewRef.open(item.noticeId)
    },
    openBiz(item) {
      const go = () => {
        const q = item.answerId ? ('?answerId=' + item.answerId) : ''
        this.$router.push('/biz/survey-answers/index/' + item.surveyId + q)
        this.noticeVisible = false
      }
      if (item.readFlag !== '1') {
        readBizNotify(item.notifyId).finally(() => {
          item.readFlag = '1'
          this.bizUnread = Math.max(0, this.bizUnread - 1)
          go()
        })
      } else {
        go()
      }
    },
    goBizDashboard() {
      this.noticeVisible = false
      this.$router.push('/biz/notify')
    },
    markAllRead() {
      if (this.tab === 'sys') {
        const ids = this.noticeList.map(n => n.noticeId).join(',')
        if (!ids) return
        markNoticeReadAll(ids).catch(() => {})
        this.noticeList = this.noticeList.map(n => ({ ...n, isRead: true }))
        this.unreadCount = 0
      } else {
        readAllBizNotify().then(() => {
          this.bizList = this.bizList.map(n => ({ ...n, readFlag: '1' }))
          this.bizUnread = 0
        }).catch(() => {})
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.notice-trigger {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 36px;
  border-radius: 8px;
  color: #5a5e66;
  cursor: pointer;
  transition: background .2s, color .2s;
  &:hover {
    background: #f3f4f6;
    color: #303133;
  }
  .svg-icon { width: 1.15em; height: 1.15em; }
  .notice-badge {
    position: absolute;
    top: 2px;
    right: 0;
    background: #f56c6c;
    color: #fff;
    border-radius: 10px;
    font-size: 10px;
    height: 16px;
    line-height: 16px;
    padding: 0 4px;
    min-width: 16px;
    text-align: center;
    white-space: nowrap;
    pointer-events: none;
  }
}
.notice-popover { padding: 0 !important; }
.notice-popover .notice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f7f9fb;
  border-bottom: 1px solid #eee;
  font-size: 13px;
  font-weight: 600;
  color: #333;
}
.notice-tabs { display: flex; gap: 12px; align-items: center; }
.notice-tabs span { cursor: pointer; color: #909399; font-weight: 500; position: relative; }
.notice-tabs span.on { color: #303133; font-weight: 600; }
.notice-tabs .tab-n {
  margin-left: 4px; background: #f56c6c; color: #fff; border-radius: 8px;
  font-size: 10px; padding: 0 4px; font-weight: 600;
}
.notice-popover .notice-mark-all {
  font-size: 12px; color: #409EFF; font-weight: normal; cursor: pointer;
}
.notice-popover .notice-mark-all:hover { color: #2b7cc1; }
.notice-popover .notice-loading,
.notice-popover .notice-empty {
  padding: 24px; text-align: center; color: #bbb; font-size: 12px; line-height: 1.8;
}
.notice-popover .notice-item {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-bottom: 1px solid #f5f5f5;
  cursor: pointer; transition: background 0.15s;
}
.notice-popover .notice-item:last-child { border-bottom: none; }
.notice-popover .notice-item:hover { background: #f7f9fb; }
.notice-popover .notice-item.is-read .notice-tag,
.notice-popover .notice-item.is-read .notice-item-title,
.notice-popover .notice-item.is-read .notice-item-date { opacity: 0.45; filter: grayscale(1); color: #999; }
.notice-popover .notice-tag { flex-shrink: 0; }
.notice-popover .notice-item-title {
  flex: 1; font-size: 12px; color: #333;
  overflow: hidden; white-space: nowrap; text-overflow: ellipsis;
}
.notice-popover .notice-item-date { flex-shrink: 0; font-size: 11px; color: #bbb; }
.notice-footer {
  padding: 10px 14px; text-align: center; font-size: 12px; color: #409EFF;
  border-top: 1px solid #eee; cursor: pointer;
}
.notice-footer:hover { background: #f7f9fb; }
</style>
