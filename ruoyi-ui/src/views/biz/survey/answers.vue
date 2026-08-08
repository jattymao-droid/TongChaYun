<template>
  <div class="biz-page answers-page">
    <div class="biz-page-head">
      <el-page-header @back="goBack" :content="'答卷列表 - ' + (survey.surveyName || '')" />
      <div class="head-actions">
        <el-button size="small" icon="el-icon-data-analysis" @click="goStats">统计分析</el-button>
      </div>
    </div>
    <el-form :model="queryParams" size="small" :inline="true" class="biz-search-bar">
      <el-form-item label="渠道">
        <el-input v-model="queryParams.channelCode" placeholder="渠道码" clearable style="width: 140px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="提交IP">
        <el-input v-model="queryParams.submitIp" placeholder="IP 片段" clearable style="width: 140px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="有效性">
        <el-select v-model="queryParams.validFlag" clearable placeholder="全部" style="width: 110px">
          <el-option label="有效" value="1" />
          <el-option label="无效" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="提交时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="yyyy-MM-dd"
          range-separator="-"
          start-placeholder="开始"
          end-placeholder="结束"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="疑似刷答">
        <el-switch v-model="onlySuspect" @change="handleQuery" />
        <span class="tip">耗时 &lt; {{ suspectMs / 1000 }} 秒</span>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['biz:survey:query']">导出答卷</el-button>
        <el-button
          type="danger"
          plain
          size="mini"
          icon="el-icon-close"
          :disabled="!selectedIds.length"
          :loading="batchLoading"
          @click="batchMarkInvalid"
          v-hasPermi="['biz:survey:edit']"
        >批量标无效</el-button>
        <el-button
          type="success"
          plain
          size="mini"
          icon="el-icon-check"
          :disabled="!selectedIds.length"
          :loading="batchLoading"
          @click="batchMarkValid"
          v-hasPermi="['biz:survey:edit']"
        >批量恢复有效</el-button>
      </el-form-item>
    </el-form>

    <div class="biz-panel is-flush">
    <el-table v-loading="loading" :data="displayList" @selection-change="onSelectionChange" row-key="answerId">
      <el-table-column type="selection" width="48" align="center" />
      <el-table-column label="答卷ID" prop="answerId" width="90" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.validFlag === '0' ? 'info' : 'success'">
            {{ scope.row.validFlag === '0' ? '无效' : '有效' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="渠道" prop="channelCode" width="100" align="center">
        <template slot-scope="scope">{{ scope.row.channelCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="提交IP" prop="submitIp" width="140" align="center" />
      <el-table-column label="耗时" width="120" align="center">
        <template slot-scope="scope">
          <span>{{ formatCost(scope.row.costMs) }}</span>
          <el-tag v-if="isSuspect(scope.row)" size="mini" type="danger" effect="plain" class="ml6">疑似</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submitTime" width="180" align="center" />
      <el-table-column label="备注" prop="remark" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="showDetail(scope.row)">详情</el-button>
          <el-button
            v-if="scope.row.validFlag !== '0'"
            type="text"
            size="mini"
            @click="markInvalid(scope.row)"
            v-hasPermi="['biz:survey:edit']"
          >标为无效</el-button>
          <el-button
            v-else
            type="text"
            size="mini"
            @click="markValid(scope.row)"
            v-hasPermi="['biz:survey:edit']"
          >恢复有效</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </div>

    <el-dialog
      title="答卷详情"
      :visible.sync="detailOpen"
      width="760px"
      append-to-body
      custom-class="answer-detail-dialog"
      @opened="onDetailOpened"
    >
      <div v-loading="detailLoading">
        <el-descriptions :column="1" border size="small" v-if="detail">
          <el-descriptions-item label="答卷ID">{{ detail.answerId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="mini" :type="detail.validFlag === '0' ? 'info' : 'success'">
              {{ detail.validFlag === '0' ? '无效' : '有效' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ detail.submitTime }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ detail.submitIp }}</el-descriptions-item>
          <el-descriptions-item label="渠道">{{ detail.channelCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注">
            <el-input
              v-model="detailRemark"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              placeholder="可填写无效原因或备注"
            />
            <el-button type="text" size="mini" class="mt8" @click="saveRemark" v-hasPermi="['biz:survey:edit']">保存备注</el-button>
          </el-descriptions-item>
        </el-descriptions>
        <el-table
          ref="detailTable"
          :data="detailItems"
          size="small"
          class="mt12 detail-answer-table"
          border
          :key="'detail-' + (detail && detail.answerId) + '-' + detailItems.length"
        >
          <el-table-column label="题目" min-width="200" :show-overflow-tooltip="true">
            <template slot-scope="scope">{{ scope.row.questionTitle || '（题目已变更）' }}</template>
          </el-table-column>
          <el-table-column label="题型" width="120" align="center">
            <template slot-scope="scope">{{ typeLabel(scope.row.qType) }}</template>
          </el-table-column>
          <el-table-column label="答案" min-width="200">
            <template slot-scope="scope">
              <template v-if="scope.row.qType === 'agreement'">
                <span>{{ scope.row.displayValue || scope.row.answerValue }}</span>
                <el-button type="text" size="mini" class="ml8" @click="openAgreement(scope.row)">查看协议</el-button>
              </template>
              <a
                v-else-if="(scope.row.qType === 'file' || scope.row.qType === 'signature') && fileHref(scope.row.answerValue)"
                :href="fileHref(scope.row.answerValue)"
                target="_blank"
              >{{ scope.row.qType === 'signature' ? '查看签名' : fileName(scope.row.answerValue) }}</a>
              <span v-else>{{ scope.row.displayValue || scope.row.answerValue }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <el-dialog
      :title="agreeDialog.title || '协议详情'"
      :visible.sync="agreeDialog.open"
      width="800px"
      append-to-body
      custom-class="agree-dialog"
    >
      <agreement-document
        v-if="agreeDialog.open"
        :title="agreeDialog.title"
        :content="agreeDialog.content"
        :agree-label="agreeDialog.agreeLabel"
        :agreed="agreeDialog.agreed"
        :answer-label="agreeDialog.label"
        :submit-time="agreeDialog.submitTime"
        :signatures="agreeDialog.signatures"
        @preview="previewImage"
      />
    </el-dialog>

    <el-dialog :visible.sync="imgPreview.open" width="640px" append-to-body title="预览">
      <img v-if="imgPreview.url" :src="imgPreview.url" class="preview-full" alt="preview" />
    </el-dialog>
  </div>
</template>

<script>
import AgreementDocument from '@/components/biz/AgreementDocument'
import { getSurvey, listSurveyAnswers, getSurveyAnswer, exportSurveyAnswers, updateSurveyAnswer, batchUpdateSurveyAnswers } from '@/api/biz/survey'
import { normalizeQuestion, getBoundSignatures, typeLabel, isDisplayOnly } from '@/utils/bizSurveyQuestion'
import { blobValidate } from '@/utils/ruoyi'
import { saveAs } from 'file-saver'

export default {
  name: 'BizSurveyAnswers',
  components: { AgreementDocument },
  data() {
    return {
      loading: false,
      surveyId: null,
      survey: {},
      questions: [],
      list: [],
      total: 0,
      selectedIds: [],
      batchLoading: false,
      onlySuspect: false,
      suspectMs: 5000,
      dateRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        surveyId: null,
        channelCode: undefined,
        submitIp: undefined,
        validFlag: undefined
      },
      detailOpen: false,
      detailLoading: false,
      detail: null,
      detailItems: [],
      detailRemark: '',
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
  created() {
    this.surveyId = this.$route.params.surveyId
    this.queryParams.surveyId = this.surveyId
    getSurvey(this.surveyId).then(res => {
      this.survey = (res.data && res.data.survey) || {}
      const raw = (res.data && res.data.questions) || []
      this.questions = raw.map((q, i) => normalizeQuestion(q, i, { keyMode: 'preview' }))
    })
    this.getList().then(() => {
      const aid = this.$route.query.answerId
      if (aid) {
        this.showDetail({ answerId: Number(aid) })
      }
    })
  },
  computed: {
    displayList() {
      if (!this.onlySuspect) return this.list
      return this.list.filter(r => this.isSuspect(r))
    }
  },
  watch: {
    '$store.state.app.sidebar.opened'() {
      if (this.detailOpen) {
        this.$nextTick(() => {
          this.relayoutDetailTable()
          setTimeout(() => this.relayoutDetailTable(), 280)
        })
      }
    }
  },
  methods: {
    typeLabel,
    goBack() {
      this.$router.push('/biz/survey')
    },
    goStats() {
      this.$router.push('/biz/survey-stats/index/' + this.surveyId)
    },
    formatCost(ms) {
      const n = Number(ms)
      if (!Number.isFinite(n) || n < 0) return '-'
      if (n < 1000) return n + ' ms'
      return (n / 1000).toFixed(1) + ' s'
    },
    isSuspect(row) {
      const n = Number(row && row.costMs)
      return Number.isFinite(n) && n >= 0 && n < this.suspectMs
    },
    onSelectionChange(rows) {
      this.selectedIds = (rows || []).map(r => r.answerId).filter(Boolean)
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.onlySuspect = false
      this.queryParams.channelCode = undefined
      this.queryParams.submitIp = undefined
      this.queryParams.validFlag = undefined
      this.handleQuery()
    },
    batchMarkInvalid() {
      if (!this.selectedIds.length) return
      this.$prompt('将选中的 ' + this.selectedIds.length + ' 条标为无效，可填写原因（可选）', '批量标无效', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '如：测试数据 / 刷卷'
      }).then(({ value }) => {
        this.batchLoading = true
        return batchUpdateSurveyAnswers({
          answerIds: this.selectedIds,
          validFlag: '0',
          remark: value || '批量标无效'
        })
      }).then(() => {
        this.$modal.msgSuccess('已批量标为无效')
        this.selectedIds = []
        this.getList()
      }).catch(() => {}).finally(() => { this.batchLoading = false })
    },
    batchMarkValid() {
      if (!this.selectedIds.length) return
      this.$modal.confirm('确认将选中的 ' + this.selectedIds.length + ' 条恢复为有效？').then(() => {
        this.batchLoading = true
        return batchUpdateSurveyAnswers({
          answerIds: this.selectedIds,
          validFlag: '1'
        })
      }).then(() => {
        this.$modal.msgSuccess('已批量恢复有效')
        this.selectedIds = []
        this.getList()
      }).catch(() => {}).finally(() => { this.batchLoading = false })
    },
    buildFilter() {
      const q = {
        channelCode: this.queryParams.channelCode,
        submitIp: this.queryParams.submitIp,
        validFlag: this.queryParams.validFlag
      }
      this.addDateRange(q, this.dateRange)
      return q
    },
    handleExport() {
      const tip = (this.queryParams.channelCode || this.queryParams.submitIp || this.queryParams.validFlag || (this.dateRange && this.dateRange.length))
        ? '确认导出当前筛选条件下的答卷明细（含有效性、备注、各题答案）？'
        : '确认导出全部答卷明细（含有效性、备注、各题答案）？'
      this.$modal.confirm(tip).then(() => exportSurveyAnswers(this.surveyId, this.buildFilter())).then(async data => {
        if (blobValidate(data)) {
          saveAs(new Blob([data]), (this.survey.surveyName || 'survey') + '-答卷明细.xlsx')
        } else {
          this.$modal.msgError('导出失败')
        }
      }).catch(() => {})
    },
    getList() {
      this.loading = true
      const params = { ...this.queryParams }
      this.addDateRange(params, this.dateRange)
      return listSurveyAnswers(params).then(res => {
        this.list = (res.rows || []).map(r => ({
          ...r,
          validFlag: r.validFlag == null || r.validFlag === '' ? '1' : r.validFlag
        }))
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    },
    fileHref(val) {
      try {
        const obj = typeof val === 'string' && val.trim().startsWith('{') ? JSON.parse(val) : { fileName: val }
        const path = obj.fileName || obj.url || ''
        if (!path) return ''
        if (path.startsWith('http') || path.startsWith('data:')) return path
        return process.env.VUE_APP_BASE_API + path
      } catch (e) { return '' }
    },
    fileName(val) {
      try {
        const obj = typeof val === 'string' && val.trim().startsWith('{') ? JSON.parse(val) : { originalFilename: val }
        return obj.originalFilename || obj.newFileName || obj.fileName || '附件'
      } catch (e) { return '附件' }
    },
    showDetail(row) {
      this.detailOpen = true
      this.detailLoading = true
      this.detailItems = []
      getSurveyAnswer(row.answerId).then(res => {
        this.detail = res.data || {}
        if (this.detail.validFlag == null || this.detail.validFlag === '') this.detail.validFlag = '1'
        this.detailRemark = this.detail.remark || ''
        this.detailItems = this.normalizeDetailItems(this.detail.items || [])
        this.$nextTick(() => this.relayoutDetailTable())
      }).finally(() => {
        this.detailLoading = false
        this.$nextTick(() => this.relayoutDetailTable())
      })
    },
    normalizeDetailItems(items) {
      const answerable = (this.questions || []).filter(q => !isDisplayOnly(q.qType))
      return (items || []).map((it, i) => {
        const qType = it.qType || it.QType || ''
        let questionTitle = it.questionTitle || ''
        let resolvedType = qType
        let q = (this.questions || []).find(x => String(x.questionId) === String(it.questionId))
        if (!q && answerable.length === items.length) q = answerable[i]
        if (q) {
          if (!questionTitle) questionTitle = q.title || ''
          if (!resolvedType) resolvedType = q.qType || ''
        }
        return {
          ...it,
          qType: resolvedType,
          questionTitle: questionTitle || '（题目已变更）',
          displayValue: it.displayValue || it.answerValue
        }
      })
    },
    onDetailOpened() {
      this.relayoutDetailTable()
      // sidebar collapse animation can finish after dialog open
      setTimeout(() => this.relayoutDetailTable(), 220)
    },
    relayoutDetailTable() {
      const table = this.$refs.detailTable
      if (table && typeof table.doLayout === 'function') table.doLayout()
    },
    openAgreement(item) {
      if (!item || !this.detail) return
      const agreementQ = this.questions.find(q => String(q.questionId) === String(item.questionId))
      const title = (agreementQ && agreementQ.title) || item.questionTitle || '协议详情'
      const content = (agreementQ && agreementQ._content) || ''
      const agreeLabel = (agreementQ && agreementQ._agreeLabel) || '我已阅读并同意'
      const agreed = item.answerValue === '1' || item.displayValue === '已同意'
      const bound = agreementQ ? getBoundSignatures(agreementQ, this.questions) : []
      const itemMap = {}
      ;(this.detailItems || []).forEach(it => {
        itemMap[String(it.questionId)] = it
      })
      const signatures = bound.map(sq => {
        const ans = itemMap[String(sq.questionId)]
        return {
          questionId: sq.questionId,
          title: sq.title || '手写签名',
          url: ans ? this.fileHref(ans.answerValue) : ''
        }
      })
      this.agreeDialog = {
        open: true,
        title,
        label: '#' + this.detail.answerId,
        submitTime: this.detail.submitTime || '',
        content: content,
        agreeLabel,
        agreed: !!agreed,
        signatures
      }
    },
    previewImage(url) {
      if (!url) return
      this.imgPreview = { open: true, url }
    },
    markInvalid(row) {
      this.$prompt('可填写无效原因（可选）', '标为无效', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '如：测试数据 / 刷卷',
        inputValue: row.remark || ''
      }).then(({ value }) => {
        return updateSurveyAnswer(row.answerId, { validFlag: '0', remark: value || '' })
      }).then(() => {
        this.$modal.msgSuccess('已标为无效（统计将排除）')
        this.getList()
      }).catch(() => {})
    },
    markValid(row) {
      updateSurveyAnswer(row.answerId, { validFlag: '1' }).then(() => {
        this.$modal.msgSuccess('已恢复为有效')
        this.getList()
      })
    },
    saveRemark() {
      if (!this.detail) return
      updateSurveyAnswer(this.detail.answerId, { remark: this.detailRemark || '' }).then(() => {
        this.detail.remark = this.detailRemark
        this.$modal.msgSuccess('备注已保存')
        this.getList()
      })
    }
  }
}
</script>

<style scoped>
.answers-page >>> .el-page-header__content {
  font-size: 18px;
  font-weight: 700;
  color: var(--biz-ink);
}
.answers-page >>> .el-table {
  border-radius: 0;
}
.mb16 { margin-bottom: 16px; }
.mt8 { margin-top: 8px; }
.mt12 { margin-top: 12px; }
.ml6 { margin-left: 6px; }
.ml8 { margin-left: 8px; }
.tip { margin-left: 6px; color: var(--biz-muted-soft); font-size: 12px; }
.preview-full { display: block; max-width: 100%; margin: 0 auto; }
.detail-answer-table {
  width: 100%;
}
</style>

<style>
.answer-detail-dialog {
  max-width: calc(100vw - 48px);
}
.answer-detail-dialog .el-dialog__body {
  padding-top: 12px;
}
.hideSidebar .answer-detail-dialog,
.openSidebar .answer-detail-dialog {
  margin-top: 8vh !important;
}
</style>
