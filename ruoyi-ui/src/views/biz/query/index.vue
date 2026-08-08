<template>
  <div class="app-container query-list-page biz-list-page">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px" class="search-bar">
      <el-form-item label="名称" prop="queryName">
        <el-input v-model="queryParams.queryName" placeholder="查询名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable>
          <el-option v-for="dict in dict.type.biz_query_status" :key="dict.value" :label="dict.label" :value="dict.value" />
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
      :title="'正在查看用户「' + ownerFilterLabel + '」的查询'"
    />

    <el-row :gutter="10" class="mb8 toolbar-row">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['biz:query:add']">新增</el-button>
        <el-button type="warning" plain icon="el-icon-s-check" size="mini" @click="openApproveList" v-hasRole="['admin']">发布审批</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-document" size="mini" @click="openTemplateDialog" v-hasPermi="['biz:query:add']">从模板创建</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['biz:query:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-checkbox :value="selectAllPage" :indeterminate="isIndeterminate" @change="toggleSelectAll">本页全选</el-checkbox>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <div v-loading="loading" class="card-grid">
      <div
        v-for="row in queryList"
        :key="row.queryId"
        class="query-card biz-list-card kind-query"
        :class="['status-' + (row.status || '0'), { selected: isSelected(row) }]"
      >
        <div class="card-body">
          <div class="card-top">
            <el-checkbox class="card-check" :value="isSelected(row)" @change="val => toggleSelect(row, val)" />
            <div class="card-mark"><i class="el-icon-search"></i></div>
            <div class="card-title-wrap" @click="goSetup(row)">
              <div class="card-title-row">
                <h3 class="card-title" :title="row.queryName">
                  {{ row.queryName || '未命名查询' }}
                  <el-tag v-if="isDemoTemplate(row)" size="mini" type="success" effect="plain" class="demo-tag">含演示数据</el-tag>
                </h3>
                <dict-tag :options="dict.type.biz_query_status" :value="row.status" />
              </div>
              <p class="card-desc" :title="row.queryDesc">{{ row.queryDesc || '暂无描述，点击进入设置完善查询页。' }}</p>
            </div>
          </div>

          <div class="card-meta">
            <div class="meta-item">
              <span class="k">数据</span>
              <span class="v strong">{{ row.rowCount || 0 }}</span>
            </div>
            <div class="meta-item">
              <span class="k">浏览</span>
              <span class="v strong">{{ row.viewCount || 0 }}</span>
            </div>
            <div class="meta-item">
              <span class="k">查询</span>
              <span class="v strong">{{ row.searchCount || 0 }}</span>
            </div>
            <div class="meta-item">
              <span class="k">解析</span>
              <el-tag size="mini" :type="parseTagType(row)">{{ parseLabel(row) }}</el-tag>
            </div>
          </div>

          <div class="card-foot">
            <div class="foot-left">
              <span class="code" v-if="row.publicCode">#{{ row.publicCode }}</span>
              <span class="time" v-if="canManageUsers" :title="ownerTitle(row)">{{ ownerLabel(row) }} · {{ row.createTime || '' }}</span>
              <span class="time" v-else>{{ row.createTime || '' }}</span>
            </div>
            <div class="foot-actions">
              <el-button type="primary" size="mini" icon="el-icon-s-operation" @click="goSetup(row)" v-hasPermi="['biz:query:edit']">设置</el-button>
              <el-dropdown trigger="click" @command="cmd => onMore(cmd, row)">
                <el-button size="mini">
                  更多<i class="el-icon-arrow-down el-icon--right"></i>
                </el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="upload" icon="el-icon-upload2" v-hasPermi="['biz:query:edit']">上传</el-dropdown-item>
                  <el-dropdown-item command="export" icon="el-icon-download" v-hasPermi="['biz:query:query']">导出 Excel</el-dropdown-item>
                  <el-dropdown-item command="exportPdf" icon="el-icon-document" v-hasPermi="['biz:query:query']">导出 PDF</el-dropdown-item>
                  <el-dropdown-item command="fields" icon="el-icon-setting" v-hasPermi="['biz:query:edit']">字段</el-dropdown-item>
                  <el-dropdown-item command="preview" icon="el-icon-view" v-hasPermi="['biz:query:query']">预览</el-dropdown-item>
                  <el-dropdown-item command="design" icon="el-icon-magic-stick" v-hasPermi="['biz:query:edit']">设计</el-dropdown-item>
                  <el-dropdown-item command="copy" icon="el-icon-document-copy" v-hasPermi="['biz:query:add']">复制</el-dropdown-item>
                  <el-dropdown-item command="publish" icon="el-icon-s-promotion" v-hasPermi="['biz:query:publish']">发布</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === '1'" command="offline" icon="el-icon-video-pause" v-hasPermi="['biz:query:publish']">停用</el-dropdown-item>
                  <el-dropdown-item v-if="row.publicCode" command="link" icon="el-icon-link">链接</el-dropdown-item>
                  <el-dropdown-item command="audit" icon="el-icon-document" v-hasPermi="['biz:query:query']">访问审计</el-dropdown-item>
                  <el-dropdown-item command="edit" icon="el-icon-edit" v-hasPermi="['biz:query:edit']">编辑</el-dropdown-item>
                  <el-dropdown-item command="admins" icon="el-icon-user" v-hasPermi="['biz:query:edit']">协作者</el-dropdown-item>
                  <el-dropdown-item command="revisions" icon="el-icon-time" v-hasPermi="['biz:query:query']">数据版本</el-dropdown-item>
                  <el-dropdown-item command="transfer" icon="el-icon-sort" v-hasPermi="['biz:user:transfer']">转让归属</el-dropdown-item>
                  <el-dropdown-item command="delete" icon="el-icon-delete" divided v-hasPermi="['biz:query:remove']">删除</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!loading && !queryList.length" class="empty-card">
        <div class="empty-icon"><i class="el-icon-folder-opened"></i></div>
        <p>暂无查询项目</p>
        <div class="empty-sub">新建项目，或从模板快速生成演示数据</div>
        <div class="empty-actions">
          <el-button type="primary" size="mini" icon="el-icon-plus" @click="handleAdd" v-hasPermi="['biz:query:add']">新建查询</el-button>
          <el-button size="mini" icon="el-icon-document" @click="openTemplateDialog" v-hasPermi="['biz:query:add']">从模板创建</el-button>
        </div>
      </div>
    </div>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="从模板创建查询" :visible.sync="templateOpen" width="720px" append-to-body custom-class="tpl-dialog">
      <div class="tpl-gallery">
        <p class="tpl-hint">点选模板即可生成字段、页面样式与演示数据，创建后可直接预览或发布。</p>
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
              <span v-if="tpl.hasSample" class="tpl-badge">含演示数据</span>
            </div>
            <div class="tpl-body">
              <h4 class="tpl-name">{{ tpl.name }}</h4>
              <p class="tpl-desc">{{ tpl.desc }}</p>
              <div class="tpl-meta">
                <span v-if="tpl.fieldCount" class="tpl-chip"><i class="el-icon-s-grid"></i>{{ tpl.fieldCount }} 字段</span>
                <span v-if="tpl.sampleCount" class="tpl-chip"><i class="el-icon-document"></i>{{ tpl.sampleCount }} 演示</span>
              </div>
              <span class="tpl-cta">使用此模板 <i class="el-icon-arrow-right"></i></span>
            </div>
          </article>
          <el-empty v-if="!templateLoading && !templates.length" description="暂无模板" style="grid-column:1/-1" />
        </div>
      </div>
    </el-dialog>

    <el-dialog :title="title" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="queryName">
          <el-input v-model="form.queryName" placeholder="请输入查询名称" />
        </el-form-item>
        <el-form-item label="描述" prop="queryDesc">
          <el-input v-model="form.queryDesc" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="访问密码">
          <el-input v-model="form.accessPwd" placeholder="留空不加密；已设置时为占位，可改或清空" show-password />
        </el-form-item>
        <el-form-item label="公开验证码">
          <el-switch v-model="form.needCaptcha" active-value="1" inactive-value="0" />
          <span class="form-tip">开启后首次查询需输入验证码（同 IP 15 分钟内免重复）</span>
        </el-form-item>
        <el-form-item label="日查询上限">
          <el-input-number v-model="form.dailyLimit" :min="0" :max="1000000" />
          <span class="form-tip">按 IP 计，0 表示不限制</span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="上传 Excel" :visible.sync="uploadOpen" width="480px" append-to-body>
      <el-radio-group v-model="uploadMode" size="small" class="mb12">
        <el-radio-button label="replace">覆盖更新</el-radio-button>
        <el-radio-button label="append">追加数据</el-radio-button>
      </el-radio-group>
      <el-alert v-if="uploadMode==='append'" title="追加要求列数与现有字段一致" type="info" :closable="false" class="mb12" />
      <el-upload
        ref="upload"
        drag
        action="#"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="onFileChange"
        :on-remove="() => { uploadFile = null }"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">仅支持 xls/xlsx，建议不超过 2 万行</div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="uploading" @click="submitUpload">开始解析</el-button>
        <el-button @click="uploadOpen = false">取 消</el-button>
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
          <el-button size="mini" @click="copyText(h5Link)">复制 H5 链接</el-button>
          <el-button size="mini" plain @click="copyText(qrLink)">复制管理端链接</el-button>
        </div>
        <p class="link-label">管理端公开页</p>
        <p class="link">{{ qrLink }}</p>
        <p class="link-label">独立 H5</p>
        <p class="link">{{ h5Link }}</p>
      </div>
    </el-dialog>

    <el-dialog :title="'访问审计 - ' + (auditQueryName || '')" :visible.sync="auditOpen" width="860px" append-to-body>
      <div class="audit-bar">
        <el-radio-group v-model="auditAction" size="mini" @change="loadAuditLogs">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="search">查询</el-radio-button>
          <el-radio-button label="view">浏览</el-radio-button>
          <el-radio-button label="export">导出</el-radio-button>
        </el-radio-group>
        <el-button size="mini" icon="el-icon-refresh" :loading="auditLoading" @click="loadAuditLogs">刷新</el-button>
      </div>
      <el-table v-loading="auditLoading" :data="auditLogs" size="mini" border max-height="460">
        <el-table-column label="时间" prop="createTime" width="160" />
        <el-table-column label="动作" prop="action" width="80" align="center" />
        <el-table-column label="IP" prop="clientIp" width="130" />
        <el-table-column label="条件/详情" min-width="260">
          <template slot-scope="scope">
            <span class="audit-detail">{{ formatAuditDetail(scope.row.detailJson) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="UA" prop="userAgent" min-width="160" show-overflow-tooltip />
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="auditOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <el-dialog title="转让查询归属" :visible.sync="transferOpen" width="480px" append-to-body>
      <p class="transfer-tip">将「{{ transferRow && transferRow.queryName }}」转让给其他用户，对方将获得完整管理权限。</p>
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

    <el-dialog :title="'协作者 - ' + ((adminRow && adminRow.queryName) || '')" :visible.sync="adminOpen" width="560px" append-to-body>
      <div class="admin-add">
        <el-select v-model="adminUserId" filterable remote clearable placeholder="搜索用户" :remote-method="searchAdminUsers" :loading="adminSearching" style="width:70%">
          <el-option v-for="u in adminUsers" :key="u.userId" :label="(u.nickName || u.userName) + ' (' + u.userName + ')'" :value="u.userId" />
        </el-select>
        <el-button type="primary" size="mini" :loading="adminAdding" :disabled="!adminUserId" @click="submitAddAdmin">添加</el-button>
      </div>
      <el-table :data="adminList" size="mini" border class="mt12" v-loading="adminLoading">
        <el-table-column label="账号" prop="userName" />
        <el-table-column label="昵称" prop="nickName" />
        <el-table-column label="操作" width="80" align="center">
          <template slot-scope="scope">
            <el-button type="text" style="color:#f56c6c" @click="removeAdmin(scope.row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer"><el-button @click="adminOpen = false">关 闭</el-button></div>
    </el-dialog>

    <el-dialog :title="'数据版本 - ' + ((revRow && revRow.queryName) || '')" :visible.sync="revOpen" width="640px" append-to-body>
      <el-table :data="revList" size="mini" border v-loading="revLoading">
        <el-table-column label="版本" prop="revNo" width="70" align="center" />
        <el-table-column label="行数" prop="rowCount" width="80" align="center" />
        <el-table-column label="说明" prop="remark" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作人" prop="createBy" width="100" />
        <el-table-column label="时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="90" align="center">
          <template slot-scope="scope">
            <el-button type="text" @click="doRollback(scope.row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer"><el-button @click="revOpen = false">关 闭</el-button></div>
    </el-dialog>

    <el-dialog title="发布审批" :visible.sync="approveOpen" width="720px" append-to-body>
      <el-table :data="approveList" size="mini" border v-loading="approveLoading">
        <el-table-column label="类型" prop="projectType" width="80" />
        <el-table-column label="名称" prop="projectName" min-width="140" />
        <el-table-column label="申请人" prop="applyBy" width="100" />
        <el-table-column label="时间" prop="applyTime" width="160" />
        <el-table-column label="操作" width="160" align="center">
          <template slot-scope="scope">
            <el-button type="text" @click="doApprove(scope.row)">通过</el-button>
            <el-button type="text" style="color:#f56c6c" @click="doReject(scope.row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer"><el-button @click="approveOpen = false">关 闭</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { listQuery, getQuery, addQuery, updateQuery, delQuery, uploadQueryExcel, publishQuery, offlineQuery, exportQuery, exportQueryPdf, copyQuery, listQueryTemplates, createQueryFromTemplate, listQueryAccessLogs, transferQuery, saveQueryPage, listQueryAdmins, searchQueryAdminUsers, addQueryAdmin, removeQueryAdmin } from '@/api/biz/query'
import { listQueryRevisions, rollbackQueryRevision, listPublishRequests, approvePublishRequest, rejectPublishRequest } from '@/api/biz/version'
import { listUser } from '@/api/system/user'
import { toQrDataUrl, buildSharePoster, downloadDataUrl, resolvePosterBg } from '@/utils/qrcode'
import { parseLayout } from '@/utils/bizQueryField'
import PosterBgForm from '@/components/PosterBgForm'
import { blobValidate } from '@/utils/ruoyi'
import { saveAs } from 'file-saver'

export default {
  name: 'BizQuery',
  dicts: ['biz_query_status'],
  components: { PosterBgForm },
  data() {
    return {
      loading: false,
      uploading: false,
      showSearch: true,
      total: 0,
      queryList: [],
      ids: [],
      multiple: true,
      open: false,
      uploadOpen: false,
      qrOpen: false,
      templateOpen: false,
      templateLoading: false,
      templates: [],
      title: '',
      uploadQueryId: null,
      uploadFile: null,
      uploadMode: 'replace',
      qrLink: '',
      h5Link: '',
      qrDataUrl: '',
      posterDataUrl: '',
      posterLoading: false,
      posterSaving: false,
      posterTitle: '',
      posterDesc: '',
      posterQueryId: null,
      posterThemeColor: '#1677ff',
      posterBg: {
        posterBgType: 'theme',
        posterBgColor: '#eef2ff',
        posterBgImage: '',
        posterBgOverlay: 40
      },
      pageSnapshot: null,
      auditOpen: false,
      auditLoading: false,
      auditQueryId: null,
      auditQueryName: '',
      auditAction: 'search',
      auditLogs: [],
      adminOpen: false,
      adminLoading: false,
      adminAdding: false,
      adminSearching: false,
      adminRow: null,
      adminList: [],
      adminUsers: [],
      adminUserId: null,
      revOpen: false,
      revLoading: false,
      revRow: null,
      revList: [],
      approveOpen: false,
      approveLoading: false,
      approveList: [],
      queryParams: { pageNum: 1, pageSize: 12, queryName: undefined, status: undefined, createUserId: undefined, createBy: undefined },
      ownerFilterLabel: '',
      transferOpen: false,
      transferring: false,
      transferRow: null,
      transferUserId: null,
      transferUsers: [],
      transferUserLoading: false,
      form: {},
      rules: {
        queryName: [{ required: true, message: '名称不能为空', trigger: 'blur' }]
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
      return this.queryList.length > 0 && this.queryList.every(r => this.ids.includes(r.queryId))
    },
    isIndeterminate() {
      const n = this.queryList.filter(r => this.ids.includes(r.queryId)).length
      return n > 0 && n < this.queryList.length
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
    getList() {
      this.loading = true
      listQuery(this.queryParams).then(res => {
        this.queryList = res.rows || []
        this.total = res.total || 0
        this.ids = this.ids.filter(id => this.queryList.some(r => r.queryId === id))
        this.multiple = !this.ids.length
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() {
      this.resetForm('queryForm')
      if (!this.ownerFilterLabel) this.queryParams.createUserId = undefined
      this.handleQuery()
    },
    isSelected(row) { return this.ids.includes(row.queryId) },
    toggleSelect(row, checked) {
      if (checked) {
        if (!this.ids.includes(row.queryId)) this.ids.push(row.queryId)
      } else {
        this.ids = this.ids.filter(id => id !== row.queryId)
      }
      this.multiple = !this.ids.length
    },
    toggleSelectAll(checked) {
      if (checked) {
        this.queryList.forEach(r => {
          if (!this.ids.includes(r.queryId)) this.ids.push(r.queryId)
        })
      } else {
        const pageIds = new Set(this.queryList.map(r => r.queryId))
        this.ids = this.ids.filter(id => !pageIds.has(id))
      }
      this.multiple = !this.ids.length
    },
    onMore(cmd, row) {
      const map = {
        upload: this.handleUpload,
        export: this.handleExport,
        exportPdf: this.handleExportPdf,
        fields: this.goFields,
        preview: this.goPreview,
        design: this.goPage,
        copy: this.handleCopy,
        publish: this.handlePublish,
        offline: this.handleOffline,
        link: this.handleQr,
        audit: this.handleAudit,
        edit: this.handleUpdate,
        admins: this.openAdmins,
        revisions: this.openRevisions,
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
      transferQuery(this.transferRow.queryId, this.transferUserId).then(() => {
        this.$modal.msgSuccess('已转让归属')
        this.transferOpen = false
        this.getList()
      }).finally(() => { this.transferring = false })
    },
    reset() {
      this.form = { queryId: undefined, queryName: undefined, queryDesc: undefined, accessPwd: undefined, needCaptcha: '0', dailyLimit: 0 }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增查询'
    },
    openTemplateDialog() {
      this.templateOpen = true
      this.templateLoading = true
      listQueryTemplates().then(res => {
        this.templates = res.data || []
      }).finally(() => { this.templateLoading = false })
    },
    tplCardStyle(tpl) {
      const themes = {
        score_lookup: { accent: '#1677ff', cover: 'linear-gradient(135deg, #dbeafe 0%, #eff6ff 45%, #ffffff 100%)' },
        class_assign: { accent: '#1d4ed8', cover: 'linear-gradient(135deg, #dbe4ff 0%, #eef2ff 45%, #ffffff 100%)' },
        staff_dir: { accent: '#0f766e', cover: 'linear-gradient(135deg, #ccfbf1 0%, #ecfdf5 45%, #ffffff 100%)' }
      }
      const t = themes[(tpl && tpl.key) || ''] || { accent: '#1d4ed8', cover: 'linear-gradient(135deg, #eff6ff 0%, #ffffff 72%)' }
      return { '--tpl-accent': t.accent, '--tpl-cover': t.cover }
    },
    applyTemplate(tpl) {
      const sampleHint = tpl.hasSample
        ? '将生成字段、页面配置，并写入 ' + (tpl.sampleCount || '') + ' 条演示数据，可直接预览发布。'
        : '将生成字段与页面配置（不含数据行）。'
      this.$modal.confirm('使用模板「' + tpl.name + '」创建查询？' + sampleHint).then(() => createQueryFromTemplate(tpl.key)).then(res => {
        const q = res.data || {}
        this.$modal.msgSuccess('已从模板创建')
        this.templateOpen = false
        if (q.queryId) {
          // 不带 step，由向导根据数据/字段自动跳到页面设计
          this.$router.push({ path: '/biz/query-setup/index/' + q.queryId, query: { fromTemplate: '1' } })
        } else {
          this.getList()
        }
      })
    },
    isDemoTemplate(row) {
      const name = (row && row.queryName) || ''
      const desc = (row && row.queryDesc) || ''
      return name.indexOf('（模板）') >= 0 || desc.indexOf('演示数据') >= 0
    },
    handleUpdate(row) {
      this.reset()
      this.form = {
        queryId: row.queryId,
        queryName: row.queryName,
        queryDesc: row.queryDesc,
        accessPwd: row.accessPwd,
        needCaptcha: row.needCaptcha || '0',
        dailyLimit: row.dailyLimit == null ? 0 : row.dailyLimit
      }
      this.open = true
      this.title = '编辑查询'
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        if (this.form.queryId) {
          updateQuery(this.form).then(() => {
            this.$modal.msgSuccess('操作成功')
            this.open = false
            this.getList()
          })
          return
        }
        addQuery(this.form).then(res => {
          const q = res.data || {}
          const id = q.queryId
          this.$modal.msgSuccess('已创建，请按步骤完成设置')
          this.open = false
          if (id) {
            this.$router.push('/biz/query-setup/index/' + id + '?step=0')
          } else {
            this.getList()
          }
        })
      })
    },
    cancel() { this.open = false; this.reset() },
    handleDelete(row) {
      const ids = (row && row.queryId) ? row.queryId : this.ids
      this.$modal.confirm('是否确认删除所选查询？').then(() => delQuery(ids)).then(() => {
        this.ids = []
        this.multiple = true
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleUpload(row) {
      this.uploadQueryId = row.queryId
      this.uploadFile = null
      this.uploadMode = 'replace'
      this.uploadOpen = true
      this.$nextTick(() => this.$refs.upload && this.$refs.upload.clearFiles())
    },
    onFileChange(file) { this.uploadFile = file.raw },
    submitUpload() {
      if (!this.uploadFile) {
        this.$modal.msgError('请先选择文件')
        return
      }
      this.uploading = true
      uploadQueryExcel(this.uploadQueryId, this.uploadFile, this.uploadMode).then(res => {
        const detail = res.data || {}
        const q = detail.query || {}
        this.uploadOpen = false
        if (q.parseStatus === '1') {
          this.$modal.msgSuccess('文件较大，已转入后台解析，请稍候刷新')
          this.pollParse(q.queryId)
        } else {
          this.$modal.msgSuccess('解析入库成功')
        }
        this.getList()
      }).finally(() => { this.uploading = false })
    },
    handleExport(row) {
      this.$modal.confirm('确认导出该查询全部数据？').then(() => {
        return exportQuery(row.queryId)
      }).then(async data => {
        const isBlob = blobValidate(data)
        if (isBlob) {
          saveAs(new Blob([data]), (row.queryName || 'query') + '.xlsx')
        } else {
          this.$modal.msgError('导出失败')
        }
      }).catch(() => {})
    },
    handleExportPdf(row) {
      this.$modal.confirm('确认导出成绩单 PDF（每人一页）？').then(() => {
        return exportQueryPdf(row.queryId)
      }).then(async data => {
        const isBlob = blobValidate(data)
        if (isBlob) {
          saveAs(new Blob([data]), (row.queryName || 'query') + '.pdf')
        } else {
          this.$modal.msgError('PDF 导出失败')
        }
      }).catch(() => {})
    },
    pollParse(queryId, tries = 0) {
      if (tries > 60) return
      setTimeout(() => {
        getQuery(queryId).then(res => {
          const q = (res.data && res.data.query) || {}
          if (q.parseStatus === '1') {
            this.pollParse(queryId, tries + 1)
          } else if (q.parseStatus === '2') {
            this.$modal.msgError('解析失败：' + (q.parseMsg || ''))
            this.getList()
          } else {
            this.$modal.msgSuccess('后台解析完成')
            this.getList()
          }
        })
      }, 2000)
    },
    parseLabel(row) {
      if (!row.rowCount && (!row.parseStatus || row.parseStatus === '0')) return '待上传'
      if (row.parseStatus === '1') return '解析中'
      if (row.parseStatus === '2') return '失败'
      return '就绪'
    },
    parseTagType(row) {
      if (row.parseStatus === '1') return 'warning'
      if (row.parseStatus === '2') return 'danger'
      if (!row.rowCount && (!row.parseStatus || row.parseStatus === '0')) return 'info'
      return 'success'
    },
    goSetup(row) { this.$router.push('/biz/query-setup/index/' + row.queryId) },
    goFields(row) { this.$router.push('/biz/query-fields/index/' + row.queryId) },
    goPreview(row) { this.$router.push('/biz/query-preview/index/' + row.queryId) },
    handleCopy(row) {
      this.$modal.confirm('确认复制查询「' + row.queryName + '」？将复制字段、页面与数据行，草稿状态。').then(() => copyQuery(row.queryId)).then(res => {
        this.$modal.msgSuccess('复制成功，可继续设置')
        const id = res.data && res.data.queryId
        if (id) {
          this.$router.push('/biz/query-setup/index/' + id)
        } else {
          this.getList()
        }
      }).catch(() => {})
    },
    goPage(row) { this.$router.push('/biz/query-page/index/' + row.queryId) },
    handlePublish(row) {
      this.$modal.confirm('确认发布该查询并生成链接？').then(() => publishQuery(row.queryId)).then(res => {
        const data = res.data || {}
        if (data.pending) {
          this.$modal.msgSuccess(data.message || '已提交发布审批')
          this.getList()
          return
        }
        const code = data.publicCode || row.publicCode
        this.$modal.msgSuccess('发布成功')
        this.getList()
        if (code) this.handleQr({ ...row, publicCode: code })
      }).catch(() => {})
    },
    handleOffline(row) {
      this.$modal.confirm('确认停用该查询？').then(() => offlineQuery(row.queryId)).then(() => {
        this.$modal.msgSuccess('已停用')
        this.getList()
      }).catch(() => {})
    },

    handleAudit(row) {
      this.auditQueryId = row.queryId
      this.auditQueryName = row.queryName || ''
      this.auditAction = 'search'
      this.auditOpen = true
      this.loadAuditLogs()
    },
    openAdmins(row) {
      this.adminRow = row
      this.adminUserId = null
      this.adminUsers = []
      this.adminOpen = true
      this.loadAdmins()
    },
    loadAdmins() {
      if (!this.adminRow) return
      this.adminLoading = true
      listQueryAdmins(this.adminRow.queryId).then(res => {
        this.adminList = res.data || []
      }).finally(() => { this.adminLoading = false })
    },
    searchAdminUsers(q) {
      this.adminSearching = true
      searchQueryAdminUsers(q || '').then(res => {
        this.adminUsers = res.data || []
      }).finally(() => { this.adminSearching = false })
    },
    submitAddAdmin() {
      if (!this.adminRow || !this.adminUserId) return
      this.adminAdding = true
      addQueryAdmin(this.adminRow.queryId, { userId: this.adminUserId }).then(() => {
        this.$modal.msgSuccess('已添加')
        this.adminUserId = null
        this.loadAdmins()
      }).finally(() => { this.adminAdding = false })
    },
    removeAdmin(row) {
      this.$modal.confirm('确认移除协作者？').then(() => removeQueryAdmin(this.adminRow.queryId, row.userId)).then(() => {
        this.$modal.msgSuccess('已移除')
        this.loadAdmins()
      }).catch(() => {})
    },
    openRevisions(row) {
      this.revRow = row
      this.revOpen = true
      this.revLoading = true
      listQueryRevisions(row.queryId).then(res => {
        this.revList = res.data || []
      }).finally(() => { this.revLoading = false })
    },
    doRollback(row) {
      this.$modal.confirm('确认回滚到版本 #' + row.revNo + '？当前数据会先自动快照。').then(() => {
        return rollbackQueryRevision(this.revRow.queryId, row.revId)
      }).then(() => {
        this.$modal.msgSuccess('已回滚')
        this.openRevisions(this.revRow)
        this.getList()
      }).catch(() => {})
    },
    openApproveList() {
      this.approveOpen = true
      this.approveLoading = true
      listPublishRequests({ status: '0' }).then(res => {
        this.approveList = res.rows || res.data || []
      }).finally(() => { this.approveLoading = false })
    },
    doApprove(row) {
      approvePublishRequest(row.requestId).then(() => {
        this.$modal.msgSuccess('已通过并发布')
        this.openApproveList()
      }).catch(() => {})
    },
    doReject(row) {
      this.$prompt('驳回原因', '驳回').then(({ value }) => rejectPublishRequest(row.requestId, value || '驳回')).then(() => {
        this.$modal.msgSuccess('已驳回')
        this.openApproveList()
      }).catch(() => {})
    },
    loadAuditLogs() {
      if (!this.auditQueryId) return
      this.auditLoading = true
      listQueryAccessLogs(this.auditQueryId, { action: this.auditAction || undefined, limit: 100 }).then(res => {
        this.auditLogs = res.data || []
      }).finally(() => { this.auditLoading = false })
    },
    formatAuditDetail(json) {
      if (!json) return '—'
      try {
        const d = typeof json === 'string' ? JSON.parse(json) : json
        const params = d.params || {}
        const parts = Object.keys(params).map(k => k + '=' + params[k])
        const hit = d.hitTotal != null ? ('命中 ' + d.hitTotal) : ''
        const page = d.pageNum ? ('第' + d.pageNum + '页') : ''
        return [parts.join(' · '), hit, page].filter(Boolean).join(' | ') || '—'
      } catch (e) {
        return String(json).slice(0, 120)
      }
    },
    downloadPoster() {
      const url = this.posterDataUrl || this.qrDataUrl
      if (!url) return
      downloadDataUrl(url, (this.posterTitle || 'share') + '-海报.png')
      this.$modal.msgSuccess('已开始下载')
    },
    async refreshPosterPreview() {
      if (!this.h5Link) return
      this.posterLoading = true
      try {
        const bg = resolvePosterBg(this.posterBg, this.posterThemeColor)
        this.posterDataUrl = await buildSharePoster({
          title: this.posterTitle,
          subtitle: this.posterDesc || '扫码即可查询成绩 / 信息',
          link: this.h5Link,
          qrDataUrl: this.qrDataUrl,
          apiBase: process.env.VUE_APP_BASE_API,
          ...bg
        })
      } catch (e) {
        /* keep plain qr */
      } finally {
        this.posterLoading = false
      }
    },
    savePosterBg() {
      if (!this.posterQueryId) return
      this.posterSaving = true
      const page = this.pageSnapshot || {}
      const layout = { ...parseLayout(page), ...this.posterBg }
      const payload = {
        queryId: Number(this.posterQueryId),
        title: page.title || this.posterTitle || '',
        subtitle: page.subtitle || '',
        themeColor: page.themeColor || this.posterThemeColor || '#1677ff',
        bannerUrl: page.bannerUrl || '',
        resultTips: page.resultTips || '未查询到相关数据',
        layoutJson: JSON.stringify(layout)
      }
      saveQueryPage(payload).then(() => {
        this.pageSnapshot = { ...page, ...payload }
        this.$modal.msgSuccess('海报背景已保存')
        return this.refreshPosterPreview()
      }).finally(() => { this.posterSaving = false })
    },
    async handleQr(row) {
      const code = row.publicCode
      if (!code) {
        this.$modal.msgWarning('请先发布生成短链')
        return
      }
      this.posterTitle = row.queryName || '查询'
      this.posterDesc = row.queryDesc || ''
      this.posterQueryId = row.queryId
      this.qrLink = window.location.origin + '/q/' + code
      const h5Base = (process.env.VUE_APP_H5_BASE || 'http://127.0.0.1:5173').replace(/\/$/, '')
      this.h5Link = h5Base + '/q/' + code
      this.qrDataUrl = await toQrDataUrl(this.h5Link)
      this.posterDataUrl = ''
      this.qrOpen = true
      try {
        const res = await getQuery(row.queryId)
        const data = res.data || {}
        const page = data.page || {}
        this.pageSnapshot = page
        const layout = parseLayout(page)
        this.posterThemeColor = page.themeColor || '#1677ff'
        this.posterBg = {
          posterBgType: layout.posterBgType || 'theme',
          posterBgColor: layout.posterBgColor || '#eef2ff',
          posterBgImage: layout.posterBgImage || '',
          posterBgOverlay: layout.posterBgOverlay == null ? 40 : layout.posterBgOverlay
        }
        if (data.query && data.query.queryDesc) this.posterDesc = data.query.queryDesc
      } catch (e) {
        this.pageSnapshot = null
      }
      await this.refreshPosterPreview()
    },
    async showQr(link) {
      this.qrLink = link
      const path = link.replace(window.location.origin, '')
      const h5Base = (process.env.VUE_APP_H5_BASE || 'http://127.0.0.1:5173').replace(/\/$/, '')
      this.h5Link = h5Base + path
      this.qrDataUrl = await toQrDataUrl(this.h5Link)
      this.qrOpen = true
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
.mb12 { margin-bottom: 12px; }
.qr-box { text-align: center; }
.qr-box img { width: 200px; height: 200px; }
.link-label { margin: 12px 0 4px; color: #909399; font-size: 12px; text-align: left; }
.qr-box .link { word-break: break-all; color: #666; font-size: 13px; margin: 0 0 8px; }
.demo-tag { margin-left: 6px; vertical-align: middle; }
.form-tip { margin-left: 8px; color: #909399; font-size: 12px; }
.audit-bar { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; }
.audit-detail { font-size:12px; color:#4b5563; word-break:break-all; }
.poster-img { width: 280px; max-width: 100%; border-radius: 12px; box-shadow: 0 8px 24px rgba(15,23,42,.12); }
.poster-actions { display:flex; flex-wrap:wrap; gap:8px; justify-content:center; margin: 12px 0 8px; }
.poster-bg-panel { max-width: 420px; margin: 12px auto 0; text-align: left; }
.transfer-tip {
  margin: 0 0 14px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}
</style>
