<template>
  <div class="app-container survey-list-page biz-list-page">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px" class="search-bar">
      <el-form-item label="名称" prop="surveyName">
        <el-input v-model="queryParams.surveyName" placeholder="问卷名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable>
          <el-option v-for="dict in dict.type.biz_survey_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="canManageUsers" label="归属" prop="createBy">
        <el-input v-model="queryParams.createBy" placeholder="账号/昵称" clearable @keyup.enter.native="handleQuery" style="width:140px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="ownerFilterLabel"
      class="mb8"
      type="info"
      :closable="true"
      show-icon
      @close="clearOwnerFilter"
      :title="'正在查看用户「' + ownerFilterLabel + '」的问卷'"
    />

    <el-row :gutter="10" class="mb8 toolbar-row">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['biz:survey:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-document" size="mini" @click="openTemplateDialog" v-hasPermi="['biz:survey:add']">从模板创建</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['biz:survey:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-checkbox :value="selectAllPage" :indeterminate="isIndeterminate" @change="toggleSelectAll">本页全选</el-checkbox>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <div v-loading="loading" class="card-grid">
      <div
        v-for="row in surveyList"
        :key="row.surveyId"
        class="survey-card biz-list-card kind-survey"
        :class="['status-' + (row.status || '0'), { selected: isSelected(row) }]"
      >
        <div class="card-body">
          <div class="card-top">
            <el-checkbox class="card-check" :value="isSelected(row)" @change="val => toggleSelect(row, val)" />
            <div class="card-mark"><i class="el-icon-document"></i></div>
            <div class="card-title-wrap" @click="goSetup(row)">
              <div class="card-title-row">
                <h3 class="card-title" :title="row.surveyName">
                  {{ row.surveyName || '未命名问卷' }}
                  <el-tag v-if="isFromTemplate(row)" size="mini" type="warning" effect="plain" class="tpl-tag">模板</el-tag>
                </h3>
                <dict-tag :options="dict.type.biz_survey_status" :value="row.status" />
              </div>
              <p class="card-desc" :title="row.surveyDesc">{{ row.surveyDesc || '暂无描述，点击进入设置或设计题目。' }}</p>
            </div>
          </div>

          <div class="card-meta">
            <div class="meta-item">
              <span class="k">答卷</span>
              <span class="v strong">{{ row.answerCount || 0 }}</span>
            </div>
            <div class="meta-item">
              <span class="k">浏览</span>
              <span class="v strong">{{ row.viewCount || 0 }}</span>
            </div>
            <div class="meta-item">
              <span class="k">短码</span>
              <span class="v code">{{ row.publicCode || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="k">创建</span>
              <span class="v">{{ shortTime(row.createTime) }}</span>
            </div>
          </div>

          <div class="card-foot">
            <div class="foot-left">
              <span class="hint" v-if="canManageUsers" :title="ownerTitle(row)">{{ ownerLabel(row) }} · 设计题目 / 设置规则</span>
              <span class="hint" v-else>常用：设计题目 / 设置规则</span>
            </div>
            <div class="foot-actions">
              <el-button type="primary" size="mini" icon="el-icon-edit" @click="goDesign(row)" v-hasPermi="['biz:survey:edit']">设计</el-button>
              <el-button size="mini" icon="el-icon-s-operation" @click="goSetup(row)" v-hasPermi="['biz:survey:edit']">设置</el-button>
              <el-button size="mini" icon="el-icon-view" @click="goPreview(row)" v-hasPermi="['biz:survey:query']">预览</el-button>
              <el-dropdown trigger="click" @command="cmd => onMore(cmd, row)">
                <el-button size="mini">更多<i class="el-icon-arrow-down el-icon--right"></i></el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="answers" icon="el-icon-s-order" v-hasPermi="['biz:survey:query']">答卷</el-dropdown-item>
                  <el-dropdown-item command="stats" icon="el-icon-data-analysis" v-hasPermi="['biz:survey:query']">统计</el-dropdown-item>
                  <el-dropdown-item command="copy" icon="el-icon-document-copy" v-hasPermi="['biz:survey:add']">复制</el-dropdown-item>
                  <el-dropdown-item command="publish" icon="el-icon-s-promotion" v-hasPermi="['biz:survey:publish']">发布</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === '1'" command="offline" icon="el-icon-video-pause" v-hasPermi="['biz:survey:publish']">停用</el-dropdown-item>
                  <el-dropdown-item v-if="row.publicCode" command="link" icon="el-icon-link">链接</el-dropdown-item>
                  <el-dropdown-item command="edit" icon="el-icon-edit-outline" v-hasPermi="['biz:survey:edit']">编辑资料</el-dropdown-item>
                  <el-dropdown-item command="transfer" icon="el-icon-sort" v-hasPermi="['biz:user:transfer']">转让归属</el-dropdown-item>
                  <el-dropdown-item command="delete" icon="el-icon-delete" divided v-hasPermi="['biz:survey:remove']">删除</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!loading && !surveyList.length" class="empty-card">
        <div class="empty-icon"><i class="el-icon-document"></i></div>
        <p>暂无问卷</p>
        <div class="empty-sub">新建空白问卷，或从模板快速起步</div>
        <div class="empty-actions">
          <el-button type="primary" size="mini" icon="el-icon-plus" @click="handleAdd" v-hasPermi="['biz:survey:add']">新建问卷</el-button>
          <el-button size="mini" icon="el-icon-document-copy" @click="openTemplateDialog" v-hasPermi="['biz:survey:add']">从模板创建</el-button>
        </div>
      </div>
    </div>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="surveyName">
          <el-input v-model="form.surveyName" placeholder="请输入问卷名称" />
        </el-form-item>
        <el-form-item label="描述" prop="surveyDesc">
          <el-input v-model="form.surveyDesc" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="访问密码">
          <el-input v-model="form.accessPwd" placeholder="留空不加密；已设置时为占位，可改或清空" show-password />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="可选" style="width:100%" />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="可选" style="width:100%" />
        </el-form-item>
        <el-form-item label="允许多次">
          <el-switch v-model="form.allowMulti" active-value="1" inactive-value="0" />
        </el-form-item>
        <el-form-item label="答卷上限">
          <el-input-number v-model="form.maxAnswers" :min="0" :max="1000000" />
          <span class="tip">0 表示不限</span>
        </el-form-item>
        <el-form-item label="每日上限">
          <el-input-number v-model="form.dailyLimit" :min="0" :max="1000000" />
          <span class="tip">0 表示不限</span>
        </el-form-item>
        <el-form-item label="提交验证码">
          <el-switch v-model="form.needCaptcha" active-value="1" inactive-value="0" />
        </el-form-item>
        <el-form-item label="主题色">
          <div class="theme-row">
            <span v-for="c in themePresets" :key="c.color" class="swatch" :style="{ background: c.color }" :class="{ active: form.themeColor === c.color }" @click="pickTheme(c)" :title="c.name" />
            <el-color-picker v-model="form.themeColor" size="mini" />
            <span class="tip">{{ form.themeColor }}</span>
          </div>
        </el-form-item>
        <el-form-item label="Webhook">
          <el-input v-model="form.webhookUrl" placeholder="答卷提交后 POST JSON，如 https://example.com/hook" clearable />
          <div class="tip">可选。提交成功后异步回调，不阻塞填写。</div>
          <el-button v-if="form.surveyId && form.webhookUrl" type="text" size="mini" @click="handleTestWebhook">发送测试</el-button>
        </el-form-item>
        <el-form-item label="签名密钥">
          <el-input v-model="form.webhookSecret" placeholder="可选，HMAC-SHA256 签名密钥" show-password clearable />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="从模板创建问卷" :visible.sync="templateOpen" width="720px" append-to-body custom-class="tpl-dialog">
      <div class="tpl-gallery">
        <p class="tpl-hint">点选模板即可生成题目与主题样式，创建后可继续调整并发布。</p>
        <div v-loading="templateLoading" class="tpl-grid">
          <article
            v-for="tpl in templates"
            :key="tpl.key"
            class="tpl-card"
            :style="tplCardStyle(tpl)"
            @click="applyTemplate(tpl)"
          >
            <div class="tpl-cover">
              <div class="tpl-icon"><i :class="tpl.icon || 'el-icon-document'"></i></div>
              <span class="tpl-badge">问卷模板</span>
            </div>
            <div class="tpl-body">
              <h4 class="tpl-name">{{ tpl.name }}</h4>
              <p class="tpl-desc">{{ tpl.desc }}</p>
              <div class="tpl-meta">
                <span v-if="tpl.questionCount" class="tpl-chip"><i class="el-icon-edit-outline"></i>{{ tpl.questionCount }} 道题</span>
              </div>
              <span class="tpl-cta">使用此模板 <i class="el-icon-arrow-right"></i></span>
            </div>
          </article>
          <el-empty v-if="!templateLoading && !templates.length" description="暂无模板" style="grid-column:1/-1" />
        </div>
      </div>
    </el-dialog>

    <el-dialog title="公开链接与分享海报" :visible.sync="qrOpen" width="560px" append-to-body>
      <div class="qr-box">
        <img v-if="posterDataUrl" :src="posterDataUrl" class="poster-img" alt="poster" />
        <img v-else-if="qrDataUrl" :src="qrDataUrl" alt="qrcode" />
        <el-form :model="posterBg" label-width="90px" size="mini" class="poster-bg-panel">
          <poster-bg-form :model="posterBg" @change="refreshPosterPreview" />
          <el-form-item>
            <el-button type="primary" size="mini" :loading="posterSaving" @click="savePosterBg">保存背景并刷新</el-button>
          </el-form-item>
        </el-form>
        <div class="poster-actions">
          <el-button type="primary" size="mini" :loading="posterLoading" icon="el-icon-download" @click="downloadPoster">下载海报</el-button>
          <el-button size="mini" @click="copyText(h5Link)">复制 H5</el-button>
          <el-button size="mini" plain @click="copyText(qrLink)">复制管理端链接</el-button>
        </div>
        <p class="link-label">管理端公开页</p>
        <p class="link">{{ qrLink }}</p>
        <p class="link-label">独立 H5</p>
        <p class="link">{{ h5Link }}</p>
      </div>
    </el-dialog>

    <el-dialog title="转让问卷归属" :visible.sync="transferOpen" width="480px" append-to-body>
      <p class="transfer-tip">将「{{ transferRow && transferRow.surveyName }}」转让给其他用户，对方将获得完整管理权限。</p>
      <el-select
        v-model="transferUserId"
        filterable
        remote
        clearable
        placeholder="搜索账号或昵称"
        :remote-method="searchTransferUsers"
        :loading="transferUserLoading"
        style="width:100%"
      >
        <el-option
          v-for="u in transferUsers"
          :key="u.userId"
          :label="(u.nickName || u.userName) + ' (' + u.userName + ')'"
          :value="u.userId"
        />
      </el-select>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="transferring" :disabled="!transferUserId" @click="submitTransfer">确认转让</el-button>
        <el-button @click="transferOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listSurvey, getSurvey, addSurvey, updateSurvey, delSurvey, publishSurvey, offlineSurvey, copySurvey, testSurveyWebhook, listSurveyTemplates, createSurveyFromTemplate, transferSurvey } from '@/api/biz/survey'
import { listUser } from '@/api/system/user'
import { toQrDataUrl, buildSharePoster, downloadDataUrl, resolvePosterBg } from '@/utils/qrcode'
import PosterBgForm from '@/components/PosterBgForm'

export default {
  name: 'BizSurvey',
  dicts: ['biz_survey_status'],
  components: { PosterBgForm },
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      surveyList: [],
      ids: [],
      multiple: true,
      open: false,
      qrOpen: false,
      templateOpen: false,
      templateLoading: false,
      templates: [],
      title: '',
      qrLink: '',
      h5Link: '',
      qrDataUrl: '',
      posterDataUrl: '',
      posterLoading: false,
      posterSaving: false,
      posterTitle: '',
      posterDesc: '',
      posterSurveyId: null,
      posterThemeColor: '#1677ff',
      posterThemeRaw: {},
      posterBg: {
        posterBgType: 'theme',
        posterBgColor: '#eef2ff',
        posterBgImage: '',
        posterBgOverlay: 40
      },
      queryParams: {
        pageNum: 1,
        pageSize: 12,
        surveyName: undefined,
        status: undefined,
        createUserId: undefined,
        createBy: undefined
      },
      ownerFilterLabel: '',
      transferOpen: false,
      transferring: false,
      transferRow: null,
      transferUserId: null,
      transferUsers: [],
      transferUserLoading: false,
      themePresets: [
        { name: '蓝', color: '#1677ff', bg: 'linear-gradient(180deg, #f5f8ff 0%, #f7f7f7 280px, #f7f7f7 100%)' },
        { name: '绿', color: '#0f766e', bg: 'linear-gradient(180deg, #ecfdf5 0%, #f7f7f7 280px, #f7f7f7 100%)' },
        { name: '橙', color: '#c2410c', bg: 'linear-gradient(180deg, #fff7ed 0%, #f7f7f7 280px, #f7f7f7 100%)' },
        { name: '墨', color: '#1f2937', bg: 'linear-gradient(180deg, #f3f4f6 0%, #f7f7f7 280px, #f7f7f7 100%)' }
      ],
      form: {},
      rules: {
        surveyName: [{ required: true, message: '名称不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    canManageUsers() {
      const perms = this.$store.getters.permissions || []
      const roles = this.$store.getters.roles || []
      return roles.includes('admin') || perms.includes('biz:user:list') || perms.includes('*:*:*')
    },
    selectAllPage() {
      return this.surveyList.length > 0 && this.surveyList.every(r => this.ids.includes(r.surveyId))
    },
    isIndeterminate() {
      const n = this.surveyList.filter(r => this.ids.includes(r.surveyId)).length
      return n > 0 && n < this.surveyList.length
    }
  },
  created() {
    this.applyOwnerRoute()
    this.getList()
    this.$nextTick(() => this.applyRouteAction())
  },
  methods: {
    applyOwnerRoute() {
      const q = this.$route.query || {}
      if (q.createUserId) {
        this.queryParams.createUserId = Number(q.createUserId) || q.createUserId
        this.ownerFilterLabel = q.ownerLabel || ('用户#' + q.createUserId)
      }
    },
    clearOwnerFilter() {
      this.queryParams.createUserId = undefined
      this.ownerFilterLabel = ''
      const q = { ...this.$route.query }
      delete q.createUserId
      delete q.ownerLabel
      this.$router.replace({ path: this.$route.path, query: q }).catch(() => {})
      this.handleQuery()
    },
    ownerLabel(row) {
      return row.ownerNickName || row.ownerName || row.createBy || '-'
    },
    ownerTitle(row) {
      const a = row.ownerNickName || ''
      const b = row.ownerName || row.createBy || ''
      return a && b && a !== b ? (a + ' / ' + b) : (a || b || '')
    },
    applyRouteAction() {
      const action = this.$route.query.action
      if (!action) return
      if (action === 'create') this.handleAdd()
      else if (action === 'template') this.openTemplateDialog()
      const q = { ...this.$route.query }
      delete q.action
      this.$router.replace({ path: this.$route.path, query: q }).catch(() => {})
    },
    shortTime(t) {
      if (!t) return '-'
      return String(t).slice(5, 16)
    },
    getList() {
      this.loading = true
      listSurvey(this.queryParams).then(res => {
        this.surveyList = res.rows || []
        this.total = res.total || 0
        this.ids = this.ids.filter(id => this.surveyList.some(r => r.surveyId === id))
        this.multiple = !this.ids.length
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      if (!this.ownerFilterLabel) this.queryParams.createUserId = undefined
      this.handleQuery()
    },
    isSelected(row) { return this.ids.includes(row.surveyId) },
    toggleSelect(row, checked) {
      if (checked) {
        if (!this.ids.includes(row.surveyId)) this.ids.push(row.surveyId)
      } else {
        this.ids = this.ids.filter(id => id !== row.surveyId)
      }
      this.multiple = !this.ids.length
    },
    toggleSelectAll(checked) {
      if (checked) {
        this.surveyList.forEach(r => {
          if (!this.ids.includes(r.surveyId)) this.ids.push(r.surveyId)
        })
      } else {
        const pageIds = new Set(this.surveyList.map(r => r.surveyId))
        this.ids = this.ids.filter(id => !pageIds.has(id))
      }
      this.multiple = !this.ids.length
    },
    onMore(cmd, row) {
      const map = {
        design: this.goDesign,
        preview: this.goPreview,
        answers: this.goAnswers,
        stats: this.goStats,
        copy: this.handleCopy,
        publish: this.handlePublish,
        offline: this.handleOffline,
        link: this.handleQr,
        edit: this.handleUpdate,
        transfer: this.openTransfer,
        delete: this.handleDelete
      }
      const fn = map[cmd]
      if (fn) fn(row)
    },
    openTransfer(row) {
      this.transferRow = row
      this.transferUserId = null
      this.transferUsers = []
      this.transferOpen = true
      this.searchTransferUsers('')
    },
    searchTransferUsers(keyword) {
      this.transferUserLoading = true
      listUser({ pageNum: 1, pageSize: 20, userName: keyword || undefined, status: '0' }).then(res => {
        this.transferUsers = res.rows || []
        this.transferUserLoading = false
      }).catch(() => { this.transferUserLoading = false })
    },
    submitTransfer() {
      if (!this.transferRow || !this.transferUserId) return
      this.transferring = true
      transferSurvey(this.transferRow.surveyId, this.transferUserId).then(() => {
        this.$modal.msgSuccess('已转让归属')
        this.transferOpen = false
        this.getList()
      }).finally(() => { this.transferring = false })
    },
    reset() {
      this.form = {
        surveyId: undefined,
        surveyName: undefined,
        surveyDesc: undefined,
        accessPwd: undefined,
        startTime: undefined,
        endTime: undefined,
        allowMulti: '1',
        maxAnswers: 0,
        dailyLimit: 0,
        needCaptcha: '0',
        themeColor: '#1677ff',
        themeBg: 'linear-gradient(180deg, #f5f8ff 0%, #f7f7f7 280px, #f7f7f7 100%)',
        themeJson: undefined,
        webhookUrl: undefined,
        webhookSecret: undefined
      }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增问卷'
    },
    openTemplateDialog() {
      this.templateOpen = true
      this.templateLoading = true
      listSurveyTemplates().then(res => {
        this.templates = res.data || []
      }).finally(() => { this.templateLoading = false })
    },
    tplCardStyle(tpl) {
      const themes = {
        satisfaction: { accent: '#1677ff', cover: 'linear-gradient(135deg, #dbeafe 0%, #eff6ff 45%, #ffffff 100%)' },
        registration: { accent: '#0f766e', cover: 'linear-gradient(135deg, #ccfbf1 0%, #ecfdf5 45%, #ffffff 100%)' },
        enrollment: { accent: '#2b6de5', cover: 'linear-gradient(135deg, #dbe4ff 0%, #eef2ff 45%, #ffffff 100%)' },
        feedback: { accent: '#c2410c', cover: 'linear-gradient(135deg, #ffedd5 0%, #fff7ed 45%, #ffffff 100%)' }
      }
      const t = themes[(tpl && tpl.key) || ''] || { accent: '#2b6de5', cover: 'linear-gradient(135deg, #e8f0fe 0%, #ffffff 72%)' }
      return { '--tpl-accent': t.accent, '--tpl-cover': t.cover }
    },
    applyTemplate(tpl) {
      const qHint = tpl.questionCount ? ('将生成 ' + tpl.questionCount + ' 道题目。') : '将生成题目。'
      this.$modal.confirm('使用模板「' + tpl.name + '」创建问卷？' + qHint).then(() => createSurveyFromTemplate(tpl.key)).then(res => {
        const s = res.data || {}
        this.$modal.msgSuccess('已从模板创建')
        this.templateOpen = false
        if (s.surveyId) {
          this.$router.push({ path: '/biz/survey-setup/index/' + s.surveyId, query: { step: '1', fromTemplate: '1' } })
        } else {
          this.getList()
        }
      })
    },
    isFromTemplate(row) {
      const name = (row && row.surveyName) || ''
      return name.indexOf('（模板）') >= 0
    },
    handleTestWebhook() {
      if (!this.form.surveyId) { this.$modal.msgWarning('请先保存问卷'); return }
      const payload = { ...this.form }
      let prevTheme = {}
      try { prevTheme = payload._themeRaw ? JSON.parse(payload._themeRaw) : {} } catch (e) { prevTheme = {} }
      payload.themeJson = JSON.stringify({
        ...prevTheme,
        color: payload.themeColor || '#1677ff',
        bg: payload.themeBg || ''
      })
      delete payload.themeColor
      delete payload.themeBg
      delete payload._themeRaw
      updateSurvey(payload).then(() => testSurveyWebhook(this.form.surveyId)).then(() => {
        this.$modal.msgSuccess('已发送测试回调')
      })
    },
    pickTheme(c) {
      this.form.themeColor = c.color
      this.form.themeBg = c.bg
    },
    handleUpdate(row) {
      this.reset()
      let theme = {}
      try { theme = row.themeJson ? JSON.parse(row.themeJson) : {} } catch (e) { theme = {} }
      this.form = {
        surveyId: row.surveyId,
        surveyName: row.surveyName,
        surveyDesc: row.surveyDesc,
        accessPwd: row.accessPwd,
        startTime: row.startTime,
        endTime: row.endTime,
        allowMulti: row.allowMulti || '1',
        maxAnswers: row.maxAnswers == null ? 0 : row.maxAnswers,
        dailyLimit: row.dailyLimit == null ? 0 : row.dailyLimit,
        needCaptcha: row.needCaptcha || '0',
        themeColor: theme.color || '#1677ff',
        themeBg: theme.bg || 'linear-gradient(180deg, #f5f8ff 0%, #f7f7f7 280px, #f7f7f7 100%)',
        _themeRaw: row.themeJson || '',
        webhookUrl: row.webhookUrl || '',
        webhookSecret: row.webhookSecret || ''
      }
      this.open = true
      this.title = '编辑问卷'
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const payload = { ...this.form }
        let prevTheme = {}
        try { prevTheme = payload._themeRaw ? JSON.parse(payload._themeRaw) : {} } catch (e) { prevTheme = {} }
        payload.themeJson = JSON.stringify({
          ...prevTheme,
          color: payload.themeColor || '#1677ff',
          bg: payload.themeBg || ''
        })
        delete payload.themeColor
        delete payload.themeBg
        delete payload._themeRaw
        if (payload.surveyId) {
          updateSurvey(payload).then(() => {
            this.$modal.msgSuccess('操作成功')
            this.open = false
            this.getList()
          })
          return
        }
        addSurvey(payload).then(res => {
          const s = res.data || {}
          const id = s.surveyId
          this.$modal.msgSuccess('已创建，请按步骤完成设置')
          this.open = false
          if (id) {
            this.$router.push('/biz/survey-setup/index/' + id + '?step=0')
          } else {
            this.getList()
          }
        })
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleDelete(row) {
      const ids = (row && row.surveyId) ? row.surveyId : this.ids
      this.$modal.confirm('是否确认删除所选问卷？').then(() => {
        return delSurvey(ids)
      }).then(() => {
        this.ids = []
        this.multiple = true
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    goSetup(row) {
      this.$router.push('/biz/survey-setup/index/' + row.surveyId)
    },
    goDesign(row) {
      this.$router.push('/biz/survey-design/index/' + row.surveyId)
    },
    goPreview(row) {
      this.$router.push('/biz/survey-preview/index/' + row.surveyId)
    },
    handleCopy(row) {
      this.$modal.confirm('确认复制问卷「' + row.surveyName + '」？将复制题目与主题设置，不含答卷，草稿状态。').then(() => copySurvey(row.surveyId)).then(res => {
        this.$modal.msgSuccess('复制成功，可继续设置')
        const id = res.data && res.data.surveyId
        if (id) {
          this.$router.push('/biz/survey-setup/index/' + id)
        } else {
          this.getList()
        }
      }).catch(() => {})
    },
    goAnswers(row) {
      this.$router.push('/biz/survey-answers/index/' + row.surveyId)
    },
    goStats(row) {
      this.$router.push('/biz/survey-stats/index/' + row.surveyId)
    },
    handlePublish(row) {
      this.$modal.confirm('确认发布问卷「' + row.surveyName + '」？').then(() => {
        return publishSurvey(row.surveyId)
      }).then(res => {
        const code = (res.data && res.data.publicCode) || row.publicCode
        this.$modal.msgSuccess('发布成功')
        this.getList()
        if (code) this.openPosterShare({ ...row, publicCode: code })
      }).catch(() => {})
    },
    handleOffline(row) {
      this.$modal.confirm('确认停用该问卷？').then(() => offlineSurvey(row.surveyId)).then(() => {
        this.$modal.msgSuccess('已停用')
        this.getList()
      }).catch(() => {})
    },
    handleQr(row) {
      this.openPosterShare(row)
    },
    async openPosterShare(row) {
      const code = row.publicCode
      if (!code) {
        this.$modal.msgWarning('请先发布生成短链')
        return
      }
      this.posterTitle = row.surveyName || '问卷'
      this.posterDesc = row.surveyDesc || ''
      this.posterSurveyId = row.surveyId
      this.qrLink = window.location.origin + '/s/' + code
      const h5Base = (process.env.VUE_APP_H5_BASE || 'http://127.0.0.1:5173').replace(/\/$/, '')
      this.h5Link = h5Base + '/s/' + code
      this.qrDataUrl = await toQrDataUrl(this.h5Link)
      this.posterDataUrl = ''
      this.qrOpen = true
      try {
        const res = await getSurvey(row.surveyId)
        const data = res.data || {}
        const s = data.survey || row
        let theme = {}
        try { theme = s.themeJson ? JSON.parse(s.themeJson) : {} } catch (e) { theme = {} }
        this.posterThemeRaw = theme
        this.posterThemeColor = theme.color || '#1677ff'
        this.posterTitle = s.surveyName || this.posterTitle
        this.posterDesc = s.surveyDesc || this.posterDesc
        this.posterBg = {
          posterBgType: theme.posterBgType || 'theme',
          posterBgColor: theme.posterBgColor || '#eef2ff',
          posterBgImage: theme.posterBgImage || '',
          posterBgOverlay: theme.posterBgOverlay == null ? 40 : theme.posterBgOverlay
        }
      } catch (e) {
        this.posterThemeRaw = {}
      }
      await this.refreshPosterPreview()
    },
    async refreshPosterPreview() {
      if (!this.h5Link) return
      this.posterLoading = true
      try {
        const bg = resolvePosterBg(this.posterBg, this.posterThemeColor)
        this.posterDataUrl = await buildSharePoster({
          title: this.posterTitle,
          subtitle: this.posterDesc || '扫码即可填写',
          brand: '通查云 · 问卷',
          link: this.h5Link,
          qrDataUrl: this.qrDataUrl,
          apiBase: process.env.VUE_APP_BASE_API,
          ...bg
        })
      } catch (e) { /* keep qr */ }
      finally { this.posterLoading = false }
    },
    savePosterBg() {
      if (!this.posterSurveyId) return
      this.posterSaving = true
      const themeJson = JSON.stringify({
        ...this.posterThemeRaw,
        color: this.posterThemeRaw.color || this.posterThemeColor || '#1677ff',
        posterBgType: this.posterBg.posterBgType || 'theme',
        posterBgColor: this.posterBg.posterBgColor || '#eef2ff',
        posterBgImage: this.posterBg.posterBgImage || '',
        posterBgOverlay: this.posterBg.posterBgOverlay == null ? 40 : this.posterBg.posterBgOverlay
      })
      updateSurvey({ surveyId: this.posterSurveyId, themeJson }).then(() => {
        this.posterThemeRaw = JSON.parse(themeJson)
        this.$modal.msgSuccess('海报背景已保存')
        return this.refreshPosterPreview()
      }).finally(() => { this.posterSaving = false })
    },
    downloadPoster() {
      const url = this.posterDataUrl || this.qrDataUrl
      if (!url) return
      downloadDataUrl(url, (this.posterTitle || 'survey') + '-海报.png')
      this.$modal.msgSuccess('已开始下载')
    },
    async showQr(link) {
      const path = link.replace(window.location.origin, '')
      const code = path.replace(/^\/s\//, '').split(/[?#]/)[0]
      const row = (this.surveyList || []).find(r => r.publicCode === code)
      if (row) {
        await this.openPosterShare(row)
        return
      }
      this.qrLink = link
      const h5Base = (process.env.VUE_APP_H5_BASE || 'http://127.0.0.1:5173').replace(/\/$/, '')
      this.h5Link = h5Base + path
      this.qrDataUrl = await toQrDataUrl(this.h5Link)
      this.posterDataUrl = ''
      this.qrOpen = true
      await this.refreshPosterPreview()
    },
    copyText(text) {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => this.$modal.msgSuccess('链接已复制'))
          .catch(() => this.fallbackCopy(text))
      } else {
        this.fallbackCopy(text)
      }
    },
    fallbackCopy(text) {
      const input = document.createElement('input')
      input.value = text
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
      this.$modal.msgSuccess('链接已复制')
    }
  }
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.theme-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.swatch { width: 22px; height: 22px; border-radius: 4px; cursor: pointer; border: 2px solid transparent; display: inline-block; }
.swatch.active { border-color: #303133; }
.tip { margin-left: 8px; color: #999; font-size: 12px; }
.qr-box { text-align: center; }
.link-label { margin: 12px 0 4px; color: #909399; font-size: 12px; text-align: left; }
.link { word-break: break-all; margin: 0 0 8px; }
.qr-box img { width: 200px; height: 200px; }
.poster-img { width: 280px; max-width: 100%; border-radius: 12px; box-shadow: 0 8px 24px rgba(15,23,42,.12); }
.poster-actions { display:flex; flex-wrap:wrap; gap:8px; justify-content:center; margin: 12px 0 8px; }
.poster-bg-panel { max-width: 420px; margin: 12px auto 0; text-align: left; }
.qr-box .link { word-break: break-all; color: #666; font-size: 13px; }
.tpl-tag { margin-left: 6px; vertical-align: middle; }
.transfer-tip {
  margin: 0 0 14px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}
</style>
